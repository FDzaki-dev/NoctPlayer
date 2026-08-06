package com.noctplayer.app.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import com.noctplayer.app.data.local.db.entity.MediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans local video files via MediaStore only (no network, no cloud index).
 *
 * Deliberately queries MediaStore.Files rather than MediaStore.Video.Media:
 * some OEM MediaProviders (this app's own reference device runs Infinix XOS)
 * fail to classify less common containers (.mkv, .ts, .flv, .avi) as
 * MEDIA_TYPE_VIDEO, which makes them invisible to a Video.Media-only query
 * even though the file is perfectly playable. Filtering MediaStore.Files by
 * DISPLAY_NAME extension instead catches those misclassified rows too.
 *
 * This is still MediaStore-indexed data, so it can't see files the system
 * hasn't scanned at all yet — that gap is covered by [SafFolderScanner].
 */
class MediaStoreScanner(private val context: Context) {

    suspend fun scan(): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaItemEntity>()
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        // Match on extension (DISPLAY_NAME) rather than trusting MIME_TYPE/MEDIA_TYPE,
        // since those are exactly the fields unreliable OEM scanners get wrong.
        val selectionParts = SUPPORTED_VIDEO_EXTENSIONS.joinToString(" OR ") {
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        }
        val selectionArgs = SUPPORTED_VIDEO_EXTENSIONS.map { "%.$it" }.toTypedArray()

        context.contentResolver.query(
            collection, projection, selectionParts, selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            // Safe (non-throwing) column lookups: some OEM "files" table schemas omit
            // width/height/duration on older Android versions. Missing -> default 0/"".
            val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val durCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION)
            val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val addedCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
            val modCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val wCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
            val hCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
            val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

            if (idCol < 0 || nameCol < 0) return@use // table doesn't match expected shape at all; bail safely

            while (cursor.moveToNext()) {
                val rowId = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol) ?: continue
                val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                if (sizeBytes <= 0L) continue // skip zero-byte/broken entries

                val uri: Uri = ContentUris.withAppendedId(collection, rowId)
                val extension = fileNameExtension(displayName)
                val mimeFromStore = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                val mimeType = if (mimeFromStore.isNullOrBlank() || mimeFromStore == "application/octet-stream") {
                    extensionToMimeType(extension)
                } else mimeFromStore

                results += MediaItemEntity(
                    id = "ms:$rowId",
                    source = MediaSource.MEDIA_STORE,
                    mediaStoreId = rowId,
                    uriString = uri.toString(),
                    displayName = displayName,
                    filePath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else "",
                    durationMs = if (durCol >= 0) cursor.getLong(durCol) else 0L,
                    sizeBytes = sizeBytes,
                    dateAddedSec = if (addedCol >= 0) cursor.getLong(addedCol) else 0L,
                    dateModifiedSec = if (modCol >= 0) cursor.getLong(modCol) else 0L,
                    width = if (wCol >= 0) cursor.getInt(wCol) else 0,
                    height = if (hCol >= 0) cursor.getInt(hCol) else 0,
                    mimeType = mimeType
                )
            }
        }
        results
    }
}
