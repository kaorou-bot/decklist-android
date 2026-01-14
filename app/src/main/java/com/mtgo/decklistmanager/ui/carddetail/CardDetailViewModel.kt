package com.mtgo.decklistmanager.ui.carddetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * CardDetailViewModel - 卡牌详情 ViewModel
 */
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: DecklistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardName: String = checkNotNull(savedStateHandle["cardName"])

    // Card info
    private val _cardInfo = MutableLiveData<CardInfo?>()
    val cardInfo: LiveData<CardInfo?> = _cardInfo

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * 加载卡牌详情
     */
    fun loadCardDetail() {
        _isLoading.value = true
        _errorMessage.value = null

        // 加载逻辑
    }

    /**
     * 重试加载
     */
    fun retry() {
        loadCardDetail()
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
