package com.ledger.app.presentation.ui.graphics3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.ledger.app.presentation.theme.LocalSkinColors
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Balance Visualization Component
 * 
 * Renders an interactive 3D visualization of balance data with:
 * - Animated 3D bars representing "They owe you" and "You owe"
 * - Tilt-responsive perspective changes
 * - Smooth animations when data changes
 * - Depth effects and shadows
 * 
 * Requirements: 6.1, 6.2, 6.3
 */
@Composable
fun Balance3DVisualization(
    totalOwedToUser: BigDecimal,
    totalUserOwes: BigDecimal,
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    isAnimated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current

    // Normalize values for visualization (max height = 1.0)
    val maxValue = maxOf(totalOwedToUser, totalUserOwes, BigDecimal.ONE)
    val owedToUserNormalized = (totalOwedToUser.toFloat() / maxValue.toFloat()).coerceIn(0.1f, 1f)
    val userOwesNormalized = (totalUserOwes.toFloat() / maxValue.toFloat()).coerceIn(0.1f, 1f)

    // Animated values for smooth data transitions
    val animatedOwedToUser = remember { Animatable(owedToUserNormalized) }
    val animatedUserOwes = remember { Animatable(userOwesNormalized) }

    LaunchedEffect(owedToUserNormalized) {
        animatedOwedToUser.animateTo(
            targetValue = owedToUserNormalized,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(userOwesNormalized) {
        animatedUserOwes.animateTo(
            targetValue = userOwesNormalized,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    // Ambient animation for floating effect
    val infiniteTransition = rememberInfiniteTransition(label = "3d_ambient")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimated) 8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    val rotationPulse by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = if (isAnimated) 2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation_pulse"
    )

    // Tilt-responsive rotation
    val effectiveTiltX by animateFloatAsState(
        targetValue = tiltX * 15f + rotationPulse,
        animationSpec = tween(100),
        label = "tilt_x"
    )
    val effectiveTiltY by animateFloatAsState(
        targetValue = tiltY * 15f,
        animationSpec = tween(100),
        label = "tilt_y"
    )

    Box(modifier = modifier.height(200.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val barWidth = size.width * 0.25f
            val maxBarHeight = size.height * 0.7f
            val depth = 30f
            val spacing = size.width * 0.15f

            // Apply perspective transformation based on tilt
            translate(
                left = effectiveTiltY * 2f,
                top = floatOffset
            ) {
                rotate(degrees = effectiveTiltX * 0.3f, pivot = Offset(centerX, centerY)) {
                    // Draw "You Owe" bar (left, red)
                    draw3DBar(
                        x = centerX - spacing - barWidth,
                        y = centerY + maxBarHeight / 2,
                        width = barWidth,
                        height = maxBarHeight * animatedUserOwes.value,
                        depth = depth,
                        frontColor = skinColors.gaveColor,
                        sideColor = skinColors.gaveColor.copy(alpha = 0.7f),
                        topColor = skinColors.gaveColor.copy(alpha = 0.85f),
                        shadowColor = Color.Black.copy(alpha = 0.2f)
                    )

                    // Draw "They Owe You" bar (right, green)
                    draw3DBar(
                        x = centerX + spacing,
                        y = centerY + maxBarHeight / 2,
                        width = barWidth,
                        height = maxBarHeight * animatedOwedToUser.value,
                        depth = depth,
                        frontColor = skinColors.receivedColor,
                        sideColor = skinColors.receivedColor.copy(alpha = 0.7f),
                        topColor = skinColors.receivedColor.copy(alpha = 0.85f),
                        shadowColor = Color.Black.copy(alpha = 0.2f)
                    )

                    // Draw center balance indicator (sphere-like)
                    val netBalance = totalOwedToUser.toFloat() - totalUserOwes.toFloat()
                    val indicatorColor = if (netBalance >= 0) skinColors.receivedColor else skinColors.gaveColor
                    draw3DSphere(
                        center = Offset(centerX, centerY - 20f + floatOffset * 0.5f),
                        radius = 25f,
                        color = indicatorColor,
                        highlightColor = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}


/**
 * Draws a 3D bar with front face, side face, and top face.
 */
private fun DrawScope.draw3DBar(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    depth: Float,
    frontColor: Color,
    sideColor: Color,
    topColor: Color,
    shadowColor: Color
) {
    // Shadow
    drawRect(
        color = shadowColor,
        topLeft = Offset(x + 8f, y - height + 8f),
        size = Size(width, height)
    )

    // Front face
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(frontColor, frontColor.copy(alpha = 0.8f)),
            startY = y - height,
            endY = y
        ),
        topLeft = Offset(x, y - height),
        size = Size(width, height)
    )

    // Top face (parallelogram)
    val topPath = Path().apply {
        moveTo(x, y - height)
        lineTo(x + depth * 0.7f, y - height - depth * 0.5f)
        lineTo(x + width + depth * 0.7f, y - height - depth * 0.5f)
        lineTo(x + width, y - height)
        close()
    }
    drawPath(
        path = topPath,
        brush = Brush.horizontalGradient(
            colors = listOf(topColor, topColor.copy(alpha = 0.9f))
        )
    )

    // Right side face (parallelogram)
    val sidePath = Path().apply {
        moveTo(x + width, y - height)
        lineTo(x + width + depth * 0.7f, y - height - depth * 0.5f)
        lineTo(x + width + depth * 0.7f, y - depth * 0.5f)
        lineTo(x + width, y)
        close()
    }
    drawPath(
        path = sidePath,
        color = sideColor
    )

    // Front face border
    drawRect(
        color = frontColor.copy(alpha = 0.3f),
        topLeft = Offset(x, y - height),
        size = Size(width, height),
        style = Stroke(width = 2f)
    )
}

/**
 * Draws a 3D sphere-like indicator with gradient and highlight.
 */
private fun DrawScope.draw3DSphere(
    center: Offset,
    radius: Float,
    color: Color,
    highlightColor: Color
) {
    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.15f),
        radius = radius,
        center = Offset(center.x + 4f, center.y + 4f)
    )

    // Main sphere with radial gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.9f),
                color,
                color.copy(alpha = 0.7f)
            ),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius * 1.5f
        ),
        radius = radius,
        center = center
    )

    // Highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                highlightColor,
                Color.Transparent
            ),
            center = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f),
            radius = radius * 0.6f
        ),
        radius = radius * 0.5f,
        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f)
    )

    // Border
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5f)
    )
}

