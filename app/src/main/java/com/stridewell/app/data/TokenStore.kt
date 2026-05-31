package com.stridewell.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(jwt: String) {
        prefs.edit().putString(KEY_JWT, jwt).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_JWT, null)

    /** Persists the access token, refresh token, and expiry from an auth/refresh response. */
    fun saveSession(jwt: String, refreshToken: String?, expiresAt: Long?) {
        prefs.edit().apply {
            putString(KEY_JWT, jwt)
            if (refreshToken != null) putString(KEY_REFRESH, refreshToken)
            if (expiresAt != null) putLong(KEY_EXPIRES_AT, expiresAt) else remove(KEY_EXPIRES_AT)
            apply()
        }
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun getExpiresAt(): Long? =
        if (prefs.contains(KEY_EXPIRES_AT)) prefs.getLong(KEY_EXPIRES_AT, 0L) else null

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun clearToken() {
        prefs.edit()
            .remove(KEY_JWT)
            .remove(KEY_REFRESH)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_USER_ID)
            .apply()
    }

    companion object {
        private const val PREFS_FILE = "stridewell_secure_prefs"
        private const val KEY_JWT    = "jwt"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
    }
}
