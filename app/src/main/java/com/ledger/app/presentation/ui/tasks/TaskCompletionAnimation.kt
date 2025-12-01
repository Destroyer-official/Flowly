package com.ledger.app.presentation.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ledger.app.presentation.theme.LocalSkinColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


/**
 * Satisfying micro-interaction animation for task completion.
 * 
 * Features:
 * - Circular progress animation
 * - Checkmark reveal with bounce
 * - Particle burst effect
 * - Skin-aware color theming
 * - Consistent with 2026 aesthetics
 * 
 * Requirements: 14.4
 */
@Composable
fun TaskCompletionAnimation(
    isAnimating: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val skinColors = LocalSkinColors.current
    val successColor = skinColors.receivedColor
    
    // Animation states
    val circleProgress = remember { Animatable(0f) }
    val checkmarkProgress = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    val scaleAnimation = remember { Animatable(1f) }
    
    // Particle data
    val particles = remember {
        List(12) { index ->
            Particle(
                angle = (index * 30f) + Random.nextFloat() * 15f,
                distance = 0.8f + Random.nextFloat() * 0.4f,
                size = 3f + Random.nextFloat() * 3f,
                color = if (index % 2 == 0) successColor else successColor.copy(alpha = 0.6f)
            )
        }
    }
    
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            // Reset all animations
            circleProgress.snapTo(0f)
            checkmarkProgress.snapTo(0f)
            particleProgress.snapTo(0f)
            scaleAnimation.snapTo(1f)
            
            // Phase 1: Circle drawing (0-300ms)
            launch {
                circleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
            
            // Phase 2: Scale bounce (200-400ms)
            launch {
                delay(200)
                scaleAnimation.animateTo(
                    targetValue = 1.15f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
                scaleAnimation.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            
            // Phase 3: Checkmark reveal (250-500ms)
            launch {
                delay(250)
                checkmarkProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            
            // Phase 4: Particle burst (300-700ms)
            launch {
                delay(300)
                particleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            }
            
            // Complete animation after all phases
            delay(700)
            onAnimationComplete()
        }
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Particle burst layer
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(scaleAnimation.value)
        ) {
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2
            val maxRadius = this.size.minDimension / 2
            
            particles.forEach { particle ->
                val progress = particleProgress.value
                val distance = maxRadius * particle.distance * progress * 1.5f
                val alpha = (1f - progress).coerceIn(0f, 1f)
                
                val angleRad = particle.angle * PI.toFloat() / 180f
                val x = centerX + cos(angleRad) * distance
                val y = centerY + sin(angleRad) * distance
                
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.size * (1f - progress * 0.5f),
                    center = Offset(x, y)
                )
            }
        }
        
        // Circle progress layer
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(scaleAnimation.value)
        ) {
            val strokeWidth = 4.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2
            
            // Background circle
            drawCircle(
                color = successColor.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            
            // Progress arc
            drawArc(
                color = successColor,
                startAngle = -90f,
                sweepAngle = 360f * circleProgress.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Checkmark icon
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Task completed",
            tint = successColor,
            modifier = Modifier
                .size(size * 0.5f)
                .scale(checkmarkProgress.value)
                .alpha(checkmarkProgress.value)
        )
    }
}

private data class Particle(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val color: Color
)


/**
 * Inline task completion animation for use within list items.
 * A smaller, more subtle version of the full animation.
 * 
 * Requirements: 14.4
 */
@Composable
fun TaskCompletionCheckmark(
    isCompleted: Boolean,
    onAnimationComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val skinColors = LocalSkinColors.current
    val successColor = skinColors.receivedColor
    
    var hasAnimated by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isCompleted && !hasAnimated) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        finishedListener = {
            if (isCompleted && !hasAnimated) {
                hasAnimated = true
                onAnimationComplete()
            }
        },
        label = "checkmark_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(200),
        label = "checkmark_alpha"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = if (isCompleted) successColor.copy(alpha = 0.15f) 
                       else Color.Gray.copy(alpha = 0.1f),
                radius = this.size.minDimension / 2
            )
        }
        
        // Checkmark
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = if (isCompleted) "Completed" else "Not completed",
            tint = successColor,
            modifier = Modifier
                .size(size * 0.6f)
                .scale(scale)
                .alpha(alpha)
        )
    }
}

/**
 * Full-screen overlay animation for celebrating task completion.
 * Used for important task completions or milestone achievements.
 * 
 * Requirements: 14.4
 */
@Composable
fun TaskCompletionOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "overlay_alpha"
    )
    
    if (overlayAlpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .alpha(overlayAlpha),
            contentAlignment = Alignment.Center
        ) {
            // Semi-transparent background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = skinColors.background.copy(alpha = 0.85f)
                )
            }
            
            // Centered completion animation
            TaskCompletionAnimation(
                isAnimating = isVisible,
                onAnimationComplete = onDismiss,
                size = 120.dp
            )
        }
    }
}
