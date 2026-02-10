package com.mtgo.decklistmanager.data.repository

import com.mtgo.decklistmanager.data.local.dao.TagDao
import com.mtgo.decklistmanager.data.local.dao.DecklistTagRelationDao
import com.mtgo.decklistmanager.data.local.entity.TagEntity
import com.mtgo.decklistmanager.data.local.entity.DecklistTagRelationEntity
import com.mtgo.decklistmanager.domain.model.Tag
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TagRepository - 标签仓库
 * 负责标签及其与套牌关联的业务逻辑
 */
@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val tagRelationDao: DecklistTagRelationDao
) {

    /**
     * 获取所有标签
     */
    suspend fun getAllTags(): List<Tag> {
        val tags = tagDao.getAll()
        return tags.map { entity ->
            val count = tagRelationDao.getDecklistCountByTag(entity.id)
            Tag(
                id = entity.id,
                name = entity.name,
                color = entity.color,
                createdAt = entity.createdAt,
                decklistCount = count
            )
        }
    }

    /**
     * 根据ID获取标签
     */
    suspend fun getTagById(tagId: Long): Tag? {
        val entity = tagDao.getById(tagId) ?: return null
        val count = tagRelationDao.getDecklistCountByTag(entity.id)
        return Tag(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            createdAt = entity.createdAt,
            decklistCount = count
        )
    }

    /**
     * 根据名称获取或创建标签
     */
    suspend fun getOrCreateTag(name: String, color: Int = 0xFF6200EE.toInt()): Tag {
        val existing = tagDao.getByName(name)
        if (existing != null) {
            return getTagById(existing.id)!!
        }

        val entity = TagEntity(
            name = name,
            color = color
        )
        val id = tagDao.insert(entity)
        return Tag(
            id = id,
            name = name,
            color = color
        )
    }

    /**
     * 创建标签
     */
    suspend fun createTag(name: String, color: Int): Tag {
        val entity = TagEntity(
            name = name,
            color = color
        )
        val id = tagDao.insert(entity)
        return Tag(
            id = id,
            name = name,
            color = color
        )
    }

    /**
     * 更新标签
     */
    suspend fun updateTag(tag: Tag) {
        val entity = TagEntity(
            id = tag.id,
            name = tag.name,
            color = tag.color,
            createdAt = tag.createdAt
        )
        tagDao.update(entity)
    }

    /**
     * 删除标签
     */
    suspend fun deleteTag(tagId: Long) {
        tagDao.deleteById(tagId)
        // 关联关系需要手动删除（因为没有 CASCADE）
        tagRelationDao.deleteByTagId(tagId)
    }

    /**
     * 为套牌添加标签
     */
    suspend fun addTagToDecklist(decklistId: Long, tagId: Long) {
        val relation = DecklistTagRelationEntity(
            decklistId = decklistId,
            tagId = tagId
        )
        tagRelationDao.insert(relation)
    }

    /**
     * 从套牌移除标签
     */
    suspend fun removeTagFromDecklist(decklistId: Long, tagId: Long) {
        val relation = DecklistTagRelationEntity(
            decklistId = decklistId,
            tagId = tagId
        )
        tagRelationDao.delete(relation)
    }

    /**
     * 获取套牌的所有标签
     */
    suspend fun getTagsForDecklist(decklistId: Long): List<Tag> {
        val tagIds = tagRelationDao.getTagIdsByDecklistId(decklistId)
        return tagIds.mapNotNull { tagId ->
            getTagById(tagId)
        }
    }

    /**
     * 获取标签下的所有套牌ID
     */
    suspend fun getDecklistIdsByTag(tagId: Long): List<Long> {
        return tagRelationDao.getDecklistIdsByTagId(tagId)
    }

    /**
     * 检查套牌是否有某标签
     */
    suspend fun isDecklistHasTag(decklistId: Long, tagId: Long): Boolean {
        return tagRelationDao.isDecklistHasTag(decklistId, tagId) > 0
    }

    /**
     * 为套牌设置标签（替换所有现有标签）
     */
    suspend fun setTagsForDecklist(decklistId: Long, tagNames: List<String>) {
        // 删除现有标签关联
        tagRelationDao.deleteByDecklistId(decklistId)

        // 添加新标签关联
        tagNames.forEach { name ->
            val tag = getOrCreateTag(name)
            addTagToDecklist(decklistId, tag.id)
        }
    }
}
