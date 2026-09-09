package com.probusiness.intranet.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EstadoDto(
    val id: Int? = null,
    val nombre: String? = null,
    val color: String? = null,
)

@Serializable
data class EstadoOpcionDto(
    val id: Int,
    val codigo: String,
    val nombre: String,
)

@Serializable
data class GestionDto(
    val es_creador: Boolean = false,
    val es_staff: Boolean = false,
    val puede_estado: Boolean = false,
    val puede_complejidad: Boolean = false,
    val puede_complejidad_pm: Boolean = false,
    val puede_complejidad_analista: Boolean = false,
    val estados: List<EstadoOpcionDto> = emptyList(),
    val estado_valor: String? = null,
    val complejidad_valor: String? = null,
    val complejidad_pm_valor: String? = null,
    val complejidad_analista_valor: String? = null,
)

@Serializable
data class SolicitudDto(
    val id: Int,
    val chat_uuid: String? = null,
    val codigo: String? = null,
    val tipo_solicitud: String? = null,
    val subtipo_b: String? = null,
    val titulo: String? = null,
    val area: String? = null,
    val solicitante: String? = null,
    val pm: String? = null,
    val analista: String? = null,
    val criticidad: String? = null,
    val estado_id: Int? = null,
    val estado: EstadoDto? = null,
    val estado_codigo: String? = null,
    val progreso: Int? = null,
    val fecha_registro: String? = null,
    val fecha_registro_iso: String? = null,
    val ultima_actualizacion: String? = null,
    val seccion_ruta: String? = null,
    val descripcion: String? = null,
    val gestion: GestionDto? = null,
)

@Serializable
data class ActualizarEstadoRequest(
    val estado_codigo: String,
)

@Serializable
data class ActualizarComplejidadRequest(
    val criticidad: String,
)

@Serializable
data class SolicitudListResponse(
    val success: Boolean = false,
    val data: List<SolicitudDto> = emptyList(),
)

@Serializable
data class SolicitudDetailResponse(
    val success: Boolean = false,
    val data: SolicitudDto? = null,
)

@Serializable
data class ImagenDto(
    val url: String? = null,
    val nombre: String? = null,
    // El backend lo devuelve formateado como texto (ej. "108 KB", "1.2 MB"), no como número de bytes.
    val tamano: String? = null,
)

@Serializable
data class ReplyToDto(
    val id: Int? = null,
    val remitente: String? = null,
    val texto: String? = null,
    val tiene_imagen: Boolean = false,
    val imagen_url: String? = null,
)

@Serializable
data class MensajeDto(
    val id: Int,
    val usuario_id: Int? = null,
    val remitente: String? = null,
    val iniciales: String? = null,
    val color: String? = null,
    val avatar_url: String? = null,
    val texto: String? = null,
    val es_sistema: Boolean = false,
    val marca_tiempo: String? = null,
    val created_at_iso: String? = null,
    val es_propio: Boolean = false,
    val leido: Boolean = false,
    val reply_to_id: Int? = null,
    val reply_to: ReplyToDto? = null,
    val imagenes: List<ImagenDto> = emptyList(),
)

@Serializable
data class PaginationDto(
    val has_more: Boolean = false,
    val oldest_id: Int? = null,
    val newest_id: Int? = null,
    val per_page: Int? = null,
)

@Serializable
data class MensajesResponse(
    val success: Boolean = false,
    val data: List<MensajeDto> = emptyList(),
    val pagination: PaginationDto? = null,
)

@Serializable
data class MensajeDetailResponse(
    val success: Boolean = false,
    val data: MensajeDto? = null,
)

@Serializable
data class MarcarLeidosRequest(
    val mensaje_ids: List<Int>,
)
