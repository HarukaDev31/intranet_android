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
        val tipo = data["tipo"]
        if (tipo != TIPO_SOPORTE_TI_MENSAJE && tipo != TIPO_SOPORTE_TI_SOLICITUD_CREADA) return

        val title = message.notification?.title ?: "Soporte TI"
        val body = message.notification?.body ?: data["body"] ?: "Tienes una notificación nueva"
        val solicitudId = data["solicitud_id"]

        NotificationHelper.showSoporteTiMensaje(applicationContext, title, body, solicitudId)
    }

    companion object {
        const val EXTRA_SOLICITUD_ID = "extra_solicitud_id"
        private const val TIPO_SOPORTE_TI_MENSAJE = "soporte_ti_mensaje"
        private const val TIPO_SOPORTE_TI_SOLICITUD_CREADA = "soporte_ti_solicitud_creada"
    }
}
