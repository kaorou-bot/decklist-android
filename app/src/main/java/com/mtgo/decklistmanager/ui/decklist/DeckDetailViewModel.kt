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
        cardSet = cardSet,
        cardNameZh = displayName  // v4.0.0: Map displayName to cardNameZh
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

                    // v4.0.0: 确保卡牌详情已获取后再加载
                    // 先触发 fetchScryfallDetails，它会异步更新数据库
                    repository.ensureCardDetails(decklistId)

                    // 等待一小段时间让数据更新
                    kotlinx.coroutines.delay(500)

                    // 加载所有卡牌
                    val allCards = repository.getCardsByDecklistId(decklistId)

                    // v4.0.0: displayName is already populated in the database, so just convert directly
                    // 分离主牌和备牌
                    val mainCards = allCards
                        .filter { it.location == "main" }
                        .map { it.toCard() }
                    _mainDeck.value = mainCards

                    val sideboardCards = allCards
                        .filter { it.location == "sideboard" }
                        .map { it.toCard() }
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
     * 查询单卡信息（带重试和错误提示）
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
                    // v4.0.0: 提供更友好的错误提示
                    _cardInfoError.value = "未找到卡牌: $cardName\n\n提示：\n" +
                        "• 请检查卡牌名称拼写\n" +
                        "• 某些特殊卡牌可能需要完整名称\n" +
                        "• 系统已自动重试3次，请稍后再试"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _cardInfoError.value = "加载失败: ${e.message}\n\n请检查网络连接后重试"
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
