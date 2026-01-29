package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Event Entity - 赛事实体
 *
 * 代表一个MTG比赛/赛事（如 MTGO Challenge 32）
 * 一个赛事包含多个卡组
 */
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["event_name"]),
        Index(value = ["date"]),
        Index(value = ["format"]),
        Index(value = ["source"])
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "event_name")
    val eventName: String,

    @ColumnInfo(name = "event_type")
    val eventType: String?,  // MTGO Challenge, League, MTGTop8 Tournament, etc.

    val format: String,  // Modern, Standard, Legacy, etc.

    val date: String,  // YYYY-MM-DD format

    @ColumnInfo(name = "source_url")
    val sourceUrl: String?,  // URL of the event page

    @ColumnInfo(name = "source")
    val source: String,  // "MTGTop8", "MTGO", etc.

    @ColumnInfo(name = "deck_count")
    val deckCount: Int = 0,  // Number of decks in this event

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
