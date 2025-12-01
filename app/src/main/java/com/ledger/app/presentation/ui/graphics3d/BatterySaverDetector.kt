package com.ledger.app.presentation.ui.graphics3d

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Detects battery saver mode and provides state for UI adaptation.
 * 
 * When battery saver is enabled, the app should fall back to static 2D graphics
 * to conserve power.
 * 
 * Requirements: 6.4
 */
class BatterySaverDetector(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private val _isBatterySaverEnabled = MutableStateFlow(checkBatterySaverStatus())
    val isBatterySaverEnabled: StateFlow<Boolean> = _isBatterySaverEnabled.asStateFlow()

    private val batterySaverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                _isBatterySaverEnabled.value = checkBatterySaverStatus()
            }
        }
    }

    private var isReceiverRegistered = false

    /**
     * Starts listening for battery saver mode changes.
     */
    fun startListening() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batterySaverReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batterySaverReceiver, filter)
            }
            isReceiverRegistered = true
            // Update initial state
            _isBatterySaverEnabled.value = checkBatterySaverStatus()
        }
    }

    /**
     * Stops listening for battery saver mode changes.
     */
    fun stopListening() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(batterySaverReceiver)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered
            }
            isReceiverRegistered = false
        }
    }

    /**
     * Checks the current battery saver status.
     */
    private fun checkBatterySaverStatus(): Boolean {
        return powerManager.isPowerSaveMode
    }

    /**
     * Gets the current battery saver status synchronously.
     */
    fun isBatterySaverOn(): Boolean = checkBatterySaverStatus()
}

/**
 * Composable that provides battery saver state with lifecycle awareness.
 * 
 * @return State indicating whether battery saver mode is enabled
 * 
 * Requirements: 6.4
 */
@Composable
fun rememberBatterySaverState(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(false) }
    val detector = remember { BatterySaverDetector(context) }

    DisposableEffect(detector) {
        detector.startListening()
        state.value = detector.isBatterySaverOn()

        // Collect updates
        val scope = CoroutineScope(Dispatchers.Main + Job())
        scope.launch {
            detector.isBatterySaverEnabled.collect { isEnabled ->
                state.value = isEnabled
            }
        }

        onDispose {
            scope.cancel()
            detector.stopListening()
        }
    }

    return state
}

/**
 * Determines whether to use 3D graphics based on battery saver and device capabilities.
 * 
 * @param forceLowPower Force low power mode regardless of battery saver status
 * @return State indicating whether 3D graphics should be used
 */
@Composable
fun shouldUse3DGraphics(forceLowPower: Boolean = false): State<Boolean> {
    val batterySaverState = rememberBatterySaverState()
    val hasTiltSensor = isTiltSensorAvailable()
    
    val state = remember { mutableStateOf(true) }
    
    // Update state based on conditions
    state.value = !forceLowPower && !batterySaverState.value && hasTiltSensor
    
    return state
}

/**
 * Graphics mode enum for explicit control.
 */
enum class GraphicsMode {
    /** Full 3D graphics with animations and tilt response */
    FULL_3D,
    /** 3D graphics without tilt response */
    STATIC_3D,
    /** Simple 2D graphics for battery saving */
    STATIC_2D,
    /** Automatic selection based on device state */
    AUTO
}

/**
 * Resolves the actual graphics mode based on AUTO setting and device state.
 * 
 * @param requestedMode The requested graphics mode
 * @param isBatterySaverOn Whether battery saver is enabled
 * @param hasTiltSensor Whether the device has a tilt sensor
 * @return The resolved graphics mode to use
 */
fun resolveGraphicsMode(
    requestedMode: GraphicsMode,
    isBatterySaverOn: Boolean,
    hasTiltSensor: Boolean
): GraphicsMode {
    return when (requestedMode) {
        GraphicsMode.AUTO -> when {
            isBatterySaverOn -> GraphicsMode.STATIC_2D
            !hasTiltSensor -> GraphicsMode.STATIC_3D
            else -> GraphicsMode.FULL_3D
        }
        GraphicsMode.FULL_3D -> if (isBatterySaverOn) GraphicsMode.STATIC_2D else requestedMode
        else -> requestedMode
    }
}
