package com.stridewell.app.ui.onboarding.guided

import com.stridewell.app.model.InterviewMessage
import com.stridewell.app.model.InterviewMessageRole

/** A chat state seeded with a single coach line, for previews. */
internal fun sampleChatState(coachLine: String): IntakeChatViewModel.UiState =
    IntakeChatViewModel.UiState(
        messages = listOf(
            InterviewMessage(
                id = "preview",
                role = InterviewMessageRole.assistant,
                content = coachLine,
                agent_used = "coach",
                created_at = ""
            )
        ),
        phase = IntakeChatViewModel.Phase.Active
    )
