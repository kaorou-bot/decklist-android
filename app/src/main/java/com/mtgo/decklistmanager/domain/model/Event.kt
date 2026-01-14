package com.mtgo.decklistmanager.domain.model

/**
 * Event - 赛事领域模型
 * 代表一个MTG比赛/赛事（如 MTGO Challenge 32）
 */
data class Event(
    val id: Long = 0,
    val eventName: String,
    val eventType: String?,
    val format: String,
    val date: String,
    val sourceUrl: String?,
    val source: String,
    val deckCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
