package com.stridewell.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterTokenRequest(
    @SerialName("device_token") val deviceToken: String,
    val platform: String,
)

@Serializable
data class RegisterTokenResponse(
    val registered: Boolean,
)
