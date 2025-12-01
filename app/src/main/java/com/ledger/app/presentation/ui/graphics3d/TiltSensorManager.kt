package com.ledger.app.presentation.ui.graphics3d

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing device tilt values.
 * 
 * @param x Tilt along X-axis (pitch) - normalized to -1.0 to 1.0
 * @param y Tilt along Y-axis (roll) - normalized to -1.0 to 1.0
 * @param z Tilt along Z-axis (azimuth) - normalized to -1.0 to 1.0
 */
data class TiltData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

/**
 * TiltSensorManager handles device orientation sensing for 3D visualization effects.
 * 
 * Uses the accelerometer sensor to detect device tilt and provides normalized
 * values that can be used to create parallax and depth effects in the UI.
 * 
 * Requirements: 6.2
 */
class TiltSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _tiltData = MutableStateFlow(TiltData())
    val tiltData: StateFlow<TiltData> = _tiltData.asStateFlow()

    private var isListening = false
    
    // Low-pass filter coefficient for smoothing sensor data
    private val alpha = 0.15f
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    /**
     * Starts listening to sensor updates.
     */
    fun startListening() {
        if (!isListening && accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
            isListening = true
        }
    }

    /**
     * Stops listening to sensor updates.
     */
    fun stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Apply low-pass filter for smooth values
                lastX = lowPassFilter(it.values[0], lastX)
                lastY = lowPassFilter(it.values[1], lastY)
                lastZ = lowPassFilter(it.values[2], lastZ)

                // Normalize to -1.0 to 1.0 range
                // Accelerometer values are typically in m/s² with gravity ~9.8
                val normalizedX = (lastX / 9.8f).coerceIn(-1f, 1f)
                val normalizedY = (lastY / 9.8f).coerceIn(-1f, 1f)
                val normalizedZ = (lastZ / 9.8f).coerceIn(-1f, 1f)

                _tiltData.value = TiltData(
                    x = normalizedX,
                    y = normalizedY,
                    z = normalizedZ
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    /**
     * Low-pass filter to smooth sensor readings.
     */
    private fun lowPassFilter(input: Float, output: Float): Float {
        return output + alpha * (input - output)
    }

    /**
     * Checks if the device has an accelerometer sensor.
     */
    fun hasAccelerometer(): Boolean = accelerometer != null
}

/**
 * Composable function that provides tilt sensor data with lifecycle awareness.
 * 
 * Automatically starts/stops sensor listening based on lifecycle state.
 * 
 * @param enabled Whether tilt sensing should be enabled
 * @return State containing current TiltData
 * 
 * Requirements: 6.2
 */
@Composable
fun rememberTiltSensor(enabled: Boolean = true): State<TiltData> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val tiltState = remember { mutableStateOf(TiltData()) }
    val tiltSensorManager = remember { TiltSensorManager(context) }

    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (enabled && tiltSensorManager.hasAccelerometer()) {
                        tiltSensorManager.startListening()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    tiltSensorManager.stopListening()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        // Start listening if already resumed and enabled
        if (enabled && 
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            tiltSensorManager.hasAccelerometer()
        ) {
            tiltSensorManager.startListening()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            tiltSensorManager.stopListening()
        }
    }

    // Collect tilt data updates
    DisposableEffect(tiltSensorManager, enabled) {
        val scope = CoroutineScope(Dispatchers.Main + Job())
        scope.launch {
            tiltSensorManager.tiltData.collect { data ->
                tiltState.value = data
            }
        }

        onDispose {
            scope.cancel()
        }
    }

    return tiltState
}

/**
 * Extension function to check if tilt sensing is available on the device.
 */
@Composable
fun isTiltSensorAvailable(): Boolean {
    val context = LocalContext.current
    return remember {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }
}
