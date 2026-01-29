package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.FavoriteDecklistEntity

/**
 * Favorite Decklist DAO - 收藏牌组数据访问接口
 */
@Dao
interface FavoriteDecklistDao {

    /**
     * 插入收藏
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteDecklistEntity): Long

    /**
     * 删除收藏
     */
    @Delete
    suspend fun delete(favorite: FavoriteDecklistEntity)

    /**
     * 批量删除指定牌组的收藏
     */
    @Query("DELETE FROM favorites WHERE decklist_id IN (:decklistIds)")
    suspend fun deleteByDecklistIds(decklistIds: List<Long>)

    /**
     * 根据牌组ID获取收藏
     */
    @Query("SELECT * FROM favorites WHERE decklist_id = :decklistId LIMIT 1")
    suspend fun getByDecklistId(decklistId: Long): FavoriteDecklistEntity?

    /**
     * 获取所有收藏的牌组ID
     */
    @Query("SELECT decklist_id FROM favorites ORDER BY created_at DESC")
    suspend fun getAllDecklistIds(): List<Long>

    /**
     * 获取收藏数量
     */
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getCount(): Int

    /**
     * 删除所有收藏
     */
    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}
