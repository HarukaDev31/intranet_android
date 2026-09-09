package com.probusiness.intranet.data.remote.realtime

import com.probusiness.intranet.data.local.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cliente WebSocket (Laravel Reverb, protocolo Pusher) para actualizaciones en tiempo real —
 * misma infraestructura que usa el frontend web (Laravel Echo / pusher-js). Implementado sobre
 * ReverbSocket (cliente propio, ver esa clase) en vez de una librería Pusher de terceros.
 */
@Singleton
class RealtimeService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
) {
    private val socket = ReverbSocket(LaravelChannelAuthorizer(sessionManager))

    fun subscribeToChat(chatUuid: String, onMensajeCreadoJson: (String) -> Unit) {
        val channelName = "private-soporte-ti.chat.$chatUuid"
        socket.subscribe(channelName) { eventName, data ->
            if (eventName != "SoporteTiMensajeCreado") return@subscribe
            val mensajeJson = runCatching {
                JSONObject(data).optJSONObject("mensaje")?.toString()
            }.getOrNull() ?: return@subscribe
            onMensajeCreadoJson(mensajeJson)
        }
    }

    fun unsubscribeFromChat(chatUuid: String) {
        socket.unsubscribe("private-soporte-ti.chat.$chatUuid")
    }

    /** Se suscribe al canal del staff para enterarse de tickets nuevos en tiempo real. */
    fun subscribeToStaffTickets(onSolicitudCreadaJson: (String) -> Unit) {
        socket.subscribe("private-soporte-ti.staff") { eventName, data ->
            if (eventName != "SoporteTiSolicitudCreada") return@subscribe
            val solicitudJson = runCatching {
                JSONObject(data).optJSONObject("solicitud")?.toString()
            }.getOrNull() ?: return@subscribe
            onSolicitudCreadaJson(solicitudJson)
        }
    }

    fun unsubscribeFromStaffTickets() {
        socket.unsubscribe("private-soporte-ti.staff")
    }
}
