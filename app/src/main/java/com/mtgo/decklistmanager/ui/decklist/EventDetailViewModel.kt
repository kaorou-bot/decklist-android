package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.domain.model.Event
import com.mtgo.decklistmanager.data.repository.DecklistRepository
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

    /**
     * 加载赛事详情
     */
    fun loadEventDetail(eventId: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // 获取赛事信息
                val eventEntity = repository.getEventById(eventId)
                _event.postValue(eventEntity)

                // 获取该赛事下的所有卡组
                val decklistEntities = repository.getDecklistsByEventId(eventId)

                // 转换为 DecklistItem
                val items = decklistEntities.map { entity ->
                    DecklistItem(
                        id = entity.id,
                        eventName = entity.eventName,
                        format = entity.format,
                        date = entity.date,
                        playerName = entity.playerName,
                        record = entity.record
                    )
                }

                _decklists.postValue(items)
                _uiState.value = UiState.Success("Loaded ${items.size} decklists")

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
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    /**
     * Decklist Item - 卡组列表项（复用 MainViewModel 的结构）
     */
    data class DecklistItem(
        val id: Long,
        val eventName: String,
        val format: String,
        val date: String,
        val playerName: String?,
        val record: String?
    )
}
