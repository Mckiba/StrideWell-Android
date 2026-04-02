package com.stridewell.app.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.stridewell.BuildConfig

/**
 * Launches the Strava OAuth authorization flow in a Chrome Custom Tab.
 *
 * After the user authorizes, Strava redirects to:
 *   stridewell://oauth/strava/callback?code=<AUTH_CODE>
 *
 * MainActivity's onNewIntent intercepts this deep link, extracts the code,
 * and emits it on the shared oauthCodeFlow so StravaConnectViewModel can
 * exchange it with the backend.
 *
 * Port of iOS StravaOAuthHelper.swift (which used ASWebAuthenticationSession).
 * Chrome Custom Tabs is the Android equivalent — it requires the deep link
 * round-trip pattern rather than a direct continuation callback.
 */
object StravaOAuthHelper {

    fun launch(context: Context) {
        val redirectUri = Uri.encode(BuildConfig.STRAVA_REDIRECT_URI)
        val authUrl = buildString {
            append("https://www.strava.com/oauth/mobile/authorize")
            append("?client_id=${BuildConfig.STRAVA_CLIENT_ID}")
            append("&redirect_uri=$redirectUri")
            append("&response_type=code")
            append("&approval_prompt=auto")
            append("&scope=activity:read_all")
        }

        CustomTabsIntent.Builder()
            .setShowTitle(false)
            .build()
            .launchUrl(context, Uri.parse(authUrl))
    }
}
