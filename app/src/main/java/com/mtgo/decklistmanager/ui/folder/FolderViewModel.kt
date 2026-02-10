package com.mtgo.decklistmanager.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.Folder
import com.mtgo.decklistmanager.data.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FolderViewModel - 文件夹管理 ViewModel
 */
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FolderUiState>(FolderUiState.Loading)
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    /**
     * 加载所有文件夹
     */
    fun loadFolders() {
        viewModelScope.launch {
            _uiState.value = FolderUiState.Loading
            try {
                val result = folderRepository.getAllFolders()
                _folders.value = result
                _uiState.value = FolderUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = FolderUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 创建新文件夹
     */
    fun createFolder(name: String, color: Int = 0xFF6200EE.toInt(), icon: String = "folder") {
        viewModelScope.launch {
            try {
                folderRepository.createFolder(name, color, icon)
                loadFolders() // 重新加载列表
            } catch (e: Exception) {
                _uiState.value = FolderUiState.Error(e.message ?: "Failed to create folder")
            }
        }
    }

    /**
     * 更新文件夹
     */
    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            try {
                folderRepository.updateFolder(folder)
                loadFolders() // 重新加载列表
            } catch (e: Exception) {
                _uiState.value = FolderUiState.Error(e.message ?: "Failed to update folder")
            }
        }
    }

    /**
     * 删除文件夹
     */
    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            try {
                folderRepository.deleteFolder(folderId)
                loadFolders() // 重新加载列表
            } catch (e: Exception) {
                _uiState.value = FolderUiState.Error(e.message ?: "Failed to delete folder")
            }
        }
    }

    /**
     * 添加套牌到文件夹
     */
    fun addDecklistToFolder(decklistId: Long, folderId: Long) {
        viewModelScope.launch {
            try {
                folderRepository.addDecklistToFolder(decklistId, folderId)
                loadFolders() // 重新加载列表以更新计数
            } catch (e: Exception) {
                _uiState.value = FolderUiState.Error(e.message ?: "Failed to add decklist to folder")
            }
        }
    }

    /**
     * 从文件夹移除套牌
     */
    fun removeDecklistFromFolder(decklistId: Long, folderId: Long) {
        viewModelScope.launch {
            try {
                folderRepository.removeDecklistFromFolder(decklistId, folderId)
                loadFolders() // 重新加载列表以更新计数
            } catch (e: Exception) {
                _uiState.value = FolderUiState.Error(e.message ?: "Failed to remove decklist from folder")
            }
        }
    }

    /**
     * 获取套牌所在的所有文件夹
     */
    suspend fun getFoldersForDecklist(decklistId: Long): List<Folder> {
        return folderRepository.getFoldersForDecklist(decklistId)
    }
}

/**
 * 文件夹 UI 状态
 */
sealed class FolderUiState {
    object Loading : FolderUiState()
    data class Success(val folders: List<Folder>) : FolderUiState()
    data class Error(val message: String) : FolderUiState()
}
