package com.noctplayer.app.data.local.db.dao

import androidx.room.*
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import com.noctplayer.app.data.local.db.entity.MediaSource
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateAddedSec DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE displayName LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaItemEntity>)

    /** Removes stale rows for one source only, so a MediaStore rescan never touches SAF-sourced rows and vice versa. */
    @Query("DELETE FROM media_items WHERE source = :source AND id NOT IN (:validIds)")
    suspend fun deleteMissingForSource(source: MediaSource, validIds: List<String>)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun delete(id: String)
}
