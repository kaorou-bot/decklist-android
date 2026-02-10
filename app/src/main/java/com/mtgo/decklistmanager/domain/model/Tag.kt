package com.mtgo.decklistmanager.domain.model

/**
 * Tag - 标签领域模型
 */
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF6200EE.toInt(),
    val createdAt: Long = System.currentTimeMillis(),
    val decklistCount: Int = 0 // 使用此标签的套牌数量
)
