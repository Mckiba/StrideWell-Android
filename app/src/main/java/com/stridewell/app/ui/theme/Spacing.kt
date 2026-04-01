package com.stridewell.app.ui.theme

import androidx.compose.ui.unit.dp

/** Direct port of iOS Spacing enum from DesignSystem.swift. */
object Spacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val xl2 = 20.dp   // icon-to-content gap (ActivityCard)
    val xxl = 48.dp
}

/** Direct port of iOS CornerRadius enum from DesignSystem.swift. */
object CornerRadius {
    val sm     = 8.dp    // buttons, tags, small chips
    val md     = 12.dp   // cards (CardView default)
    val lg     = 16.dp   // sheets, large containers
    val bubble = 18.dp   // chat bubbles
    val input  = 20.dp   // text input fields
}
