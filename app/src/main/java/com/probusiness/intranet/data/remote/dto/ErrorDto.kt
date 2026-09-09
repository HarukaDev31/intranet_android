package com.probusiness.intranet.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val status: String? = null,
    val message: String? = null,
)
