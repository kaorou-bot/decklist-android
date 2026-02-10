package com.mtgo.decklistmanager.domain.model

/**
 * Folder - 收藏夹文件夹领域模型
 */
data class Folder(
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF6200EE.toInt(),
    val icon: String = "folder",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val decklistCount: Int = 0 // 文件夹内的套牌数量
)
