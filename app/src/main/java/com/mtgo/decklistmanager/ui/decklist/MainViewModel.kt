package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.*
import com.mtgo.decklistmanager.data.repository.DecklistRepository
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
    private val repository: DecklistRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Decklists LiveData - 改为使用 LiveData 从 repository 获取
    private val _decklists = MutableLiveData<List<DecklistItem>>()
    val decklists: LiveData<List<DecklistItem>> = _decklists

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

    // Statistics
    private val _statistics = MutableStateFlow<Statistics?>(null)
    val statistics: StateFlow<Statistics?> = _statistics.asStateFlow()

    // Status message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // 初始化时加载筛选选项
        loadFilterOptions()
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
                        format = entity.format,
                        date = entity.date,
                        playerName = entity.playerName,
                        record = entity.record
                    )
                }

                _decklists.postValue(items)
                _uiState.value = UiState.Success("Loaded ${items.size} decklists")

                // 如果没有数据，提示用户
                if (items.isEmpty()) {
                    _statusMessage.value = "No decklists found. Try scraping or adding test data."
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load: ${e.message}")
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
                        format = entity.format,
                        date = entity.date,
                        playerName = entity.playerName,
                        record = entity.record
                    )
                }

                _decklists.postValue(items)
                _uiState.value = UiState.Success("Found ${items.size} decklists")

                // 如果没有结果，提示用户
                if (items.isEmpty()) {
                    _statusMessage.value = "No decklists found matching '$query'"
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Search failed: ${e.message}")
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * 应用格式筛选
     */
    fun applyFormatFilter(format: String?) {
        _selectedFormat.value = if (format == "All Formats") null else format
        loadDecklists()
    }

    /**
     * 应用日期筛选
     */
    fun applyDateFilter(date: String?) {
        _selectedDate.value = if (date == "All Dates") null else date
        loadDecklists()
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
    fun startMtgTop8Scraping(format: String, maxDecks: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Scraping
            try {
                val result = repository.scrapeFromMtgTop8(format, maxDecks)

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
     * Decklist Item - 牌组列表项
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
