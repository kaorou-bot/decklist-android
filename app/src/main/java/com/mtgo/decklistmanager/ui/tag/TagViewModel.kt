package com.mtgo.decklistmanager.ui.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.Tag
import com.mtgo.decklistmanager.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TagViewModel - 标签管理 ViewModel
 */
@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TagUiState>(TagUiState.Loading)
    val uiState: StateFlow<TagUiState> = _uiState.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    /**
     * 加载所有标签
     */
    fun loadTags() {
        viewModelScope.launch {
            _uiState.value = TagUiState.Loading
            try {
                val result = tagRepository.getAllTags()
                _tags.value = result
                _uiState.value = TagUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = TagUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 创建新标签
     */
    suspend fun createTag(name: String, color: Int = 0xFF6200EE.toInt()): Tag {
        val tag = tagRepository.createTag(name, color)
        loadTags() // 重新加载列表
        return tag
    }

    /**
     * 更新标签
     */
    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            try {
                tagRepository.updateTag(tag)
                loadTags() // 重新加载列表
            } catch (e: Exception) {
                _uiState.value = TagUiState.Error(e.message ?: "Failed to update tag")
            }
        }
    }

    /**
     * 删除标签
     */
    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.deleteTag(tagId)
                loadTags() // 重新加载列表
            } catch (e: Exception) {
                _uiState.value = TagUiState.Error(e.message ?: "Failed to delete tag")
            }
        }
    }

    /**
     * 为套牌添加标签
     */
    fun addTagToDecklist(decklistId: Long, tagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.addTagToDecklist(decklistId, tagId)
                loadTags() // 重新加载列表以更新计数
            } catch (e: Exception) {
                _uiState.value = TagUiState.Error(e.message ?: "Failed to add tag to decklist")
            }
        }
    }

    /**
     * 从套牌移除标签
     */
    fun removeTagFromDecklist(decklistId: Long, tagId: Long) {
        viewModelScope.launch {
            try {
                tagRepository.removeTagFromDecklist(decklistId, tagId)
                loadTags() // 重新加载列表以更新计数
            } catch (e: Exception) {
                _uiState.value = TagUiState.Error(e.message ?: "Failed to remove tag from decklist")
            }
        }
    }

    /**
     * 为套牌设置标签（替换所有现有标签）
     */
    fun setTagsForDecklist(decklistId: Long, tagNames: List<String>) {
        viewModelScope.launch {
            try {
                tagRepository.setTagsForDecklist(decklistId, tagNames)
                loadTags() // 重新加载列表以更新计数
            } catch (e: Exception) {
                _uiState.value = TagUiState.Error(e.message ?: "Failed to set tags for decklist")
            }
        }
    }

    /**
     * 获取套牌的所有标签
     */
    suspend fun getTagsForDecklist(decklistId: Long): List<Tag> {
        return tagRepository.getTagsForDecklist(decklistId)
    }

    /**
     * 获取所有标签
     */
    suspend fun getAllTags(): List<Tag> {
        return tagRepository.getAllTags()
    }
}

/**
 * 标签 UI 状态
 */
sealed class TagUiState {
    object Loading : TagUiState()
    data class Success(val tags: List<Tag>) : TagUiState()
    data class Error(val message: String) : TagUiState()
}
