package com.probusiness.intranet.data.remote

import com.probusiness.intranet.BuildConfig
import com.probusiness.intranet.data.local.SessionManager
import com.probusiness.intranet.data.remote.dto.RefreshResponse
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Route
import javax.inject.Inject

/**
 * Ante un 401, intenta refrescar el token UNA vez (POST auth/refresh con el token actual).
 * Si falla, limpia la sesión (la UI reacciona a SessionManager.isLoggedIn y vuelve al login).
 */
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
) : Authenticator {

    private val json = Json { ignoreUnknownKeys = true }
    private val plainClient = OkHttpClient()

    override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
        if (responseCount(response) >= 2) return null
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null
        if (response.request.url.encodedPath.endsWith("/auth/login")) return null

        val currentToken = sessionManager.token() ?: return null

        val refreshRequest = Request.Builder()
            .url(BuildConfig.BASE_URL + "auth/refresh")
            .addHeader("Authorization", "Bearer $currentToken")
            .post(ByteArray(0).toRequestBody(null))
            .build()

        return try {
            plainClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) {
                    sessionManager.clearSession()
                    return null
                }
                val body = refreshResponse.body?.string() ?: return null
                val parsed = json.decodeFromString(RefreshResponse.serializer(), body)
                val newToken = parsed.access_token ?: return null

                sessionManager.updateToken(newToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
        } catch (e: Exception) {
            sessionManager.clearSession()
            null
        }
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
