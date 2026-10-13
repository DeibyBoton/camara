package com.xperiaprocam

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xperiaprocam.presentation.navigation.AppNavHost
import com.xperiaprocam.presentation.theme.XperiaProCamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var hasCameraPermission by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

        setContent {
            XperiaProCamTheme {
                if (hasCameraPermission) {
                    AppNavHost()
                } else {
                    PermissionRationale(
                        onRequest = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun PermissionRationale(onRequest: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Xperia ProCam necesita acceso a la cámara para mostrar la vista previa y capturar fotos. " +
                    "No se requiere ningún otro permiso."
            )
            Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) {
                Text("Conceder permiso de cámara")
            }
        }
    }
}
