package com.probusiness.intranet.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class SessionUser(
    val id: Int,
    val fullName: String,
    val email: String,
    val photoUrl: String?,
)

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "intranet_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _isLoggedIn = MutableStateFlow(hasToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun currentUser(): SessionUser? {
        val id = prefs.getInt(KEY_USER_ID, -1)
        if (id < 0) return null
        return SessionUser(
            id = id,
            fullName = prefs.getString(KEY_USER_NAME, "") ?: "",
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            photoUrl = prefs.getString(KEY_USER_PHOTO, null),
        )
    }

    fun saveSession(token: String, user: SessionUser) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.fullName)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHOTO, user.photoUrl)
            .apply()
        _isLoggedIn.update { true }
    }

    fun updateToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHOTO)
            .apply()
        _isLoggedIn.update { false }
    }

    /** Token FCM pendiente de enviar al backend porque llegó (onNewToken) sin sesión activa. */
    fun savePendingFcmToken(token: String) {
        prefs.edit().putString(KEY_PENDING_FCM_TOKEN, token).apply()
    }

    fun consumePendingFcmToken(): String? {
        val token = prefs.getString(KEY_PENDING_FCM_TOKEN, null)
        if (token != null) prefs.edit().remove(KEY_PENDING_FCM_TOKEN).apply()
        return token
    }

    fun lastKnownFcmToken(): String? = prefs.getString(KEY_LAST_FCM_TOKEN, null)

    fun saveLastKnownFcmToken(token: String) {
        prefs.edit().putString(KEY_LAST_FCM_TOKEN, token).apply()
    }

    private fun hasToken(): Boolean = !prefs.getString(KEY_TOKEN, null).isNullOrBlank()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_PHOTO = "user_photo"
        const val KEY_PENDING_FCM_TOKEN = "pending_fcm_token"
        const val KEY_LAST_FCM_TOKEN = "last_fcm_token"
    }
}
