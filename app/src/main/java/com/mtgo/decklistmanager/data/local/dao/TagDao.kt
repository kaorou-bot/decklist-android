package com.mtgo.decklistmanager.data.local.dao

import androidx.room.*
import com.mtgo.decklistmanager.data.local.entity.TagEntity

/**
 * Tag DAO - 标签数据访问接口
 */
@Dao
interface TagDao {

    /**
     * 插入标签
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    /**
     * 更新标签
     */
    @Update
    suspend fun update(tag: TagEntity)

    /**
     * 删除标签
     */
    @Delete
    suspend fun delete(tag: TagEntity)

    /**
     * 根据 ID 删除标签
     */
    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteById(tagId: Long)

    /**
     * 获取所有标签
     */
    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAll(): List<TagEntity>

    /**
     * 根据 ID 获取标签
     */
    @Query("SELECT * FROM tags WHERE id = :tagId LIMIT 1")
    suspend fun getById(tagId: Long): TagEntity?

    /**
     * 根据名称获取标签
     */
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    /**
     * 根据名称搜索标签
     */
    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<TagEntity>

    /**
     * 获取标签数量
     */
    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getCount(): Int

    /**
     * 批量插入标签，返回已存在的和新插入的标签ID
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<TagEntity>): List<Long>
}
