package com.mtgo.decklistmanager.ui.carddetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CardDetailViewModel - 卡牌详情 ViewModel
 * 支持印刷版本切换
 */
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: DecklistRepository,
    private val mtgchApi: MtgchApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardName: String = checkNotNull(savedStateHandle["cardName"])
    private var oracleId: String? = savedStateHandle["oracleId"]

    // Card info
    private val _cardInfo = MutableLiveData<CardInfo?>()
    val cardInfo: LiveData<CardInfo?> = _cardInfo

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 印刷版本列表
    private val _printings = MutableLiveData<List<MtgchCardDto>>()
    val printings: LiveData<List<MtgchCardDto>> = _printings

    // 当前选中的印刷版本索引
    private val _currentPrintingIndex = MutableLiveData(0)
    val currentPrintingIndex: LiveData<Int> = _currentPrintingIndex

    // 印刷版本总数
    private val _totalPrintings = MutableLiveData(0)
    val totalPrintings: LiveData<Int> = _totalPrintings

    // 加载印刷版本中
    private val _isLoadingPrintings = MutableLiveData(false)
    val isLoadingPrintings: LiveData<Boolean> = _isLoadingPrintings

    /**
     * 加载卡牌详情
     */
    fun loadCardDetail() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val cardInfo = repository.getCardInfo(cardName)
                if (cardInfo != null) {
                    _cardInfo.value = cardInfo

                    // 如果有 oracleId，加载印刷版本
                    oracleId?.let { loadCardPrintings(it) }
                } else {
                    _errorMessage.value = "Failed to load card: $cardName"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading card: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 设置卡牌详情（直接从搜索结果传入）
     */
    fun setCardInfo(cardInfo: CardInfo, cardOracleId: String?) {
        _cardInfo.value = cardInfo
        oracleId = cardOracleId

        // 加载印刷版本
        cardOracleId?.let { loadCardPrintings(it) }
    }

    /**
     * 加载卡牌的所有印刷版本
     */
    fun loadCardPrintings(oracleId: String) {
        _isLoadingPrintings.value = true

        viewModelScope.launch {
            try {
                AppLogger.d("CardDetailViewModel", "Loading printings for oracleId: $oracleId")

                val response = mtgchApi.getCardPrintings(oracleId, limit = 100, offset = 0)

                if (response.isSuccessful && response.body()?.success == true) {
                    val cards = response.body()?.cards ?: emptyList()
                    val total = response.body()?.total ?: cards.size

                    AppLogger.d("CardDetailViewModel", "Loaded ${cards.size} printings (total: $total)")

                    _printings.value = cards
                    _totalPrintings.value = total

                    // 找到当前版本对应的索引
                    val currentIndex = cards.indexOfFirst { it.oracleId == oracleId }
                    if (currentIndex >= 0) {
                        _currentPrintingIndex.value = currentIndex
                    }
                } else {
                    AppLogger.w("CardDetailViewModel", "No printings found")
                    _printings.value = emptyList()
                    _totalPrintings.value = 0
                }
            } catch (e: Exception) {
                AppLogger.e("CardDetailViewModel", "Error loading printings", e)
                _errorMessage.value = "Error loading printings: ${e.message}"
                _printings.value = emptyList()
                _totalPrintings.value = 0
            } finally {
                _isLoadingPrintings.value = false
            }
        }
    }

    /**
     * 切换到指定索引的印刷版本
     */
    fun switchToPrinting(index: Int) {
        val printingsList = _printings.value ?: return

        if (index in printingsList.indices) {
            _currentPrintingIndex.value = index

            // 将新版本的卡牌转换为 CardInfo
            val newCard = printingsList[index]
            val newCardInfo = com.mtgo.decklistmanager.util.CardDetailHelper.buildCardInfo(
                mtgchCard = newCard,
                cardInfoId = newCard.idString ?: newCard.oracleId ?: ""
            )

            _cardInfo.value = newCardInfo
        }
    }

    /**
     * 获取当前印刷版本
     */
    fun getCurrentPrinting(): MtgchCardDto? {
        val index = _currentPrintingIndex.value ?: 0
        return _printings.value?.getOrNull(index)
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
