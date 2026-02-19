package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.domain.model.CardLocation
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import com.mtgo.decklistmanager.data.local.entity.DecklistEntity
import com.mtgo.decklistmanager.data.local.entity.CardEntity
import com.mtgo.decklistmanager.data.local.dao.CardDao
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.remote.api.ServerApi
import com.mtgo.decklistmanager.data.remote.api.dto.DecklistDetailResponse
import com.mtgo.decklistmanager.data.remote.api.dto.CardInfoDto
import com.mtgo.decklistmanager.exporter.DecklistExporter
import com.mtgo.decklistmanager.exporter.ExportResult
import com.mtgo.decklistmanager.exporter.format.MtgoFormatExporter
import com.mtgo.decklistmanager.exporter.format.ArenaFormatExporter
import com.mtgo.decklistmanager.exporter.format.TextFormatExporter
import com.mtgo.decklistmanager.util.LanguagePreferenceManager
import com.mtgo.decklistmanager.util.AppLogger
import com.mtgo.decklistmanager.data.remote.api.mtgch.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DeckDetailViewModel - 牌组详情 ViewModel
 * v4.2.0: 支持从服务端 API 获取套牌详情（含卡牌）
 */
@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    private val repository: DecklistRepository,
    private val cardDao: CardDao,
    private val decklistDao: DecklistDao,
    private val serverApi: ServerApi,
    private val languagePreferenceManager: LanguagePreferenceManager,
    private val mtgoExporter: MtgoFormatExporter,
    private val arenaExporter: ArenaFormatExporter,
    private val textExporter: TextFormatExporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val decklistId: Long = checkNotNull(savedStateHandle["decklistId"])

    // Decklist detail
    private val _decklist = MutableLiveData<Decklist?>()
    val decklist: LiveData<Decklist?> = _decklist

    // Cards grouped by location
    private val _mainDeck = MutableLiveData<List<Card>>()
    val mainDeck: LiveData<List<Card>> = _mainDeck

    private val _sideboard = MutableLiveData<List<Card>>()
    val sideboard: LiveData<List<Card>> = _sideboard

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Card info for popup
    private val _cardInfo = MutableLiveData<CardInfo?>()
    val cardInfo: LiveData<CardInfo?> = _cardInfo

    // 卡牌信息由数据库缓存管理，无需内存缓存

    // Card info loading state
    private val _isCardInfoLoading = MutableLiveData<Boolean>(false)
    val isCardInfoLoading: LiveData<Boolean> = _isCardInfoLoading

    // Card info error message
    private val _cardInfoError = MutableLiveData<String?>()
    val cardInfoError: LiveData<String?> = _cardInfoError

    // Export result
    private val _exportResult = MutableLiveData<ExportResult?>()
    val exportResult: LiveData<ExportResult?> = _exportResult

    // Export error message
    private val _exportError = MutableLiveData<String?>()
    val exportError: LiveData<String?> = _exportError

    // Helper function to convert entities
    private fun DecklistEntity.toDecklist() = Decklist(
        id = id,
        eventName = eventName,
        eventType = eventType,
        deckName = deckName,
        format = format,
        date = date,
        url = url,
        playerName = playerName,
        playerId = playerId,
        record = record,
        createdAt = createdAt
    )

    private fun CardEntity.toCard() = Card(
        id = id,
        decklistId = decklistId,
        cardName = cardName,
        quantity = quantity,
        location = if (location == "main") CardLocation.MAIN else CardLocation.SIDEBOARD,
        cardOrder = cardOrder,
        manaCost = manaCost,
        rarity = rarity,
        color = color,
        cardType = cardType,
        cardSet = cardSet,
        // 若 displayName 为 null，则回退使用英文名，确保中文名始终有值
        cardNameZh = displayName ?: cardName
    ).also {
        // 调试日志 - 记录所有卡牌
        AppLogger.d("DeckDetailViewModel", "CardEntity.toCard(): ${it.cardNameZh}, manaCost=${it.manaCost}")
    }

    /**
     * 加载牌组详情
     * v4.2.0: 优先从服务端 API 获取，失败则从本地数据库读取
     */
    fun loadDecklistDetail(fromServer: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (fromServer) {
                    loadDecklistDetailFromServer()
                } else {
                    loadDecklistDetailFromLocal()
                }
            } catch (e: Exception) {
                AppLogger.e("DeckDetailViewModel", "Error loading decklist: ${e.message}", e)
                // 服务器失败时，尝试从本地加载
                loadDecklistDetailFromLocal()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 从服务端 API 加载套牌详情
     * v5.1.0: 服务器直接返回完整的卡牌信息，无需再调用搜索 API
     */
    private suspend fun loadDecklistDetailFromServer() {
        AppLogger.d("DeckDetailViewModel", "Loading decklist from server: $decklistId")

        val response = serverApi.getDecklistDetail(decklistId)
        if (!response.isSuccessful || response.body()?.success != true) {
            throw Exception("Failed to load decklist: ${response.code()}")
        }

        val detail = response.body()!!.data!!
        AppLogger.d("DeckDetailViewModel", "Server returned ${detail.mainDeck.size} main cards, ${detail.sideboard.size} sideboard cards")

        // 保存套牌信息到本地
        val decklistEntity = DecklistEntity(
            id = detail.id,
            eventId = detail.eventId,
            eventName = detail.eventName,
            deckName = detail.deckName,
            format = detail.format,
            date = detail.date,
            url = "", // 服务端不提供 URL
            playerName = detail.playerName,
            playerId = null,
            record = detail.record,
            eventType = null,
            createdAt = System.currentTimeMillis()
        )
        decklistDao.insert(decklistEntity)

        // 直接使用服务器返回的完整卡牌信息，无需再调用搜索 API
        val cardEntities = detail.mainDeck.mapIndexed { index, card ->
            CardEntity(
                decklistId = decklistId,
                cardName = card.name,
                quantity = card.quantity,
                location = "main",
                cardOrder = index,
                manaCost = card.manaCost,
                displayName = card.nameZh,
                rarity = card.rarity?.replaceFirstChar { it.uppercase() },
                color = card.colors?.joinToString(","),
                cardType = card.typeLineZh ?: card.typeLine,
                cardSet = card.setNameZh ?: card.setName
            ).also {
                AppLogger.d("DeckDetailViewModel", "Main card: ${it.displayName} (${it.manaCost})")
            }
        } + detail.sideboard.mapIndexed { index, card ->
            CardEntity(
                decklistId = decklistId,
                cardName = card.name,
                quantity = card.quantity,
                location = "sideboard",
                cardOrder = index,
                manaCost = card.manaCost,
                displayName = card.nameZh,
                rarity = card.rarity?.replaceFirstChar { it.uppercase() },
                color = card.colors?.joinToString(","),
                cardType = card.typeLineZh ?: card.typeLine,
                cardSet = card.setNameZh ?: card.setName
            ).also {
                AppLogger.d("DeckDetailViewModel", "Sideboard card: ${it.displayName} (${it.manaCost})")
            }
        }

        // 删除旧数据并插入新数据
        cardDao.deleteByDecklistId(decklistId)
        cardDao.insertAll(cardEntities)

        AppLogger.d("DeckDetailViewModel", "Inserted ${cardEntities.size} cards with full details from server")

        // 直接从数据库加载
        val allCards = cardDao.getCardsByDecklistId(decklistId)
        val mainCards = allCards.filter { it.location == "main" }.map { it.toCard() }
        val sideboardCards = allCards.filter { it.location == "sideboard" }.map { it.toCard() }

        // 加载到 LiveData
        _decklist.value = decklistEntity.toDecklist()
        _mainDeck.value = mainCards
        _sideboard.value = sideboardCards

        AppLogger.d("DeckDetailViewModel", "Loaded ${mainCards.size} main cards, ${sideboardCards.size} sideboard cards")
    }

    /**
     * 从本地数据库加载套牌详情（后备方案）
     * v5.0: 简化逻辑，直接从数据库读取，不再调用 MTGCH API
     */
    private suspend fun loadDecklistDetailFromLocal() {
        AppLogger.d("DeckDetailViewModel", "Loading decklist from local database: $decklistId")

        // 加载牌组信息
        val decklistEntity = decklistDao.getDecklistById(decklistId)
        if (decklistEntity != null) {
            _decklist.value = decklistEntity.toDecklist()

            // 加载所有卡牌
            val allCards = cardDao.getCardsByDecklistId(decklistId)

            // 分离主牌和备牌
            val mainCards = allCards
                .filter { it.location == "main" }
                .map { it.toCard() }
            _mainDeck.value = mainCards

            val sideboardCards = allCards
                .filter { it.location == "sideboard" }
                .map { it.toCard() }
            _sideboard.value = sideboardCards

            AppLogger.d("DeckDetailViewModel", "Loaded from local: ${mainCards.size} main cards, ${sideboardCards.size} sideboard cards")
        } else {
            AppLogger.w("DeckDetailViewModel", "Decklist not found in local database: $decklistId")
        }
    }

    /**
     * 查询单卡信息
     * v4.1.0: 直接使用 repository.getCardInfo()，依赖数据库缓存
     * 数据库缓存速度很快（< 50ms）且持久化，无需内存缓存
     */
    fun loadCardInfo(cardName: String) {
        viewModelScope.launch {
            try {
                _isCardInfoLoading.value = true
                _cardInfoError.value = null

                AppLogger.d("DeckDetailViewModel", "loadCardInfo called for: $cardName")

                // 直接使用 repository.getCardInfo()，它会自动处理数据库缓存
                val cardInfo = repository.getCardInfo(cardName)

                if (cardInfo != null) {
                    _cardInfo.value = cardInfo
                } else {
                    // v4.0.0: 提供更友好的错误提示
                    _cardInfoError.value = "未找到卡牌: $cardName\n\n提示：\n" +
                        "• 请检查卡牌名称拼写\n" +
                        "• 某些特殊卡牌可能需要完整名称\n" +
                        "• 系统已自动重试3次，请稍后再试"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _cardInfoError.value = "加载失败: ${e.message}\n\n请检查网络连接后重试"
            } finally {
                _isCardInfoLoading.value = false
            }
        }
    }

    /**
     * 清除卡牌信息错误
     */
    fun clearCardInfoError() {
        _cardInfoError.value = null
    }

    /**
     * 清除卡牌信息
     */
    fun clearCardInfo() {
        _cardInfo.value = null
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(decklistId: Long): Boolean {
        return repository.toggleFavorite(decklistId)
    }

    /**
     * 检查是否已收藏
     */
    suspend fun isFavorite(decklistId: Long): Boolean {
        return repository.isFavorite(decklistId)
    }

    /**
     * 导出套牌为指定格式
     */
    fun exportDecklist(format: String, includeSideboard: Boolean = true) {
        viewModelScope.launch {
            try {
                val currentDecklist = _decklist.value
                if (currentDecklist == null) {
                    _exportError.value = "套牌数据未加载"
                    return@launch
                }

                // 获取所有卡牌
                val allCards = (_mainDeck.value ?: emptyList()) + (_sideboard.value ?: emptyList())

                // 选择对应的导出器
                val exporter: DecklistExporter = when (format) {
                    "mtgo" -> mtgoExporter
                    "arena" -> arenaExporter
                    "text" -> textExporter
                    else -> textExporter
                }

                // 执行导出
                val content = exporter.export(currentDecklist, allCards, includeSideboard)

                // 生成文件名
                val sanitizedName = (currentDecklist.eventName ?: "decklist")
                    .replace(Regex("[^a-zA-Z0-9\\s\\-_\\u4e00-\\u9fa5]"), "")
                    .trim()
                val fileName = "${sanitizedName}.${exporter.getFileExtension()}"

                // 创建导出结果
                val result = ExportResult(
                    content = content,
                    fileName = fileName,
                    formatName = exporter.getFormatName(),
                    fileSize = content.toByteArray().size
                )

                _exportResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _exportError.value = "导出失败: ${e.message}"
            }
        }
    }

    /**
     * 清除导出结果
     */
    fun clearExportResult() {
        _exportResult.value = null
    }

    /**
     * 清除导出错误
     */
    fun clearExportError() {
        _exportError.value = null
    }

    /**
     * 获取所有卡牌（用于剪贴板复制）
     */
    fun getAllCards(): List<Card> {
        return (_mainDeck.value ?: emptyList()) + (_sideboard.value ?: emptyList())
    }

    /**
     * 格式化卡牌名称以用于搜索
     * 处理特殊卡牌类型（split/fusion/adventure 等）
     *
     * 例如:
     * - "Wear/Tear" -> "Wear // Tear"
     * - "Become // immense" -> "Become // immense" (保持不变)
     * - "Fire // Ice" -> "Fire // Ice" (保持不变)
     */
    private fun formatCardNameForSearch(cardName: String): String {
        // 如果已经是正确的格式（双斜杠加空格），直接返回
        if (" // " in cardName) {
            return cardName
        }

        // 将单斜杠转换为双斜杠加空格（处理 split/fusion 卡牌）
        // 例如: "Wear/Tear" -> "Wear // Tear"
        return cardName.replace("/", " // ")
    }
}
