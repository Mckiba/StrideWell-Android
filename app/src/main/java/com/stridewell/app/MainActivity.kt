package com.stridewell.app

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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var unauthorizedFlow: MutableSharedFlow<Unit>

    private val launchViewModel: LaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold splash until the launch auth check resolves
        splashScreen.setKeepOnScreenCondition {
            launchViewModel.state.value == LaunchViewModel.LaunchState.Loading
        }

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
}
