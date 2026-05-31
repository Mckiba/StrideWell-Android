package com.stridewell.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wraps the composable's bottom edge with a coloured stroke that curves with
 * the card's [cornerRadius].
 *
 * Implementation: paints a fully-rounded rectangle in [color] across the
 * outer bounds, then shrinks the inner area by [height] at the bottom via
 * padding. The card surface (added by a later `.background(...)`) covers the
 * top of the stroke; the bottom band and corner curves peek out below.
 *
 * Modifier order matters — apply BEFORE the card's own `.background(...)`
 * and `.clip(...)`:
 *
 * ```
 * Modifier
 *   .shadow(...)
 *   .cardBottomStroke(strokeColor)        // outer stroke layer
 *   .background(cardColor, RoundedShape)  // inner card surface, 3dp shorter
 *   .clip(RoundedShape)
 *   ...
 * ```
 */
fun Modifier.cardBottomStroke(
    color: Color,
    height: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
): Modifier = this
    .background(color = color, shape = RoundedCornerShape(cornerRadius))
    .padding(bottom = height)

/** No-op when [color] is null, so planned/rest cards render with no stroke. */
fun Modifier.cardBottomStroke(
    color: Color?,
    height: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
): Modifier = if (color != null) cardBottomStroke(color, height, cornerRadius) else this
