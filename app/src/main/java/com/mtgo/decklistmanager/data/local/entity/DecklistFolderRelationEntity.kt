package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DecklistFolderRelation Entity - 套牌与文件夹关联表
 * 多对多关系：一个套牌可以在多个文件夹，一个文件夹可以有多个套牌
 */
@Entity(
    tableName = "decklist_folder_relations",
    indices = [Index(value = ["decklist_id"]), Index(value = ["folder_id"])],
    foreignKeys = [
        ForeignKey(
            entity = DecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["decklist_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DecklistFolderRelationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "decklist_id")
    val decklistId: Long,

    @ColumnInfo(name = "folder_id")
    val folderId: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
