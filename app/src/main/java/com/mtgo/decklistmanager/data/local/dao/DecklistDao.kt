package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.DecklistEntity

/**
 * Decklist DAO - 牌组数据访问接口
 */
@Dao
interface DecklistDao {

    /**
     * 插入单个牌组
     * 如果 URL 已存在，则返回已存在的 ID
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(decklist: DecklistEntity): Long

    /**
     * 插入多个牌组
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(decklists: List<DecklistEntity>): List<Long>

    /**
     * 更新牌组
     */
    @Update
    suspend fun update(decklist: DecklistEntity)

    /**
     * 删除牌组
     */
    @Delete
    suspend fun delete(decklist: DecklistEntity)

    /**
     * 根据 ID 获取牌组
     */
    @Query("SELECT * FROM decklists WHERE id = :decklistId")
    suspend fun getDecklistById(decklistId: Long): DecklistEntity?

    /**
     * 根据格式获取牌组列表
     */
    @Query("SELECT * FROM decklists WHERE format = :format ORDER BY date DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getDecklistsByFormat(
        format: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<DecklistEntity>

    /**
     * 根据日期获取牌组列表
     */
    @Query("SELECT * FROM decklists WHERE date = :date ORDER BY id DESC LIMIT :limit")
    suspend fun getDecklistsByDate(
        date: String,
        limit: Int = 100
    ): List<DecklistEntity>

    /**
     * 根据格式和日期获取牌组列表
     */
    @Query("""
        SELECT * FROM decklists
        WHERE (:format IS NULL OR format = :format)
        AND (:date IS NULL OR date = :date)
        ORDER BY date DESC, id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getDecklists(
        format: String? = null,
        date: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<DecklistEntity>

    /**
     * 获取所有格式
     */
    @Query("SELECT DISTINCT format FROM decklists ORDER BY format")
    suspend fun getAllFormats(): List<String>

    /**
     * 获取所有日期
     */
    @Query("SELECT DISTINCT date FROM decklists ORDER BY date DESC")
    suspend fun getAllDates(): List<String>

    /**
     * 获取所有事件类型
     */
    @Query("SELECT DISTINCT event_type FROM decklists WHERE event_type IS NOT NULL AND event_type != '' ORDER BY event_type")
    suspend fun getAllEventTypes(): List<String>

    /**
     * 获取统计信息
     */
    @Query("SELECT COUNT(*) FROM decklists")
    suspend fun getDecklistCount(): Int

    /**
     * 删除所有牌组
     */
    @Query("DELETE FROM decklists")
    suspend fun clearAll()

    /**
     * 根据 URL 查询牌组 ID
     */
    @Query("SELECT id FROM decklists WHERE url = :url LIMIT 1")
    suspend fun getDecklistIdByUrl(url: String): Long?

    /**
     * 根据 URL 查询完整牌组信息
     */
    @Query("SELECT * FROM decklists WHERE url = :url LIMIT 1")
    suspend fun getDecklistByUrl(url: String): DecklistEntity?

    /**
     * 根据赛事ID获取该赛事的所有牌组
     */
    @Query("SELECT * FROM decklists WHERE event_id = :eventId ORDER BY id")
    suspend fun getDecklistsByEventId(eventId: Long): List<DecklistEntity>

    /**
     * 根据赛事ID获取该赛事的所有牌组ID
     */
    @Query("SELECT id FROM decklists WHERE event_id = :eventId ORDER BY id")
    suspend fun getDecklistIdsByEventId(eventId: Long): List<Long>

    /**
     * 获取有事件关联的牌组（新数据）
     */
    @Query("""
        SELECT * FROM decklists
        WHERE event_id IS NOT NULL
        AND (:format IS NULL OR format = :format)
        AND (:date IS NULL OR date = :date)
        ORDER BY date DESC, id DESC
        LIMIT :limit
    """)
    suspend fun getDecklistsWithEvent(
        format: String? = null,
        date: String? = null,
        limit: Int = 100
    ): List<DecklistEntity>

    /**
     * 获取没有事件关联的牌组（旧数据）
     */
    @Query("""
        SELECT * FROM decklists
        WHERE event_id IS NULL
        AND (:format IS NULL OR format = :format)
        AND (:date IS NULL OR date = :date)
        ORDER BY date DESC, id DESC
        LIMIT :limit
    """)
    suspend fun getDecklistsWithoutEvent(
        format: String? = null,
        date: String? = null,
        limit: Int = 100
    ): List<DecklistEntity>

    /**
     * 根据ID列表获取牌组
     */
    @Query("SELECT * FROM decklists WHERE id IN (:ids)")
    suspend fun getDecklistsByIds(ids: List<Long>): List<DecklistEntity>
}
