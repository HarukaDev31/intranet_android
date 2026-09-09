package com.probusiness.intranet.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val No_Usuario: String,
    val No_Password: String,
    val platform: String = "android",
    val fcm_token: String? = null,
    val device_id: String? = null,
    val app_version: String? = null,
)

@Serializable
data class LoginResponse(
    val success: Boolean = false,
    val message: String? = null,
    val token: String? = null,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val user: UserDto? = null,
)

@Serializable
data class UserDto(
    val id: Int,
    val nombre: String? = null,
    val nombres_apellidos: String? = null,
    val fullName: String? = null,
    val photoUrl: String? = null,
    val email: String? = null,
)

@Serializable
data class MeResponse(
    val success: Boolean = false,
    val user: UserDto? = null,
)

@Serializable
data class RefreshResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Long? = null,
)

@Serializable
data class LogoutRequest(
    val fcm_token: String? = null,
)

@Serializable
data class DeviceTokenRequest(
    val fcm_token: String,
    val platform: String = "android",
    val device_id: String? = null,
    val app_version: String? = null,
)

@Serializable
data class ApiMessageResponse(
    val success: Boolean = false,
    val message: String? = null,
)
