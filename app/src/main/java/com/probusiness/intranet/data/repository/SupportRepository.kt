package com.probusiness.intranet.data.repository

import com.probusiness.intranet.data.remote.ApiService
import com.probusiness.intranet.data.remote.dto.MarcarLeidosRequest
import com.probusiness.intranet.data.remote.dto.MensajeDto
import com.probusiness.intranet.data.remote.dto.MensajesResponse
import com.probusiness.intranet.data.remote.dto.SolicitudDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val apiService: ApiService,
) {

    suspend fun listarSolicitudes(): Result<List<SolicitudDto>> = runCatching {
        apiService.listarSolicitudes().data
    }

    suspend fun obtenerSolicitud(id: Int): Result<SolicitudDto> = runCatching {
        apiService.obtenerSolicitud(id).data ?: error("Solicitud no encontrada")
    }

    suspend fun crearSolicitud(
        tipoSolicitud: String,
        subtipoB: String?,
        titulo: String,
        area: String,
        seccionRuta: String?,
        descripcion: String,
        imagenes: List<File>,
    ): Result<SolicitudDto> = runCatching {
        val response = apiService.crearSolicitud(
            tipoSolicitud = tipoSolicitud.toPlainBody(),
            subtipoB = subtipoB?.toPlainBody(),
            titulo = titulo.toPlainBody(),
            area = area.toPlainBody(),
            seccionRuta = seccionRuta?.toPlainBody(),
            descripcion = descripcion.toPlainBody(),
            imagenes = imagenes.mapIndexed { index, file -> file.toMultipart("imagenes[$index]") },
        )
        response.data ?: error("No se pudo crear la solicitud")
    }

    suspend fun listarMensajes(chatUuid: String, beforeId: Int? = null): Result<MensajesResponse> = runCatching {
        apiService.listarMensajes(chatUuid = chatUuid, beforeId = beforeId)
    }

    suspend fun enviarMensaje(
        solicitudId: Int,
        texto: String?,
        replyToId: Int?,
        imagenes: List<File>,
    ): Result<MensajeDto> = runCatching {
        val response = apiService.enviarMensaje(
            solicitudId = solicitudId,
            texto = texto?.toPlainBody(),
            replyToId = replyToId?.toString()?.toPlainBody(),
            imagenes = imagenes.mapIndexed { index, file -> file.toMultipart("imagenes[$index]") },
        )
        response.data ?: error("No se pudo enviar el mensaje")
    }

    suspend fun marcarLeidos(chatUuid: String, mensajeIds: List<Int>): Result<Unit> = runCatching {
        if (mensajeIds.isEmpty()) return@runCatching
        apiService.marcarLeidos(chatUuid, MarcarLeidosRequest(mensajeIds))
        Unit
    }

    private fun String.toPlainBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun File.toMultipart(fieldName: String): MultipartBody.Part {
        val body = this.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(fieldName, this.name, body)
    }
}
