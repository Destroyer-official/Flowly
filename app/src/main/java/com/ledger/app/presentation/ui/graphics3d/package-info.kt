/**
 * 3D Graphics Package for Ledger App
 * 
 * This package contains components for immersive 3D balance visualization:
 * 
 * - [Balance3DVisualization]: Main 3D visualization component with animated bars and spheres
 * - [Balance2DVisualization]: Static 2D fallback for battery saver mode
 * - [AdaptiveBalanceVisualization]: Smart component that auto-selects visualization mode
 * - [TiltSensorManager]: Handles device tilt sensing for parallax effects
 * - [BatterySaverDetector]: Detects battery saver mode for automatic fallback
 * - [BalanceAnimations]: Animation utilities for smooth data transitions
 * 
 * Requirements covered:
 * - 6.1: Animated elements for balance visualization
 * - 6.2: Tilt-responsive visual elements with depth effects
 * - 6.3: Smooth animations when data changes
 * - 6.4: Battery saver fallback to static 2D graphics
 * 
 * Usage:
 * ```kotlin
 * AdaptiveBalanceVisualization(
 *     totalOwedToUser = BigDecimal("1000"),
 *     totalUserOwes = BigDecimal("500"),
 *     graphicsMode = GraphicsMode.AUTO
 * )
 * ```
 */
package com.ledger.app.presentation.ui.graphics3d
