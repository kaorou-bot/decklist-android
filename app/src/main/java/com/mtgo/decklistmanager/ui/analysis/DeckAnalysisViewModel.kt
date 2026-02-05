package com.mtgo.decklistmanager.ui.analysis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.data.analyzer.DeckAnalyzer
import com.mtgo.decklistmanager.domain.model.DeckAnalysis
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 套牌分析 ViewModel
 */
@HiltViewModel
class DeckAnalysisViewModel @Inject constructor(
    private val analyzer: DeckAnalyzer
) : ViewModel() {

    private val _analysis = MutableLiveData<DeckAnalysis?>()
    val analysis: LiveData<DeckAnalysis?> = _analysis

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * 加载套牌分析
     */
    fun loadAnalysis(decklistId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                AppLogger.d("DeckAnalysisViewModel", "Loading analysis for decklist: $decklistId")

                val result = analyzer.analyze(decklistId)
                _analysis.value = result

                AppLogger.d("DeckAnalysisViewModel", "Analysis loaded successfully")
            } catch (e: Exception) {
                AppLogger.e("DeckAnalysisViewModel", "Failed to load analysis: ${e.message}", e)
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
