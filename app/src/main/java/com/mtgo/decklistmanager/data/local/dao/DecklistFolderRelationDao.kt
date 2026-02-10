package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.DecklistFolderRelationEntity

/**
 * DecklistFolderRelation DAO - 套牌文件夹关联数据访问接口
 */
@Dao
interface DecklistFolderRelationDao {

    /**
     * 插入关联
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(relation: DecklistFolderRelationEntity): Long

    /**
     * 批量插入关联
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(relations: List<DecklistFolderRelationEntity>)

    /**
     * 删除关联
     */
    @Delete
    suspend fun delete(relation: DecklistFolderRelationEntity)

    /**
     * 根据套牌和文件夹删除关联
     */
    @Query("DELETE FROM decklist_folder_relations WHERE decklist_id = :decklistId AND folder_id = :folderId")
    suspend fun deleteByDecklistAndFolder(decklistId: Long, folderId: Long)

    /**
     * 根据套牌 ID 删除所有关联
     */
    @Query("DELETE FROM decklist_folder_relations WHERE decklist_id = :decklistId")
    suspend fun deleteByDecklistId(decklistId: Long)

    /**
     * 根据文件夹 ID 删除所有关联
     */
    @Query("DELETE FROM decklist_folder_relations WHERE folder_id = :folderId")
    suspend fun deleteByFolderId(folderId: Long)

    /**
     * 获取套牌所在的所有文件夹 ID
     */
    @Query("SELECT folder_id FROM decklist_folder_relations WHERE decklist_id = :decklistId")
    suspend fun getFolderIdsByDecklistId(decklistId: Long): List<Long>

    /**
     * 获取文件夹内的所有套牌 ID
     */
    @Query("SELECT decklist_id FROM decklist_folder_relations WHERE folder_id = :folderId")
    suspend fun getDecklistIdsByFolderId(folderId: Long): List<Long>

    /**
     * 检查套牌是否在文件夹中
     */
    @Query("SELECT COUNT(*) FROM decklist_folder_relations WHERE decklist_id = :decklistId AND folder_id = :folderId")
    suspend fun isDecklistInFolder(decklistId: Long, folderId: Long): Int

    /**
     * 获取文件夹内的套牌数量
     */
    @Query("SELECT COUNT(*) FROM decklist_folder_relations WHERE folder_id = :folderId")
    suspend fun getDecklistCountInFolder(folderId: Long): Int
}
