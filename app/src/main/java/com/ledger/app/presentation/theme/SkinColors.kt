package com.ledger.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for a design skin.
 * Defines all colors needed for the app's visual appearance.
 */
data class SkinColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val gaveColor: Color,        // Outflow/gave money color
    val receivedColor: Color,    // Inflow/received money color
    val textPrimary: Color,
    val textSecondary: Color,
    val error: Color = Color(0xFFDC2626),
    val onPrimary: Color = Color.White,
    val onSecondary: Color = Color.White,
    val onBackground: Color = textPrimary,
    val onSurface: Color = textPrimary
)
