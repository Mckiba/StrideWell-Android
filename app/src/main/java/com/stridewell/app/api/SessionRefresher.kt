package com.stridewell.app.api

import com.stridewell.BuildConfig
import com.stridewell.app.data.SessionTokens
import com.stridewell.app.model.LoginResponse
import com.stridewell.app.model.RefreshRequest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Owns every call to /auth/refresh.
 *
 * The distinction it exists to draw is between a refresh token the server has
 * rejected and a refresh that simply could not be completed. Treating the two
 * alike is what signed users out whenever the network hiccuped during a refresh:
 * a timeout wiped the session exactly as a revoked token would have.
 *
 * Refreshes are serialized, so a burst of requests hitting an expired token
 * produces one refresh rather than one per request.
 */
class SessionRefresher(
    private val tokenStore: SessionTokens,
    private val json: Json,
    private val baseUrl: String = BuildConfig.API_BASE_URL,
    private val client: OkHttpClient = defaultClient(),
    /** Injectable so tests do not actually wait out the backoff. */
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
) {

    enum class Outcome {
        /** A usable access token is stored — either just refreshed or still fresh. */
        REFRESHED,

        /** The server rejected the refresh token. The session cannot be recovered. */
        AUTH_FAILED,

        /** The refresh could not be completed. The session is untouched — retry later. */
        TRANSIENT_FAILED,

        /** Nothing to refresh with. */
        NO_REFRESH_TOKEN,
    }

    /**
     * @param force refresh even when the stored token is not near expiry, as when
     *   a request has already come back 401.
     */
    @Synchronized
    fun refreshIfNeeded(force: Boolean): Outcome {
        val refreshToken = tokenStore.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) return Outcome.NO_REFRESH_TOKEN
        if (!force && !isAccessTokenNearExpiry()) return Outcome.REFRESHED

        var attempt = 0
        while (true) {
            when (val result = attempt(refreshToken)) {
                is Attempt.Success -> {
                    tokenStore.saveSession(
                        jwt = result.session.token,
                        refreshToken = result.session.refresh_token ?: refreshToken,
                        expiresAt = result.session.expires_at,
                    )
                    return Outcome.REFRESHED
                }

                Attempt.AuthFailed -> return Outcome.AUTH_FAILED

                Attempt.Transient -> {
                    attempt++
                    if (attempt > MAX_TRANSIENT_RETRIES) return Outcome.TRANSIENT_FAILED
                    sleep(attempt * BACKOFF_STEP_MS)
                }
            }
        }
    }

    /** True when the stored token expires within [EXPIRY_SKEW_SECONDS], or is unknown. */
    fun isAccessTokenNearExpiry(): Boolean {
        val expiresAt = tokenStore.getExpiresAt() ?: return true
        val now = System.currentTimeMillis() / 1000
        return expiresAt - now <= EXPIRY_SKEW_SECONDS
    }

    private sealed interface Attempt {
        data class Success(val session: LoginResponse) : Attempt
        data object AuthFailed : Attempt
        data object Transient : Attempt
    }

    private fun attempt(refreshToken: String): Attempt {
        val payload = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
        val request = Request.Builder()
            .url("$baseUrl/auth/refresh")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                when {
                    // 401 is the server's way of saying the refresh token itself is
                    // no longer valid; everything else non-2xx (503, 429, 5xx) means
                    // it could not answer right now.
                    response.code == 401 -> Attempt.AuthFailed
                    !response.isSuccessful || body == null -> Attempt.Transient
                    else -> runCatching {
                        Attempt.Success(json.decodeFromString(LoginResponse.serializer(), body))
                    }.getOrElse { Attempt.Transient }
                }
            }
        } catch (e: IOException) {
            // Unreachable server, DNS failure, timeout — the token is untouched.
            Attempt.Transient
        }
    }

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val MAX_TRANSIENT_RETRIES = 2
        private const val BACKOFF_STEP_MS = 500L
        private const val EXPIRY_SKEW_SECONDS = 300L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
