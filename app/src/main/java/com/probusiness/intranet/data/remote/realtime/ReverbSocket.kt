package com.probusiness.intranet.data.remote.realtime

import android.util.Log
import com.probusiness.intranet.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "ReverbSocket"

/**
 * Cliente mínimo del protocolo Pusher (usado por Laravel Reverb) sobre WebSocket puro de OkHttp.
 * Se implementa a mano (en vez de depender de una librería tipo pusher-java-client, poco
 * mantenida y con incompatibilidades reportadas con Reverb) para tener control y visibilidad
 * total de cada frame — clave para poder diagnosticar problemas de entrega en tiempo real.
 */
class ReverbSocket(
    private val authorizer: LaravelChannelAuthorizer,
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var socketId: String? = null
    private val pendingSubscriptions = mutableSetOf<String>()
    private val listenersByChannel = mutableMapOf<String, MutableList<(eventName: String, data: String) -> Unit>>()

    @Synchronized
    fun connectIfNeeded() {
        if (webSocket != null) return

        val scheme = if (BuildConfig.REVERB_USE_TLS) "wss" else "ws"
        val url = "$scheme://${BuildConfig.REVERB_HOST}:${BuildConfig.REVERB_PORT}/app/${BuildConfig.REVERB_APP_KEY}" +
            "?protocol=7&client=android-native&version=1.0.0&flash=false"

        Log.i(TAG, "Conectando a $url")

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "onOpen (handshake WS OK, esperando pusher:connection_established)")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.i(TAG, "Frame recibido: $text")
                handleFrame(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "onClosing code=$code reason=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "onClosed code=$code reason=$reason")
                this@ReverbSocket.webSocket = null
                socketId = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "onFailure: ${t.javaClass.simpleName}: ${t.message} (httpCode=${response?.code})", t)
                this@ReverbSocket.webSocket = null
                socketId = null
            }
        })
    }

    private fun handleFrame(text: String) {
        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            Log.w(TAG, "Frame no es JSON válido: $text")
            return
        }

        val event = json.optString("event")
        when (event) {
            "pusher:connection_established" -> {
                val dataStr = json.optString("data")
                val innerData = runCatching { JSONObject(dataStr) }.getOrNull()
                socketId = innerData?.optString("socket_id")
                Log.i(TAG, "Conexión establecida, socket_id=$socketId")
                pendingSubscriptions.forEach { subscribeInternal(it) }
                pendingSubscriptions.clear()
            }
            "pusher:error" -> {
                Log.w(TAG, "pusher:error -> $text")
            }
            "pusher_internal:subscription_succeeded" -> {
                val channel = json.optString("channel")
                Log.i(TAG, "Suscripción OK a $channel")
            }
            else -> {
                val channel = json.optString("channel")
                if (channel.isNotBlank() && event.isNotBlank()) {
                    val rawData = json.opt("data")
                    val dataStr = when (rawData) {
                        is String -> rawData
                        null -> "{}"
                        else -> rawData.toString()
                    }
                    listenersByChannel[channel]?.forEach { it(event, dataStr) }
                }
            }
        }
    }

    @Synchronized
    fun subscribe(channelName: String, onEvent: (eventName: String, data: String) -> Unit) {
        listenersByChannel.getOrPut(channelName) { mutableListOf() }.add(onEvent)
        connectIfNeeded()
        if (socketId != null) {
            subscribeInternal(channelName)
        } else {
            pendingSubscriptions.add(channelName)
        }
    }

    private fun subscribeInternal(channelName: String) {
        val sid = socketId ?: return
        val auth = try {
            authorizer.authorize(channelName, sid)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo autorizar $channelName: ${e.message}", e)
            return
        }
        val authJson = try {
            JSONObject(auth)
        } catch (e: Exception) {
            Log.w(TAG, "Respuesta de auth no es JSON válido: $auth")
            return
        }

        val payload = JSONObject().apply {
            put("event", "pusher:subscribe")
            put(
                "data",
                JSONObject().apply {
                    put("channel", channelName)
                    put("auth", authJson.optString("auth"))
                    if (authJson.has("channel_data")) {
                        put("channel_data", authJson.optString("channel_data"))
                    }
                },
            )
        }
        Log.i(TAG, "Enviando pusher:subscribe para $channelName")
        webSocket?.send(payload.toString())
    }

    @Synchronized
    fun unsubscribe(channelName: String) {
        listenersByChannel.remove(channelName)
        pendingSubscriptions.remove(channelName)
        val payload = JSONObject().apply {
            put("event", "pusher:unsubscribe")
            put("data", JSONObject().apply { put("channel", channelName) })
        }
        webSocket?.send(payload.toString())
    }
}
