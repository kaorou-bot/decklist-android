package com.mtgo.decklistmanager.ui.comparison.model

/**
 * ComparisonResult - 对比结果
 */
data class ComparisonResult(
    val statComparisons: List<StatComparison>,
    val cardComparisons: List<CardComparison>
) {
    companion object {
        val Empty = ComparisonResult(emptyList(), emptyList())
    }
}

/**
 * StatComparison - 统计对比
 */
data class StatComparison(
    val statName: String,
    val value1: Int,
    val value2: Int
) {
    val difference: Int get() = value2 - value1
}

/**
 * CardComparison - 卡牌对比
 */
data class CardComparison(
    val cardName: String,
    val count1: Int,
    val count2: Int,
    val difference: Int
) {
    val onlyInDeck1: Boolean get() = count1 > 0 && count2 == 0
    val onlyInDeck2: Boolean get() = count2 > 0 && count1 == 0
    val differentCount: Boolean get() = count1 > 0 && count2 > 0 && count1 != count2
    val sameCount: Boolean get() = count1 > 0 && count2 > 0 && count1 == count2
}
