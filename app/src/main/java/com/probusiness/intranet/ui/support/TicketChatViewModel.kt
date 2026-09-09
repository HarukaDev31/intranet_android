package com.probusiness.intranet.ui.support

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.probusiness.intranet.data.remote.dto.MensajeDto
import com.probusiness.intranet.data.remote.dto.SolicitudDto
import com.probusiness.intranet.data.repository.SupportRepository
import com.probusiness.intranet.ui.navigation.Routes
import com.probusiness.intranet.util.UriFileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketChatUiState(
    val isLoadingHeader: Boolean = true,
    val solicitud: SolicitudDto? = null,
    val mensajes: List<MensajeDto> = emptyList(),
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val texto: String = "",
    val imagenes: List<Uri> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TicketChatViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val solicitudId: Int = checkNotNull(savedStateHandle[Routes.TICKET_CHAT_ARG])

    private val _uiState = MutableStateFlow(TicketChatUiState())
    val uiState: StateFlow<TicketChatUiState> = _uiState

    init {
        cargarSolicitudYMensajes()
    }

    private fun cargarSolicitudYMensajes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHeader = true, error = null) }
            supportRepository.obtenerSolicitud(solicitudId)
                .onSuccess { solicitud ->
                    _uiState.update { it.copy(isLoadingHeader = false, solicitud = solicitud) }
                    solicitud.chat_uuid?.let { cargarMensajesIniciales(it) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingHeader = false, error = throwable.message ?: "No se pudo cargar el ticket")
                    }
                }
        }
    }

    private fun cargarMensajesIniciales(chatUuid: String) {
        viewModelScope.launch {
            supportRepository.listarMensajes(chatUuid).onSuccess { response ->
                _uiState.update {
                    it.copy(mensajes = response.data, hasMore = response.pagination?.has_more ?: false)
                }
                marcarComoLeidos(chatUuid, response.data)
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

    fun onTextoChange(value: String) = _uiState.update { it.copy(texto = value) }
    fun onImagenesSeleccionadas(uris: List<Uri>) = _uiState.update { it.copy(imagenes = uris) }

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
                replyToId = null,
                imagenes = archivos,
            ).onSuccess { mensaje ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        texto = "",
                        imagenes = emptyList(),
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
