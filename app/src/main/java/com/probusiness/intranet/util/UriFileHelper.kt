package com.probusiness.intranet.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

data class CopiedAttachment(
    val file: File,
    val displayName: String,
    val mime: String,
)

/** Copia un content:// Uri a un archivo temporal, conservando nombre y mime. */
object UriFileHelper {

    fun copyToCache(context: Context, uri: Uri): File? = copyAttachment(context, uri)?.file

    fun copyAttachment(context: Context, uri: Uri): CopiedAttachment? {
        return try {
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val displayName = queryDisplayName(context, uri) ?: "archivo_${UUID.randomUUID()}"
            val ext = displayName.substringAfterLast('.', mime.substringAfterLast('/', "bin"))
            val file = File(context.cacheDir, "upload_${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            if (file.exists() && file.length() > 0) {
                CopiedAttachment(file = file, displayName = displayName, mime = mime)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun pendingFromUri(context: Context, uri: Uri): PendingAttachment {
        val mime = context.contentResolver.getType(uri)
        val name = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "archivo"
        return PendingAttachment(uri = uri, displayName = name, mime = mime)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        return null
    }
}
