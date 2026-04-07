package com.stridewell.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.util.AppTheme
import com.stridewell.app.util.isDark
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.navigation.StridewellNavHost
import com.stridewell.app.ui.auth.LaunchViewModel
import com.stridewell.app.ui.theme.StridewellTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var unauthorizedFlow: MutableSharedFlow<Unit>

    /** Receives the Strava OAuth code from the deep link redirect. */
    @Inject
    @Named("oauthCode")
    lateinit var oauthCodeFlow: MutableStateFlow<String?>

    /** Receives the Apple id_token from the deep link redirect. */
    @Inject
    @Named("appleOAuthToken")
    lateinit var appleOAuthTokenFlow: MutableStateFlow<String?>

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /** Receives the deep_link extra from a notification tap. */
    @Inject
    @Named("notificationDeepLink")
    lateinit var notificationDeepLinkFlow: MutableStateFlow<String?>

    private val launchViewModel: LaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold splash until the launch auth check resolves
        splashScreen.setKeepOnScreenCondition {
            launchViewModel.state.value == LaunchViewModel.LaunchState.Loading
        }

        // Handle deep link if app was cold-started via the OAuth callback or notification tap
        handleOAuthIntent(intent)
        handleNotificationIntent(intent)

        enableEdgeToEdge()

        setContent {
            val appTheme by settingsRepository.appTheme
                .collectAsStateWithLifecycle(initialValue = AppTheme.DEVICE)
            val systemIsDark = isSystemInDarkTheme()

            StridewellTheme(darkTheme = appTheme.isDark(systemIsDark)) {
                val launchState by launchViewModel.state.collectAsStateWithLifecycle()

                StridewellNavHost(
                    launchState               = launchState,
                    unauthorizedFlow          = unauthorizedFlow,
                    notificationDeepLinkFlow  = notificationDeepLinkFlow,
                )
            }
        }
    }

    /**
     * Called when the app is already running and the Strava OAuth redirect
     * brings the user back. The deep link intent arrives here as a new intent.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
        handleNotificationIntent(intent)
    }

    // ── Deep link handling ────────────────────────────────────────────────────

    private fun handleNotificationIntent(intent: Intent?) {
        val deepLink = intent?.getStringExtra("deep_link") ?: return
        notificationDeepLinkFlow.value = deepLink
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "stridewell") return

        when (uri.host) {
            // Strava: stridewell://localhost?code=...
            "localhost" -> {
                val code = uri.getQueryParameter("code") ?: return
                oauthCodeFlow.value = code
            }
            // Apple: stridewell://oauth/apple/callback?id_token=...
            "oauth" -> {
                val path = uri.path ?: return
                if (!path.startsWith("/apple/callback")) return
                val token = uri.getQueryParameter("id_token")
                    ?: uri.fragment
                        ?.split("&")
                        ?.firstOrNull { it.startsWith("id_token=") }
                        ?.removePrefix("id_token=")
                    ?: return
                appleOAuthTokenFlow.value = token
            }
        }
    }
}
