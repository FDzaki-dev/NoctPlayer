package com.noctplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import com.noctplayer.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { DATE_ADDED, NAME, DURATION, SIZE }

data class LibraryUiState(
    val items: List<MediaItemEntity> = emptyList(),
    val isScanning: Boolean = false,
    val sortOrder: SortOrder = SortOrder.DATE_ADDED,
    val query: String = ""
)

class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {

    private val sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    private val query = MutableStateFlow("")
    private val isScanning = MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> = kotlinx.coroutines.flow.combine(
        repository.observeLibrary(), sortOrder, query, isScanning
    ) { items, sort, q, scanning ->
        val filtered = if (q.isBlank()) items else items.filter {
            it.displayName.contains(q, ignoreCase = true)
        }
        val sorted = when (sort) {
            SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.dateAddedSec }
            SortOrder.NAME -> filtered.sortedBy { it.displayName.lowercase() }
            SortOrder.DURATION -> filtered.sortedByDescending { it.durationMs }
            SortOrder.SIZE -> filtered.sortedByDescending { it.sizeBytes }
        }
        LibraryUiState(sorted, scanning, sort, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isScanning.value = true
            try {
                repository.refreshLibrary()
            } finally {
                isScanning.value = false
            }
        }
    }

    fun setSortOrder(order: SortOrder) { sortOrder.value = order }
    fun setQuery(q: String) { query.value = q }

    fun deleteItem(id: Long) {
        viewModelScope.launch { repository.deleteFromLibrary(id) }
    }
}
