package com.noctplayer.app.data.repository

import android.net.Uri
import com.noctplayer.app.data.local.db.dao.FavoriteDao
import com.noctplayer.app.data.local.db.dao.MediaDao
import com.noctplayer.app.data.local.db.dao.WatchProgressDao
import com.noctplayer.app.data.local.db.entity.FavoriteEntity
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import com.noctplayer.app.data.local.db.entity.MediaSource
import com.noctplayer.app.data.local.db.entity.WatchProgressEntity
import com.noctplayer.app.data.local.prefs.SafFolderPrefs
import com.noctplayer.app.data.scanner.MediaStoreScanner
import com.noctplayer.app.data.scanner.SafFolderScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MediaRepository(
    private val mediaStoreScanner: MediaStoreScanner,
    private val safFolderScanner: SafFolderScanner,
    private val safFolderPrefs: SafFolderPrefs,
    private val mediaDao: MediaDao,
    private val watchProgressDao: WatchProgressDao,
    private val favoriteDao: FavoriteDao
) {
    fun observeLibrary(): Flow<List<MediaItemEntity>> = mediaDao.observeAll()

    fun observeRecentlyPlayed(): Flow<List<WatchProgressEntity>> =
        watchProgressDao.observeRecentlyPlayed()

    fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.observeAll()

    fun isFavorite(id: String): Flow<Boolean> = favoriteDao.isFavorite(id)

    fun observeSafFolders(): Flow<Set<String>> = safFolderPrefs.folderUris

    /**
     * Refreshes the full library: MediaStore first (fast), then every user-added
     * SAF folder (slower, but the guaranteed-visibility fallback). Each source's
     * stale rows are cleared independently so one source's rescan never deletes
     * the other's entries.
     */
    suspend fun refreshLibrary() {
        val scanned = mediaStoreScanner.scan()
        mediaDao.upsertAll(scanned)
        mediaDao.deleteMissingForSource(MediaSource.MEDIA_STORE, scanned.map { it.id })

        val folders = safFolderPrefs.folderUris.first()
        val safResults = mutableListOf<MediaItemEntity>()
        for (folderUriString in folders) {
            val uri = Uri.parse(folderUriString)
            safResults += safFolderScanner.scanFolder(uri)
        }
        if (folders.isNotEmpty()) {
            mediaDao.upsertAll(safResults)
            mediaDao.deleteMissingForSource(MediaSource.SAF, safResults.map { it.id })
        }
    }

    suspend fun addSafFolder(uriString: String) {
        safFolderPrefs.addFolder(uriString)
        refreshLibrary()
    }

    suspend fun removeSafFolder(uriString: String) {
        safFolderPrefs.removeFolder(uriString)
        refreshLibrary()
    }

    suspend fun getMediaItem(id: String): MediaItemEntity? = mediaDao.getById(id)

    suspend fun getProgress(id: String): WatchProgressEntity? = watchProgressDao.getProgress(id)

    suspend fun saveProgress(id: String, positionMs: Long, durationMs: Long) {
        val finished = durationMs > 0 && positionMs >= durationMs - 2000
        watchProgressDao.upsert(
            WatchProgressEntity(
                id = id,
                positionMs = if (finished) 0 else positionMs,
                durationMs = durationMs,
                lastPlayedAt = System.currentTimeMillis(),
                isFinished = finished
            )
        )
    }

    suspend fun toggleFavorite(id: String, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) favoriteDao.remove(id)
        else favoriteDao.add(FavoriteEntity(id, System.currentTimeMillis()))
    }

    suspend fun deleteFromLibrary(id: String) = mediaDao.delete(id)
}
