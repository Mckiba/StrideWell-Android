package com.stridewell.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stridewell.R

// ── Font families ──────────────────────────────────────────────────────────────

val InterFamily = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold),
)

val SofiaSansFamily = FontFamily(
    Font(R.font.sofia_sans_regular,  FontWeight.Normal),
    Font(R.font.sofia_sans_medium,   FontWeight.Medium),
    Font(R.font.sofia_sans_semibold, FontWeight.SemiBold),
    Font(R.font.sofia_sans_bold,     FontWeight.Bold),
)

// ── Activity card text styles ──────────────────────────────────────────────────
//   .activityTimestamp → Font.sofiaSans(size: 12, weight: .regular)
//   .activityName      → Font.inter(size: 12, weight: .bold)
//   .activityStatLabel → Font.inter(size: 10)
//   .activityStatValue → Font.inter(size: 11, weight: .bold)

val ActivityTimestampStyle = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = FontWeight.Normal,
    fontSize   = 12.sp
)

val ActivityNameStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Bold,
    fontSize   = 12.sp
)

val ActivityStatLabelStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal,
    fontSize   = 10.sp
)

val ActivityStatValueStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Bold,
    fontSize   = 11.sp
)
