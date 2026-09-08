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
        if (downloadJob?.isActive == true) return
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
                if (downloadJob?.isActive != true) {
                    _uiState.value = UiState.Success("已加载 ${items.size} 套牌")
                }

                AppLogger.d("EventDetailViewModel", "Loaded ${items.size} decklists for event $eventId")

                if (items.isEmpty()) {
                    _statusMessage.value = "该赛事暂无本地套牌，可获取赛事套牌"
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("EventDetailViewModel", "加载赛事失败", e)
                _uiState.value = UiState.Error("赛事加载失败，请重试")
                _statusMessage.value = "赛事加载失败，请重试"
            }
        }
    }

    /**
     * 下载该赛事的所有卡组
     * @param sourceUrl 赛事的源 URL
     * @param format 赛制代码 (ST, MO, PI, etc.)
     */
    private var downloadJob: kotlinx.coroutines.Job? = null

    fun downloadEventDecklists(sourceUrl: String, format: String) {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            _uiState.value = UiState.Downloading()
            try {
                // 直接使用 MtgTop8Scraper 下载卡组
                val result = repository.scrapeSingleEvent(sourceUrl, format) { message ->
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _uiState.value = UiState.Downloading(message)
                    }
                }

                result.fold(
                    onSuccess = { result ->
                        val message = buildString {
                            append("已保存 ${result.saved} / ${result.total} 副完整牌表，可直接打开浏览。")
                            if (result.saved < result.total) append(" 部分牌表下载失败，请重新获取。")
                            if (result.incompleteDetails > 0) append(" ${result.incompleteDetails} 副的中文名等资料未补齐，将显示已有牌名。")
                        }
                        _statusMessage.value = message
                        _uiState.value = UiState.Success(message)

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
                        AppLogger.e("EventDetailViewModel", "获取套牌失败", error)
                        _statusMessage.value = "获取套牌失败，请检查网络后重试"
                        _uiState.value = UiState.Error("获取套牌失败，请检查网络后重试")
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("EventDetailViewModel", "获取套牌失败", e)
                _statusMessage.value = "获取套牌失败，请稍后重试"
                _uiState.value = UiState.Error("获取套牌失败，请稍后重试")
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
        data class Downloading(val message: String = "正在下载全部套牌的完整牌表…") : UiState()
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
