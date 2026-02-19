package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.dao.EventDao
import com.mtgo.decklistmanager.data.local.entity.DecklistEntity
import com.mtgo.decklistmanager.data.remote.api.ServerApi
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.domain.model.Event
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event Detail ViewModel - 赛事详情 ViewModel
 * v4.2.0: 支持从服务端 API 获取赛事套牌列表
 */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventDao: EventDao,
    private val decklistDao: DecklistDao,
    private val serverApi: ServerApi
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
     * @param fromServer 是否从服务器加载（默认 true）
     */
    fun loadEventDetail(eventId: Long, fromServer: Boolean = true) {
        // 重置标志，每次加载新赛事时重新评估
        hasShownDownloadDialog = false
        AppLogger.d("EventDetailViewModel", "loadEventDetail called for eventId: $eventId, fromServer: $fromServer")

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                if (fromServer) {
                    // 从服务器 API 加载
                    loadEventDetailFromServer(eventId)
                } else {
                    // 从本地数据库加载
                    loadEventDetailFromLocal(eventId)
                }
            } catch (e: Exception) {
                AppLogger.e("EventDetailViewModel", "Error loading event: ${e.message}", e)
                _uiState.value = UiState.Error("Error loading event: ${e.message}")
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * 从服务器 API 加载赛事详情
     */
    private suspend fun loadEventDetailFromServer(eventId: Long) {
        AppLogger.d("EventDetailViewModel", "Loading event from server: $eventId")

        // 1. 获取赛事信息
        val eventResponse = serverApi.getEventDetail(eventId)
        if (!eventResponse.isSuccessful || eventResponse.body()?.success != true) {
            _uiState.value = UiState.Error("Failed to load event: ${eventResponse.code()}")
            _statusMessage.value = "Failed to load event"
            return
        }

        val eventDto = eventResponse.body()!!.data!!
        val eventEntity = com.mtgo.decklistmanager.data.local.entity.EventEntity(
            id = eventDto.id,
            eventName = eventDto.eventName,
            eventType = eventDto.eventType,
            format = eventDto.format,
            date = eventDto.date,
            sourceUrl = eventDto.sourceUrl,
            source = eventDto.source,
            deckCount = eventDto.deckCount,
            createdAt = System.currentTimeMillis()
        )

        // 保存赛事信息到本地
        eventDao.insert(eventEntity)

        // 2. 获取该赛事下的套牌列表（从服务器）
        val decklistsResponse = serverApi.getEventDecklists(eventId, limit = 200, offset = 0)
        if (!decklistsResponse.isSuccessful || decklistsResponse.body()?.success != true) {
            _uiState.value = UiState.Error("Failed to load decklists")
            _statusMessage.value = "Failed to load decklists"
            // 仍然显示赛事信息，但没有套牌
            _event.value = eventEntity.toDomainModel()
            _decklists.value = emptyList()
            return
        }

        val decklistDtos = decklistsResponse.body()!!.data!!.decklists
        AppLogger.d("EventDetailViewModel", "Server returned ${decklistDtos.size} decklists")

        // Log first decklist for debugging
        if (decklistDtos.isNotEmpty()) {
            val first = decklistDtos[0]
            AppLogger.d("EventDetailViewModel", "First decklist: id=${first.id}, deckName=${first.deckName}, playerName=${first.playerName}")
        }

        // 3. 转换并保存套牌到本地数据库
        val decklistEntities = decklistDtos.map { dto ->
            DecklistEntity(
                id = dto.id,
                eventId = dto.eventId,
                eventName = dto.eventName,
                deckName = dto.deckName,
                format = dto.format,
                date = dto.date,
                url = dto.url ?: "",
                playerName = dto.playerName,
                playerId = null,
                record = dto.record,
                eventType = null,
                createdAt = System.currentTimeMillis()
            )
        }

        // 先删除该赛事下的所有套牌（如果存在），然后插入新的
        // 这样可以确保数据是最新的
        eventDao.deleteDecklistsByEventId(eventId)
        decklistDao.insertAll(decklistEntities)

        // 4. 转换为 DecklistItem 并显示
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

        _decklists.value = items
        _event.value = eventEntity.toDomainModel()
        _uiState.value = UiState.Success("Loaded ${items.size} decklists")

        AppLogger.d("EventDetailViewModel", "Loaded ${items.size} decklists for event $eventId")
        AppLogger.d("EventDetailViewModel", "_decklists.value.size = ${_decklists.value?.size}")

        if (items.isEmpty()) {
            _statusMessage.value = "No decklists found in this event"
        }
    }

    /**
     * 从本地数据库加载赛事详情（后备方案）
     */
    private suspend fun loadEventDetailFromLocal(eventId: Long) {
        AppLogger.d("EventDetailViewModel", "Loading event from local database: $eventId")

        // 获取赛事信息
        val eventEntity = eventDao.getEventById(eventId)
        if (eventEntity == null) {
            _uiState.value = UiState.Error("Event not found")
            _statusMessage.value = "Event not found in local database"
            return
        }

        // 获取该赛事下的所有卡组
        val decklistEntities = decklistDao.getDecklistsByEventId(eventId)

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

        _decklists.value = items
        _event.value = eventEntity.toDomainModel()
        _uiState.value = UiState.Success("Loaded ${items.size} decklists (local)")

        AppLogger.d("EventDetailViewModel", "Loaded ${items.size} decklists for event $eventId (local)")

        if (items.isEmpty()) {
            _statusMessage.value = "No decklists found in this event"
        }
    }

    /**
     * 下载该赛事的所有卡组（已废弃，现在通过 loadEventDetail 自动从服务器获取）
     * @param sourceUrl 赛事的源 URL
     * @param format 赛制代码 (ST, MO, PI, etc.)
     */
    @Deprecated("Use loadEventDetail(eventId, fromServer=true) instead")
    fun downloadEventDecklists(sourceUrl: String, format: String) {
        AppLogger.w("EventDetailViewModel", "downloadEventDecklists is deprecated, use loadEventDetail instead")
        // 重新从服务器加载
        val currentEvent = _event.value
        if (currentEvent != null) {
            loadEventDetail(currentEvent.id, fromServer = true)
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

/**
 * Extension function to convert EventEntity to Event domain model
 */
private fun com.mtgo.decklistmanager.data.local.entity.EventEntity.toDomainModel(): Event {
    return Event(
        id = id,
        eventName = eventName,
        eventType = eventType,
        format = format,
        date = date,
        sourceUrl = sourceUrl,
        source = source,
        deckCount = deckCount,
        createdAt = createdAt
    )
}
