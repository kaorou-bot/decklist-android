package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Folder Entity - 收藏夹文件夹
 * 用于分类收藏的套牌
 */
@Entity(
    tableName = "folders",
    indices = [Index(value = ["name"])]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: Int = 0xFF6200EE.toInt(), // 默认紫色

    @ColumnInfo(name = "icon")
    val icon: String = "folder", // folder, star, heart, etc.

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
