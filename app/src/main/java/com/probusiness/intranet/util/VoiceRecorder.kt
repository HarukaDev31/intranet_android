package com.probusiness.intranet.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/** Grabación de notas de voz (AAC/M4A) para el chat de Soporte TI. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    val elapsedMs: Long
        get() = if (startedAtMs <= 0L) 0L else System.currentTimeMillis() - startedAtMs

    fun start(): Boolean {
        stopInternal(discard = true)
        val file = File(context.cacheDir, "voice_${UUID.randomUUID()}.m4a")
        return try {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(64_000)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            outputFile = file
            startedAtMs = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            runCatching { file.delete() }
            recorder = null
            outputFile = null
            startedAtMs = 0L
            false
        }
    }

    /** Detiene y devuelve el archivo si la grabación es válida. */
    fun stop(minDurationMs: Long = 400L): CopiedAttachment? {
        val file = outputFile
        val elapsed = elapsedMs
        stopInternal(discard = false)
        if (file == null || !file.exists() || file.length() < 200L || elapsed < minDurationMs) {
            runCatching { file?.delete() }
            return null
        }
        return CopiedAttachment(
            file = file,
            displayName = "nota_voz_${System.currentTimeMillis()}.m4a",
            mime = "audio/mp4",
        )
    }

    fun cancel() {
        stopInternal(discard = true)
    }

    private fun stopInternal(discard: Boolean) {
        val rec = recorder
        val file = outputFile
        recorder = null
        outputFile = null
        startedAtMs = 0L
        if (rec != null) {
            runCatching {
                rec.stop()
            }
            runCatching { rec.release() }
        }
        if (discard) {
            runCatching { file?.delete() }
        }
    }
}
