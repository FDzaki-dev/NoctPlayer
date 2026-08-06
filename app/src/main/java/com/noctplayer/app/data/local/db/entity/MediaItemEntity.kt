package com.noctplayer.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Mirrors a video file discovered via MediaStore. mediaStoreId is the stable join key. */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val mediaStoreId: Long,
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
