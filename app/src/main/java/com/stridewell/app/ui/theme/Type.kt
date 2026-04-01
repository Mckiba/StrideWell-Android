package com.stridewell.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Semantic typography scale ported from iOS DesignSystem.swift.
 *
 * System font used for now — custom Inter / SofiaSans fonts will be
 * wired in when TTF assets are added to the project.
 *
 * iOS → Material3 mapping:
 *   screenTitle  (.title2.bold)      → titleLarge   (20sp bold)
 *   sectionTitle (.headline)         → titleMedium  (17sp semibold)
 *   cardTitle    (.body.semibold)    → bodyLarge    (16sp semibold)
 *   cardBody     (.subheadline)      → bodyMedium   (15sp normal)
 *   cardCaption  (.caption)          → labelSmall   (11sp normal)
 *   dateDay      (.caption2.medium)  → labelSmall   (11sp medium)
 *   dateNumber   (.callout.semibold) → bodySmall    (14sp semibold)
 */
val StridewellTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
