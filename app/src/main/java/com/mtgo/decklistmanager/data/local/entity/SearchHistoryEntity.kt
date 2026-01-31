package com.mtgo.decklistmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 搜索历史实体
 *
 * @property id 主键
 * @property query 搜索关键词
 * @property searchTime 搜索时间戳
 * @property resultCount 结果数量
 */
@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["searchTime"], name = "index_search_time"),
        Index(value = ["query"], name = "index_query")
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val query: String,

    val searchTime: Long,

    val resultCount: Int = 0
)
