package com.ledger.app.presentation.ui.graphics3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.math.BigDecimal

/**
 * Animation specifications for balance data changes.
 * 
 * Requirements: 6.3
 */
object BalanceAnimationSpecs {
    
    /**
     * Default animation for balance value changes.
     * Uses a smooth ease-out curve for natural feel.
     */
    val defaultValueChange: AnimationSpec<Float> = tween(
        durationMillis = 800,
        easing = FastOutSlowInEasing
    )
    
    /**
     * Spring animation for bouncy effects on significant changes.
     */
    val springBounce: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    /**
     * Quick animation for minor updates.
     */
    val quickUpdate: AnimationSpec<Float> = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
    
    /**
     * Slow animation for dramatic reveals.
     */
    val dramaticReveal: AnimationSpec<Float> = tween(
        durationMillis = 1200,
        easing = FastOutSlowInEasing
    )
}

/**
 * Data class representing animated balance values.
 */
data class AnimatedBalanceState(
    val owedToUser: Float,
    val userOwes: Float,
    val netBalance: Float,
    val isAnimating: Boolean
)

/**
 * Composable that provides animated balance values with smooth transitions.
 * 
 * @param totalOwedToUser Current value of money owed to user
 * @param totalUserOwes Current value of money user owes
 * @param animationSpec Animation specification to use
 * @return State containing animated balance values
 * 
 * Requirements: 6.3
 */
@Composable
fun rememberAnimatedBalance(
    totalOwedToUser: BigDecimal,
    totalUserOwes: BigDecimal,
    animationSpec: AnimationSpec<Float> = BalanceAnimationSpecs.defaultValueChange
): State<AnimatedBalanceState> {
    val state = remember { mutableStateOf(
        AnimatedBalanceState(
            owedToUser = totalOwedToUser.toFloat(),
            userOwes = totalUserOwes.toFloat(),
            netBalance = (totalOwedToUser - totalUserOwes).toFloat(),
            isAnimating = false
        )
    )}
    
    val animatedOwedToUser = remember { Animatable(totalOwedToUser.toFloat()) }
    val animatedUserOwes = remember { Animatable(totalUserOwes.toFloat()) }
    
    LaunchedEffect(totalOwedToUser) {
        state.value = state.value.copy(isAnimating = true)
        animatedOwedToUser.animateTo(
            targetValue = totalOwedToUser.toFloat(),
            animationSpec = animationSpec
        )
        state.value = state.value.copy(
            owedToUser = animatedOwedToUser.value,
            netBalance = animatedOwedToUser.value - animatedUserOwes.value,
            isAnimating = animatedUserOwes.isRunning
        )
    }
    
    LaunchedEffect(totalUserOwes) {
        state.value = state.value.copy(isAnimating = true)
        animatedUserOwes.animateTo(
            targetValue = totalUserOwes.toFloat(),
            animationSpec = animationSpec
        )
        state.value = state.value.copy(
            userOwes = animatedUserOwes.value,
            netBalance = animatedOwedToUser.value - animatedUserOwes.value,
            isAnimating = animatedOwedToUser.isRunning
        )
    }
    
    // Update state continuously during animation
    LaunchedEffect(animatedOwedToUser.value, animatedUserOwes.value) {
        state.value = AnimatedBalanceState(
            owedToUser = animatedOwedToUser.value,
            userOwes = animatedUserOwes.value,
            netBalance = animatedOwedToUser.value - animatedUserOwes.value,
            isAnimating = animatedOwedToUser.isRunning || animatedUserOwes.isRunning
        )
    }
    
    return state
}

/**
 * Determines the appropriate animation spec based on the magnitude of change.
 * 
 * @param oldValue Previous balance value
 * @param newValue New balance value
 * @return Appropriate AnimationSpec for the change magnitude
 */
fun getAnimationSpecForChange(
    oldValue: BigDecimal,
    newValue: BigDecimal
): AnimationSpec<Float> {
    val changePercent = if (oldValue != BigDecimal.ZERO) {
        ((newValue - oldValue).abs().toFloat() / oldValue.abs().toFloat()) * 100
    } else {
        100f
    }
    
    return when {
        changePercent > 50 -> BalanceAnimationSpecs.dramaticReveal
        changePercent > 20 -> BalanceAnimationSpecs.springBounce
        changePercent > 5 -> BalanceAnimationSpecs.defaultValueChange
        else -> BalanceAnimationSpecs.quickUpdate
    }
}

/**
 * Pulse animation effect for highlighting changes.
 */
data class PulseEffect(
    val scale: Float = 1f,
    val alpha: Float = 1f
)

/**
 * Composable that provides a pulse effect when value changes.
 * 
 * @param trigger Value that triggers the pulse when changed
 * @return State containing pulse effect parameters
 */
@Composable
fun rememberPulseEffect(trigger: Any): State<PulseEffect> {
    val state = remember { mutableStateOf(PulseEffect()) }
    val scaleAnimatable = remember { Animatable(1f) }
    val alphaAnimatable = remember { Animatable(1f) }
    
    LaunchedEffect(trigger) {
        // Scale up
        scaleAnimatable.animateTo(
            targetValue = 1.1f,
            animationSpec = tween(150)
        )
        // Scale back down with bounce
        scaleAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    
    LaunchedEffect(scaleAnimatable.value, alphaAnimatable.value) {
        state.value = PulseEffect(
            scale = scaleAnimatable.value,
            alpha = alphaAnimatable.value
        )
    }
    
    return state
}
