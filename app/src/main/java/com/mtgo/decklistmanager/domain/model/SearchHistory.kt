package com.mtgo.decklistmanager.domain.model

/**
 * 搜索历史
 */
data class SearchHistory(
    val id: Long,
    val query: String,
    val searchTime: Long,
    val resultCount: Int
)
