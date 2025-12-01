package com.ledger.app.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shape and styling properties for a design skin.
 * Defines corner radii, borders, elevation, and effects.
 */
data class SkinShapes(
    val cardCornerRadius: Dp,
    val buttonCornerRadius: Dp,
    val borderWidth: Dp,
    val elevation: Dp,
    val blurRadius: Dp = 0.dp  // For glassmorphism effect
)
