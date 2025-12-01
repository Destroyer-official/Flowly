package com.ledger.app.presentation.theme.skins

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ledger.app.presentation.theme.SkinColors
import com.ledger.app.presentation.theme.SkinShapes

/**
 * Neobrutalism design skin.
 * Features: Bold borders, high contrast, chunky components, deliberately imperfect layouts.
 */

// Light mode colors (high contrast)
private val BrutalBlack = Color(0xFF000000)
private val BrutalWhite = Color(0xFFFFFFFF)
private val BrutalYellow = Color(0xFFFBBF24)
private val BrutalRed = Color(0xFFEF4444)
private val BrutalBlue = Color(0xFF3B82F6)
private val BrutalGreen = Color(0xFF10B981)
private val BrutalPink = Color(0xFFEC4899)
private val BrutalBg = Color(0xFFFEFCE8)

val NeoBrutalLightColors = SkinColors(
    primary = BrutalBlue,
    secondary = BrutalPink,
    background = BrutalBg,
    surface = BrutalWhite,
    cardBackground = BrutalWhite,
    gaveColor = BrutalRed,
    receivedColor = BrutalGreen,
    textPrimary = BrutalBlack,
    textSecondary = Color(0xFF374151),
    error = BrutalRed,
    onPrimary = BrutalWhite,
    onSecondary = BrutalWhite
)

// Dark mode colors (inverted high contrast)
private val BrutalDarkBg = Color(0xFF1A1A1A)
private val BrutalDarkSurface = Color(0xFF2A2A2A)
private val BrutalDarkCard = Color(0xFF2A2A2A)
private val BrutalDarkYellow = Color(0xFFFCD34D)
private val BrutalDarkBlue = Color(0xFF60A5FA)
private val BrutalDarkPink = Color(0xFFF472B6)
private val BrutalDarkGreen = Color(0xFF34D399)
private val BrutalDarkRed = Color(0xFFF87171)

val NeoBrutalDarkColors = SkinColors(
    primary = BrutalDarkBlue,
    secondary = BrutalDarkPink,
    background = BrutalDarkBg,
    surface = BrutalDarkSurface,
    cardBackground = BrutalDarkCard,
    gaveColor = BrutalDarkRed,
    receivedColor = BrutalDarkGreen,
    textPrimary = BrutalWhite,
    textSecondary = Color(0xFFD1D5DB),
    error = BrutalDarkRed,
    onPrimary = BrutalBlack,
    onSecondary = BrutalBlack
)

val NeoBrutalShapes = SkinShapes(
    cardCornerRadius = 8.dp,      // Sharp, minimal rounding
    buttonCornerRadius = 8.dp,
    borderWidth = 3.dp,           // Bold, thick borders
    elevation = 0.dp,             // Flat, no shadows
    blurRadius = 0.dp
)