/**
 * Static 2D fallback visualization for battery saver mode.
 * 
 * Requirements: 6.4
 */
@Composable
fun Balance2DVisualization(
    totalOwedToUser: BigDecimal,
    totalUserOwes: BigDecimal,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current

    // Normalize values
    val maxValue = maxOf(totalOwedToUser, totalUserOwes, BigDecimal.ONE)
    val owedToUserNormalized = (totalOwedToUser.toFloat() / maxValue.toFloat()).coerceIn(0.1f, 1f)
    val userOwesNormalized = (totalUserOwes.toFloat() / maxValue.toFloat()).coerceIn(0.1f, 1f)

    Box(modifier = modifier.height(150.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val centerX = size.width / 2
            val barWidth = size.width * 0.3f
            val maxBarHeight = size.height * 0.8f
            val spacing = size.width * 0.1f
            val baseY = size.height * 0.9f

            // "You Owe" bar (left)
            drawRect(
                color = skinColors.gaveColor,
                topLeft = Offset(centerX - spacing - barWidth, baseY - maxBarHeight * userOwesNormalized),
                size = Size(barWidth, maxBarHeight * userOwesNormalized)
            )

            // "They Owe You" bar (right)
            drawRect(
                color = skinColors.receivedColor,
                topLeft = Offset(centerX + spacing, baseY - maxBarHeight * owedToUserNormalized),
                size = Size(barWidth, maxBarHeight * owedToUserNormalized)
            )

            // Base line
            drawLine(
                color = skinColors.textSecondary.copy(alpha = 0.5f),
                start = Offset(0f, baseY),
                end = Offset(size.width, baseY),
                strokeWidth = 2f
            )
        }
    }
}
