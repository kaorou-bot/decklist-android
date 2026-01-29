package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Decklist Entity - 牌组表
 */
@Entity(
    tableName = "decklists",
    indices = [
        Index(value = ["format"]),
        Index(value = ["date"]),
        Index(value = ["event_type"])
    ]
)
data class DecklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "event_name")
    @SerializedName("event_name")
    val eventName: String,

    @ColumnInfo(name = "event_type")
    @SerializedName("event_type")
    val eventType: String?,

    @ColumnInfo(name = "deck_name")
    @SerializedName("deck_name")
    val deckName: String?,  // 套牌名称

    val format: String,

    val date: String, // Format: YYYY-MM-DD

    val url: String,

    @ColumnInfo(name = "player_name")
    @SerializedName("player_name")
    val playerName: String?,

    @ColumnInfo(name = "player_id")
    @SerializedName("player_id")
    val playerId: String?,

    val record: String?,

    @ColumnInfo(name = "event_id")
    val eventId: Long?,  // 外键，关联到 EventEntity.id

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)
