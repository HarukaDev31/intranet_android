package com.probusiness.intranet.ui.support

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.probusiness.intranet.data.remote.dto.MensajeDto
import com.probusiness.intranet.data.remote.dto.SolicitudDto
import com.probusiness.intranet.data.remote.realtime.ActiveChatTracker
import com.probusiness.intranet.data.remote.realtime.RealtimeService
import com.probusiness.intranet.data.repository.SupportRepository
import com.probusiness.intranet.ui.navigation.Routes
import com.probusiness.intranet.util.UriFileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
    val imagenes: List<Uri> = emptyList(),
    val error: String? = null,
    val mensajesError: String? = null,
    val replyTarget: MensajeDto? = null,
    val isUpdatingGestion: Boolean = false,
    val gestionError: String? = null,
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
        realtimeService.subscribeToChat(chatUuid) { mensajeJson ->
            val nuevo = runCatching { json.decodeFromString(MensajeDto.serializer(), mensajeJson) }.getOrNull()
                ?: return@subscribeToChat
            _uiState.update { state ->
                if (state.mensajes.any { it.id == nuevo.id }) return@update state
                state.copy(mensajes = state.mensajes + nuevo)
            }
            if (!nuevo.leido && !nuevo.es_propio) {
                marcarComoLeidos(chatUuid, listOf(nuevo))
            }
        }
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
                            mensajes = response.data,
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
        val oldestId = state.mensajes.firstOrNull()?.id ?: return
        if (!state.hasMore || state.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            supportRepository.listarMensajes(chatUuid, beforeId = oldestId)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            mensajes = response.data + it.mensajes,
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
        val idsNoLeidos = mensajes.filter { !it.leido && !it.es_propio }.map { it.id }
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

    fun onTextoChange(value: String) = _uiState.update { it.copy(texto = value) }
    fun onImagenesSeleccionadas(uris: List<Uri>) = _uiState.update { it.copy(imagenes = uris) }
    fun onReplyToMessage(mensaje: MensajeDto) = _uiState.update { it.copy(replyTarget = mensaje) }
    fun onCancelReply() = _uiState.update { it.copy(replyTarget = null) }

    fun enviar(context: Context) {
        val state = _uiState.value
        val texto = state.texto.trim()
        if (texto.isEmpty() && state.imagenes.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            val archivos = state.imagenes.mapNotNull { UriFileHelper.copyToCache(context, it) }

            supportRepository.enviarMensaje(
                solicitudId = solicitudId,
                texto = texto.ifBlank { null },
                replyToId = state.replyTarget?.id,
                imagenes = archivos,
            ).onSuccess { mensaje ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        texto = "",
                        imagenes = emptyList(),
                        replyTarget = null,
                        mensajes = it.mensajes + mensaje,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isSending = false, error = throwable.message ?: "No se pudo enviar el mensaje")
                }
            }
        }
    }
}
