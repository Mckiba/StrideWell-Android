package com.stridewell.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Semantic typography scale ported from iOS DesignSystem.swift.
 *
 * Font assignment:
 *   SofiaSans → display headings (titleLarge, titleMedium)
 *   Inter     → body + label text (bodyLarge … labelMedium)
 *
 * iOS → Material3 mapping:
 *   screenTitle  (.title2.bold)      → titleLarge   (20sp bold,     SofiaSans)
 *   sectionTitle (.headline)         → titleMedium  (17sp semibold, SofiaSans)
 *   cardTitle    (.body.semibold)    → bodyLarge    (16sp semibold, Inter)
 *   cardBody     (.subheadline)      → bodyMedium   (15sp normal,   Inter)
 *   dateNumber   (.callout.semibold) → bodySmall    (14sp semibold, Inter)
 *   dateDay      (.caption2.medium)  → labelSmall   (11sp medium,   Inter)
 *   cardCaption  (.caption)          → labelMedium  (12sp normal,   Inter)
 *
 * Activity-card–specific styles live in Font.kt as top-level TextStyle vals.
 */
val StridewellTypography = Typography(
    // ── Display / headings — SofiaSans ────────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily    = SofiaSansFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 20.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = SofiaSansFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 17.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.sp
    ),
    // ── Body / labels — Inter ─────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily    = InterFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = InterFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 15.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = InterFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = InterFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = InterFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp
    )
)
