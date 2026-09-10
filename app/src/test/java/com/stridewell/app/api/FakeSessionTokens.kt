package com.stridewell.app.api

import com.stridewell.app.data.SessionTokens

/** In-memory [SessionTokens] so the refresh logic can be exercised on the JVM. */
class FakeSessionTokens(
    private var token: String? = "access_v1",
    private var refreshToken: String? = "refresh_v1",
    private var expiresAt: Long? = null,
) : SessionTokens {

    var cleared = false
        private set

    var saveCount = 0
        private set

    init {
        if (expiresAt == null) expiresAt = System.currentTimeMillis() / 1000 + 3600
    }

    override fun getToken(): String? = token
    override fun getRefreshToken(): String? = refreshToken
    override fun getExpiresAt(): Long? = expiresAt

    override fun saveSession(jwt: String, refreshToken: String?, expiresAt: Long?) {
        saveCount++
        this.token = jwt
        this.refreshToken = refreshToken
        this.expiresAt = expiresAt
    }

    override fun clearToken() {
        cleared = true
        token = null
        refreshToken = null
        expiresAt = null
    }

    fun expireIn(seconds: Long) {
        expiresAt = System.currentTimeMillis() / 1000 + seconds
    }
}
