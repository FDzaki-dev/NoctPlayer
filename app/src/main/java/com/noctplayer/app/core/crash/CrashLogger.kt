package com.noctplayer.app.core.crash

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Fail-safe uncaught exception logger.
 * Writes crash reports to Documents/NoctPlayer/logs/ via MediaStore (API 29+),
 * no legacy storage permission required. Falls back silently (never throws)
 * so a logging failure can never cause a secondary crash.
 *
 * Retention: FIFO, keeps the 50 most recent log files.
 */
class CrashLogger(private val appContext: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private const val TAG = "CrashLogger"
        private const val APP_DIR = "NoctPlayer"
        private const val MAX_LOGS = 50

        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashLogger(context.applicationContext))
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            writeCrashLog(thread, throwable)
        } catch (secondary: Throwable) {
            Log.e(TAG, "Failed to persist crash log", secondary)
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val uuid = UUID.randomUUID().toString().take(8)
        val fileName = "crash_${timestamp}_$uuid.txt"

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val versionName = try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }

        val body = buildString {
            appendLine("App Version: $versionName")
            appendLine("OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Timestamp: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine("--- Stacktrace ---")
            append(sw.toString())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(fileName, body)
        } else {
            writeLegacy(fileName, body)
        }

        enforceRetention()
    }

    private fun writeViaMediaStore(fileName: String, body: String) {
        val resolver = appContext.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$APP_DIR/logs"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return
        resolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
    }

    private fun writeLegacy(fileName: String, body: String) {
        val dir = java.io.File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "$APP_DIR/logs"
        )
        if (!dir.exists()) dir.mkdirs()
        java.io.File(dir, fileName).writeText(body)
    }

    private fun enforceRetention() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = appContext.contentResolver
            val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$APP_DIR/logs/"
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED)
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
            val cursor = resolver.query(
                MediaStore.Files.getContentUri("external"),
                projection, selection, arrayOf(relativePath),
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            ) ?: return

            val idsToDelete = mutableListOf<Long>()
            cursor.use {
                var count = 0
                val idCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (it.moveToNext()) {
                    count++
                    if (count > MAX_LOGS) idsToDelete.add(it.getLong(idCol))
                }
            }
            idsToDelete.forEach { id ->
                resolver.delete(MediaStore.Files.getContentUri("external"), "_id=?", arrayOf(id.toString()))
            }
        } else {
            val dir = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "$APP_DIR/logs"
            )
            val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
            files.drop(MAX_LOGS).forEach { it.delete() }
        }
    }
}
