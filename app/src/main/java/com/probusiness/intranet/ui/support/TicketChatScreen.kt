package com.probusiness.intranet.ui.support

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.probusiness.intranet.data.remote.dto.MensajeDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketChatScreen(
    onBack: () -> Unit,
    viewModel: TicketChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.mensajes.size) {
        if (uiState.mensajes.isNotEmpty()) {
            listState.animateScrollToItem(uiState.mensajes.lastIndex)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex == 0) {
            viewModel.cargarMasAntiguos()
        }
    }

    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris -> viewModel.onImagenesSeleccionadas(uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.solicitud?.codigo ?: "Ticket")
                        Text(
                            text = uiState.solicitud?.titulo.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { pickImages.launch("image/*") }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Adjuntar imagen")
                    }
                    OutlinedTextField(
                        value = uiState.texto,
                        onValueChange = viewModel::onTextoChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe un mensaje...") },
                    )
                    IconButton(
                        onClick = { viewModel.enviar(context) },
                        enabled = !uiState.isSending,
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        } else {
                            Icon(Icons.Filled.Send, contentDescription = "Enviar")
                        }
                    }
                }
                if (uiState.imagenes.isNotEmpty()) {
                    Text(
                        text = "${uiState.imagenes.size} imagen(es) adjunta(s)",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
    ) { padding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.isLoadingHeader) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (uiState.isLoadingMore) {
                        item {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        }
                    }
                    items(uiState.mensajes, key = { it.id }) { mensaje ->
                        MensajeBubble(mensaje)
                    }
                }
            }
        }
    }
}

@Composable
private fun MensajeBubble(mensaje: MensajeDto) {
    val alignment = if (mensaje.es_propio) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (mensaje.es_propio) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (mensaje.es_propio) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (!mensaje.es_propio) {
                    Text(
                        text = mensaje.remitente ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                    )
                }
                if (!mensaje.texto.isNullOrBlank()) {
                    Text(text = mensaje.texto, color = textColor)
                }
                if (mensaje.imagenes.isNotEmpty()) {
                    Text(
                        text = "📎 ${mensaje.imagenes.size} imagen(es)",
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = mensaje.marca_tiempo ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                )
            }
        }
    }
}
