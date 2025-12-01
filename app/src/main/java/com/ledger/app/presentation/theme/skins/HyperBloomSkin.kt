package com.ledger.app.presentation.theme.skins

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ledger.app.presentation.theme.SkinColors
import com.ledger.app.presentation.theme.SkinShapes

/**
 * Hyper-Bloom design skin.
 * Features: Fluid organic shapes, soft dreamy palette, nature-inspired textures,
 * extra rounded corners, calming approachable feel.
 * 
 * This is an enhanced version of Organic Bloom with more vibrant organic aesthetics.
 */

// Light mode colors (soft, dreamy, nature-inspired)
private val HyperBloomLavender = Color(0xFFE8D5E8)
private val HyperBloomMint = Color(0xFFB8E0D2)
private val HyperBloomCoral = Color(0xFFF8B4B4)
private val HyperBloomCream = Color(0xFFFFF8E7)
private val HyperBloomSage = Color(0xFF9DC08B)
private val HyperBloomPeach = Color(0xFFFFCBA4)
private val HyperBloomSky = Color(0xFFA8D8EA)
private val HyperBloomRose = Color(0xFFE8A0BF)

val HyperBloomLightColors = SkinColors(
    primary = Color(0xFF7B68EE),      // Medium slate blue
    secondary = Color(0xFFDDA0DD),     // Plum
    background = HyperBloomCream,
    surface = Color(0xFFFFFDF7),
    cardBackground = Color(0xFFFFFFFF).copy(alpha = 0.9f),
    gaveColor = HyperBloomCoral,
    receivedColor = HyperBloomMint,
    textPrimary = Color(0xFF2D3436),
    textSecondary = Color(0xFF636E72),
    error = Color(0xFFE17055),
    onPrimary = Color.White,
    onSecondary = Color(0xFF2D3436)
)

// Dark mode colors (deep, calming, organic)
private val HyperBloomDarkBg = Color(0xFF1A1A2E)
private val HyperBloomDarkSurface = Color(0xFF252542)
private val HyperBloomDarkCard = Color(0xFF2D2D4A)
private val HyperBloomDarkLavender = Color(0xFFB794F4)
private val HyperBloomDarkMint = Color(0xFF68D391)
private val HyperBloomDarkCoral = Color(0xFFFEB2B2)
private val HyperBloomDarkRose = Color(0xFFF687B3)

val HyperBloomDarkColors = SkinColors(
    primary = HyperBloomDarkLavender,
    secondary = HyperBloomDarkRose,
    background = HyperBloomDarkBg,
    surface = HyperBloomDarkSurface,
    cardBackground = HyperBloomDarkCard,
    gaveColor = HyperBloomDarkCoral,
    receivedColor = HyperBloomDarkMint,
    textPrimary = Color(0xFFF7FAFC),
    textSecondary = Color(0xFFA0AEC0),
    error = Color(0xFFFC8181),
    onPrimary = Color(0xFF1A1A2E),
    onSecondary = Color(0xFF1A1A2E)
)

val HyperBloomShapes = SkinShapes(
    cardCornerRadius = 28.dp,     // Extra rounded for organic feel
    buttonCornerRadius = 24.dp,   // Pill-shaped buttons
    borderWidth = 0.dp,           // No harsh borders
    elevation = 4.dp,             // Soft, subtle shadows
    blurRadius = 8.dp             // Slight blur for dreamy effect
)
