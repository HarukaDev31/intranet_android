package com.probusiness.intranet.ui.support

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.probusiness.intranet.data.remote.dto.ImagenDto
import com.probusiness.intranet.data.remote.dto.MensajeDto
import com.probusiness.intranet.data.remote.dto.SolicitudDto
import com.probusiness.intranet.data.remote.realtime.ActiveChatTracker
import com.probusiness.intranet.data.remote.realtime.RealtimeService
import com.probusiness.intranet.data.repository.SupportRepository
import com.probusiness.intranet.ui.navigation.Routes
import com.probusiness.intranet.util.CopiedAttachment
import com.probusiness.intranet.util.PendingAttachment
import com.probusiness.intranet.util.UriFileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

data class TicketChatUiState(
    val isLoadingHeader: Boolean = true,
    val isLoadingMensajes: Boolean = true,
    val solicitud: SolicitudDto? = null,
    val mensajes: List<MensajeDto> = emptyList(),
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val texto: String = "",
    val adjuntos: List<PendingAttachment> = emptyList(),
    val error: String? = null,
    val mensajesError: String? = null,
    val replyTarget: MensajeDto? = null,
    val isUpdatingGestion: Boolean = false,
    val gestionError: String? = null,
    val scrollToMessageId: Int? = null,
)

