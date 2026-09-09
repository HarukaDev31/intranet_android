package com.probusiness.intranet.ui.support

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.probusiness.intranet.ui.theme.Orange600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTicketScreen(
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
    viewModel: NewTicketViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.createdSolicitudId) {
        uiState.createdSolicitudId?.let(onCreated)
    }

    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris -> viewModel.onImagenesSeleccionadas(uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo ticket", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel("Tipo de solicitud")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.tipoSolicitud == "A",
                    onClick = { viewModel.onTipoChange("A") },
                    label = { Text("Tipo A") },
                    colors = brandChipColors(),
                )
                FilterChip(
                    selected = uiState.tipoSolicitud == "B",
                    onClick = { viewModel.onTipoChange("B") },
                    label = { Text("Tipo B") },
                    colors = brandChipColors(),
                )
            }

            if (uiState.tipoSolicitud == "B") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = (uiState.subtipoB ?: "B1") == "B1",
                        onClick = { viewModel.onSubtipoChange("B1") },
                        label = { Text("B1") },
                        colors = brandChipColors(),
                    )
                    FilterChip(
                        selected = uiState.subtipoB == "B2",
                        onClick = { viewModel.onSubtipoChange("B2") },
                        label = { Text("B2") },
                        colors = brandChipColors(),
                    )
                }
            }

            OutlinedTextField(
                value = uiState.titulo,
                onValueChange = viewModel::onTituloChange,
                label = { Text("Título") },
                colors = brandFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.area,
                onValueChange = viewModel::onAreaChange,
                label = { Text("Área") },
                colors = brandFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.seccionRuta,
                onValueChange = viewModel::onSeccionRutaChange,
                label = { Text("Sección / ruta en la intranet (opcional)") },
                colors = brandFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = { Text("Descripción") },
                minLines = 4,
                colors = brandFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { pickImages.launch("image/*") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null)
                Text(" Adjuntar imágenes (${uiState.imagenes.size})")
            }

            if (uiState.error != null) {
                Text(text = uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.crear(context) },
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
                Text("Crear ticket", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun brandChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Orange600,
    selectedLabelColor = Color.White,
)

@Composable
private fun brandFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Orange600,
    focusedLabelColor = Orange600,
    cursorColor = Orange600,
)
