package com.mtgo.decklistmanager.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtgo.decklistmanager.domain.model.DecklistNote
import com.mtgo.decklistmanager.data.repository.DecklistNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DecklistNoteViewModel - 套牌备注 ViewModel
 */
@HiltViewModel
class DecklistNoteViewModel @Inject constructor(
    private val noteRepository: DecklistNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteUiState>(NoteUiState.Idle)
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _currentNote = MutableStateFlow<DecklistNote?>(null)
    val currentNote: StateFlow<DecklistNote?> = _currentNote.asStateFlow()

    /**
     * 加载套牌的备注
     */
    fun loadNote(decklistId: Long) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading
            try {
                val note = noteRepository.getNoteByDecklistId(decklistId)
                _currentNote.value = note
                _uiState.value = NoteUiState.Success(note)
            } catch (e: Exception) {
                _uiState.value = NoteUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 保存备注
     */
    fun saveNote(decklistId: Long, noteText: String) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Saving
            try {
                val note = noteRepository.saveNote(decklistId, noteText)
                _currentNote.value = note
                _uiState.value = NoteUiState.Success(note)
            } catch (e: Exception) {
                _uiState.value = NoteUiState.Error(e.message ?: "Failed to save note")
            }
        }
    }

    /**
     * 删除备注
     */
    fun deleteNote(decklistId: Long) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Saving
            try {
                noteRepository.deleteNote(decklistId)
                _currentNote.value = null
                _uiState.value = NoteUiState.Success(null)
            } catch (e: Exception) {
                _uiState.value = NoteUiState.Error(e.message ?: "Failed to delete note")
            }
        }
    }

    /**
     * 搜索备注
     */
    fun searchNotes(query: String) {
        viewModelScope.launch {
            _uiState.value = NoteUiState.Loading
            try {
                val notes = noteRepository.searchNotes(query)
                _uiState.value = NoteUiState.SearchResults(notes)
            } catch (e: Exception) {
                _uiState.value = NoteUiState.Error(e.message ?: "Failed to search notes")
            }
        }
    }
}

/**
 * 备注 UI 状态
 */
sealed class NoteUiState {
    object Idle : NoteUiState()
    object Loading : NoteUiState()
    object Saving : NoteUiState()
    data class Success(val note: DecklistNote?) : NoteUiState()
    data class Error(val message: String) : NoteUiState()
    data class SearchResults(val notes: List<DecklistNote>) : NoteUiState()
}
