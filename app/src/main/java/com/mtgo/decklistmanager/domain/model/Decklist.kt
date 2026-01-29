package com.mtgo.decklistmanager.domain.model

/**
 * Decklist - 牌组领域模型
 */
data class Decklist(
    val id: Long = 0,
    val eventName: String,
    val eventType: String?,
    val deckName: String?,  // 套牌名称
    val format: String,
    val date: String,
    val url: String,
    val playerName: String?,
    val playerId: String?,
    val record: String?,
    val isLoading: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