@HiltViewModel
class TicketChatViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
    private val realtimeService: RealtimeService,
    private val activeChatTracker: ActiveChatTracker,
    private val json: Json,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val solicitudId: Int = checkNotNull(savedStateHandle[Routes.TICKET_CHAT_ARG])
    private var subscribedChatUuid: String? = null

    private val _uiState = MutableStateFlow(TicketChatUiState())
    val uiState: StateFlow<TicketChatUiState> = _uiState

    init {
        activeChatTracker.onChatOpened(solicitudId)
        cargarSolicitudYMensajes()
    }

    override fun onCleared() {
        super.onCleared()
        activeChatTracker.onChatClosed(solicitudId)
        subscribedChatUuid?.let { realtimeService.unsubscribeFromChat(it) }
    }

    private fun cargarSolicitudYMensajes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHeader = true, isLoadingMensajes = true, error = null, mensajesError = null) }
            supportRepository.obtenerSolicitud(solicitudId)
                .onSuccess { solicitud ->
                    _uiState.update { it.copy(isLoadingHeader = false, solicitud = solicitud) }
                    val chatUuid = solicitud.chat_uuid
                    if (chatUuid != null) {
                        cargarMensajesIniciales(chatUuid)
                        suscribirseATiempoReal(chatUuid)
                    } else {
                        _uiState.update { it.copy(isLoadingMensajes = false) }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingHeader = false,
                            isLoadingMensajes = false,
                            error = throwable.message ?: "No se pudo cargar el ticket",
                        )
                    }
                }
        }
    }

    private fun suscribirseATiempoReal(chatUuid: String) {
        if (subscribedChatUuid == chatUuid) return
        subscribedChatUuid = chatUuid
        realtimeService.subscribeToChat(
            chatUuid,
            onMensajeCreadoJson = { mensajeJson ->
                val nuevo = runCatching { json.decodeFromString(MensajeDto.serializer(), mensajeJson) }.getOrNull()
                if (nuevo != null) {
                    _uiState.update { state ->
                        if (state.mensajes.any { it.id == nuevo.id }) return@update state
                        val sinOptimista = state.mensajes.filterNot { local ->
                            local.id < 0 &&
                                local.es_propio &&
                                nuevo.es_propio &&
                                (local.texto ?: "") == (nuevo.texto ?: "")
                        }
                        state.copy(mensajes = sinOptimista + nuevo.withEstado(if (nuevo.leido) "leido" else "entregado"))
                    }
                    if (!nuevo.leido && !nuevo.es_propio) {
                        marcarComoLeidos(chatUuid, listOf(nuevo))
                    }
                }
            },
            onMensajeActualizadoJson = { mensajeJson ->
                val actualizado = runCatching { json.decodeFromString(MensajeDto.serializer(), mensajeJson) }.getOrNull()
                if (actualizado != null) {
                    _uiState.update { state ->
                        if (state.mensajes.none { it.id == actualizado.id }) return@update state
                        state.copy(
                            mensajes = state.mensajes.map { existing ->
                                if (existing.id != actualizado.id) existing
                                else actualizado.copy(
                                    estado_envio = existing.estado_envio,
                                    client_id = existing.client_id,
                                )
                            },
                        )
                    }
                }
            },
        )
    }

    fun reintentarCargarMensajes() {
        val chatUuid = _uiState.value.solicitud?.chat_uuid ?: return
        _uiState.update { it.copy(isLoadingMensajes = true, mensajesError = null) }
        cargarMensajesIniciales(chatUuid)
    }

    private fun cargarMensajesIniciales(chatUuid: String) {
        viewModelScope.launch {
            supportRepository.listarMensajes(chatUuid)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            mensajes = response.data.map { msg ->
                                msg.withEstado(
                                    when {
                                        !msg.es_propio -> null
                                        msg.leido -> "leido"
                                        else -> "entregado"
                                    },
                                )
                            },
                            hasMore = response.pagination?.has_more ?: false,
                            isLoadingMensajes = false,
                        )
                    }
                    marcarComoLeidos(chatUuid, response.data)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingMensajes = false,
                            mensajesError = throwable.message ?: "No se pudieron cargar los mensajes",
                        )
                    }
                }
        }
    }

    fun cargarMasAntiguos() {
        val state = _uiState.value
        val chatUuid = state.solicitud?.chat_uuid ?: return
        val oldestId = state.mensajes.firstOrNull { it.id > 0 }?.id ?: return
        if (!state.hasMore || state.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            supportRepository.listarMensajes(chatUuid, beforeId = oldestId)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            mensajes = response.data + it.mensajes.filter { msg ->
                                response.data.none { incoming -> incoming.id == msg.id }
                            },
                            hasMore = response.pagination?.has_more ?: false,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    private fun marcarComoLeidos(chatUuid: String, mensajes: List<MensajeDto>) {
        val idsNoLeidos = mensajes.filter { !it.leido && !it.es_propio && it.id > 0 }.map { it.id }
        if (idsNoLeidos.isEmpty()) return
        viewModelScope.launch {
            supportRepository.marcarLeidos(chatUuid, idsNoLeidos)
        }
    }

    fun cambiarEstado(estadoCodigo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingGestion = true, gestionError = null) }
            supportRepository.actualizarEstado(solicitudId, estadoCodigo)
                .onSuccess { solicitud ->
                    _uiState.update { it.copy(isUpdatingGestion = false, solicitud = solicitud) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isUpdatingGestion = false, gestionError = throwable.message ?: "No se pudo cambiar el estado")
                    }
                }
        }
    }

    fun cambiarComplejidad(criticidad: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingGestion = true, gestionError = null) }
            supportRepository.actualizarComplejidad(solicitudId, criticidad)
                .onSuccess { solicitud ->
                    _uiState.update { it.copy(isUpdatingGestion = false, solicitud = solicitud) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isUpdatingGestion = false, gestionError = throwable.message ?: "No se pudo cambiar la complejidad")
                    }
                }
        }
    }

    fun toggleRevisado(mensaje: MensajeDto) {
        val chatUuid = _uiState.value.solicitud?.chat_uuid ?: return
        if (mensaje.id <= 0 || mensaje.es_sistema) return
        val nuevo = !mensaje.revisado
        _uiState.update { state ->
            state.copy(
                mensajes = state.mensajes.map {
                    if (it.id == mensaje.id) it.copy(revisado = nuevo) else it
                },
            )
        }
        viewModelScope.launch {
            supportRepository.marcarRevisado(chatUuid, mensaje.id, nuevo)
                .onSuccess { actualizado ->
                    _uiState.update { state ->
                        state.copy(
                            mensajes = state.mensajes.map { existing ->
                                if (existing.id != actualizado.id) existing
                                else actualizado.copy(
                                    estado_envio = existing.estado_envio,
                                    client_id = existing.client_id,
                                )
                            },
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            error = throwable.message ?: "No se pudo marcar el mensaje",
                            mensajes = state.mensajes.map {
                                if (it.id == mensaje.id) it.copy(revisado = mensaje.revisado) else it
                            },
                        )
                    }
                }
        }
    }

    fun onTextoChange(value: String) = _uiState.update { it.copy(texto = value) }

    fun insertarEmoji(emoji: String) {
        _uiState.update { it.copy(texto = it.texto + emoji) }
    }

    fun onAdjuntosSeleccionados(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val nuevos = uris.map { UriFileHelper.pendingFromUri(context, it) }
        _uiState.update { it.copy(adjuntos = (it.adjuntos + nuevos).distinctBy { att -> att.uri }) }
    }

    fun quitarAdjunto(uri: Uri) {
        _uiState.update { it.copy(adjuntos = it.adjuntos.filterNot { att -> att.uri == uri }) }
    }

    fun onReplyToMessage(mensaje: MensajeDto) = _uiState.update { it.copy(replyTarget = mensaje) }
    fun onCancelReply() = _uiState.update { it.copy(replyTarget = null) }

    fun irAlMensaje(id: Int?) {
        if (id == null) return
        _uiState.update { it.copy(scrollToMessageId = id) }
    }

    fun onScrolledToMessage() {
        _uiState.update { it.copy(scrollToMessageId = null) }
    }

    fun reintentarEnvio(context: Context, mensaje: MensajeDto) {
        if (mensaje.estado_envio != "error") return
        _uiState.update { state ->
            state.copy(mensajes = state.mensajes.filterNot { it.client_id == mensaje.client_id && it.id == mensaje.id })
        }
        enviar(
            context = context,
            textoForzado = mensaje.texto,
            replyToId = mensaje.reply_to_id,
        )
    }

    fun enviar(context: Context) {
        val state = _uiState.value
        enviar(context, state.texto.trim().ifBlank { null }, state.replyTarget?.id)
    }

    fun enviarNotaVoz(attachment: CopiedAttachment) {
        val state = _uiState.value
        if (state.isSending) return
        val replyToId = state.replyTarget?.id
        val clientId = UUID.randomUUID().toString()
        val optimistic = MensajeDto(
            id = -(kotlin.math.abs(clientId.hashCode()).coerceAtLeast(1)),
            remitente = "Tú",
            texto = null,
            es_propio = true,
            marca_tiempo = "ahora",
            reply_to_id = replyToId,
            reply_to = state.replyTarget?.let {
                com.probusiness.intranet.data.remote.dto.ReplyToDto(
                    id = it.id,
                    remitente = it.remitente,
                    texto = it.texto,
                    tiene_imagen = it.imagenes.isNotEmpty(),
                    imagen_url = it.imagenes.firstOrNull()?.url,
                )
            },
            imagenes = listOf(
                ImagenDto(
                    url = attachment.file.toURI().toString(),
                    nombre = attachment.displayName,
                ),
            ),
            client_id = clientId,
            estado_envio = "enviando",
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    error = null,
                    replyTarget = null,
                    mensajes = it.mensajes + optimistic,
                )
            }
            supportRepository.enviarMensaje(
                solicitudId = solicitudId,
                texto = null,
                replyToId = replyToId,
                imagenes = listOf(attachment),
            ).onSuccess { mensaje ->
                _uiState.update { current ->
                    current.copy(
                        isSending = false,
                        mensajes = current.mensajes.map { existing ->
                            if (existing.client_id == clientId) {
                                mensaje.withEstado("entregado").copy(client_id = clientId)
                            } else {
                                existing
                            }
                        }.let { lista ->
                            if (lista.none { it.id == mensaje.id || it.client_id == clientId }) {
                                lista + mensaje.withEstado("entregado")
                            } else {
                                lista
                            }
                        },
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { current ->
                    current.copy(
                        isSending = false,
                        error = throwable.message ?: "No se pudo enviar la nota de voz",
                        mensajes = current.mensajes.map { existing ->
                            if (existing.client_id == clientId) existing.copy(estado_envio = "error") else existing
                        },
                    )
                }
            }
        }
    }

    private fun enviar(context: Context, textoForzado: String?, replyToId: Int?) {
        val state = _uiState.value
        val texto = textoForzado?.trim().orEmpty()
        if (texto.isEmpty() && state.adjuntos.isEmpty()) return

        val clientId = UUID.randomUUID().toString()
        val optimistic = MensajeDto(
            id = -(kotlin.math.abs(clientId.hashCode()).coerceAtLeast(1)),
            remitente = "Tú",
            texto = texto.ifBlank { null },
            es_propio = true,
            marca_tiempo = "ahora",
            reply_to_id = replyToId,
            reply_to = state.replyTarget?.let {
                com.probusiness.intranet.data.remote.dto.ReplyToDto(
                    id = it.id,
                    remitente = it.remitente,
                    texto = it.texto,
                    tiene_imagen = it.imagenes.isNotEmpty(),
                    imagen_url = it.imagenes.firstOrNull()?.url,
                )
            },
            imagenes = state.adjuntos.map { ImagenDto(url = it.uri.toString(), nombre = it.displayName) },
            client_id = clientId,
            estado_envio = "enviando",
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    error = null,
                    texto = "",
                    replyTarget = null,
                    adjuntos = emptyList(),
                    mensajes = it.mensajes + optimistic,
                )
            }
            val archivos = state.adjuntos.mapNotNull { UriFileHelper.copyAttachment(context, it.uri) }

            supportRepository.enviarMensaje(
                solicitudId = solicitudId,
                texto = texto.ifBlank { null },
                replyToId = replyToId,
                imagenes = archivos,
            ).onSuccess { mensaje ->
                _uiState.update { current ->
                    current.copy(
                        isSending = false,
                        mensajes = current.mensajes.map { existing ->
                            if (existing.client_id == clientId) {
                                mensaje.withEstado("entregado").copy(client_id = clientId)
                            } else {
                                existing
                            }
                        }.let { lista ->
                            if (lista.none { it.id == mensaje.id || it.client_id == clientId }) {
                                lista + mensaje.withEstado("entregado")
                            } else {
                                lista
                            }
                        },
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { current ->
                    current.copy(
                        isSending = false,
                        error = throwable.message ?: "No se pudo enviar el mensaje",
                        mensajes = current.mensajes.map { existing ->
                            if (existing.client_id == clientId) existing.copy(estado_envio = "error") else existing
                        },
                    )
                }
            }
        }
    }
}

private fun MensajeDto.withEstado(estado: String?): MensajeDto = copy(estado_envio = estado)
