package com.probusiness.intranet.ui.support

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.probusiness.intranet.data.repository.SupportRepository
import com.probusiness.intranet.util.UriFileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewTicketUiState(
    val tipoSolicitud: String = "A",
    val subtipoB: String? = null,
    val titulo: String = "",
    val area: String = "",
    val seccionRuta: String = "",
    val descripcion: String = "",
    val imagenes: List<Uri> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val createdSolicitudId: Int? = null,
)

@HiltViewModel
class NewTicketViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTicketUiState())
    val uiState: StateFlow<NewTicketUiState> = _uiState

    fun onTipoChange(tipo: String) = _uiState.update {
        it.copy(tipoSolicitud = tipo, subtipoB = if (tipo == "B") it.subtipoB ?: "B1" else null)
    }

    fun onSubtipoChange(subtipo: String) = _uiState.update { it.copy(subtipoB = subtipo) }
    fun onTituloChange(value: String) = _uiState.update { it.copy(titulo = value, error = null) }
    fun onAreaChange(value: String) = _uiState.update { it.copy(area = value, error = null) }
    fun onSeccionRutaChange(value: String) = _uiState.update { it.copy(seccionRuta = value) }
    fun onDescripcionChange(value: String) = _uiState.update { it.copy(descripcion = value, error = null) }

    fun onImagenesSeleccionadas(uris: List<Uri>) = _uiState.update { it.copy(imagenes = uris) }

    fun crear(context: Context) {
        val state = _uiState.value
        if (state.titulo.isBlank() || state.area.isBlank() || state.descripcion.isBlank()) {
            _uiState.update { it.copy(error = "Completa título, área y descripción") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val archivos = state.imagenes.mapNotNull { UriFileHelper.copyToCache(context, it) }

            supportRepository.crearSolicitud(
                tipoSolicitud = state.tipoSolicitud,
                subtipoB = state.subtipoB,
                titulo = state.titulo.trim(),
                area = state.area.trim(),
                seccionRuta = state.seccionRuta.trim().ifBlank { null },
                descripcion = state.descripcion.trim(),
                imagenes = archivos,
            ).onSuccess { solicitud ->
                _uiState.update { it.copy(isSaving = false, createdSolicitudId = solicitud.id) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isSaving = false, error = throwable.message ?: "No se pudo crear el ticket")
                }
            }
        }
    }
}
