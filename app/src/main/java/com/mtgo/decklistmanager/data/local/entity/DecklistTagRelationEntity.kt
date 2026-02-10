package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DecklistTagRelation Entity - 套牌与标签关联表
 * 多对多关系
 */
@Entity(
    tableName = "decklist_tag_relations",
    indices = [Index(value = ["decklist_id"]), Index(value = ["tag_id"])]
)
data class DecklistTagRelationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "decklist_id")
    val decklistId: Long,

    @ColumnInfo(name = "tag_id")
    val tagId: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
