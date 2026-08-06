package com.noctplayer.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaSource { MEDIA_STORE, SAF }

/**
 * A discovered video file, from either MediaStore (fast, system-indexed) or
 * SAF folder scan (slower, but guaranteed to see files MediaStore missed —
 * e.g. formats an OEM MediaProvider misclassifies, or files not yet indexed).
 *
 * [id] is a source-prefixed stable key ("ms:<mediaStoreId>" or "saf:<docId-hash>")
 * so both sources can coexist in one table without collision.
 */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val source: MediaSource,
    val mediaStoreId: Long?,
    val uriString: String,
    val displayName: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
)
