package com.ledger.app.presentation.theme.skins

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ledger.app.presentation.theme.SkinColors
import com.ledger.app.presentation.theme.SkinShapes

/**
 * Glassmorphism design skin.
 * Features: Frosted glass effect, blur, transparency, layered depth, soft glows.
 * Works best with dark mode.
 */

// Light mode colors (subtle glass effect)
private val GlassLightBg = Color(0xFFF0F4F8)
private val GlassLightSurface = Color(0xFFFAFBFC).copy(alpha = 0.85f)
private val GlassLightCard = Color(0xFFFFFFFF).copy(alpha = 0.75f)
private val GlassAccent = Color(0xFF6366F1)
private val GlassSecondary = Color(0xFF8B5CF6)
private val GlassCyan = Color(0xFF06B6D4)
private val GlassPink = Color(0xFFEC4899)

val GlassLightColors = SkinColors(
    primary = GlassAccent,
    secondary = GlassSecondary,
    background = GlassLightBg,
    surface = GlassLightSurface,
    cardBackground = GlassLightCard,
    gaveColor = GlassPink,
    receivedColor = GlassCyan,
    textPrimary = Color(0xFF1E293B),
    textSecondary = Color(0xFF64748B),
    error = Color(0xFFEF4444),
    onPrimary = Color.White,
    onSecondary = Color.White
)

// Dark mode colors (full glass effect) - Premium 2026 styling
private val GlassDarkBg = Color(0xFF080B14)
private val GlassDarkSurface = Color(0xFF141929)
private val GlassDarkCard = Color(0xFF1C2235)
private val GlassDarkAccent = Color(0xFF818CF8)
private val GlassDarkSecondary = Color(0xFFA78BFA)
private val GlassDarkCyan = Color(0xFF22D3EE)
private val GlassDarkPink = Color(0xFFF472B6)

val GlassDarkColors = SkinColors(
    primary = GlassDarkAccent,
    secondary = GlassDarkSecondary,
    background = GlassDarkBg,
    surface = GlassDarkSurface,
    cardBackground = GlassDarkCard,
    gaveColor = GlassDarkPink,
    receivedColor = GlassDarkCyan,
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    error = Color(0xFFF87171),
    onPrimary = Color.White,
    onSecondary = Color.White
)

val GlassShapes = SkinShapes(
    cardCornerRadius = 20.dp,
    buttonCornerRadius = 16.dp,
    borderWidth = 1.dp,           // Subtle borders for glass edges
    elevation = 8.dp,             // Higher elevation for layered depth
    blurRadius = 24.dp            // Blur for frosted glass effect
)
