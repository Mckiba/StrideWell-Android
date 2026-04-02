package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val id: String,
    val email: String? = null,
    val onboarding_status: OnboardingStatus? = null
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user_id: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ForgotPasswordResponse(
    val message: String
)

@Serializable
data class GoogleSignInRequest(
    val id_token: String,
    val nonce: String
)

@Serializable
data class AppleSignInRequest(
    val id_token: String,
    val nonce: String
)
