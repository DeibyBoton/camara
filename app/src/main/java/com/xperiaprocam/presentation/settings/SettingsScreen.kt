package com.xperiaprocam.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingRow("Grid de composición", settings.showGrid, viewModel::setShowGrid)
            SettingRow("Nivel horizontal", settings.showLevel, viewModel::setShowLevel)
            SettingRow("Feedback háptico", settings.hapticFeedback, viewModel::setHaptics)
            SettingRow("Captura RAW / DNG (junto a JPEG)", settings.rawCapture, viewModel::setRawCapture)

            Text("Temporizador por defecto")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 5, 10).forEach { s ->
                    FilterChip(
                        selected = settings.defaultTimerSeconds == s,
                        onClick = { viewModel.setTimer(s) },
                        label = { Text(if (s == 0) "Off" else "${s}s") },
                    )
                }
            }

            Text("Aspecto creativo (Perfil de color)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.xperiaprocam.domain.model.CaptureLook.values().forEach { look ->
                    FilterChip(
                        selected = settings.look == look,
                        onClick = { viewModel.setLook(look) },
                        label = { Text(look.label) },
                    )
                }
            }

            androidx.compose.material3.OutlinedButton(onClick = onOpenDiagnostics) {
                Text("Diagnóstico de cámara")
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = value, onCheckedChange = onChange)
    }
}
