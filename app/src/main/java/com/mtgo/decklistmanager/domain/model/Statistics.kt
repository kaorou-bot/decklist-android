package com.mtgo.decklistmanager.domain.model

/**
 * Statistics - 统计信息模型
 */
data class Statistics(
    val totalDecklists: Int,
    val totalCards: Int,
    val totalFormats: Int,
    val cachedCards: Int
)
