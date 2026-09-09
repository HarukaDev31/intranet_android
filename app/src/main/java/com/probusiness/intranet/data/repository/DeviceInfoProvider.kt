package com.probusiness.intranet.data.repository

import android.content.Context
import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessaging
import com.probusiness.intranet.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    val appVersion: String
        get() = BuildConfig.VERSION_NAME

    suspend fun currentFcmToken(): String? = try {
        FirebaseMessaging.getInstance().token.await()
    } catch (e: Exception) {
        null
    }
}
