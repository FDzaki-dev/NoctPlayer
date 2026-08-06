package com.noctplayer.app.data.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans local video files via MediaStore only. No network, no cloud index.
 * Covers internal storage and any mounted SD card that MediaStore has indexed.
 */
class MediaStoreScanner(private val context: Context) {

    suspend fun scan(): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaItemEntity>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE
        )

        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                results += MediaItemEntity(
                    mediaStoreId = id,
                    uriString = uri.toString(),
                    displayName = cursor.getString(nameCol) ?: "Unknown",
                    filePath = cursor.getString(dataCol) ?: "",
                    durationMs = cursor.getLong(durCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    dateAddedSec = cursor.getLong(addedCol),
                    dateModifiedSec = cursor.getLong(modCol),
                    width = cursor.getInt(wCol),
                    height = cursor.getInt(hCol),
                    mimeType = cursor.getString(mimeCol) ?: "video/*"
                )
            }
        }
        results
    }
}
