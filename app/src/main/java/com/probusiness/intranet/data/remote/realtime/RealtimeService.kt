package com.probusiness.intranet.data.remote.realtime

import com.probusiness.intranet.BuildConfig
import com.probusiness.intranet.data.local.SessionManager
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cliente WebSocket (Laravel Reverb, protocolo Pusher) para actualizaciones en tiempo real —
 * misma infraestructura que usa el frontend web (Laravel Echo / pusher-js).
 */
@Singleton
class RealtimeService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
) {
    private var pusher: Pusher? = null

    @Synchronized
    private fun client(): Pusher {
        pusher?.let { return it }

        val options = PusherOptions()
            .setHost(BuildConfig.REVERB_HOST)
            .setWsPort(BuildConfig.REVERB_PORT)
            .setWssPort(BuildConfig.REVERB_PORT)
            .setUseTLS(BuildConfig.REVERB_USE_TLS)
            .setChannelAuthorizer(LaravelChannelAuthorizer(sessionManager))

        val instance = Pusher(BuildConfig.REVERB_APP_KEY, options)
        instance.connect()
        pusher = instance
        return instance
    }

    /**
     * Se suscribe al hilo de un ticket puntual y entrega el JSON crudo del campo `mensaje`
     * cada vez que llega el evento SoporteTiMensajeCreado.
     */
    fun subscribeToChat(chatUuid: String, onMensajeCreadoJson: (String) -> Unit) {
        val channelName = "private-soporte-ti.chat.$chatUuid"
        client().subscribePrivate(
            channelName,
            object : PrivateChannelEventListener {
                override fun onAuthenticationFailure(message: String, e: Exception) {
                    android.util.Log.w("RealtimeService", "Auth de canal falló ($channelName): $message", e)
                }

                override fun onSubscriptionSucceeded(channelName: String) {}

                override fun onEvent(event: PusherEvent) {
                    if (event.eventName != "SoporteTiMensajeCreado") return
                    val mensajeJson = runCatching {
                        org.json.JSONObject(event.data).optJSONObject("mensaje")?.toString()
                    }.getOrNull() ?: return
                    onMensajeCreadoJson(mensajeJson)
                }
            },
        )
    }

    fun unsubscribeFromChat(chatUuid: String) {
        pusher?.unsubscribe("private-soporte-ti.chat.$chatUuid")
    }

    /** Se suscribe al canal del staff para enterarse de tickets nuevos en tiempo real. */
    fun subscribeToStaffTickets(onSolicitudCreadaJson: (String) -> Unit) {
        client().subscribePrivate(
            "private-soporte-ti.staff",
            object : PrivateChannelEventListener {
                override fun onAuthenticationFailure(message: String, e: Exception) {
                    android.util.Log.w("RealtimeService", "Auth de canal staff falló: $message", e)
                }

                override fun onSubscriptionSucceeded(channelName: String) {}

                override fun onEvent(event: PusherEvent) {
                    if (event.eventName != "SoporteTiSolicitudCreada") return
                    val solicitudJson = runCatching {
                        org.json.JSONObject(event.data).optJSONObject("solicitud")?.toString()
                    }.getOrNull() ?: return
                    onSolicitudCreadaJson(solicitudJson)
                }
            },
        )
    }

    fun unsubscribeFromStaffTickets() {
        pusher?.unsubscribe("private-soporte-ti.staff")
    }
}
