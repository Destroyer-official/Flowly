package com.ledger.app.presentation.ui.graphics3d

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.presentation.theme.LocalSkinColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptive Balance Visualization Component
 * 
 * Automatically switches between 3D and 2D visualization based on:
 * - Battery saver mode
 * - Device sensor availability
 * - User preference
 * 
 * This is the main entry point for balance visualization in the app.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 */
@Composable
fun AdaptiveBalanceVisualization(
    totalOwedToUser: BigDecimal,
    totalUserOwes: BigDecimal,
    graphicsMode: GraphicsMode = GraphicsMode.AUTO,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    
    // Check device state
    val isBatterySaverOn by rememberBatterySaverState()
    val hasTiltSensor = isTiltSensorAvailable()
    
    // Resolve actual graphics mode
    val resolvedMode = remember(graphicsMode, isBatterySaverOn, hasTiltSensor) {
        resolveGraphicsMode(graphicsMode, isBatterySaverOn, hasTiltSensor)
    }
    
    // Get tilt data if using full 3D
    val tiltData by rememberTiltSensor(enabled = resolvedMode == GraphicsMode.FULL_3D)
    
    // Calculate net balance for display
    val netBalance = totalOwedToUser - totalUserOwes
    val isPositive = netBalance >= BigDecimal.ZERO
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visualization area with crossfade transition
        Crossfade(
            targetState = resolvedMode,
            animationSpec = tween(500),
            label = "graphics_mode_transition"
        ) { mode ->
            when (mode) {
                GraphicsMode.FULL_3D -> {
                    Balance3DVisualization(
                        totalOwedToUser = totalOwedToUser,
                        totalUserOwes = totalUserOwes,
                        tiltX = tiltData.x,
                        tiltY = tiltData.y,
                        isAnimated = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                GraphicsMode.STATIC_3D -> {
                    Balance3DVisualization(
                        totalOwedToUser = totalOwedToUser,
                        totalUserOwes = totalUserOwes,
                        tiltX = 0f,
                        tiltY = 0f,
                        isAnimated = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                GraphicsMode.STATIC_2D, GraphicsMode.AUTO -> {
                    Balance2DVisualization(
                        totalOwedToUser = totalOwedToUser,
                        totalUserOwes = totalUserOwes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Net balance text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Net Balance",
                    fontSize = 12.sp,
                    color = skinColors.textSecondary
                )
                Text(
                    text = formatCurrency(netBalance),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) skinColors.receivedColor else skinColors.gaveColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isPositive) "You're owed money" else "You owe money",
                    fontSize = 11.sp,
                    color = skinColors.textSecondary
                )
            }
        }
        
        // Battery saver indicator
        if (isBatterySaverOn && graphicsMode == GraphicsMode.AUTO) {
            Text(
                text = "⚡ Battery saver mode - simplified graphics",
                fontSize = 10.sp,
                color = skinColors.textSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Formats a BigDecimal as currency.
 */
private fun formatCurrency(amount: BigDecimal): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount.abs())
}

/**
 * Compact version of the balance visualization for smaller spaces.
 */
@Composable
fun CompactBalanceVisualization(
    totalOwedToUser: BigDecimal,
    totalUserOwes: BigDecimal,
    modifier: Modifier = Modifier
) {
    val isBatterySaverOn by rememberBatterySaverState()
    
    if (isBatterySaverOn) {
        // Ultra-simple visualization for battery saver
        Balance2DVisualization(
            totalOwedToUser = totalOwedToUser,
            totalUserOwes = totalUserOwes,
            modifier = modifier
        )
    } else {
        // Simplified 3D without tilt
        Balance3DVisualization(
            totalOwedToUser = totalOwedToUser,
            totalUserOwes = totalUserOwes,
            tiltX = 0f,
            tiltY = 0f,
            isAnimated = true,
            modifier = modifier
        )
    }
}
