package com.mtgo.decklistmanager.domain.model

/**
 * DecklistNote - 套牌备注领域模型
 */
data class DecklistNote(
    val id: Long = 0,
    val decklistId: Long,
    val note: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
