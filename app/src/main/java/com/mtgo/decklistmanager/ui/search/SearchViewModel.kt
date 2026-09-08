package com.mtgo.decklistmanager.ui.search

import androidx.lifecycle.SavedStateHandle
import com.google.gson.Gson
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.data.local.dao.SearchHistoryDao
import com.mtgo.decklistmanager.data.local.entity.SearchHistoryEntity
import com.mtgo.decklistmanager.domain.model.SearchHistory
import com.mtgo.decklistmanager.ui.search.model.*
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

/**
 * 搜索 ViewModel - 使用自有服务端 API
 *
 * 自有服务端完全兼容 MTGCH 搜索接口
 * API 文档: 见项目根目录 API_DOCUMENTATION.md
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mtgchApi: MtgchApi,
    private val searchHistoryDao: SearchHistoryDao,
    private val savedState: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        data class Results(val items: List<SearchResultItem>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState = _uiState.asStateFlow()
    private var searchJob: Job? = null
    var activeFilters: SearchFilters = savedState.get<String>("filters")?.let {
        runCatching { Gson().fromJson(it, SearchFilters::class.java) }.getOrNull()
    } ?: SearchFilters.empty()
        set(value) {
            field = value
            savedState["filters"] = Gson().toJson(value)
        }

    private val _searchQuery = MutableStateFlow(savedState.get<String>("query") ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistory>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistory>> = _searchHistory.asStateFlow()

    init {
        loadSearchHistory()
        if (_searchQuery.value.isNotBlank() || hasActiveFilters(activeFilters)) {
            search(_searchQuery.value, filters = activeFilters)
        }
    }

    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        searchJob?.cancel()
        _searchQuery.value = query
        savedState["query"] = query
        _uiState.value = if (query.isBlank()) UiState.Initial else UiState.Loading
    }

    /**
     * 执行在线搜索（使用自有服务端 API）
     *
     * 新 API 参数:
     * - @param query 搜索关键词
     * - @param offset 偏移量（默认0）
     * - @param limit 每页数量（默认20）
     * - @param filters 搜索过滤器（可选）
     */
    fun search(
        query: String,
        offset: Int = 0,
        limit: Int = 20,
        filters: SearchFilters? = null
    ) {
        searchJob?.cancel()
        _searchQuery.value = query
        savedState["query"] = query
        activeFilters = filters ?: SearchFilters.empty()
        // 允许空文本搜索（当有筛选条件时）
        val hasFilters = hasActiveFilters(filters)
        if (query.isBlank() && !hasFilters) {
            _uiState.value = UiState.Initial
            return
        }

        _uiState.value = UiState.Loading
        searchJob = viewModelScope.launch {
            try {
                // 构建搜索查询字符串
                val searchQuery = buildSearchQuery(query, filters)

                AppLogger.d("SearchViewModel", "Searching: $searchQuery")

                // 调用自有服务端 API 搜索（新 API 使用 offset/limit）
                val response = mtgchApi.searchCard(
                    query = searchQuery,
                    offset = offset,
                    limit = limit
                )
                ensureActive()

                if (response.isSuccessful) {
                    val body = response.body()

                    // 检查响应的 success 字段
                    if (body?.success == true) {
                        val cards = body.cards ?: emptyList()

                        AppLogger.d("SearchViewModel", "Found ${cards.size} results (total: ${body.total})")

                        // 转换为 SearchResult
                        val results = cards.map { it.toSearchResultItem() }
                        _uiState.value = UiState.Results(results)

                        // 保存搜索历史（仅在非空搜索时）
                        if (query.isNotBlank() && results.isNotEmpty()) {
                            saveSearchHistory(query, results.size)
                        }
                    } else {
                        val errorMsg = "搜索失败: ${body?.success}"
                        AppLogger.e("SearchViewModel", errorMsg)
                        _uiState.value = UiState.Error("搜索服务暂时不可用，请重试")
                    }
                } else {
                    val errorMsg = "搜索失败: ${response.code()} ${response.message()}"
                    AppLogger.e("SearchViewModel", errorMsg)
                    _uiState.value = UiState.Error("搜索失败，请稍后重试")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ensureActive()
                AppLogger.e("SearchViewModel", "Search error", e)
                _uiState.value = UiState.Error("无法完成搜索，请检查网络连接后重试")
            }
        }
    }

    /**
     * 检查是否有有效的筛选条件
     */
    private fun hasActiveFilters(filters: SearchFilters?): Boolean {
        return filters?.hasActiveFilters ?: false
    }

    /**
     * 构建搜索查询字符串
     * 完全复制 MTGCH 的搜索语法
     * 参考: https://mtgch.com/search
     */
    private fun buildSearchQuery(
        query: String,
        filters: SearchFilters?
    ): String {
        val parts = mutableListOf<String>()

        filters?.let { f ->
            // 1. 名称 (name)
            f.name?.let {
                if (it.isNotBlank()) parts.add("name:\"$it\"")
            }

            // 2. 规则概述 (o, oracle, text)
            f.oracleText?.let {
                if (it.isNotBlank()) parts.add("o:\"$it\"")
            }

            // 3. 类别 (t, type)
            f.type?.let {
                if (it.isNotBlank()) parts.add("t:\"$it\"")
            }

            // 4. 颜色筛选 (c) - 支持颜色模式
            if (f.colors.isNotEmpty()) {
                val colorStr = f.getColorString()
                val operator = when (f.colorMode) {
                    ColorMatchMode.EXACT -> "="
                    ColorMatchMode.AT_MOST -> "<="
                    ColorMatchMode.AT_LEAST -> ">="
                }
                parts.add("c$operator$colorStr")
            }

            // 5. 颜色标识 (ci)
            if (f.searchColorIdentity && f.colorIdentity?.isNotEmpty() == true) {
                parts.add("ci:${f.getColorIdentityString()}")
            }

            // 6. 法术力值 (mv, cmc, mana_value)
            f.manaValue?.let { mv ->
                if (mv.isActive) {
                    parts.add(mv.toQueryPart("mv"))
                }
            }

            // 7. 力量 (po, power)
            f.power?.let { p ->
                if (p.isActive) {
                    parts.add(p.toQueryPart("po"))
                }
            }

            // 8. 防御力 (to, toughness)
            f.toughness?.let { t ->
                if (t.isActive) {
                    parts.add(t.toQueryPart("to"))
                }
            }

            // 9. 赛制与合法性 (f, l)
            if (f.format != null) {
                val formatPart = "f:${f.format.value}"
                val legalityPart = f.legality?.let { " l:${it.value}" } ?: ""
                parts.add(formatPart + legalityPart)
            }

            // 10. 系列 (s, set)
            f.setCode?.let {
                if (it.isNotBlank()) parts.add("s:$it")
            }

            // 11. 稀有度 (r, rarity) - 支持多选
            f.rarities.forEach { rarity ->
                if (rarity.isNotBlank()) parts.add("r:$rarity")
            }

            // 12. 背景叙述 (ft, flavor_text)
            f.flavorText?.let {
                if (it.isNotBlank()) parts.add("ft:\"$it\"")
            }

            // 13. 画师 (a, artist)
            f.artist?.let {
                if (it.isNotBlank()) parts.add("a:\"$it\"")
            }

            // 14. 游戏平台
            f.game?.let {
                parts.add("game:${it.value}")
            }

            // 15. 额外卡牌
            if (f.includeExtras) {
                parts.add("is:extra")
            }
        }

        // 如果没有任何筛选条件，直接返回query
        if (parts.isEmpty() && query.isNotBlank()) {
            return query
        }

        // 合并所有查询条件
        val allParts = if (query.isNotBlank()) {
            listOf(query) + parts
        } else {
            parts
        }

        return allParts.joinToString(" ")
    }

    /**
     * 保存搜索历史
     */
    private suspend fun saveSearchHistory(query: String, resultCount: Int) {
        val history = SearchHistoryEntity(
            query = query,
            searchTime = System.currentTimeMillis(),
            resultCount = resultCount
        )
        searchHistoryDao.insert(history)

        // 限制历史记录数量（保留最近 50 条）
        searchHistoryDao.deleteOldSearchHistory(50)

        // Room 的现有 Flow 订阅会接收更新，不重复创建收集协程。
    }

    /**
     * 加载搜索历史
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            searchHistoryDao.getRecentSearchHistory(10)
                .collect { entities ->
                    _searchHistory.value = entities.map { it.toSearchHistory() }
                }
        }
    }

    /**
     * 删除搜索历史项
     */
    fun deleteSearchHistory(historyId: Long) {
        viewModelScope.launch {
            val entity = searchHistoryDao.getById(historyId)
            entity?.let {
                searchHistoryDao.delete(it)
            }
        }
    }

    /**
     * 清空所有搜索历史
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            searchHistoryDao.clearAll()
        }
    }

    /**
     * 点击历史记录项进行搜索
     */
    fun searchFromHistory(query: String) {
        updateSearchQuery(query)
        search(query)
    }

    /**
     * MtgchCardDto 转 SearchResultItem
     */
    private fun MtgchCardDto.toSearchResultItem(): SearchResultItem {
        // 按 layout 区分真双面牌与多部分牌（历险/连体等）：
        // 搜索接口可能不含 layout，此时用 back_image_url（isDoubleFaced 由 DTO 推导）判断
        val isDualFaced = (isDoubleFaced == true) ||
            com.mtgo.decklistmanager.util.CardLayouts.isTrueDualFace(layout)

        // 获取中文名称（优先使用新字段 nameZh）
        val getZhsName = nameZh ?: atomicTranslatedName

        // 获取中文类型行（优先使用新字段 typeLineZh）
        val getTypeLineZh = typeLineZh ?: atomicTranslatedType

        // 获取中文规则文本（优先使用新字段 oracleTextZh）
        val getOracleTextZh = oracleTextZh ?: atomicTranslatedText

        return SearchResultItem(
            cardInfoId = oracleId?.hashCode()?.toLong() ?: id?.hashCode()?.toLong() ?: 0L,
            name = getZhsName ?: name ?: "",
            displayName = getZhsName,
            manaCost = manaCost,
            typeLine = getTypeLineZh ?: typeLine,
            colors = colors ?: emptyList(),
            cmc = cmc?.toDouble(),
            rarity = rarity,
            imageUrl = zhsImageUris?.normal ?: imageUris?.normal,
            isDualFaced = isDualFaced,
            // 额外信息（用于详情展示）
            oracleText = getOracleTextZh ?: oracleText,
            power = power,
            toughness = toughness,
            loyalty = loyalty,
            setCode = setCode,
            setName = setNameZh ?: setTranslatedName ?: setName,
            collectorNumber = collectorNumber,
            artist = artist,
            colorIdentity = colorIdentity,
            type = getTypeLineZh ?: typeLine,
            // 完整的卡牌数据
            mtgchCard = this
        )
    }

    /**
     * SearchHistoryEntity 转 SearchHistory
     */
    private fun SearchHistoryEntity.toSearchHistory(): SearchHistory {
        return SearchHistory(
            id = id,
            query = query,
            searchTime = searchTime,
            resultCount = resultCount
        )
    }

    /**
     * 获取完整的卡牌详情（用于双面牌背面图片等需要额外数据的情况）
     * 使用自有服务端 API
     *
     * 新 API 直接返回卡牌对象（不需要解析 {success, card} 包装）
     *
     * @param oracleId 卡牌 Oracle ID
     * @return 完整的卡牌数据
     */
    suspend fun getFullCardDetails(oracleId: String): MtgchCardDto? {
        return try {
            AppLogger.d("SearchViewModel", "获取完整卡牌详情: $oracleId")
            val response = mtgchApi.getCardById(oracleId)
            if (response.isSuccessful && response.body() != null) {
                // 新 API 直接返回卡牌对象
                response.body()
            } else {
                AppLogger.e("SearchViewModel", "获取完整卡牌详情失败: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            AppLogger.e("SearchViewModel", "获取完整卡牌详情异常", e)
            null
        }
    }

    /**
     * 获取卡牌的所有印刷版本
     *
     * @param oracleId 卡牌 Oracle ID
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 印刷版本列表和总数
     */
    suspend fun getCardPrintings(
        oracleId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Pair<List<MtgchCardDto>, Int?>? {
        return try {
            AppLogger.d("SearchViewModel", "获取印刷版本: $oracleId")
            val response = mtgchApi.getCardPrintings(oracleId, limit, offset)
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                Pair(body.cards ?: emptyList(), body.total)
            } else {
                AppLogger.e("SearchViewModel", "获取印刷版本失败: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            AppLogger.e("SearchViewModel", "获取印刷版本异常", e)
            null
        }
    }
}

/**
 * 搜索结果（增强版）
 */
data class SearchResultItem(
    val cardInfoId: Long,
    val name: String,
    val displayName: String?,
    val manaCost: String?,
    val typeLine: String?,
    val colors: List<String>,
    val cmc: Double?,
    val rarity: String?,
    val imageUrl: String?,
    val isDualFaced: Boolean = false,

    // 额外字段（用于详情展示）
    val oracleText: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val loyalty: String? = null,
    val setCode: String? = null,
    val setName: String? = null,
    val collectorNumber: String? = null,
    val artist: String? = null,
    val colorIdentity: List<String>? = null,
    val type: String? = null,

    // 完整的 MTGCH 卡牌数据
    val mtgchCard: MtgchCardDto? = null
)
