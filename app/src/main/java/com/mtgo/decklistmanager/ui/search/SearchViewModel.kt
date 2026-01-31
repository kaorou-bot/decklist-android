package com.mtgo.decklistmanager.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.data.local.dao.SearchHistoryDao
import com.mtgo.decklistmanager.data.local.entity.SearchHistoryEntity
import com.mtgo.decklistmanager.domain.model.SearchHistory
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索 ViewModel - 在线搜索版本
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mtgchApi: MtgchApi,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistory>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistory>> = _searchHistory.asStateFlow()

    private val _showHistory = MutableStateFlow(true)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSearchHistory()
    }

    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _showHistory.value = query.isEmpty()
    }

    /**
     * 执行在线搜索
     * @param query 搜索关键词
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认50）
     * @param filters 搜索过滤器（可选）
     */
    fun search(
        query: String,
        page: Int = 1,
        pageSize: Int = 50,
        filters: SearchFilters? = null
    ) {
        // 允许空文本搜索（当有筛选条件时）
        val hasFilters = hasActiveFilters(filters)
        if (query.isBlank() && !hasFilters) {
            _searchResults.value = emptyList()
            _showHistory.value = true
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _showHistory.value = false
            _errorMessage.value = null

            try {
                // 构建搜索查询字符串
                val searchQuery = buildSearchQuery(query, filters)

                AppLogger.d("SearchViewModel", "Searching: $searchQuery")

                // 调用 MTGCH API 搜索
                val response = mtgchApi.searchCard(
                    query = searchQuery,
                    page = page,
                    pageSize = pageSize,
                    priorityChinese = true,
                    view = 0  // 0 = 详细信息
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val cards = body?.data ?: emptyList()

                    AppLogger.d("SearchViewModel", "Found ${cards.size} results")

                    // 转换为 SearchResult
                    val results = cards.map { it.toSearchResultItem() }
                    _searchResults.value = results

                    // 保存搜索历史（仅在非空搜索时）
                    if (query.isNotBlank() && results.isNotEmpty()) {
                        saveSearchHistory(query, results.size)
                    }
                } else {
                    val errorMsg = "搜索失败: ${response.code()} ${response.message()}"
                    AppLogger.e("SearchViewModel", errorMsg)
                    _errorMessage.value = errorMsg
                    _searchResults.value = emptyList()
                }
            } catch (e: Exception) {
                AppLogger.e("SearchViewModel", "Search error", e)
                _errorMessage.value = "搜索出错: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * 检查是否有有效的筛选条件
     */
    private fun hasActiveFilters(filters: SearchFilters?): Boolean {
        return filters?.let {
            it.colors.isNotEmpty() ||
            it.colorIdentity != null ||
            it.cmc != null ||
            it.types != null ||
            it.rarity != null ||
            it.set != null ||
            it.partner != null
        } ?: false
    }

    /**
     * 构建搜索查询字符串
     * 参考: http://mtgch.com/search
     */
    private fun buildSearchQuery(
        query: String,
        filters: SearchFilters?
    ): String {
        val parts = mutableListOf(query)

        filters?.let {
            // 颜色筛选: c:w, c:u, c:b, c:r, c:g
            it.colors.forEach { color ->
                parts.add("c:$color")
            }

            // 颜色标识: ci:wubrg
            it.colorIdentity?.forEach { color ->
                parts.add("ci:$color")
            }

            // 法术力值: cmc>2, cmc<5, cmc=3
            it.cmc?.let { cmc ->
                when (cmc.operator) {
                    "=" -> parts.add("cmc=$cmc.value")
                    ">" -> parts.add("cmc>$cmc.value")
                    "<" -> parts.add("cmc<$cmc.value")
                    ">=" -> parts.add("cmc>=$cmc.value")
                    "<=" -> parts.add("cmc<=$cmc.value")
                    else -> parts.add("cmc=${cmc.value}")
                }
            }

            // 类型: t:creature, t:instant
            it.types?.forEach { type ->
                parts.add("t:$type")
            }

            // 稀有度: r:common, r:uncommon, r:rare, r:mythic
            it.rarity?.let { rarity ->
                parts.add("r:$rarity")
            }

            // 系列: s:cmr, s:mom
            it.set?.let { set ->
                parts.add("s:$set")
            }

            // 伙伴: p:white, p:blue
            it.partner?.let { partner ->
                parts.add("p:$partner")
            }
        }

        return parts.joinToString(" ")
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

        // 重新加载历史
        loadSearchHistory()
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
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * MtgchCardDto 转 SearchResultItem
     */
    private fun MtgchCardDto.toSearchResultItem(): SearchResultItem {
        return SearchResultItem(
            cardInfoId = oracleId?.hashCode()?.toLong() ?: id.hashCode().toLong(),
            name = zhsName ?: atomicTranslatedName ?: name ?: "",
            displayName = zhsName ?: atomicTranslatedName,
            manaCost = manaCost,
            typeLine = zhsTypeLine ?: atomicTranslatedType ?: typeLine,
            colors = colors ?: emptyList(),
            cmc = cmc?.toDouble(),
            rarity = rarity,
            imageUrl = zhsImageUris?.normal ?: imageUris?.normal,
            isDualFaced = otherFaces != null || cardFaces != null,
            // 额外信息（用于详情展示）
            oracleText = zhsText ?: atomicTranslatedText ?: oracleText,
            power = power,
            toughness = toughness,
            loyalty = loyalty,
            setCode = setCode,
            setName = setTranslatedName ?: setName,
            collectorNumber = collectorNumber,
            artist = artist,
            colorIdentity = colorIdentity,
            type = zhsTypeLine ?: typeLine,
            // 完整的 MTGCH 卡牌数据
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
}

/**
 * 搜索过滤器
 * 参考: http://mtgch.com/search 的高级筛选功能
 */
data class SearchFilters(
    // 颜色筛选: w, u, b, r, g
    val colors: List<String> = emptyList(),

    // 颜色标识: wubrg
    val colorIdentity: List<String>? = null,

    // 法术力值筛选
    val cmc: CmcFilter? = null,

    // 类型筛选: creature, instant, sorcery, etc.
    val types: List<String>? = null,

    // 稀有度: common, uncommon, rare, mythic
    val rarity: String? = null,

    // 系列代码: cmr, mom, one, etc.
    val set: String? = null,

    // 伙伴颜色
    val partner: String? = null
)

/**
 * 法术力值筛选
 */
data class CmcFilter(
    val operator: String,  // =, >, <, >=, <=
    val value: Int
)

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
