package com.stridewell.app.data

/**
 * The slice of session storage the networking layer needs.
 *
 * [TokenStore] is backed by EncryptedSharedPreferences and so cannot be built
 * without an Android Context; depending on this interface instead keeps the
 * refresh and 401 logic testable on the JVM.
 */
interface SessionTokens {
    fun getToken(): String?
    fun getRefreshToken(): String?
    fun getExpiresAt(): Long?
    fun saveSession(jwt: String, refreshToken: String?, expiresAt: Long?)
    fun clearToken()
}
