package com.ledger.app.presentation.theme.skins

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ledger.app.presentation.theme.SkinColors
import com.ledger.app.presentation.theme.SkinShapes

/**
 * Neo-Minimalism design skin.
 * Features: Clean spacious layouts, earthy palette, soft shadows, 16dp rounded corners.
 */

// Light mode colors
private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate400 = Color(0xFF94A3B8)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)
private val Sage600 = Color(0xFF4A6B4A)
private val Sage500 = Color(0xFF6B8E6B)
private val Sage100 = Color(0xFFE8F3E8)
private val WarmGray = Color(0xFFF5F5F4)
private val Terracotta = Color(0xFFE07856)
private val MossGreen = Color(0xFF10B981)

val NeoMinimalLightColors = SkinColors(
    primary = Sage600,
    secondary = Slate600,
    background = Slate50,
    surface = Color.White,
    cardBackground = Color.White,
    gaveColor = Terracotta,        // Warm earthy red for outflow
    receivedColor = MossGreen,     // Natural green for inflow
    textPrimary = Slate900,
    textSecondary = Slate600,
    error = Color(0xFFDC2626),
    onPrimary = Color.White,
    onSecondary = Color.White
)

// Dark mode colors
private val DarkSlate900 = Color(0xFF0F172A)
private val DarkSlate800 = Color(0xFF1E293B)
private val DarkSlate700 = Color(0xFF334155)
private val DarkSlate300 = Color(0xFFCBD5E1)
private val DarkSlate200 = Color(0xFFE2E8F0)
private val DarkSage400 = Color(0xFF8FAF8F)
private val DarkSage300 = Color(0xFFB0CFB0)

val NeoMinimalDarkColors = SkinColors(
    primary = DarkSage400,
    secondary = DarkSlate700,
    background = DarkSlate900,
    surface = DarkSlate800,
    cardBackground = DarkSlate800,
    gaveColor = Color(0xFFF87171),     // Softer red for dark mode
    receivedColor = Color(0xFF34D399), // Softer green for dark mode
    textPrimary = DarkSlate200,
    textSecondary = DarkSlate300,
    error = Color(0xFFF87171),
    onPrimary = DarkSlate900,
    onSecondary = Color.White
)

val NeoMinimalShapes = SkinShapes(
    cardCornerRadius = 16.dp,
    buttonCornerRadius = 12.dp,
    borderWidth = 0.dp,           // No borders for minimal aesthetic
    elevation = 4.dp,             // Soft shadows
    blurRadius = 0.dp
)
