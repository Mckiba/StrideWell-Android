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

    fun clearToken() {
        prefs.edit().remove(KEY_JWT).apply()
    }

    companion object {
        private const val PREFS_FILE = "stridewell_secure_prefs"
        private const val KEY_JWT    = "jwt"
    }
}
