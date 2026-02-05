package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.CardEntity

/**
 * Card DAO - 卡牌数据访问接口
 */
@Dao
interface CardDao {

    /**
     * 插入卡牌
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CardEntity): Long

    /**
     * 批量插入卡牌
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)

    /**
     * 删除指定牌组的所有卡牌
     */
    @Query("DELETE FROM cards WHERE decklist_id = :decklistId")
    suspend fun deleteByDecklistId(decklistId: Long)

    /**
     * 批量删除多个牌组的所有卡牌
     */
    @Query("DELETE FROM cards WHERE decklist_id IN (:decklistIds)")
    suspend fun deleteByDecklistIds(decklistIds: List<Long>)

    /**
     * 获取指定牌组的所有卡牌
     */
    @Query("SELECT * FROM cards WHERE decklist_id = :decklistId ORDER BY location, card_order")
    suspend fun getCardsByDecklistId(decklistId: Long): List<CardEntity>

    /**
     * 获取指定牌组的主牌
     */
    @Query("SELECT * FROM cards WHERE decklist_id = :decklistId AND location = 'main' ORDER BY card_order")
    suspend fun getMainDeckCards(decklistId: Long): List<CardEntity>

    /**
     * 获取指定牌组的备牌
     */
    @Query("SELECT * FROM cards WHERE decklist_id = :decklistId AND location = 'sideboard' ORDER BY card_order")
    suspend fun getSideboardCards(decklistId: Long): List<CardEntity>

    /**
     * 根据卡牌名称搜索牌组
     */
    @Query("""
        SELECT DISTINCT decklist_id
        FROM cards
        WHERE card_name LIKE '%' || :cardName || '%'
    """)
    suspend fun searchDecklistsByCardName(cardName: String): List<Long>

    /**
     * 获取卡牌总数
     */
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int

    /**
     * 删除所有卡牌
     */
    @Query("DELETE FROM cards")
    suspend fun clearAll()

    /**
     * 更新卡牌的法术力值和其他详情
     */
    @Query("""
        UPDATE cards
        SET mana_cost = :manaCost,
            color = :color,
            rarity = :rarity,
            card_type = :cardType,
            card_set = :cardSet,
            display_name = :displayName
        WHERE id = :cardId
    """)
    suspend fun updateDetails(
        cardId: Long,
        manaCost: String?,
        color: String?,
        rarity: String?,
        cardType: String?,
        cardSet: String?,
        displayName: String? = null
    )

    /**
     * 根据卡牌名称更新所有同名卡牌的法术力值和其他详情
     */
    @Query("""
        UPDATE cards
        SET mana_cost = :manaCost,
            color = :color,
            rarity = :rarity,
            card_type = :cardType,
            card_set = :cardSet
        WHERE card_name = :cardName
    """)
    suspend fun updateManaCostByName(
        cardName: String,
        manaCost: String?,
        color: String?,
        rarity: String?,
        cardType: String?,
        cardSet: String?
    )

    /**
     * 根据卡牌名称更新所有同名卡牌的显示名称（中文名）
     * v4.1.0: 确保所有套牌中的同名卡牌都能显示中文名
     */
    @Query("""
        UPDATE cards
        SET display_name = :displayName
        WHERE card_name = :cardName
          AND display_name IS NULL
    """)
    suspend fun updateDisplayNameByName(
        cardName: String,
        displayName: String
    )

    /**
     * 搜索卡牌（按名称）
     */
    @Query("""
        SELECT DISTINCT card_name, display_name, mana_cost, color
        FROM cards
        WHERE card_name LIKE '%' || :query || '%' OR display_name LIKE '%' || :query || '%'
        LIMIT :limit
    """)
    suspend fun searchCardsByName(query: String, limit: Int = 50): List<CardSearchResult>

    /**
     * 卡牌搜索结果
     */
    data class CardSearchResult(
        val card_name: String,
        val display_name: String?,
        val mana_cost: String?,
        val color: String?
    )

    /**
     * 获取所有 display_name 为 NULL 的卡牌
     * v4.1.0: 用于批量修复中文名称和法术力值
     */
    @Query("SELECT * FROM cards WHERE display_name IS NULL")
    suspend fun getCardsWithNullDisplayName(): List<CardEntity>

    /**
     * 根据 ID 更新单张卡牌的 display_name
     * v4.1.0: 用于批量修复中文名称
     */
    @Query("UPDATE cards SET display_name = :displayName WHERE id = :cardId")
    suspend fun updateDisplayNameById(cardId: Long, displayName: String)

    /**
     * 根据 ID 同时更新 display_name 和 mana_cost
     * v4.1.0: 用于批量修复缺失的数据
     */
    @Query("""
        UPDATE cards
        SET display_name = :displayName,
            mana_cost = :manaCost
        WHERE id = :cardId
    """)
    suspend fun updateCardDetails(cardId: Long, displayName: String, manaCost: String?)
}
