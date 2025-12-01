package com.ledger.app.presentation.theme.skins

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ledger.app.presentation.theme.SkinColors
import com.ledger.app.presentation.theme.SkinShapes

/**
 * Retrofuturism (Frutiger Aero) design skin.
 * Features: Glossy surfaces, vibrant gradients, bubble-like elements, optimistic aesthetic.
 */

// Light mode colors (vibrant and glossy)
private val RetroSky = Color(0xFF38BDF8)
private val RetroBlue = Color(0xFF3B82F6)
private val RetroViolet = Color(0xFF8B5CF6)
private val RetroLime = Color(0xFF84CC16)
private val RetroOrange = Color(0xFFF97316)
private val RetroWhite = Color(0xFFFFFFFF)
private val RetroLightBg = Color(0xFFE0F2FE)

val RetroFuturismLightColors = SkinColors(
    primary = RetroBlue,
    secondary = RetroViolet,
    background = RetroLightBg,
    surface = RetroWhite,
    cardBackground = RetroWhite,
    gaveColor = RetroOrange,
    receivedColor = RetroLime,
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    error = Color(0xFFEF4444),
    onPrimary = Color.White,
    onSecondary = Color.White
)

// Dark mode colors (neon glow aesthetic)
private val RetroDarkBg = Color(0xFF0C1222)
private val RetroDarkSurface = Color(0xFF1A2332)
private val RetroDarkCard = Color(0xFF243447)
private val RetroNeonBlue = Color(0xFF60A5FA)
private val RetroNeonPurple = Color(0xFFA78BFA)
private val RetroNeonGreen = Color(0xFF4ADE80)
private val RetroNeonPink = Color(0xFFF472B6)

val RetroFuturismDarkColors = SkinColors(
    primary = RetroNeonBlue,
    secondary = RetroNeonPurple,
    background = RetroDarkBg,
    surface = RetroDarkSurface,
    cardBackground = RetroDarkCard,
    gaveColor = RetroNeonPink,
    receivedColor = RetroNeonGreen,
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFCBD5E1),
    error = Color(0xFFF87171),
    onPrimary = Color.White,
    onSecondary = Color.White
)

val RetroFuturismShapes = SkinShapes(
    cardCornerRadius = 24.dp,     // Bubble-like rounded corners
    buttonCornerRadius = 20.dp,
    borderWidth = 0.dp,
    elevation = 6.dp,             // Glossy depth
    blurRadius = 0.dp
)
