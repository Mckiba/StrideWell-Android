package com.stridewell.app.ui.onboarding.guided

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.stridewell.app.ui.theme.InterFamily
import com.stridewell.app.ui.theme.Spacing

/**
 * Shared layout for the guided intake screens: full-bleed photo, a title pinned to the top,
 * and an [ExpandableSheetScaffold] holding the screen's structured controls above the chat.
 *
 * @param autoExpand expands the sheet while the coach is thinking (phase == Waiting).
 * @param chat receives an `onInteract` callback fired on input focus to expand the sheet.
 */
@Composable
fun GuidedScreenScaffold(
    title: String,
    @DrawableRes imageRes: Int,
    autoExpand: Boolean,
    structuredInputs: @Composable ColumnScope.() -> Unit,
    chat: @Composable ColumnScope.(onInteract: () -> Unit) -> Unit
) {
    ExpandableSheetScaffold(
        autoExpand = autoExpand,
        background = {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
            Text(
                text = title,
                color = Color.White,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 50.sp,
                lineHeight = 54.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.lg)
            )
        },
        sheetContent = { onInteract ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            ) {
                structuredInputs()
            }
            chat(onInteract)
        }
    )
}
