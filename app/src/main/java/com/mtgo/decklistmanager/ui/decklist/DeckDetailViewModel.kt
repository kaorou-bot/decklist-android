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
import com.mtgo.decklistmanager.exporter.DecklistExporter
import com.mtgo.decklistmanager.exporter.ExportResult
import com.mtgo.decklistmanager.exporter.format.MtgoFormatExporter
import com.mtgo.decklistmanager.exporter.format.ArenaFormatExporter
import com.mtgo.decklistmanager.exporter.format.TextFormatExporter
import com.mtgo.decklistmanager.util.LanguagePreferenceManager
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DeckDetailViewModel - 牌组详情 ViewModel
 */
@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    private val repository: DecklistRepository,
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

    // 卡牌信息缓存 (卡牌名称 -> 卡牌信息)
    private val cardInfoCache = mutableMapOf<String, CardInfo>()

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
    )

    /**
     * 加载牌组详情
     */
    fun loadDecklistDetail() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 首次加载时修复双面牌数据
                repository.fixDualFacedCards()

                // 加载牌组信息
                val decklistEntity = repository.getDecklistById(decklistId)
                if (decklistEntity != null) {
                    _decklist.value = decklistEntity.toDecklist()

                    // v4.1.0: 确保卡牌详情已获取后再加载
                    // 先触发 fetchScryfallDetails，它会异步更新数据库
                    repository.ensureCardDetails(decklistId)

                    // 等待足够长的时间让数据更新完成（从 500ms 增加到 1500ms）
                    kotlinx.coroutines.delay(1500)

                    // 再次调用 ensureCardDetails 以确保所有卡牌都有详细信息
                    repository.ensureCardDetails(decklistId)

                    // 再等待一段时间让第二次更新完成
                    kotlinx.coroutines.delay(500)

                    // 加载所有卡牌
                    val allCards = repository.getCardsByDecklistId(decklistId)

                    // v4.1.0: 预取所有唯一卡牌的详细信息到缓存
                    val uniqueCardNames = allCards.map { it.cardName }.distinct()
                    prefetchCardInfo(uniqueCardNames)

                    // v4.0.0: displayName is already populated in the database, so just convert directly
                    // 分离主牌和备牌
                    val mainCards = allCards
                        .filter { it.location == "main" }
                        .map { it.toCard() }
                    _mainDeck.value = mainCards

                    val sideboardCards = allCards
                        .filter { it.location == "sideboard" }
                        .map { it.toCard() }
                    _sideboard.value = sideboardCards
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 预取卡牌信息到缓存（后台异步执行，不阻塞UI）
     */
    private fun prefetchCardInfo(cardNames: List<String>) {
        viewModelScope.launch {
            coroutineScope {
                val semaphore = kotlinx.coroutines.sync.Semaphore(5) // 最多5个并发请求

                cardNames.map { cardName ->
                    async {
                        semaphore.acquire()
                        try {
                            if (!cardInfoCache.containsKey(cardName)) {
                                val cardInfo = repository.getCardInfo(cardName)
                                if (cardInfo != null) {
                                    cardInfoCache[cardName] = cardInfo
                                }
                            }
                        } catch (e: Exception) {
                            // 静默失败，不影响缓存
                        } finally {
                            semaphore.release()
                        }
                    }
                }.awaitAll()
            }
        }
    }

    /**
     * 查询单卡信息（优先使用缓存，缓存未命中时才调用API）
     * v4.1.0: 优化性能 - 使用缓存避免重复API调用
     */
    fun loadCardInfo(cardName: String) {
        viewModelScope.launch {
            try {
                _isCardInfoLoading.value = true
                _cardInfoError.value = null

                // 首先检查缓存
                val cachedInfo = cardInfoCache[cardName]
                if (cachedInfo != null) {
                    AppLogger.d("DeckDetailViewModel", "✓ Cache hit for: $cardName")
                    _cardInfo.value = cachedInfo
                    return@launch
                }

                AppLogger.d("DeckDetailViewModel", "✗ Cache miss for: $cardName, fetching from API")

                // 缓存未命中，调用API获取
                val cardInfo = repository.getCardInfo(cardName)

                if (cardInfo != null) {
                    // 存入缓存
                    cardInfoCache[cardName] = cardInfo
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
}
