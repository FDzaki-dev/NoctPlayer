package com.noctplayer.app.data.local.db.dao

import androidx.room.*
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateAddedSec DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE displayName LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mediaStoreId = :id LIMIT 1")
    suspend fun getById(id: Long): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE mediaStoreId NOT IN (:validIds)")
    suspend fun deleteMissing(validIds: List<Long>)

    @Query("DELETE FROM media_items WHERE mediaStoreId = :id")
    suspend fun delete(id: Long)
}
