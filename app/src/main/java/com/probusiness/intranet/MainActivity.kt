package com.probusiness.intranet

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.probusiness.intranet.notifications.IntranetFirebaseMessagingService
import com.probusiness.intranet.ui.navigation.IntranetNavGraph
import com.probusiness.intranet.ui.theme.IntranetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingSolicitudId by mutableStateOf<Int?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingSolicitudId = extractSolicitudId(intent)
        requestNotificationPermissionIfNeeded()

        setContent {
            IntranetTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    IntranetNavGraph(pendingSolicitudId = pendingSolicitudId)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSolicitudId = extractSolicitudId(intent)
    }

    private fun extractSolicitudId(intent: android.content.Intent?): Int? {
        // Con la app en foreground usamos nuestro extra propio (NotificationHelper);
        // si Android mostró la notificación automáticamente (app en background/cerrada
        // con payload notification+data), el tap abre la app con las claves originales
        // del data payload de FCM, de ahí el fallback a "solicitud_id".
        val raw = intent?.getStringExtra(IntranetFirebaseMessagingService.EXTRA_SOLICITUD_ID)
            ?: intent?.getStringExtra("solicitud_id")
        return raw?.toIntOrNull()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
