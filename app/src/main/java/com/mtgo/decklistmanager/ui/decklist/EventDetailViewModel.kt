package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.domain.model.Event
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event Detail ViewModel - 赛事详情 ViewModel
 */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: DecklistRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Event LiveData
    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    // Decklists in this event
    private val _decklists = MutableLiveData<List<DecklistItem>>()
    val decklists: LiveData<List<DecklistItem>> = _decklists

    // Status message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // 标志：是否已经显示过下载对话框
    private var hasShownDownloadDialog = false

    /**
     * 判断是否应该显示下载对话框
     * 只在第一次加载且没有套牌时显示一次
     */
    fun shouldShowDownloadDialog(): Boolean {
        val currentDecklists = _decklists.value
        // 只有当 value 不为 null 且为空列表时才显示
        val shouldShow = currentDecklists != null && currentDecklists.isEmpty()

        AppLogger.d("EventDetailViewModel", "shouldShowDownloadDialog check - hasShown: $hasShownDownloadDialog, decklists: ${currentDecklists?.size}, shouldShow: $shouldShow")

        if (hasShownDownloadDialog) {
            AppLogger.d("EventDetailViewModel", "Already shown dialog, returning false")
            return false
        }

        if (shouldShow) {
            hasShownDownloadDialog = true
            AppLogger.d("EventDetailViewModel", "First time with empty decklists, showing dialog")
        }

        return shouldShow
    }

    /**
     * 加载赛事详情
     */
    fun loadEventDetail(eventId: Long) {
        // 重置标志，每次加载新赛事时重新评估
        hasShownDownloadDialog = false
        AppLogger.d("EventDetailViewModel", "loadEventDetail called for eventId: $eventId, hasShownDownloadDialog reset to false")

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // 获取赛事信息
                val eventEntity = repository.getEventById(eventId)

                // 获取该赛事下的所有卡组
                val decklistEntities = repository.getDecklistsByEventId(eventId)

                // 转换为 DecklistItem
                val items = decklistEntities.map { entity ->
                    DecklistItem(
                        id = entity.id,
                        eventName = entity.eventName,
                        deckName = entity.deckName,
                        format = entity.format,
                        date = entity.date,
                        playerName = entity.playerName,
                        record = entity.record
                    )
                }

                // 使用 setValue 而不是 postValue，确保立即更新
                // 这样在 shouldAutoDownload() 检查时能获取到正确的值
                _decklists.value = items
                _event.value = eventEntity
                _uiState.value = UiState.Success("Loaded ${items.size} decklists")

                AppLogger.d("EventDetailViewModel", "Loaded ${items.size} decklists for event $eventId")

                if (items.isEmpty()) {
                    _statusMessage.value = "No decklists found in this event"
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error loading event: ${e.message}")
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * 下载该赛事的所有卡组
     * @param sourceUrl 赛事的源 URL
     * @param format 赛制代码 (ST, MO, PI, etc.)
     */
    fun downloadEventDecklists(sourceUrl: String, format: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Downloading
            try {
                // 直接使用 MtgTop8Scraper 下载卡组
                val result = repository.scrapeSingleEvent(sourceUrl, format)

                result.fold(
                    onSuccess = { count ->
                        _statusMessage.value = "Successfully downloaded $count decklists"
                        _uiState.value = UiState.Success("Downloaded $count decklists")

                        // 重新加载赛事详情
                        val eventEntity = repository.getEventById(
                            _event.value?.id ?: 0L
                        )
                        _event.postValue(eventEntity)

                        // 重新加载卡组列表
                        eventEntity?.let {
                            val decklistEntities = repository.getDecklistsByEventId(it.id)
                            val items = decklistEntities.map { entity ->
                                DecklistItem(
                                    id = entity.id,
                                    eventName = entity.eventName,
                                    deckName = entity.deckName,
                                    format = entity.format,
                                    date = entity.date,
                                    playerName = entity.playerName,
                                    record = entity.record
                                )
                            }
                            _decklists.postValue(items)
                        }
                    },
                    onFailure = { error ->
                        _statusMessage.value = "Download failed: ${error.message}"
                        _uiState.value = UiState.Error("Download failed: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _statusMessage.value = "Download error: ${e.message}"
                _uiState.value = UiState.Error("Download error: ${e.message}")
            }
        }
    }

    /**
     * 清除状态消息
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * UI State 封装类
     */
    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        object Downloading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    /**
     * Decklist Item - 卡组列表项（复用 MainViewModel 的结构）
     */
    data class DecklistItem(
        val id: Long,
        val eventName: String,
        val deckName: String?,
        val format: String,
        val date: String,
        val playerName: String?,
        val record: String?,
        val isLoading: Boolean = false
    )
}
