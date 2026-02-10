package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tag Entity - 标签
 * 用于标记套牌
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: Int = 0xFF6200EE.toInt(), // 默认紫色

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
