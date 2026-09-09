package com.probusiness.intranet.data.remote.realtime

import com.probusiness.intranet.BuildConfig
import com.probusiness.intranet.data.local.SessionManager
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class AuthorizationFailureException(message: String) : Exception(message)

/**
 * Autoriza canales privados de Reverb/Pusher contra el mismo endpoint que usa el frontend web
 * (POST /broadcasting/auth) con el JWT de la sesión actual.
 */
class LaravelChannelAuthorizer(
    private val sessionManager: SessionManager,
) {

    private val client = OkHttpClient()

    fun authorize(channelName: String, socketId: String): String {
        val token = sessionManager.token()
            ?: throw AuthorizationFailureException("No hay sesión activa para autorizar el canal")

        val formBody = FormBody.Builder()
            .add("channel_name", channelName)
            .add("socket_id", socketId)
            .build()

        val request = Request.Builder()
            .url(BuildConfig.BASE_URL + "broadcasting/auth")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrBlank()) {
                    throw AuthorizationFailureException("Fallo al autorizar canal $channelName: HTTP ${response.code}")
                }
                body
            }
        } catch (e: AuthorizationFailureException) {
            throw e
        } catch (e: Exception) {
            throw AuthorizationFailureException("Fallo al autorizar canal $channelName: ${e.message}")
        }
    }
}
