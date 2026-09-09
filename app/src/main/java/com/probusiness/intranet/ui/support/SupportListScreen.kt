package com.probusiness.intranet.ui.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.probusiness.intranet.data.remote.dto.SolicitudDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportListScreen(
    onOpenTicket: (Int) -> Unit,
    onNewTicket: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SupportListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Soporte TI") },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTicket) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo ticket")
            }
        },
    ) { padding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading && uiState.solicitudes.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.solicitudes.isEmpty() -> {
                    Text(
                        text = uiState.error.orEmpty(),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.solicitudes.isEmpty() -> {
                    Text(
                        text = "No tienes tickets de soporte todavía",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(uiState.solicitudes, key = { it.id }) { solicitud ->
                            TicketCard(solicitud = solicitud, onClick = { onOpenTicket(solicitud.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(solicitud: SolicitudDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = solicitud.codigo ?: "Ticket #${solicitud.id}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = solicitud.titulo ?: "(Sin título)",
                style = MaterialTheme.typography.titleLarge,
            )
            if (!solicitud.estado?.nombre.isNullOrBlank() || !solicitud.fecha_registro.isNullOrBlank()) {
                Text(
                    text = listOfNotNull(solicitud.estado?.nombre, solicitud.fecha_registro).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
