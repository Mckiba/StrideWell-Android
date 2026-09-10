package com.stridewell.app.api

import com.stridewell.app.data.SessionTokens
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Refreshes an expired access token when a protected request returns 401.
 *
 * OkHttp calls this after a 401; on success the original request is retried with
 * the new token, so callers never see the expiry.
 *
 * Only a refresh the server actively rejects clears the session. When the refresh
 * cannot be completed at all — no network, a timeout, a 503 — the session is left
 * intact and the 401 is returned to the caller as an ordinary failure, because the
 * stored refresh token is still perfectly good and will work on the next attempt.
 */
class TokenAuthenticator(
    private val tokenStore: SessionTokens,
    private val unauthorizedFlow: MutableSharedFlow<Unit>,
    private val refresher: SessionRefresher,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once with a fresh token and still 401 — stop.
        if (priorResponseCount(response) >= 2) return giveUp()

        if (tokenStore.getRefreshToken().isNullOrEmpty()) return giveUp()

        // Another request may have refreshed while this one waited. If the stored
        // token now differs from the one that failed, just reuse it.
        val current = tokenStore.getToken()
        val failed = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (current != null && current != failed) {
            return response.request.retryWith(current)
        }

        return when (refresher.refreshIfNeeded(force = true)) {
            SessionRefresher.Outcome.REFRESHED ->
                tokenStore.getToken()?.let { response.request.retryWith(it) } ?: giveUp()

            // Keep the session and let the 401 surface. UnauthorizedInterceptor
            // defers to this decision while a refresh token is still stored.
            SessionRefresher.Outcome.TRANSIENT_FAILED -> null

            SessionRefresher.Outcome.AUTH_FAILED,
            SessionRefresher.Outcome.NO_REFRESH_TOKEN -> giveUp()
        }
    }

    private fun Request.retryWith(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()

    private fun giveUp(): Request? {
        tokenStore.clearToken()
        unauthorizedFlow.tryEmit(Unit)
        return null
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
}
