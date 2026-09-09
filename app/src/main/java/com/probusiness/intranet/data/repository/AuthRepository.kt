package com.probusiness.intranet.data.repository

import com.probusiness.intranet.data.local.SessionManager
import com.probusiness.intranet.data.local.SessionUser
import com.probusiness.intranet.data.remote.ApiService
import com.probusiness.intranet.data.remote.dto.DeviceTokenRequest
import com.probusiness.intranet.data.remote.dto.ErrorResponse
import com.probusiness.intranet.data.remote.dto.LoginRequest
import com.probusiness.intranet.data.remote.dto.LogoutRequest
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val json: Json,
) {

    val isLoggedIn = sessionManager.isLoggedIn

    suspend fun login(usuario: String, password: String): Result<Unit> {
        return try {
            val fcmToken = deviceInfoProvider.currentFcmToken()
            val response = apiService.login(
                LoginRequest(
                    No_Usuario = usuario,
                    No_Password = password,
                    fcm_token = fcmToken,
                    device_id = deviceInfoProvider.deviceId,
                    app_version = deviceInfoProvider.appVersion,
                )
            )

            val token = response.token
            val user = response.user
            if (!response.success || token.isNullOrBlank() || user == null) {
                return Result.failure(Exception(response.message ?: "No se pudo iniciar sesión"))
            }

            sessionManager.saveSession(
                token = token,
                user = SessionUser(
                    id = user.id,
                    fullName = user.fullName ?: user.nombres_apellidos ?: user.nombre ?: "",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl,
                ),
            )

            if (fcmToken != null) {
                sessionManager.saveLastKnownFcmToken(fcmToken)
            }

            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception(extractErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar. Verifica tu conexión a internet."))
        }
    }

    suspend fun logout() {
        try {
            apiService.logout(LogoutRequest(fcm_token = sessionManager.lastKnownFcmToken()))
        } catch (e: Exception) {
            // best-effort: si falla la llamada, igual limpiamos la sesión local
        }
        sessionManager.clearSession()
    }

    suspend fun registerFcmToken(token: String) {
        if (!sessionManager.isLoggedIn.value) {
            sessionManager.savePendingFcmToken(token)
            return
        }
        try {
            apiService.registerDeviceToken(
                DeviceTokenRequest(
                    fcm_token = token,
                    device_id = deviceInfoProvider.deviceId,
                    app_version = deviceInfoProvider.appVersion,
                )
            )
            sessionManager.saveLastKnownFcmToken(token)
        } catch (e: Exception) {
            // se reintentará en el próximo login/uso de la app
        }
    }

    suspend fun syncPendingFcmTokenIfAny() {
        val pending = sessionManager.consumePendingFcmToken() ?: return
        registerFcmToken(pending)
    }

    private fun extractErrorMessage(e: HttpException): String {
        return try {
            val body = e.response()?.errorBody()?.string()
            if (body.isNullOrBlank()) {
                "Usuario o contraseña incorrectos"
            } else {
                json.decodeFromString(ErrorResponse.serializer(), body).message
                    ?: "Usuario o contraseña incorrectos"
            }
        } catch (parseError: Exception) {
            "Usuario o contraseña incorrectos"
        }
    }
}
