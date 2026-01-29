package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.Event
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import com.mtgo.decklistmanager.util.AppLogger
import com.mtgo.decklistmanager.util.FormatMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
                        sourceUrl = entity.sourceUrl,
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
     * @param formatName 格式名称（如 "Modern"），null表示所有格式
     */
    fun applyFormatFilter(formatName: String?) {
        // 将格式名称转换为格式代码
        val formatCode = if (formatName == null || formatName == "All Formats") {
            null
        } else {
            FormatMapper.nameToCode(formatName)
        }
        _selectedFormat.value = formatCode
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
                // 获取数据库中已有的格式代码
                val existingFormatCodes = repository.getAllFormats()

                // 使用FormatMapper获取所有格式名称（有数据的优先显示）
                val formatNames = FormatMapper.getAllFormatNamesSorted(existingFormatCodes)

                _availableFormats.value = formatNames

                val dates = repository.getAllDates()
                _availableDates.value = dates
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load filters: ${e.message}"
            }
        }
    }

    /**
     * 开始爬取赛事列表（只下载比赛，不下载卡组）
     *
     * @param format 格式代码
     * @param date 日期筛选
     * @param maxEvents 最大赛事数量
     * @param maxDecksPerEvent 未使用（保留参数）
     */
    fun startEventScraping(format: String, date: String?, maxEvents: Int, maxDecksPerEvent: Int = 0) {
        viewModelScope.launch {
            _uiState.value = UiState.Scraping
            _statusMessage.value = "正在下载比赛列表，请稍候..."

            try {
                // 添加60秒超时
                val result = withTimeout(60000L) {
                    repository.scrapeEventsFromMtgTop8(
                        format = format,
                        date = date,
                        maxEvents = maxEvents,
                        maxDecksPerEvent = maxDecksPerEvent
                    )
                }

                result.fold(
                    onSuccess = { count ->
                        _statusMessage.value = "成功下载 $count 个比赛！点击比赛查看详情"
                        _uiState.value = UiState.Success("Event scraping complete")

                        // 重新加载数据
                        loadFilterOptions()
                        loadEvents()
                    },
                    onFailure = { error ->
                        val errorMsg = "下载失败: ${error.message}"
                        _statusMessage.value = errorMsg
                        _uiState.value = UiState.Error("Event scraping failed")
                        AppLogger.e("EventListViewModel", errorMsg, error)
                    }
                )
            } catch (e: TimeoutCancellationException) {
                val errorMsg = "下载超时（60秒）。请检查网络连接或减少下载数量。"
                _statusMessage.value = errorMsg
                _uiState.value = UiState.Error("Timeout")
                AppLogger.e("EventListViewModel", errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "下载失败: ${e.message}"
                _statusMessage.value = errorMsg
                _uiState.value = UiState.Error("Event scraping failed")
                AppLogger.e("EventListViewModel", errorMsg, e)
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
        val sourceUrl: String?,
        val source: String,
        val deckCount: Int
    )
}
