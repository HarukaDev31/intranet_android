package com.probusiness.intranet.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.probusiness.intranet.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IntranetFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            authRepository.registerFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        if (data["tipo"] != TIPO_SOPORTE_TI_MENSAJE) return

        val title = message.notification?.title ?: "Soporte TI"
        val body = message.notification?.body ?: data["body"] ?: "Tienes un nuevo mensaje"
        val solicitudId = data["solicitud_id"]

        NotificationHelper.showSoporteTiMensaje(applicationContext, title, body, solicitudId)
    }

    companion object {
        const val EXTRA_SOLICITUD_ID = "extra_solicitud_id"
        private const val TIPO_SOPORTE_TI_MENSAJE = "soporte_ti_mensaje"
    }
}
