package com.probusiness.intranet.ui.support

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
                title = { Text("Nuevo ticket") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row2(uiState.tipoSolicitud, onTipoA = { viewModel.onTipoChange("A") }, onTipoB = { viewModel.onTipoChange("B") })

            if (uiState.tipoSolicitud == "B") {
                Row2Sub(
                    subtipo = uiState.subtipoB ?: "B1",
                    onB1 = { viewModel.onSubtipoChange("B1") },
                    onB2 = { viewModel.onSubtipoChange("B2") },
                )
            }

            OutlinedTextField(
                value = uiState.titulo,
                onValueChange = viewModel::onTituloChange,
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.area,
                onValueChange = viewModel::onAreaChange,
                label = { Text("Área") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.seccionRuta,
                onValueChange = viewModel::onSeccionRutaChange,
                label = { Text("Sección / ruta en la intranet (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = { Text("Descripción") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { pickImages.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null)
                Text(" Adjuntar imágenes (${uiState.imagenes.size})")
            }

            if (uiState.error != null) {
                Text(text = uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.crear(context) },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text("Crear ticket")
            }
        }
    }
}

@Composable
private fun Row2(tipo: String, onTipoA: () -> Unit, onTipoB: () -> Unit) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = tipo == "A", onClick = onTipoA, label = { Text("Tipo A") })
        FilterChip(selected = tipo == "B", onClick = onTipoB, label = { Text("Tipo B") })
    }
}

@Composable
private fun Row2Sub(subtipo: String, onB1: () -> Unit, onB2: () -> Unit) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = subtipo == "B1", onClick = onB1, label = { Text("B1") })
        FilterChip(selected = subtipo == "B2", onClick = onB2, label = { Text("B2") })
    }
}
