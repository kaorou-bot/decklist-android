package com.mtgo.decklistmanager.ui.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.data.local.entity.CardEntity
import com.mtgo.decklistmanager.data.repository.DecklistRepository
import com.mtgo.decklistmanager.ui.comparison.model.CardComparison
import com.mtgo.decklistmanager.ui.comparison.model.ComparisonResult
import com.mtgo.decklistmanager.ui.comparison.model.StatComparison
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DeckComparisonViewModel - 套牌对比 ViewModel
 */
@HiltViewModel
class DeckComparisonViewModel @Inject constructor(
    private val decklistRepository: DecklistRepository
) : ViewModel() {

    private val _comparisonResult = MutableStateFlow<ComparisonResult>(ComparisonResult.Empty)
    val comparisonResult: StateFlow<ComparisonResult> = _comparisonResult.asStateFlow()

    /**
     * 对比两个套牌
     */
    fun compareDecks(decklistId1: Long, decklistId2: Long) {
        viewModelScope.launch {
            try {
                // 获取两个套牌的卡牌
                val cards1 = decklistRepository.getCardsByDecklistId(decklistId1)
                    .filter { it.location == "main" }
                val cards2 = decklistRepository.getCardsByDecklistId(decklistId2)
                    .filter { it.location == "main" }

                // 创建卡牌名称到数量的映射
                val deck1Map = cards1.groupBy { it.cardName }.mapValues { it.value.sumOf { c -> c.quantity } }
                val deck2Map = cards2.groupBy { it.cardName }.mapValues { it.value.sumOf { c -> c.quantity } }

                // 找出所有独特的卡牌名称
                val allCardNames = (deck1Map.keys + deck2Map.keys).sorted()

                // 创建卡牌对比列表
                val cardComparisons = allCardNames.map { cardName ->
                    val count1 = deck1Map[cardName] ?: 0
                    val count2 = deck2Map[cardName] ?: 0
                    CardComparison(
                        cardName = cardName,
                        count1 = count1,
                        count2 = count2,
                        difference = count2 - count1
                    )
                }

                // 创建统计对比
                val statComparisons = listOf(
                    StatComparison("总卡牌数", cards1.sumOf { it.quantity }, cards2.sumOf { it.quantity }),
                    StatComparison("独特卡牌数", deck1Map.size, deck2Map.size),
                    StatComparison("非地平均法术力", calculateAverageManaValue(cards1).toInt(), calculateAverageManaValue(cards2).toInt())
                )

                _comparisonResult.value = ComparisonResult(
                    statComparisons = statComparisons,
                    cardComparisons = cardComparisons
                )
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    /**
     * 计算平均法术力值
     */
    private fun calculateAverageManaValue(cards: List<CardEntity>): Double {
        val nonLandCards = cards.filter { !it.cardType.orEmpty().contains("Land", ignoreCase = true) }
        if (nonLandCards.isEmpty()) return 0.0

        var totalManaValue = 0.0
        nonLandCards.forEach { card ->
            // 从法术力成本中提取数值（简单实现：计算数字和X的数量）
            val manaCost = card.manaCost ?: ""
            totalManaValue += parseManaCost(manaCost)
        }

        return totalManaValue / nonLandCards.size
    }

    /**
     * 解析法术力成本
     */
    private fun parseManaCost(manaCost: String): Double {
        // 移除花色符号 {}
        val cleanCost = manaCost.replace(Regex("\\{.*?\\}"), "")
        // 计算通用法术力
        val numbers = Regex("\\d+").findAll(cleanCost).map { it.value.toInt() }.sumOf { it.toDouble() }
        // 计算有色符号
        val symbols = cleanCost.replace(Regex("\\d"), "").length.toDouble()
        return numbers + symbols
    }
}
