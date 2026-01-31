package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 搜索历史 DAO
 */
@Dao
interface SearchHistoryDao {

    /**
     * 插入搜索历史
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistoryEntity): Long

    /**
     * 删除搜索历史
     */
    @Delete
    suspend fun delete(searchHistory: SearchHistoryEntity)

    /**
     * 清空所有搜索历史
     */
    @Query("DELETE FROM search_history")
    suspend fun clearAll()

    /**
     * 删除指定搜索历史
     */
    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取指定ID的搜索历史
     */
    @Query("SELECT * FROM search_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SearchHistoryEntity?

    /**
     * 获取所有搜索历史（按时间倒序）
     */
    @Query("SELECT * FROM search_history ORDER BY searchTime DESC")
    fun getAllSearchHistory(): Flow<List<SearchHistoryEntity>>

    /**
     * 获取最近的搜索历史（限制数量）
     */
    @Query("SELECT * FROM search_history ORDER BY searchTime DESC LIMIT :limit")
    fun getRecentSearchHistory(limit: Int = 10): Flow<List<SearchHistoryEntity>>

    /**
     * 搜索包含关键词的历史记录
     */
    @Query("SELECT * FROM search_history WHERE query LIKE '%' || :keyword || '%' ORDER BY searchTime DESC LIMIT :limit")
    fun searchByKeyword(keyword: String, limit: Int = 10): Flow<List<SearchHistoryEntity>>

    /**
     * 获取搜索历史数量
     */
    @Query("SELECT COUNT(*) FROM search_history")
    fun getSearchHistoryCount(): Flow<Int>

    /**
     * 删除旧的搜索历史（保留最近的 N 条）
     */
    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY searchTime DESC LIMIT :limit)")
    suspend fun deleteOldSearchHistory(limit: Int = 50)
}
