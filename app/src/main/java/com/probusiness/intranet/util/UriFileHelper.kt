package com.probusiness.intranet.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/** Copia un content:// Uri (elegido con el selector de imágenes del sistema) a un archivo temporal. */
object UriFileHelper {

    fun copyToCache(context: Context, uri: Uri): File? {
        return try {
            val extension = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg"
            val file = File(context.cacheDir, "upload_${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            if (file.exists() && file.length() > 0) file else null
        } catch (e: Exception) {
            null
        }
    }
}
