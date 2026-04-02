package com.stridewell.app.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.stridewell.BuildConfig

/**
 * Launches the Apple Sign In authorization flow in a Chrome Custom Tab.
 *
 * After the user authorizes, Apple redirects via the backend relay to:
 *   stridewell://oauth/apple/callback?id_token=<ID_TOKEN>
 *
 * Note: Apple's web OAuth does not support direct custom-scheme redirects.
 * The backend must provide a relay endpoint at:
 *   POST {API_BASE_URL}/auth/apple/android-callback
 * which receives Apple's form_post (containing code + id_token) and
 * then redirects the browser to stridewell://oauth/apple/callback?id_token=<ID_TOKEN>.
 *
 * MainActivity's onNewIntent intercepts this deep link, extracts the id_token,
 * and emits it on appleOAuthTokenFlow so SocialAuthViewModel can exchange it
 * with the backend.
 *
 * @param nonce SHA-256 hash of the raw nonce (passed to Apple for replay protection)
 */
object AppleOAuthHelper {

    fun launch(context: Context, nonce: String) {
        val redirectUri = Uri.encode("${BuildConfig.API_BASE_URL}/auth/apple/android-callback")
        val authUrl = buildString {
            append("https://appleid.apple.com/auth/authorize")
            append("?client_id=${BuildConfig.APPLE_CLIENT_ID}")
            append("&redirect_uri=$redirectUri")
            append("&response_type=code%20id_token")
            append("&response_mode=form_post")
            append("&scope=name%20email")
            append("&nonce=$nonce")
        }

        CustomTabsIntent.Builder()
            .setShowTitle(false)
            .build()
            .launchUrl(context, Uri.parse(authUrl))
    }
}
