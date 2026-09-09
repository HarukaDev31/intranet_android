package com.probusiness.intranet.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.probusiness.intranet.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "DeviceInfoProvider"

class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    val appVersion: String
        get() = BuildConfig.VERSION_NAME

    suspend fun currentFcmToken(): String? = try {
        val token = FirebaseMessaging.getInstance().token.await()
        Log.i(TAG, "Token FCM obtenido: ${token.take(12)}… (len=${token.length})")
        token
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo obtener el token FCM: ${e.javaClass.simpleName}: ${e.message}", e)
        null
    }
}
