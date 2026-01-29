package com.mtgo.decklistmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mtgo.decklistmanager.data.local.entity.EventEntity

/**
 * Event DAO - 赛事数据访问对象
 */
@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>): List<Long>

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventEntity?

    @Query("SELECT * FROM events WHERE source_url = :url LIMIT 1")
    suspend fun getEventByUrl(url: String): EventEntity?

    @Query("SELECT * FROM events ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllEvents(limit: Int = 100, offset: Int = 0): List<EventEntity>

    @Query("""
        SELECT * FROM events
        WHERE (:format IS NULL OR format = :format)
        AND (:date IS NULL OR date = :date)
        ORDER BY date DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getEvents(
        format: String? = null,
        date: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<EventEntity>

    @Query("SELECT DISTINCT format FROM events ORDER BY format")
    suspend fun getAllFormats(): List<String>

    @Query("SELECT DISTINCT date FROM events ORDER BY date DESC")
    suspend fun getAllDates(): List<String>

    @Query("SELECT COUNT(*) FROM events")
    suspend fun getEventCount(): Int

    @Query("DELETE FROM events")
    suspend fun clearAll()

    @Query("UPDATE events SET deck_count = :count WHERE id = :eventId")
    suspend fun updateDeckCount(eventId: Long, count: Int)

    /**
     * 删除赛事及其所有关联的卡组和卡牌
     * 需要在事务中手动删除关联数据
     */
    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)

    /**
     * 根据赛事ID删除所有关联的卡组
     */
    @Query("DELETE FROM decklists WHERE event_id = :eventId")
    suspend fun deleteDecklistsByEventId(eventId: Long)
}
