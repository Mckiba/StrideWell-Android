package com.stridewell.app.ui.onboarding.guided

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/** Builds the screen-scoped [IntakeChatViewModel] carrying its [screenContext]. */
@Composable
fun intakeChatViewModel(screenContext: String?): IntakeChatViewModel =
    hiltViewModel<IntakeChatViewModel, IntakeChatViewModel.Factory>(
        creationCallback = { factory -> factory.create(screenContext) }
    )
