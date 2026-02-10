package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.DecklistNoteEntity

/**
 * DecklistNote DAO - 套牌备注数据访问接口
 */
@Dao
interface DecklistNoteDao {

    /**
     * 插入备注
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: DecklistNoteEntity): Long

    /**
     * 更新备注
     */
    @Update
    suspend fun update(note: DecklistNoteEntity)

    /**
     * 删除备注
     */
    @Delete
    suspend fun delete(note: DecklistNoteEntity)

    /**
     * 根据套牌 ID 删除备注
     */
    @Query("DELETE FROM decklist_notes WHERE decklist_id = :decklistId")
    suspend fun deleteByDecklistId(decklistId: Long)

    /**
     * 根据套牌 ID 获取备注
     */
    @Query("SELECT * FROM decklist_notes WHERE decklist_id = :decklistId LIMIT 1")
    suspend fun getByDecklistId(decklistId: Long): DecklistNoteEntity?

    /**
     * 获取所有备注
     */
    @Query("SELECT * FROM decklist_notes ORDER BY updated_at DESC")
    suspend fun getAll(): List<DecklistNoteEntity>

    /**
     * 获取备注数量
     */
    @Query("SELECT COUNT(*) FROM decklist_notes")
    suspend fun getCount(): Int

    /**
     * 搜索备注内容
     */
    @Query("SELECT * FROM decklist_notes WHERE note LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    suspend fun searchByContent(query: String): List<DecklistNoteEntity>
}
