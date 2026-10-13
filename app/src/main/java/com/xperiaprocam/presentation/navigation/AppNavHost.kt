package com.xperiaprocam.presentation.navigation

import androidx.compose.runtime.Composable
import android.content.Intent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xperiaprocam.presentation.camera.CameraScreen
import com.xperiaprocam.presentation.diagnostics.DiagnosticsScreen
import com.xperiaprocam.presentation.settings.SettingsScreen

object Routes {
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    NavHost(navController = navController, startDestination = Routes.CAMERA) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenGallery = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        )
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
            )
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }
    }
}
