package com.noctplayer.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noctplayer.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val title: String = "",
    val uri: String = "",
    val resumePositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isReady: Boolean = false,
    val errorMessage: String? = null
)

class PlayerViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentId: Long = -1L

    fun load(mediaStoreId: Long) {
        currentId = mediaStoreId
        viewModelScope.launch {
            try {
                val item = repository.getMediaItem(mediaStoreId)
                val progress = repository.getProgress(mediaStoreId)
                if (item == null) {
                    _uiState.value = _uiState.value.copy(errorMessage = "File not found or was moved/deleted")
                    return@launch
                }
                _uiState.value = PlayerUiState(
                    title = item.displayName,
                    uri = item.uriString,
                    resumePositionMs = progress?.positionMs ?: 0L,
                    isReady = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to load video: ${e.message}")
            }
        }
    }

    fun savePosition(positionMs: Long, durationMs: Long) {
        if (currentId < 0) return
        viewModelScope.launch {
            repository.saveProgress(currentId, positionMs, durationMs)
        }
    }

    fun toggleFavorite() {
        if (currentId < 0) return
        viewModelScope.launch {
            repository.toggleFavorite(currentId, _uiState.value.isFavorite)
            _uiState.value = _uiState.value.copy(isFavorite = !_uiState.value.isFavorite)
        }
    }
}
