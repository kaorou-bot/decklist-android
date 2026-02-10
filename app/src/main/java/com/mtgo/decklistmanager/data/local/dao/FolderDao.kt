package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.FolderEntity

/**
 * Folder DAO - 文件夹数据访问接口
 */
@Dao
interface FolderDao {

    /**
     * 插入文件夹
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    /**
     * 更新文件夹
     */
    @Update
    suspend fun update(folder: FolderEntity)

    /**
     * 删除文件夹
     */
    @Delete
    suspend fun delete(folder: FolderEntity)

    /**
     * 根据 ID 删除文件夹
     */
    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteById(folderId: Long)

    /**
     * 获取所有文件夹
     */
    @Query("SELECT * FROM folders ORDER BY updated_at DESC")
    suspend fun getAll(): List<FolderEntity>

    /**
     * 根据 ID 获取文件夹
     */
    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getById(folderId: Long): FolderEntity?

    /**
     * 根据名称搜索文件夹
     */
    @Query("SELECT * FROM folders WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<FolderEntity>

    /**
     * 获取文件夹数量
     */
    @Query("SELECT COUNT(*) FROM folders")
    suspend fun getCount(): Int
}
