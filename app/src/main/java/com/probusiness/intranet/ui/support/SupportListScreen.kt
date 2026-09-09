package com.probusiness.intranet.ui.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.probusiness.intranet.data.remote.dto.SolicitudDto
import com.probusiness.intranet.ui.theme.AppBackgroundLight
import com.probusiness.intranet.ui.theme.Orange600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportListScreen(
    onOpenTicket: (Int) -> Unit,
    onNewTicket: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SupportListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val filteredSolicitudes = remember(uiState.solicitudes, selectedFilter) {
        if (selectedFilter == null) {
            uiState.solicitudes
        } else {
            uiState.solicitudes.filter { it.estado_codigo == selectedFilter }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Soporte TI", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackgroundLight),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewTicket,
                containerColor = Orange600,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nuevo ticket") },
            )
        },
        containerColor = AppBackgroundLight,
    ) { padding: PaddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.solicitudes.isNotEmpty(),
            onRefresh = { viewModel.load() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading && uiState.solicitudes.isEmpty() -> TicketListSkeleton()
                uiState.error != null && uiState.solicitudes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error.orEmpty(),
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                uiState.solicitudes.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { StatsRow(uiState.solicitudes) }
                        item {
                            EstadoFilterRow(
                                selected = selectedFilter,
                                onSelect = { selectedFilter = it },
                            )
                        }
                        if (filteredSolicitudes.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay tickets con este filtro",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp),
                                )
                            }
                        }
                        itemsIndexed(filteredSolicitudes, key = { _, item -> item.id }) { index, solicitud ->
                            AnimatedEntrance(index) {
                                TicketCard(solicitud = solicitud, onClick = { onOpenTicket(solicitud.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedEntrance(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, delayMillis = (index * 40).coerceAtMost(320))) +
            slideInVertically(
                initialOffsetY = { it / 8 },
                animationSpec = tween(300, delayMillis = (index * 40).coerceAtMost(320)),
            ),
    ) {
        content()
    }
}

@Composable
private fun StatsRow(solicitudes: List<SolicitudDto>) {
    val total = solicitudes.size
    val pendientes = solicitudes.count { it.estado_codigo == "pendiente" }
    val enProgreso = solicitudes.count { it.estado_codigo == "en_progreso" || it.estado_codigo == "en_maqueta" }
    val completadas = solicitudes.count { it.estado_codigo == "hecho" || it.estado_codigo == "desplegado" || it.estado_codigo == "operativo" }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("Total", total, StatsKind.TOTAL, Modifier.weight(1f))
        StatCard("Pendientes", pendientes, StatsKind.PENDIENTES, Modifier.weight(1f))
        StatCard("En curso", enProgreso, StatsKind.EN_PROGRESO, Modifier.weight(1f))
        StatCard("Listas", completadas, StatsKind.COMPLETADAS, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: Int, kind: StatsKind, modifier: Modifier = Modifier) {
    val accent = statsCardAccentColor(kind)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value.toString(), style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val ESTADO_FILTROS = listOf(
    null to "Todos",
    "pendiente" to "Pendiente",
    "en_maqueta" to "En maqueta",
    "en_progreso" to "En progreso",
    "hecho" to "Hecho",
    "desplegado" to "Desplegado",
    "observado" to "Observado",
    "operativo" to "Operativo",
)

@Composable
private fun EstadoFilterRow(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
    ) {
        ESTADO_FILTROS.forEach { (codigo, label) ->
            FilterChip(
                selected = selected == codigo,
                onClick = { onSelect(codigo) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Orange600,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun TicketCard(solicitud: SolicitudDto, onClick: () -> Unit) {
    val badge = estadoBadgeStyle(solicitud.estado_codigo)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = solicitud.codigo ?: "Ticket #${solicitud.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EstadoPill(badge = badge, text = solicitud.estado?.nombre ?: solicitud.estado_codigo.orEmpty())
            }

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = solicitud.titulo ?: "(Sin título)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!solicitud.area.isNullOrBlank() || !solicitud.fecha_registro.isNullOrBlank()) {
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = listOfNotNull(solicitud.area, solicitud.fecha_registro).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun EstadoPill(badge: BadgeStyle, text: String) {
    if (text.isBlank()) return
    Surface(
        color = badge.background,
        contentColor = badge.foreground,
        border = BorderStroke(1.dp, badge.border),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "No tienes tickets de soporte todavía",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TicketListSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
            )
        }
    }
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {}
}
