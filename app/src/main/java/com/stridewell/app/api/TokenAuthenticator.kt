package com.stridewell.app.api

import com.stridewell.BuildConfig
import com.stridewell.app.data.TokenStore
import com.stridewell.app.model.LoginResponse
import com.stridewell.app.model.RefreshRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * Refreshes an expired access token when a protected request returns 401.
 *
 * OkHttp calls this after a 401; on success the original request is retried with
 * the new token, so callers never see the expiry. If there is no refresh token
 * or the refresh itself fails, the session is cleared and [unauthorizedFlow]
 * fires so the app routes back to sign-in.
 *
 * The refresh request uses a dedicated bare client (no auth interceptor, no
 * authenticator) to avoid attaching the stale token or recursing.
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val unauthorizedFlow: MutableSharedFlow<Unit>,
    private val json: Json,
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once with a fresh token and still 401 — stop.
        if (priorResponseCount(response) >= 2) return giveUp()

        val refreshToken = tokenStore.getRefreshToken() ?: return giveUp()

        // Another request may have refreshed while this one waited on the lock.
        // If the stored token now differs from the one that failed, just reuse it.
        val current = tokenStore.getToken()
        val failed = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (current != null && current != failed) {
            return response.request.retryWith(current)
        }

        val session = runCatching { refresh(refreshToken) }.getOrNull() ?: return giveUp()

        tokenStore.saveSession(
            jwt = session.token,
            refreshToken = session.refresh_token ?: refreshToken,
            expiresAt = session.expires_at,
        )
        return response.request.retryWith(session.token)
    }

    private fun Request.retryWith(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()

    private fun giveUp(): Request? {
        tokenStore.clearToken()
        unauthorizedFlow.tryEmit(Unit)
        return null
    }

    private fun refresh(refreshToken: String): LoginResponse {
        val payload = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
        val request = Request.Builder()
            .url("$baseUrl/auth/refresh")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        refreshClient.newCall(request).execute().use { resp ->
            val body = resp.body?.string()
            check(resp.isSuccessful && body != null) { "refresh failed: ${resp.code}" }
            return json.decodeFromString(LoginResponse.serializer(), body)
        }
    }

    private fun priorResponseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
