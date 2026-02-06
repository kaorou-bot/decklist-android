package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.dao.EventDao
import com.mtgo.decklistmanager.domain.model.*
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import com.mtgo.decklistmanager.util.AppLogger
import com.mtgo.decklistmanager.util.FormatMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel - 主界面 ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DecklistRepository,
    private val eventDao: EventDao,
    private val decklistDao: DecklistDao
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Decklists LiveData - 改为使用 LiveData 从 repository 获取
    private val _decklists = MutableLiveData<List<DecklistItem>>()
    val decklists: LiveData<List<DecklistItem>> = _decklists

    // Events LiveData - 赛事列表
    private val _events = MutableLiveData<List<EventItem>>()
    val events: LiveData<List<EventItem>> = _events

    // Filter states
    private val _selectedFormat = MutableStateFlow<String?>(null)
    val selectedFormat: StateFlow<String?> = _selectedFormat.asStateFlow()

    private val _selectedFormatName = MutableStateFlow<String?>("All Formats")
    val selectedFormatName: StateFlow<String?> = _selectedFormatName.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    // Available options
    private val _availableFormats = MutableStateFlow<List<String>>(emptyList())
    val availableFormats: StateFlow<List<String>> = _availableFormats.asStateFlow()

    private val _availableDates = MutableStateFlow<List<String>>(emptyList())
    val availableDates: StateFlow<List<String>> = _availableDates.asStateFlow()

    // Statistics
    private val _statistics = MutableStateFlow<Statistics?>(null)
    val statistics: StateFlow<Statistics?> = _statistics.asStateFlow()

    // Status message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Favorite count
    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount.asStateFlow()

    init {
        // 初始化时加载筛选选项
        loadFilterOptions()
        loadFavoriteCount()

        // 修复数据库中双面牌的标记
        viewModelScope.launch {
            try {
                val fixed = repository.fixDualFacedCards()
                if (fixed > 0) {
                    AppLogger.d("MainViewModel", "已修复 $fixed 张双面牌的数据")
                }
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "修复双面牌失败: ${e.message}", e)
            }
        }
    }

    /**
     * 加载牌组列表
     */
    fun loadDecklists() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val format = _selectedFormat.value
                val date = _selectedDate.value

                // 从 repository 获取数据
                val decklistEntities = repository.getDecklists(format, date, 100)

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

                _decklists.postValue(items)
                _uiState.value = UiState.Success("Loaded ${items.size} decklists")

                // 移除提示，不显示"No decklists found"
                /*
                if (items.isEmpty()) {
                    _statusMessage.value = "No decklists found. Try scraping from MTGTop8."
                }
                */
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error loading decklists: ${e.message}")
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * 搜索牌组
     */
    fun searchDecklists(query: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val format = _selectedFormat.value
                val date = _selectedDate.value

                // 从 repository 获取数据
                val decklistEntities = repository.getDecklists(format, date, 100)

                // 过滤匹配的牌组
                val filteredEntities = decklistEntities.filter { entity ->
                    entity.eventName.contains(query, ignoreCase = true) ||
                    entity.playerName?.contains(query, ignoreCase = true) == true ||
                    entity.format.contains(query, ignoreCase = true)
                }

                // 转换为 DecklistItem
                val items = filteredEntities.map { entity ->
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
                _uiState.value = UiState.Success("Found ${items.size} decklists")

                // 移除提示
                /*
                if (items.isEmpty()) {
                    _statusMessage.value = "No decklists found matching '$query'"
                }
                */
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Search failed: ${e.message}")
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
            _selectedFormatName.value = "All Formats"
            null
        } else {
            _selectedFormatName.value = formatName
            FormatMapper.nameToCode(formatName)
        }
        _selectedFormat.value = formatCode
        // 筛选赛制时加载赛事列表（不是decklists）
        loadEvents()
    }

    /**
     * 应用日期筛选
     */
    fun applyDateFilter(date: String?) {
        _selectedDate.value = if (date == "All Dates") null else date
        // 筛选日期时加载赛事列表（不是decklists）
        loadEvents()
    }

    /**
     * 加载可用的筛选选项（从卡组和赛事获取）
     */
    fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                // 从卡组获取格式代码
                val decklistFormats = decklistDao.getAllFormats()
                // 从赛事获取格式代码
                val eventFormats = eventDao.getAllFormats()
                // 合并去重 - 这些是有数据的格式代码
                val existingFormatCodes = (decklistFormats + eventFormats).distinct().sorted()

                // 使用FormatMapper获取所有格式名称（有数据的优先显示）
                val formatNames = FormatMapper.getAllFormatNamesSorted(existingFormatCodes)

                _availableFormats.value = formatNames

                // 从卡组获取日期
                val decklistDates = decklistDao.getAllDates()
                // 从赛事获取日期
                val eventDates = eventDao.getAllDates()
                // 合并去重
                val allDates = (decklistDates + eventDates).distinct().sortedDescending()
                _availableDates.value = allDates
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load filters: ${e.message}"
            }
        }
    }

    /**
     * 获取统计信息
     */
    fun loadStatistics() {
        viewModelScope.launch {
            try {
                val stats = repository.getStatistics()
                _statistics.value = stats

                _statusMessage.value = "Total: ${stats.totalDecklists} decklists, ${stats.totalCards} cards"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load statistics: ${e.message}"
            }
        }
    }

    /**
     * 清空数据
     */
    fun clearData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                _decklists.postValue(emptyList())
                _statistics.value = null
                loadFilterOptions()
                _statusMessage.value = "All data cleared"
                _uiState.value = UiState.Success("Data cleared")
            } catch (e: Exception) {
                _statusMessage.value = "Failed to clear: ${e.message}"
                _uiState.value = UiState.Error("Clear failed")
            }
        }
    }

    /**
     * 开始爬取（Magic.gg - 已弃用）
     */
    fun startScraping(formatFilter: String?, dateFilter: String?) {
        viewModelScope.launch {
            _uiState.value = UiState.Scraping
            try {
                val result = repository.scrapeDecklists(
                    formatFilter = formatFilter,
                    dateFilter = dateFilter,
                    eventFilter = null // 不再过滤事件类型
                )

                result.fold(
                    onSuccess = { count ->
                        _statusMessage.value = "Scraped $count decklists"
                        _uiState.value = UiState.Success("Scraping complete")

                        // 重新加载数据
                        loadFilterOptions()
                        loadDecklists()
                    },
                    onFailure = { error ->
                        _statusMessage.value = "Scraping failed: ${error.message}"
                        _uiState.value = UiState.Error("Scraping failed")
                    }
                )
            } catch (e: Exception) {
                _statusMessage.value = "Scraping failed: ${e.message}"
                _uiState.value = UiState.Error("Scraping failed")
            }
        }
    }

    /**
     * 开始爬取（MTGTop8）
     */
    fun startMtgTop8Scraping(format: String, date: String?, maxDecks: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Scraping
            try {
                val result = repository.scrapeFromMtgTop8(format, date, maxDecks)

                result.fold(
                    onSuccess = { count ->
                        _statusMessage.value = "Scraped $count decklists from MTGTop8"
                        _uiState.value = UiState.Success("MTGTop8 scraping complete")

                        // 重新加载数据
                        loadFilterOptions()
                        loadDecklists()
                    },
                    onFailure = { error ->
                        _statusMessage.value = "MTGTop8 scraping failed: ${error.message}"
                        _uiState.value = UiState.Error("MTGTop8 scraping failed")
                    }
                )
            } catch (e: Exception) {
                _statusMessage.value = "MTGTop8 scraping failed: ${e.message}"
                _uiState.value = UiState.Error("MTGTop8 scraping failed")
            }
        }
    }

    /**
     * 开始爬取赛事列表（MTGTop8）
     * @param format 格式代码
     * @param date 日期筛选
     * @param maxEvents 最大赛事数量
     */
    fun startEventScraping(format: String, date: String?, maxEvents: Int, maxDecksPerEvent: Int = 0) {
        viewModelScope.launch {
            _uiState.value = UiState.Scraping
            _statusMessage.value = "正在下载比赛列表，请稍候..."

            try {
                // 添加60秒超时
                val result = kotlinx.coroutines.withTimeout(60000L) {
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
                        AppLogger.e("MainViewModel", errorMsg, error)
                    }
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                val errorMsg = "下载超时（60秒）。请检查网络连接或减少下载数量。"
                _statusMessage.value = errorMsg
                _uiState.value = UiState.Error("Timeout")
                AppLogger.e("MainViewModel", errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "下载失败: ${e.message}"
                _statusMessage.value = errorMsg
                _uiState.value = UiState.Error("Event scraping failed")
                AppLogger.e("MainViewModel", errorMsg, e)
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
     * 加载收藏的牌组列表
     */
    fun loadFavoriteDecklists() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val decklistEntities = repository.getFavoriteDecklists()

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
                _uiState.value = UiState.Success("Loaded ${items.size} favorites")

                // 移除提示
                /*
                if (items.isEmpty()) {
                    _statusMessage.value = "No favorites yet. Tap the star on any deck to add it."
                }
                */
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error loading favorites: ${e.message}")
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * 切换收藏状态
     * v4.2.1: 改为suspend函数以便调用者获取返回值
     */
    suspend fun toggleFavorite(decklistId: Long): Boolean {
        return try {
            val isFavorite = repository.toggleFavorite(decklistId)
            loadFavoriteCount()

            _statusMessage.value = if (isFavorite) {
                "Added to favorites"
            } else {
                "Removed from favorites"
            }
            isFavorite
        } catch (e: Exception) {
            _statusMessage.value = "Failed to toggle favorite: ${e.message}"
            false
        }
    }

    /**
     * 加载收藏数量
     */
    private fun loadFavoriteCount() {
        viewModelScope.launch {
            try {
                val count = repository.getFavoriteCount()
                _favoriteCount.value = count
            } catch (e: Exception) {
                // 静默失败，不影响主流程
            }
        }
    }

    /**
     * 检查是否已收藏
     */
    suspend fun checkIsFavorite(decklistId: Long): Boolean {
        return repository.isFavorite(decklistId)
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

                // 从 repository 获取赛事数据
                val eventEntities = repository.getEvents(format, date, 100)

                // 转换为 EventItem
                val items = eventEntities.map { entity ->
                    EventItem(
                        id = entity.id,
                        eventName = entity.eventName,
                        eventType = entity.eventType,
                        format = entity.format,
                        date = normalizeDate(entity.date),  // 统一日期格式
                        sourceUrl = entity.sourceUrl,
                        source = entity.source,
                        deckCount = entity.deckCount
                    )
                }

                _events.postValue(items)
                _uiState.value = UiState.Success("Loaded ${items.size} events")

                // 移除提示
                /*
                if (items.isEmpty()) {
                    _statusMessage.value = "No events found. Try downloading from MTGTop8."
                }
                */
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error loading events: ${e.message}")
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * 删除赛事
     */
    fun deleteEvent(eventId: Long, eventName: String) {
        viewModelScope.launch {
            try {
                val result = repository.deleteEvent(eventId)
                result.fold(
                    onSuccess = {
                        _statusMessage.value = "已删除赛事: $eventName"
                        // 重新加载赛事列表
                        loadEvents()
                        // 更新筛选选项
                        loadFilterOptions()
                        // 更新收藏数量
                        loadFavoriteCount()
                    },
                    onFailure = { error ->
                        _statusMessage.value = "删除失败: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _statusMessage.value = "删除失败: ${e.message}"
            }
        }
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
     * Decklist Item - 牌组列表项
     */
    data class DecklistItem(
        val id: Long,
        val eventName: String,
        val deckName: String?,
        val format: String,
        val date: String,
        val playerName: String?,
        val record: String?
    )

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

    /**
     * 清除卡牌缓存（用于语言切换）
     */
    fun clearCardCache() {
        viewModelScope.launch {
            try {
                repository.clearCardInfoCache()
                AppLogger.d("MainViewModel", "Card cache cleared")
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "Error clearing card cache: ${e.message}", e)
            }
        }
    }

    /**
     * 统一日期格式为 yyyy-MM-dd
     * 支持格式: dd/MM/yyyy, MM/dd/yyyy, yyyy-MM-dd 等
     */
    private fun normalizeDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""

        return try {
            // 尝试解析 dd/MM/yyyy 或 dd/MM/yy
            if (dateStr.contains("/")) {
                val parts = dateStr.split("/")
                when (parts.size) {
                    3 -> {
                        val day = parts[0].padStart(2, '0')
                        val month = parts[1].padStart(2, '0')
                        val year = if (parts[2].length == 2) "20${parts[2]}" else parts[2]
                        "$year-$month-$day"
                    }
                    else -> dateStr
                }
            } else {
                dateStr
            }
        } catch (e: Exception) {
            AppLogger.w("MainViewModel", "Failed to parse date: $dateStr", e)
            dateStr
        }
    }

    /**
     * 一次性修复：批量更新所有 NULL 的 display_name
     * v4.1.0: 在应用启动时自动修复所有缺失的中文名称
     */
    fun fixAllNullDisplayNames() {
        viewModelScope.launch {
            try {
                val fixedCount = repository.fixAllNullDisplayNames()
                if (fixedCount > 0) {
                    AppLogger.d("MainViewModel", "✅ Fixed $fixedCount card display names")
                }
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "Failed to fix display names: ${e.message}")
            }
        }
    }
}
