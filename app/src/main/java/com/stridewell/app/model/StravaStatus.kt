package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
data class StravaStatusResponse(
    val connected: Boolean,
    val expires_at: String? = null,
    val scope: String? = null
)

@Serializable
data class StravaDisconnectResponse(
    val connected: Boolean
)

@Serializable
data class EmptyResponse(
    val ok: Boolean? = null
)
