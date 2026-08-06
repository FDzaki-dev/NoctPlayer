package com.noctplayer.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Resume position + last-played bookkeeping, keyed by the shared media item [id]. */
@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val id: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long,
    val isFinished: Boolean = false
)
