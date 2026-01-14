package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.Event
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event List ViewModel - 赛事列表 ViewModel
 */
@HiltViewModel
class EventListViewModel @Inject constructor(
    private val repository: DecklistRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Events LiveData
    private val _events = MutableLiveData<List<EventItem>>()
    val events: LiveData<List<EventItem>> = _events

    // Filter states
    private val _selectedFormat = MutableStateFlow<String?>(null)
    val selectedFormat: StateFlow<String?> = _selectedFormat.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    // Available options
    private val _availableFormats = MutableStateFlow<List<String>>(emptyList())
    val availableFormats: StateFlow<List<String>> = _availableFormats.asStateFlow()

    private val _availableDates = MutableStateFlow<List<String>>(emptyList())
    val availableDates: StateFlow<List<String>> = _availableDates.asStateFlow()

    // Status message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // 初始化时加载筛选选项
        loadFilterOptions()
    }

    /**
     * 加载赛事列表
     */
    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val format = _selectedFormat.value
                val date = _selectedDate.value

                // 从 repository 获取数据
                val eventEntities = repository.getEvents(format, date, 100, 0)

                // 转换为 EventItem
                val items = eventEntities.map { entity ->
                    EventItem(
                        id = entity.id,
                        eventName = entity.eventName,
                        eventType = entity.eventType,
                        format = entity.format,
                        date = entity.date,
                        source = entity.source,
                        deckCount = entity.deckCount
                    )
                }

                _events.postValue(items)
                _uiState.value = UiState.Success("Loaded ${items.size} events")

                // 如果没有结果，提示用户
                if (items.isEmpty()) {
                    _statusMessage.value = "No events found. Try scraping from MTGTop8."
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error loading events: ${e.message}")
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * 应用格式筛选
     */
    fun applyFormatFilter(format: String?) {
        _selectedFormat.value = if (format == "All Formats") null else format
        loadEvents()
    }

    /**
     * 应用日期筛选
     */
    fun applyDateFilter(date: String?) {
        _selectedDate.value = if (date == "All Dates") null else date
        loadEvents()
    }

    /**
     * 加载可用的筛选选项
     */
    fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                val formats = repository.getAllFormats()
                _availableFormats.value = formats

                val dates = repository.getAllDates()
                _availableDates.value = dates
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load filters: ${e.message}"
            }
        }
    }

    /**
     * 开始爬取赛事（MTGTop8 三级结构）
     */
    fun startEventScraping(format: String, date: String?, maxEvents: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Scraping
            try {
                val result = repository.scrapeEventsFromMtgTop8(format, date, maxEvents)

                result.fold(
                    onSuccess = { count ->
                        _statusMessage.value = "Scraped $count decklists from events"
                        _uiState.value = UiState.Success("Event scraping complete")

                        // 重新加载数据
                        loadFilterOptions()
                        loadEvents()
                    },
                    onFailure = { error ->
                        _statusMessage.value = "Event scraping failed: ${error.message}"
                        _uiState.value = UiState.Error("Event scraping failed")
                    }
                )
            } catch (e: Exception) {
                _statusMessage.value = "Event scraping failed: ${e.message}"
                _uiState.value = UiState.Error("Event scraping failed")
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
        object Scraping : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    /**
     * Event Item - 赛事列表项
     */
    data class EventItem(
        val id: Long,
        val eventName: String,
        val eventType: String?,
        val format: String,
        val date: String,
        val source: String,
        val deckCount: Int
    )
}
