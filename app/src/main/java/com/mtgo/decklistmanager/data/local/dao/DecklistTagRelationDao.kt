package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.DecklistTagRelationEntity

/**
 * DecklistTagRelation DAO - 套牌标签关联数据访问接口
 */
@Dao
interface DecklistTagRelationDao {

    /**
     * 插入关联
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(relation: DecklistTagRelationEntity): Long

    /**
     * 批量插入关联
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(relations: List<DecklistTagRelationEntity>)

    /**
     * 删除关联
     */
    @Delete
    suspend fun delete(relation: DecklistTagRelationEntity)

    /**
     * 根据套牌 ID 删除所有关联
     */
    @Query("DELETE FROM decklist_tag_relations WHERE decklist_id = :decklistId")
    suspend fun deleteByDecklistId(decklistId: Long)

    /**
     * 根据标签 ID 删除所有关联
     */
    @Query("DELETE FROM decklist_tag_relations WHERE tag_id = :tagId")
    suspend fun deleteByTagId(tagId: Long)

    /**
     * 获取套牌的所有标签 ID
     */
    @Query("SELECT tag_id FROM decklist_tag_relations WHERE decklist_id = :decklistId")
    suspend fun getTagIdsByDecklistId(decklistId: Long): List<Long>

    /**
     * 获取标签下的所有套牌 ID
     */
    @Query("SELECT decklist_id FROM decklist_tag_relations WHERE tag_id = :tagId")
    suspend fun getDecklistIdsByTagId(tagId: Long): List<Long>

    /**
     * 检查套牌是否有某标签
     */
    @Query("SELECT COUNT(*) FROM decklist_tag_relations WHERE decklist_id = :decklistId AND tag_id = :tagId")
    suspend fun isDecklistHasTag(decklistId: Long, tagId: Long): Int

    /**
     * 获取标签下的套牌数量
     */
    @Query("SELECT COUNT(*) FROM decklist_tag_relations WHERE tag_id = :tagId")
    suspend fun getDecklistCountByTag(tagId: Long): Int
}
