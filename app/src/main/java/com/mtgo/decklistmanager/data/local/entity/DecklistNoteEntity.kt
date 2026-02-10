package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DecklistNote Entity - 套牌备注
 */
@Entity(
    tableName = "decklist_notes",
    indices = [Index(value = ["decklist_id"], unique = true)]
)
data class DecklistNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "decklist_id")
    val decklistId: Long,

    @ColumnInfo(name = "note")
    val note: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
