package com.noctplayer.app.data.scanner

/** Container extensions the app claims to support, per product spec. Lowercase, no dot. */
val SUPPORTED_VIDEO_EXTENSIONS = listOf(
    "mp4", "mkv", "avi", "mov", "webm", "flv", "ts", "m4v", "3gp", "3gpp", "mpg", "mpeg"
)

fun extensionToMimeType(extension: String): String = when (extension.lowercase()) {
    "mp4", "m4v" -> "video/mp4"
    "mkv" -> "video/x-matroska"
    "avi" -> "video/avi"
    "mov" -> "video/quicktime"
    "webm" -> "video/webm"
    "flv" -> "video/x-flv"
    "ts" -> "video/mp2t"
    "3gp", "3gpp" -> "video/3gpp"
    "mpg", "mpeg" -> "video/mpeg"
    else -> "video/*"
}

fun fileNameExtension(name: String): String = name.substringAfterLast('.', "")
