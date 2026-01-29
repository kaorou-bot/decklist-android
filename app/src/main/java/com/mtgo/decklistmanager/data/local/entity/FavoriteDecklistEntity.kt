package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Favorite Decklist Entity - 收藏的牌组
 */
@Entity(
    tableName = "favorites",
    indices = [Index(value = ["decklist_id"])]
)
data class FavoriteDecklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "decklist_id")
    val decklistId: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
