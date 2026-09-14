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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.probusiness.intranet.data.remote.dto.ImagenDto
import com.probusiness.intranet.data.remote.dto.MensajeDto
import com.probusiness.intranet.data.remote.dto.SolicitudDto
import com.probusiness.intranet.ui.theme.Green600
import com.probusiness.intranet.ui.theme.Orange600
import com.probusiness.intranet.ui.theme.Sky500
import com.probusiness.intranet.util.PendingAttachment
import com.probusiness.intranet.util.extensionOf
import com.probusiness.intranet.util.isInlineImage
import com.probusiness.intranet.util.openAttachment
import kotlinx.coroutines.launch

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
    ) { uris -> viewModel.onAdjuntosSeleccionados(context, uris) }
    val pickFiles = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.onAdjuntosSeleccionados(context, uris) }

    var showInfoSheet by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }
    var previewRequest by remember { mutableStateOf<ImagePreviewRequest?>(null) }

    LaunchedEffect(uiState.scrollToMessageId, uiState.mensajes.size) {
        val targetId = uiState.scrollToMessageId ?: return@LaunchedEffect
        val index = uiState.mensajes.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            viewModel.onScrolledToMessage()
        }
    }

    previewRequest?.let { request ->
        ImagePreviewDialog(request = request, onDismiss = { previewRequest = null })
    }

    if (showInfoSheet) {
        ModalBottomSheet(onDismissRequest = { showInfoSheet = false }) {
            TicketInfoSheetContent(
                solicitud = uiState.solicitud,
                isUpdating = uiState.isUpdatingGestion,
                error = uiState.gestionError,
                onCambiarEstado = viewModel::cambiarEstado,
                onCambiarComplejidad = viewModel::cambiarComplejidad,
            )
        }
    }

    if (showEmojiSheet) {
        ModalBottomSheet(onDismissRequest = { showEmojiSheet = false }) {
            EmojiPickerSheet(
                onPick = { emoji ->
                    viewModel.insertarEmoji(emoji)
                    showEmojiSheet = false
                },
            )
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
                if (uiState.error != null) {
                    Text(
                        text = uiState.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (uiState.replyTarget != null) {
                    ReplyPreviewBar(mensaje = uiState.replyTarget!!, onCancel = viewModel::onCancelReply)
                }
                if (uiState.adjuntos.isNotEmpty()) {
                    PendingAttachmentsRow(
                        adjuntos = uiState.adjuntos,
                        onRemove = viewModel::quitarAdjunto,
                    )
                }
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            IconButton(onClick = { showAttachMenu = true }) {
                                Icon(Icons.Outlined.AttachFile, contentDescription = "Adjuntar")
                            }
                            DropdownMenu(expanded = showAttachMenu, onDismissRequest = { showAttachMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Imágenes") },
                                    leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                                    onClick = {
                                        showAttachMenu = false
                                        pickImages.launch("image/*")
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Documento") },
                                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                                    onClick = {
                                        showAttachMenu = false
                                        pickFiles.launch(arrayOf("*/*"))
                                    },
                                )
                            }
                        }
                        IconButton(onClick = { showEmojiSheet = true }) {
                            Icon(Icons.Filled.InsertEmoticon, contentDescription = "Emoji")
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
                        items(uiState.mensajes, key = { "${it.client_id ?: it.id}" }) { mensaje ->
                            MensajeBubble(
                                mensaje = mensaje,
                                puedeMarcarRevisado = uiState.solicitud?.gestion?.puede_marcar_revisado == true,
                                onReply = viewModel::onReplyToMessage,
                                onImageClick = { urls, index -> previewRequest = ImagePreviewRequest(urls, index) },
                                onOpenDocument = { url, nombre -> openAttachment(context, url) },
                                onJumpToReply = viewModel::irAlMensaje,
                                onRetry = { viewModel.reintentarEnvio(context, mensaje) },
                                onToggleRevisado = viewModel::toggleRevisado,
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
    puedeMarcarRevisado: Boolean,
    onReply: (MensajeDto) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onOpenDocument: (String, String) -> Unit,
    onJumpToReply: (Int?) -> Unit,
    onRetry: () -> Unit,
    onToggleRevisado: (MensajeDto) -> Unit,
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
    val backgroundColor = when {
        mensaje.revisado -> Green600.copy(alpha = 0.12f)
        mensaje.es_propio -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        mensaje.revisado -> Green600.copy(alpha = 0.4f)
        mensaje.es_propio -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val textColor = if (mensaje.es_propio) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (mensaje.es_propio) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!mensaje.es_propio) {
                ChatAvatar(url = mensaje.avatar_url, iniciales = mensaje.iniciales, colorHex = mensaje.color)
                Spacer(modifier = Modifier.size(6.dp))
            }
            Surface(
            color = backgroundColor,
            shape = shape,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = { if (mensaje.estado_envio == "error") onRetry() },
                    onLongClick = { onReply(mensaje) },
                ),
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
                            .padding(bottom = 6.dp)
                            .clickable { onJumpToReply(reply.id) },
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(
                                text = reply.remitente ?: "Mensaje",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Orange600,
                            )
                            Text(
                                text = reply.texto ?: if (reply.tiene_imagen) "Adjunto" else "",
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
                    AdjuntosMensaje(
                        adjuntos = mensaje.imagenes,
                        onImageClick = onImageClick,
                        onOpenDocument = onOpenDocument,
                    )
                }
                Spacer(modifier = Modifier.size(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (mensaje.estado_envio == "error") {
                        Text(
                            text = "Reintentar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                    Text(
                        text = mensaje.marca_tiempo ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (mensaje.es_propio) {
                        Spacer(modifier = Modifier.size(4.dp))
                        LecturaIndicator(estado = mensaje.estado_envio, leido = mensaje.leido)
                    }
                    if (puedeMarcarRevisado && mensaje.id > 0) {
                        Spacer(modifier = Modifier.size(2.dp))
                        Icon(
                            imageVector = if (mensaje.revisado) Icons.Filled.CheckCircle else Icons.Filled.Done,
                            contentDescription = if (mensaje.revisado) "Quitar hecho" else "Marcar como hecho",
                            tint = if (mensaje.revisado) Green600 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onToggleRevisado(mensaje) },
                        )
                    } else if (mensaje.revisado) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Hecho",
                            tint = Green600,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun AdjuntosMensaje(
    adjuntos: List<ImagenDto>,
    onImageClick: (List<String>, Int) -> Unit,
    onOpenDocument: (String, String) -> Unit,
) {
    val imagenes = adjuntos.filter { isInlineImage(it.nombre, null) && !it.url.isNullOrBlank() }
    val documentos = adjuntos.filterNot { isInlineImage(it.nombre, null) }
    val imageUrls = imagenes.mapNotNull { it.url }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (imageUrls.isNotEmpty()) {
            imageUrls.chunked(2).forEachIndexed { filaIndex, fila ->
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
                                .clickable { onImageClick(imageUrls, globalIndex) },
                        )
                    }
                }
            }
        }
        documentos.forEach { doc ->
            val url = doc.url ?: return@forEach
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDocument(url, doc.nombre ?: "archivo") },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.nombre ?: "Documento",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = listOfNotNull(extensionOf(doc.nombre), doc.tamano).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LecturaIndicator(estado: String?, leido: Boolean) {
    val icon = when (estado) {
        "enviando", "pendiente" -> Icons.Outlined.Schedule
        "error" -> Icons.Outlined.ErrorOutline
        "leido" -> Icons.Filled.DoneAll
        else -> if (leido) Icons.Filled.DoneAll else Icons.Filled.Done
    }
    val tint = when (estado) {
        "error" -> MaterialTheme.colorScheme.error
        "leido" -> Sky500
        else -> if (leido) Sky500 else MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
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
private fun ChatAvatar(url: String?, iniciales: String?, colorHex: String?) {
    val bg = remember(colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrElse { Sky500 }
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = (iniciales ?: "?").take(2),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PendingAttachmentsRow(
    adjuntos: List<PendingAttachment>,
    onRemove: (android.net.Uri) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        adjuntos.forEach { adjunto ->
            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 1.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    if (isInlineImage(adjunto.displayName, adjunto.mime)) {
                        AsyncImage(
                            model = adjunto.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = adjunto.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .widthIn(max = 120.dp),
                        maxLines = 1,
                    )
                    IconButton(onClick = { onRemove(adjunto.uri) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Quitar adjunto")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiPickerSheet(onPick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Emojis",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            gridItems(SOPORTE_TI_CHAT_EMOJIS) { emoji ->
                Text(
                    text = emoji,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onPick(emoji) }
                        .padding(4.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
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

private val COMPLEJIDAD_OPCIONES = listOf("Baja", "Media", "Alta", "Máxima")

@Composable
private fun TicketInfoSheetContent(
    solicitud: SolicitudDto?,
    isUpdating: Boolean,
    error: String?,
    onCambiarEstado: (String) -> Unit,
    onCambiarComplejidad: (String) -> Unit,
) {
    if (solicitud == null) return
    val gestion = solicitud.gestion

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // --- Encabezado ---
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
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            }
        }

        Text(
            text = solicitud.titulo ?: "(Sin título)",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (!solicitud.descripcion.isNullOrBlank()) {
            InfoRow(label = "Descripción", value = solicitud.descripcion)
        }

        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        // --- Gestión (solo si el backend habilita la acción para este usuario) ---
        val puedeComplejidad = gestion != null &&
            (gestion.puede_complejidad || gestion.puede_complejidad_pm || gestion.puede_complejidad_analista)

        if (gestion != null && (gestion.puede_estado || puedeComplejidad)) {
            HorizontalDivider()
            Text(
                text = "Gestión",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            if (gestion.puede_estado && gestion.estados.isNotEmpty()) {
                SectionLabel("Estado")
                val actual = gestion.estado_valor ?: solicitud.estado_codigo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    gestion.estados.forEach { opcion ->
                        val selected = opcion.codigo == actual
                        val bloqueadoEnProgreso = opcion.codigo == "en_progreso" && !gestion.puede_en_progreso && !selected
                        val enabled = !isUpdating && (selected || (gestion.estado_editable && !bloqueadoEnProgreso))
                        FilterChip(
                            selected = selected,
                            enabled = enabled,
                            onClick = { if (enabled && !selected) onCambiarEstado(opcion.codigo) },
                            label = { Text(opcion.nombre) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Orange600,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }

            if (puedeComplejidad) {
                val actual = gestion.complejidad_pm_valor ?: gestion.complejidad_analista_valor ?: gestion.complejidad_valor
                SectionLabel("Complejidad")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COMPLEJIDAD_OPCIONES.forEach { opcion ->
                        FilterChip(
                            selected = opcion == actual,
                            onClick = { if (!isUpdating) onCambiarComplejidad(opcion) },
                            label = { Text(opcion) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Orange600,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }
            HorizontalDivider()
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
