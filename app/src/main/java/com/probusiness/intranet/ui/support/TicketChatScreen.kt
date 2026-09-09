package com.probusiness.intranet.ui.support

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.probusiness.intranet.data.remote.dto.MensajeDto
import com.probusiness.intranet.data.remote.dto.SolicitudDto
import kotlinx.coroutines.launch
import com.probusiness.intranet.ui.theme.Orange600
import com.probusiness.intranet.ui.theme.Sky500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketChatScreen(
    onBack: () -> Unit,
    viewModel: TicketChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible >= total - 3
        }
    }

    LaunchedEffect(uiState.mensajes.size) {
        if (uiState.mensajes.isNotEmpty() && isNearBottom) {
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

    var showInfoSheet by remember { mutableStateOf(false) }
    var previewRequest by remember { mutableStateOf<ImagePreviewRequest?>(null) }

    previewRequest?.let { request ->
        ImagePreviewDialog(request = request, onDismiss = { previewRequest = null })
    }

    if (showInfoSheet) {
        ModalBottomSheet(onDismissRequest = { showInfoSheet = false }) {
            TicketInfoSheetContent(uiState.solicitud)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.solicitud?.codigo ?: "Ticket",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = uiState.solicitud?.titulo.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Detalle del ticket")
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                if (uiState.replyTarget != null) {
                    ReplyPreviewBar(mensaje = uiState.replyTarget!!, onCancel = viewModel::onCancelReply)
                }
                if (uiState.imagenes.isNotEmpty()) {
                    Text(
                        text = "${uiState.imagenes.size} imagen(es) adjunta(s)",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { pickImages.launch("image/*") }) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = "Adjuntar imagen")
                        }
                        OutlinedTextField(
                            value = uiState.texto,
                            onValueChange = viewModel::onTextoChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Escribe un mensaje...") },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Orange600,
                                cursorColor = Orange600,
                            ),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = Orange600,
                            modifier = Modifier.size(44.dp),
                        ) {
                            IconButton(
                                onClick = { viewModel.enviar(context) },
                                enabled = !uiState.isSending,
                            ) {
                                if (uiState.isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Send,
                                        contentDescription = "Enviar",
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoadingHeader || uiState.isLoadingMensajes -> ChatSkeleton()
                uiState.mensajesError != null && uiState.mensajes.isEmpty() -> {
                    ChatErrorState(
                        message = uiState.mensajesError.orEmpty(),
                        onRetry = viewModel::reintentarCargarMensajes,
                    )
                }
                uiState.mensajes.isEmpty() -> ChatEmptyState()
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (uiState.isLoadingMore) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                        items(uiState.mensajes, key = { it.id }) { mensaje ->
                            MensajeBubble(
                                mensaje = mensaje,
                                onReply = viewModel::onReplyToMessage,
                                onImageClick = { urls, index -> previewRequest = ImagePreviewRequest(urls, index) },
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isNearBottom && uiState.mensajes.isNotEmpty(),
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (uiState.mensajes.isNotEmpty()) {
                                listState.animateScrollToItem(uiState.mensajes.lastIndex)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Ir al final")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MensajeBubble(
    mensaje: MensajeDto,
    onReply: (MensajeDto) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    if (mensaje.es_sistema) {
        SystemMessageBubble(mensaje)
        return
    }

    val alignment = if (mensaje.es_propio) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (mensaje.es_propio) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    }
    val backgroundColor = if (mensaje.es_propio) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (mensaje.es_propio) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val textColor = if (mensaje.es_propio) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = backgroundColor,
            shape = shape,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(onClick = {}, onLongClick = { onReply(mensaje) }),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!mensaje.es_propio) {
                    Text(
                        text = mensaje.remitente ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                }
                mensaje.reply_to?.let { reply ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(
                                text = reply.remitente ?: "Mensaje",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Orange600,
                            )
                            Text(
                                text = reply.texto ?: if (reply.tiene_imagen) "Imagen adjunta" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (!mensaje.texto.isNullOrBlank()) {
                    Text(text = mensaje.texto, color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
                if (mensaje.imagenes.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(4.dp))
                    ImagenesGrid(
                        imagenes = mensaje.imagenes.map { it.url },
                        onImageClick = { urls, index -> onImageClick(urls, index) },
                    )
                }
                Spacer(modifier = Modifier.size(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = mensaje.marca_tiempo ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (mensaje.es_propio) {
                        Spacer(modifier = Modifier.size(4.dp))
                        LecturaIndicator(leido = mensaje.leido)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagenesGrid(imagenes: List<String?>, onImageClick: (List<String>, Int) -> Unit) {
    val urls = imagenes.filterNotNull()
    if (urls.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        urls.chunked(2).forEachIndexed { filaIndex, fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                fila.forEachIndexed { itemIndex, url ->
                    val globalIndex = filaIndex * 2 + itemIndex
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(urls, globalIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LecturaIndicator(leido: Boolean) {
    Icon(
        imageVector = if (leido) Icons.Filled.DoneAll else Icons.Filled.Done,
        contentDescription = null,
        tint = if (leido) Sky500 else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(14.dp),
    )
}

@Composable
private fun SystemMessageBubble(mensaje: MensajeDto) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Text(
                    text = mensaje.texto.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChatSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val widths = listOf(0.55f, 0.4f, 0.6f, 0.45f, 0.35f, 0.5f)
        widths.forEachIndexed { index, widthFraction ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (index % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(44.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(16.dp))
        androidx.compose.material3.OutlinedButton(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun ChatEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Aún no hay mensajes en este ticket",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReplyPreviewBar(mensaje: MensajeDto, onCancel: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Respondiendo a ${mensaje.remitente ?: "mensaje"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Orange600,
                )
                Text(
                    text = mensaje.texto ?: if (mensaje.imagenes.isNotEmpty()) "Imagen adjunta" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = "Cancelar respuesta")
            }
        }
    }
}

@Composable
private fun TicketInfoSheetContent(solicitud: SolicitudDto?) {
    if (solicitud == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = solicitud.codigo ?: "Ticket #${solicitud.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EstadoPill(
                badge = estadoBadgeStyle(solicitud.estado_codigo),
                text = solicitud.estado?.nombre ?: solicitud.estado_codigo.orEmpty(),
            )
        }

        Text(
            text = solicitud.titulo ?: "(Sin título)",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (!solicitud.descripcion.isNullOrBlank()) {
            InfoRow(label = "Descripción", value = solicitud.descripcion)
        }
        if (!solicitud.area.isNullOrBlank()) {
            InfoRow(label = "Área", value = solicitud.area)
        }
        if (!solicitud.criticidad.isNullOrBlank()) {
            InfoRow(label = "Criticidad", value = solicitud.criticidad)
        }
        if (!solicitud.solicitante.isNullOrBlank()) {
            InfoRow(label = "Solicitante", value = solicitud.solicitante)
        }
        if (!solicitud.pm.isNullOrBlank()) {
            InfoRow(label = "PM asignado", value = solicitud.pm)
        }
        if (!solicitud.analista.isNullOrBlank()) {
            InfoRow(label = "Analista", value = solicitud.analista)
        }
        if (!solicitud.fecha_registro.isNullOrBlank()) {
            InfoRow(label = "Fecha de registro", value = solicitud.fecha_registro)
        }
        if (!solicitud.seccion_ruta.isNullOrBlank()) {
            InfoRow(label = "Sección / ruta", value = solicitud.seccion_ruta)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
