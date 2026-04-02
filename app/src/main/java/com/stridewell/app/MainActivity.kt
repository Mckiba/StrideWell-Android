package com.stridewell.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
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

    private val launchViewModel: LaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold splash until the launch auth check resolves
        splashScreen.setKeepOnScreenCondition {
            launchViewModel.state.value == LaunchViewModel.LaunchState.Loading
        }

        // Handle deep link if app was cold-started via the OAuth callback
        handleOAuthIntent(intent)

        enableEdgeToEdge()

        setContent {
            StridewellTheme {
                val launchState by launchViewModel.state.collectAsStateWithLifecycle()

                StridewellNavHost(
                    launchState      = launchState,
                    unauthorizedFlow = unauthorizedFlow
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
    }

    // ── Deep link handling ────────────────────────────────────────────────────

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "stridewell" && uri.host == "oauth") {
            val code = uri.getQueryParameter("code") ?: return
            oauthCodeFlow.value = code
        }
    }
}
