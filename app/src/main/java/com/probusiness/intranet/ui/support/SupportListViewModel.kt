package com.probusiness.intranet.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.probusiness.intranet.data.remote.dto.SolicitudDto
import com.probusiness.intranet.data.repository.AuthRepository
import com.probusiness.intranet.data.repository.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportListUiState(
    val isLoading: Boolean = false,
    val solicitudes: List<SolicitudDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SupportListViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportListUiState())
    val uiState: StateFlow<SupportListUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            supportRepository.listarSolicitudes()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, solicitudes = list) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "No se pudieron cargar los tickets")
                    }
                }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
