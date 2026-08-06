package com.noctplayer.app.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.noctplayer.app.data.local.db.entity.MediaItemEntity
import com.noctplayer.app.data.local.db.entity.MediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Scans user-granted SAF folder trees directly via DocumentsProvider, bypassing
 * MediaStore indexing entirely. This is the guaranteed fallback for videos
 * MediaStoreScanner can't see — freshly copied files the system hasn't scanned
 * yet, or OEM MediaProvider quirks. Slower than a MediaStore query (walks the
 * tree via DocumentFile), so it only runs over folders the user explicitly adds.
 */
class SafFolderScanner(private val context: Context) {

    suspend fun scanFolder(treeUri: Uri): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val results = mutableListOf<MediaItemEntity>()
        walk(root, results)
        results
    }

    private fun walk(dir: DocumentFile, out: MutableList<MediaItemEntity>) {
        val children = try { dir.listFiles() } catch (e: Exception) { return }
        for (child in children) {
            if (child.isDirectory) {
                walk(child, out)
                continue
            }
            val name = child.name ?: continue
            val extension = fileNameExtension(name)
            if (extension.lowercase() !in SUPPORTED_VIDEO_EXTENSIONS) continue
            if (child.length() <= 0L) continue

            val stableId = "saf:${sha1(child.uri.toString())}"
            out += MediaItemEntity(
                id = stableId,
                source = MediaSource.SAF,
                mediaStoreId = null,
                uriString = child.uri.toString(),
                displayName = name,
                filePath = child.uri.toString(),
                durationMs = 0L, // unknown until ExoPlayer opens it; library UI shows "--:--" gracefully
                sizeBytes = child.length(),
                dateAddedSec = child.lastModified() / 1000,
                dateModifiedSec = child.lastModified() / 1000,
                width = 0,
                height = 0,
                mimeType = extensionToMimeType(extension)
            )
        }
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
