package com.mtgo.decklistmanager.domain.model

/**
 * Card - 卡牌领域模型
 */
data class Card(
    val id: Long = 0,
    val decklistId: Long,
    val cardName: String,
    val quantity: Int,
    val location: CardLocation,
    val cardOrder: Int = 0,
    val manaCost: String?,
    val rarity: String?,
    val color: String?,
    val cardType: String?,
    val cardSet: String?,
    val cardNameZh: String? = null  // 中文名称，用于显示
)

/**
 * 卡牌位置枚举
 */
enum class CardLocation {
    MAIN,
    SIDEBOARD
}
