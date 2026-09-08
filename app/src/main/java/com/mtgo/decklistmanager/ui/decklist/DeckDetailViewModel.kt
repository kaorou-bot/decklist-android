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
import com.mtgo.decklistmanager.data.remote.api.mtgch.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
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
    private val deckImageExporter: com.mtgo.decklistmanager.exporter.DeckImageExporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val decklistId: Long = checkNotNull(savedStateHandle["decklistId"])

    sealed class ImageExportState {
        object Idle : ImageExportState()
        data class Loading(val message: String) : ImageExportState()
        data class Ready(val result: com.mtgo.decklistmanager.exporter.DeckImageExporter.Result) : ImageExportState()
        data class Error(val message: String) : ImageExportState()
    }
    private val _imageExportState = MutableLiveData<ImageExportState>(ImageExportState.Idle)
    val imageExportState: LiveData<ImageExportState> = _imageExportState
    private var imageExportJob: Job? = null

    fun exportDeckImage() {
        if (imageExportJob?.isActive == true) return
        val deck = _decklist.value ?: return
        val cards = getAllCards().toList()
        _imageExportState.value = ImageExportState.Loading("正在生成套牌图片…")
        imageExportJob = viewModelScope.launch {
            try {
                val result = deckImageExporter.export(deck, cards) { done, total ->
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _imageExportState.value = ImageExportState.Loading("正在生成图片：$done / $total")
                    }
                }
                _imageExportState.value = ImageExportState.Ready(result)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _imageExportState.value = ImageExportState.Error("图片生成失败，请稍后重试。") }
        }
    }
    fun clearImageExportState() { _imageExportState.value = ImageExportState.Idle }

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
        // 调试日志
        if (it.cardName.contains("Force of Negation") || it.cardName.contains("Subtlety") || it.cardName.contains("Flash")) {
            AppLogger.d("DeckDetailViewModel", "CardEntity.toCard(): ${it.cardNameZh}, manaCost=${it.manaCost}")
        }
    }

    /**
     * 加载牌组详情
     */
    fun loadDecklistDetail() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 加载牌组信息
                val decklistEntity = repository.getDecklistById(decklistId)
                if (decklistEntity != null) {
                    _decklist.value = decklistEntity.toDecklist()

                    // 完整牌表在赛事下载阶段准备；打开详情只读取本地数据。
                    // 加载所有卡牌（重新查询确保获取最新数据）
                    val allCards = repository.getCardsByDecklistId(decklistId)

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
     * 查询单卡信息
     * v4.1.0: 直接使用 repository.getCardInfo()，依赖数据库缓存
     * 数据库缓存速度很快（< 50ms）且持久化，无需内存缓存
     */
    private var cardInfoJob: Job? = null
    var lastRequestedCardName: String? = null
        private set

    fun loadCardInfo(cardName: String) {
        cardInfoJob?.cancel()
        lastRequestedCardName = cardName
        _cardInfo.value = null
        _cardInfoError.value = null
        _isCardInfoLoading.value = true
        cardInfoJob = viewModelScope.launch {
            try {
                val cardInfo = repository.getCardInfo(cardName)
                coroutineContext.ensureActive()
                if (cardInfo != null) {
                    _cardInfo.value = cardInfo
                } else {
                    _cardInfoError.value = "未能获取「$cardName」的详情，请检查网络或稍后重试。"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                AppLogger.e("DeckDetailViewModel", "Card detail request failed: ${e.message}")
                _cardInfoError.value = "加载「$cardName」失败，请重试。"
            } finally {
                if (coroutineContext[Job]?.isActive == true) _isCardInfoLoading.value = false
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
