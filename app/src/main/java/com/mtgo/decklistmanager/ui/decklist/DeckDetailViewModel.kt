package com.mtgo.decklistmanager.ui.decklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.domain.model.CardLocation
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import com.mtgo.decklistmanager.data.local.entity.DecklistEntity
import com.mtgo.decklistmanager.data.local.entity.CardEntity
import com.mtgo.decklistmanager.util.LanguagePreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DeckDetailViewModel - 牌组详情 ViewModel
 */
@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    private val repository: DecklistRepository,
    private val languagePreferenceManager: LanguagePreferenceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val decklistId: Long = checkNotNull(savedStateHandle["decklistId"])

    // Decklist detail
    private val _decklist = MutableLiveData<Decklist?>()
    val decklist: LiveData<Decklist?> = _decklist

    // Cards grouped by location
    private val _mainDeck = MutableLiveData<List<Card>>()
    val mainDeck: LiveData<List<Card>> = _mainDeck

    private val _sideboard = MutableLiveData<List<Card>>()
    val sideboard: LiveData<List<Card>> = _sideboard

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Card info for popup
    private val _cardInfo = MutableLiveData<CardInfo?>()
    val cardInfo: LiveData<CardInfo?> = _cardInfo

    // Card info loading state
    private val _isCardInfoLoading = MutableLiveData<Boolean>(false)
    val isCardInfoLoading: LiveData<Boolean> = _isCardInfoLoading

    // Card info error message
    private val _cardInfoError = MutableLiveData<String?>()
    val cardInfoError: LiveData<String?> = _cardInfoError

    // Helper function to convert entities
    private fun DecklistEntity.toDecklist() = Decklist(
        id = id,
        eventName = eventName,
        eventType = eventType,
        deckName = deckName,
        format = format,
        date = date,
        url = url,
        playerName = playerName,
        playerId = playerId,
        record = record,
        createdAt = createdAt
    )

    private fun CardEntity.toCard() = Card(
        id = id,
        decklistId = decklistId,
        cardName = cardName,
        quantity = quantity,
        location = if (location == "main") CardLocation.MAIN else CardLocation.SIDEBOARD,
        cardOrder = cardOrder,
        manaCost = manaCost,
        rarity = rarity,
        color = color,
        cardType = cardType,
        cardSet = cardSet
    )

    /**
     * 加载牌组详情
     */
    fun loadDecklistDetail() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 首次加载时修复双面牌数据
                repository.fixDualFacedCards()

                // 加载牌组信息
                val decklistEntity = repository.getDecklistById(decklistId)
                if (decklistEntity != null) {
                    _decklist.value = decklistEntity.toDecklist()

                    // 加载所有卡牌
                    val allCards = repository.getCardsByDecklistId(decklistId)

                    // 检查是否使用中文
                    val useChinese = languagePreferenceManager.getCardLanguage() == LanguagePreferenceManager.LANGUAGE_CHINESE

                    // 如果使用中文，获取所有卡牌的中文名映射
                    val chineseNameMap = if (useChinese) {
                        repository.getChineseNamesForCards(allCards.map { it.cardName })
                    } else {
                        emptyMap()
                    }

                    // 辅助函数：添加中文名
                    fun CardEntity.toCardWithZh() = Card(
                        id = id,
                        decklistId = decklistId,
                        cardName = cardName,
                        quantity = quantity,
                        location = if (location == "main") CardLocation.MAIN else CardLocation.SIDEBOARD,
                        cardOrder = cardOrder,
                        manaCost = manaCost,
                        rarity = rarity,
                        color = color,
                        cardType = cardType,
                        cardSet = cardSet,
                        cardNameZh = chineseNameMap[cardName]
                    )

                    // 分离主牌和备牌
                    val mainCards = allCards
                        .filter { it.location == "main" }
                        .map { it.toCardWithZh() }
                    _mainDeck.value = mainCards

                    val sideboardCards = allCards
                        .filter { it.location == "sideboard" }
                        .map { it.toCardWithZh() }
                    _sideboard.value = sideboardCards
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 查询单卡信息
     */
    fun loadCardInfo(cardName: String) {
        viewModelScope.launch {
            try {
                _isCardInfoLoading.value = true
                _cardInfoError.value = null

                val cardInfo = repository.getCardInfo(cardName)

                if (cardInfo != null) {
                    _cardInfo.value = cardInfo
                } else {
                    _cardInfoError.value = "Card '$cardName' not found"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _cardInfoError.value = "Failed to load card info: ${e.message}"
            } finally {
                _isCardInfoLoading.value = false
            }
        }
    }

    /**
     * 清除卡牌信息错误
     */
    fun clearCardInfoError() {
        _cardInfoError.value = null
    }

    /**
     * 清除卡牌信息
     */
    fun clearCardInfo() {
        _cardInfo.value = null
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(decklistId: Long): Boolean {
        return repository.toggleFavorite(decklistId)
    }

    /**
     * 检查是否已收藏
     */
    suspend fun isFavorite(decklistId: Long): Boolean {
        return repository.isFavorite(decklistId)
    }
}
