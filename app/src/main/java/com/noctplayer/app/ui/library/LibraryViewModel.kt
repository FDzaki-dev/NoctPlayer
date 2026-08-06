package com.noctplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import com.noctplayer.app.data.local.db.entity.WatchProgressEntity
import com.noctplayer.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { DATE_ADDED, NAME, DURATION, SIZE }

/** A media item paired with its continue-watching progress, if any (0f..1f, or null if unwatched/finished). */
data class LibraryGridItem(
    val media: MediaItemEntity,
    val progressFraction: Float?
)

data class LibraryUiState(
    val items: List<LibraryGridItem> = emptyList(),
    val isScanning: Boolean = false,
    val sortOrder: SortOrder = SortOrder.DATE_ADDED,
    val query: String = "",
    val safFolderCount: Int = 0
)

/** Intermediate bundle so we can combine >5 flows without kotlinx.coroutines' unsafe vararg overload. */
private data class LibraryFilterState(
    val items: List<MediaItemEntity>,
    val sort: SortOrder,
    val query: String,
    val scanning: Boolean,
    val safFolderCount: Int
)

class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {

    private val sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    private val query = MutableStateFlow("")
    private val isScanning = MutableStateFlow(false)

    private val filterState = combine(
        repository.observeLibrary(), sortOrder, query, isScanning, repository.observeSafFolders()
    ) { items, sort, q, scanning, folders ->
        LibraryFilterState(items, sort, q, scanning, folders.size)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        filterState, repository.observeRecentlyPlayed()
    ) { filter, progressList ->
        val progressById = progressList.associateBy(WatchProgressEntity::id)

        val filtered = if (filter.query.isBlank()) filter.items else filter.items.filter {
            it.displayName.contains(filter.query, ignoreCase = true)
        }
        val sorted = when (filter.sort) {
            SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.dateAddedSec }
            SortOrder.NAME -> filtered.sortedBy { it.displayName.lowercase() }
            SortOrder.DURATION -> filtered.sortedByDescending { it.durationMs }
            SortOrder.SIZE -> filtered.sortedByDescending { it.sizeBytes }
        }
        val withProgress = sorted.map { media ->
            val progress = progressById[media.id]
            val fraction = if (progress != null && progress.durationMs > 0 && !progress.isFinished) {
                (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
            } else null
            LibraryGridItem(media, fraction)
        }
        LibraryUiState(withProgress, filter.scanning, filter.sort, filter.query, filter.safFolderCount)
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

    fun addSafFolder(uriString: String) {
        viewModelScope.launch {
            isScanning.value = true
            try {
                repository.addSafFolder(uriString)
            } finally {
                isScanning.value = false
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { repository.deleteFromLibrary(id) }
    }
}
