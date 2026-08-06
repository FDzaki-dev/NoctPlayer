package com.noctplayer.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Resume position + last-played bookkeeping, keyed by the MediaStore id of the file. */
@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val mediaStoreId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long,
    val isFinished: Boolean = false
)
