package com.noctplayer.app.data.local.db.dao

import androidx.room.*
import com.noctplayer.app.data.local.db.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE mediaStoreId = :id LIMIT 1")
    suspend fun getProgress(id: Long): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress ORDER BY lastPlayedAt DESC LIMIT 50")
    fun observeRecentlyPlayed(): Flow<List<WatchProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE mediaStoreId = :id")
    suspend fun clear(id: Long)
}
