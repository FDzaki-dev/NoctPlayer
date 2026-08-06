package com.noctplayer.app.data.repository

import com.noctplayer.app.data.local.db.dao.FavoriteDao
import com.noctplayer.app.data.local.db.dao.MediaDao
import com.noctplayer.app.data.local.db.dao.WatchProgressDao
import com.noctplayer.app.data.local.db.entity.FavoriteEntity
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import com.noctplayer.app.data.local.db.entity.WatchProgressEntity
import com.noctplayer.app.data.scanner.MediaStoreScanner
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val scanner: MediaStoreScanner,
    private val mediaDao: MediaDao,
    private val watchProgressDao: WatchProgressDao,
    private val favoriteDao: FavoriteDao
) {
    fun observeLibrary(): Flow<List<MediaItemEntity>> = mediaDao.observeAll()

    fun observeRecentlyPlayed(): Flow<List<WatchProgressEntity>> =
        watchProgressDao.observeRecentlyPlayed()

    fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.observeAll()

    fun isFavorite(mediaStoreId: Long): Flow<Boolean> = favoriteDao.isFavorite(mediaStoreId)

    suspend fun refreshLibrary() {
        val scanned = scanner.scan()
        mediaDao.upsertAll(scanned)
        mediaDao.deleteMissing(scanned.map { it.mediaStoreId })
    }

    suspend fun getMediaItem(id: Long): MediaItemEntity? = mediaDao.getById(id)

    suspend fun getProgress(id: Long): WatchProgressEntity? = watchProgressDao.getProgress(id)

    suspend fun saveProgress(id: Long, positionMs: Long, durationMs: Long) {
        val finished = durationMs > 0 && positionMs >= durationMs - 2000
        watchProgressDao.upsert(
            WatchProgressEntity(
                mediaStoreId = id,
                positionMs = if (finished) 0 else positionMs,
                durationMs = durationMs,
                lastPlayedAt = System.currentTimeMillis(),
                isFinished = finished
            )
        )
    }

    suspend fun toggleFavorite(id: Long, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) favoriteDao.remove(id)
        else favoriteDao.add(FavoriteEntity(id, System.currentTimeMillis()))
    }

    suspend fun deleteFromLibrary(id: Long) = mediaDao.delete(id)
}
