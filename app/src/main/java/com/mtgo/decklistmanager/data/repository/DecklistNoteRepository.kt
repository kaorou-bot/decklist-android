package com.mtgo.decklistmanager.data.repository

import com.mtgo.decklistmanager.data.local.dao.DecklistNoteDao
import com.mtgo.decklistmanager.data.local.entity.DecklistNoteEntity
import com.mtgo.decklistmanager.domain.model.DecklistNote
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DecklistNoteRepository - 套牌备注仓库
 */
@Singleton
class DecklistNoteRepository @Inject constructor(
    private val noteDao: DecklistNoteDao
) {

    /**
     * 根据套牌ID获取备注
     */
    suspend fun getNoteByDecklistId(decklistId: Long): DecklistNote? {
        val entity = noteDao.getByDecklistId(decklistId)
        return entity?.let { toDomainModel(it) }
    }

    /**
     * 保存或更新备注
     */
    suspend fun saveNote(decklistId: Long, note: String): DecklistNote {
        val existing = noteDao.getByDecklistId(decklistId)

        return if (existing != null) {
            // 更新现有备注
            val updated = existing.copy(
                note = note,
                updatedAt = System.currentTimeMillis()
            )
            noteDao.update(updated)
            toDomainModel(updated)
        } else {
            // 创建新备注
            val newNote = DecklistNoteEntity(
                decklistId = decklistId,
                note = note
            )
            val id = noteDao.insert(newNote)
            toDomainModel(newNote.copy(id = id))
        }
    }

    /**
     * 删除备注
     */
    suspend fun deleteNote(decklistId: Long) {
        noteDao.deleteByDecklistId(decklistId)
    }

    /**
     * 获取所有备注
     */
    suspend fun getAllNotes(): List<DecklistNote> {
        return noteDao.getAll().map { toDomainModel(it) }
    }

    /**
     * 搜索备注内容
     */
    suspend fun searchNotes(query: String): List<DecklistNote> {
        return noteDao.searchByContent(query).map { toDomainModel(it) }
    }

    /**
     * 获取备注数量
     */
    suspend fun getNoteCount(): Int {
        return noteDao.getCount()
    }

    private fun toDomainModel(entity: DecklistNoteEntity): DecklistNote {
        return DecklistNote(
            id = entity.id,
            decklistId = entity.decklistId,
            note = entity.note,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
