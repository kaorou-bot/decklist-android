package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity

/**
 * CardInfo DAO - 单卡信息缓存数据访问接口
 */
@Dao
interface CardInfoDao {

    /**
     * 插入或更新单卡信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cardInfo: CardInfoEntity)

    /**
     * 批量插入或更新
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(cards: List<CardInfoEntity>)

    /**
     * 根据 Scryfall ID 获取卡牌信息
     */
    @Query("SELECT * FROM card_info WHERE id = :cardId")
    suspend fun getCardInfoById(cardId: String): CardInfoEntity?

    /**
     * 根据卡牌名称获取信息
     */
    @Query("SELECT * FROM card_info WHERE name = :name LIMIT 1")
    suspend fun getCardInfoByName(name: String): CardInfoEntity?

    /**
     * 根据英文名称获取信息
     */
    @Query("SELECT * FROM card_info WHERE en_name = :enName LIMIT 1")
    suspend fun getCardInfoByEnName(enName: String): CardInfoEntity?

    /**
     * 根据卡牌名称或英文名称获取信息
     */
    @Query("SELECT * FROM card_info WHERE name = :name OR en_name = :name LIMIT 1")
    suspend fun getCardInfoByNameOrEnName(name: String): CardInfoEntity?

    /**
     * 模糊搜索卡牌
     */
    @Query("SELECT * FROM card_info WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchCardsByName(query: String, limit: Int = 20): List<CardInfoEntity>

    /**
     * 获取所有缓存的卡牌数量
     */
    @Query("SELECT COUNT(*) FROM card_info")
    suspend fun getCachedCardCount(): Int

    /**
     * 获取所有缓存卡牌
     */
    @Query("SELECT * FROM card_info ORDER BY name LIMIT :limit OFFSET :offset")
    suspend fun getAllCachedCards(limit: Int = 100, offset: Int = 0): List<CardInfoEntity>

    /**
     * 删除单个卡牌信息
     */
    @Delete
    suspend fun delete(cardInfo: CardInfoEntity)

    /**
     * 清空所有缓存
     */
    @Query("DELETE FROM card_info")
    suspend fun clearAll()

    /**
     * 清理过期缓存（超过指定天数未更新）
     */
    @Query("DELETE FROM card_info WHERE last_updated < :expireTimestamp")
    suspend fun clearExpiredCache(expireTimestamp: Long): Int

    /**
     * 获取所有可能是双面牌的卡牌（名称包含 " // "）
     */
    @Query("SELECT * FROM card_info WHERE name LIKE '% // %' AND is_dual_faced = 0")
    suspend fun getPossibleDualFacedCards(): List<CardInfoEntity>

    /**
     * 批量更新卡牌信息
     */
    @Update
    suspend fun updateAll(cards: List<CardInfoEntity>)
}
