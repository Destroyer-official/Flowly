package com.ledger.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ledger.app.presentation.theme.skins.*

/**
 * Theme mode options for the Ledger app.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Main theme composable for the Offline Ledger app.
 * Supports light/dark mode switching and multiple design skins.
 * 
 * @param themeMode Light, Dark, or System theme mode
 * @param designSkin Selected design skin (NEO_MINIMAL, GLASS, etc.)
 * @param content The composable content to theme
 */
@Composable
fun LedgerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    designSkin: DesignSkin = DesignSkin.NEO_MINIMAL,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }
    
    val skinColors = getSkinColors(designSkin, darkTheme)
    val skinShapes = getSkinShapes(designSkin)
    val materialColorScheme = skinColors.toMaterialColorScheme()
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = skinColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalSkinColors provides skinColors,
        LocalSkinShapes provides skinShapes
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}

/**
 * Get colors for the specified design skin and theme mode.
 */
private fun getSkinColors(skin: DesignSkin, darkTheme: Boolean): SkinColors {
    return when (skin) {
        DesignSkin.NEO_MINIMAL -> if (darkTheme) NeoMinimalDarkColors else NeoMinimalLightColors
        DesignSkin.GLASS -> if (darkTheme) GlassDarkColors else GlassLightColors
        DesignSkin.RETRO_FUTURISM -> if (darkTheme) RetroFuturismDarkColors else RetroFuturismLightColors
        DesignSkin.NEO_BRUTAL -> if (darkTheme) NeoBrutalDarkColors else NeoBrutalLightColors
        DesignSkin.HYPER_BLOOM -> if (darkTheme) HyperBloomDarkColors else HyperBloomLightColors
    }
}

/**
 * Get shapes for the specified design skin.
 */
private fun getSkinShapes(skin: DesignSkin): SkinShapes {
    return when (skin) {
        DesignSkin.NEO_MINIMAL -> NeoMinimalShapes
        DesignSkin.GLASS -> GlassShapes
        DesignSkin.RETRO_FUTURISM -> RetroFuturismShapes
        DesignSkin.NEO_BRUTAL -> NeoBrutalShapes
        DesignSkin.HYPER_BLOOM -> HyperBloomShapes
    }
}

/**
 * Convert SkinColors to Material3 ColorScheme.
 */
private fun SkinColors.toMaterialColorScheme(): ColorScheme {
    return ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primary,
        onPrimaryContainer = onPrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondary,
        onSecondaryContainer = onSecondary,
        tertiary = gaveColor,
        onTertiary = onPrimary,
        tertiaryContainer = receivedColor,
        onTertiaryContainer = onPrimary,
        error = error,
        onError = onPrimary,
        errorContainer = error,
        onErrorContainer = onPrimary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = cardBackground,
        onSurfaceVariant = textSecondary,
        outline = textSecondary,
        outlineVariant = textSecondary.copy(alpha = 0.5f),
        scrim = textPrimary.copy(alpha = 0.3f),
        inverseSurface = textPrimary,
        inverseOnSurface = surface,
        inversePrimary = primary,
        surfaceTint = primary
    )
}
