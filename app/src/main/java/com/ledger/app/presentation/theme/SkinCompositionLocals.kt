package com.ledger.app.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal for accessing current skin colors throughout the app.
 */
val LocalSkinColors = staticCompositionLocalOf {
    // Default colors (will be overridden by theme)
    SkinColors(
        primary = Color(0xFF6B8E6B),
        secondary = Color(0xFF475569),
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        cardBackground = Color.White,
        gaveColor = Color(0xFFEF4444),
        receivedColor = Color(0xFF10B981),
        textPrimary = Color(0xFF334155),
        textSecondary = Color(0xFF64748B)
    )
}

/**
 * CompositionLocal for accessing current skin shapes throughout the app.
 */
val LocalSkinShapes = staticCompositionLocalOf {
    // Default shapes (will be overridden by theme)
    SkinShapes(
        cardCornerRadius = 16.dp,
        buttonCornerRadius = 12.dp,
        borderWidth = 1.dp,
        elevation = 4.dp,
        blurRadius = 0.dp
    )
}
