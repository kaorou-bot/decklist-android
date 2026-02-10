package com.mtgo.decklistmanager.data.repository

import com.mtgo.decklistmanager.data.local.dao.DecklistFolderRelationDao
import com.mtgo.decklistmanager.data.local.dao.DecklistNoteDao
import com.mtgo.decklistmanager.data.local.dao.DecklistTagRelationDao
import com.mtgo.decklistmanager.data.local.dao.FolderDao
import com.mtgo.decklistmanager.data.local.dao.TagDao
import com.mtgo.decklistmanager.data.local.entity.*
import com.mtgo.decklistmanager.domain.model.DecklistNote
import com.mtgo.decklistmanager.domain.model.Folder
import com.mtgo.decklistmanager.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FolderRepository - 文件夹仓库
 * 负责文件夹及其与套牌关联的业务逻辑
 */
@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val folderRelationDao: DecklistFolderRelationDao
) {

    /**
     * 获取所有文件夹及其套牌数量
     */
    suspend fun getAllFolders(): List<Folder> {
        val folders = folderDao.getAll()
        return folders.map { entity ->
            val count = folderRelationDao.getDecklistCountInFolder(entity.id)
            Folder(
                id = entity.id,
                name = entity.name,
                color = entity.color,
                icon = entity.icon,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                decklistCount = count
            )
        }
    }

    /**
     * 根据ID获取文件夹
     */
    suspend fun getFolderById(folderId: Long): Folder? {
        val entity = folderDao.getById(folderId) ?: return null
        val count = folderRelationDao.getDecklistCountInFolder(entity.id)
        return Folder(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            icon = entity.icon,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            decklistCount = count
        )
    }

    /**
     * 创建文件夹
     */
    suspend fun createFolder(name: String, color: Int, icon: String): Folder {
        val entity = FolderEntity(
            name = name,
            color = color,
            icon = icon
        )
        val id = folderDao.insert(entity)
        return Folder(
            id = id,
            name = name,
            color = color,
            icon = icon
        )
    }

    /**
     * 更新文件夹
     */
    suspend fun updateFolder(folder: Folder) {
        val entity = FolderEntity(
            id = folder.id,
            name = folder.name,
            color = folder.color,
            icon = folder.icon,
            createdAt = folder.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        folderDao.update(entity)
    }

    /**
     * 删除文件夹
     */
    suspend fun deleteFolder(folderId: Long) {
        folderDao.deleteById(folderId)
        // 关联关系会通过 CASCADE 自动删除
    }

    /**
     * 添加套牌到文件夹
     */
    suspend fun addDecklistToFolder(decklistId: Long, folderId: Long) {
        // 先检查是否已存在
        val existing = folderRelationDao.isDecklistInFolder(decklistId, folderId)
        if (existing > 0) return // 已存在，不需要重复添加

        val relation = DecklistFolderRelationEntity(
            decklistId = decklistId,
            folderId = folderId
        )
        folderRelationDao.insert(relation)
    }

    /**
     * 从文件夹移除套牌
     */
    suspend fun removeDecklistFromFolder(decklistId: Long, folderId: Long) {
        // 通过查询获取关系ID然后删除，或者直接使用 Query
        folderRelationDao.deleteByDecklistAndFolder(decklistId, folderId)
    }

    /**
     * 获取套牌所在的所有文件夹
     */
    suspend fun getFoldersForDecklist(decklistId: Long): List<Folder> {
        val folderIds = folderRelationDao.getFolderIdsByDecklistId(decklistId)
        return folderIds.mapNotNull { folderId ->
            getFolderById(folderId)
        }
    }

    /**
     * 获取文件夹内的所有套牌ID
     */
    suspend fun getDecklistIdsInFolder(folderId: Long): List<Long> {
        return folderRelationDao.getDecklistIdsByFolderId(folderId)
    }

    /**
     * 检查套牌是否在文件夹中
     */
    suspend fun isDecklistInFolder(decklistId: Long, folderId: Long): Boolean {
        return folderRelationDao.isDecklistInFolder(decklistId, folderId) > 0
    }
}
