package com.probusiness.intranet.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import java.util.Locale

private val INLINE_IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif")
private val AUDIO_EXT = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "webm", "3gp", "oga", "caf")

fun extensionOf(name: String?): String {
    val base = (name ?: "").substringBefore('?')
    val ext = base.substringAfterLast('.', "")
    return ext.uppercase(Locale.ROOT).ifBlank { "ARCHIVO" }
}

fun isInlineImage(name: String?, mime: String? = null): Boolean {
    val ext = extensionOf(name).lowercase(Locale.ROOT)
    if (ext == "webp") return false
    if (ext in INLINE_IMAGE_EXT) return true
    return mime?.startsWith("image/") == true
}

fun isAudioAttachment(name: String?, mime: String? = null): Boolean {
    if (mime?.startsWith("audio/") == true) return true
    val ext = extensionOf(name).lowercase(Locale.ROOT)
    return ext in AUDIO_EXT
}

fun openAttachment(context: Context, url: String, mimeHint: String? = null) {
    val uri = url.toUri()
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeHint ?: guessMime(url))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

private fun guessMime(nameOrUrl: String): String {
    return when (extensionOf(nameOrUrl).lowercase(Locale.ROOT)) {
        "pdf" -> "application/pdf"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
        "txt" -> "text/plain"
        "csv" -> "text/csv"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/mp4"
        "ogg", "opus", "oga" -> "audio/ogg"
        "wav" -> "audio/wav"
        "webm" -> "audio/webm"
        "3gp" -> "audio/3gpp"
        else -> "*/*"
    }
}

data class PendingAttachment(
    val uri: Uri,
    val displayName: String,
    val mime: String?,
)
