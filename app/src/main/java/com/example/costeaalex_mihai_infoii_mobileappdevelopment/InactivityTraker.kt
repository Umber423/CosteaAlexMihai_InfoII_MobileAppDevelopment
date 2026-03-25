package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlin.math.sqrt

class InactivityTracker(
    private val context: Context,
    private val stillThresholdMinutes: Long = 10,
    private val onStillTooLong: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastMovementTime = System.currentTimeMillis()
    private var isTracking = false

    private val movementThreshold = 1.5f
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    private val checkHandler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            val stillDuration = System.currentTimeMillis() - lastMovementTime
            val thresholdMs = stillThresholdMinutes * 60 * 1000

            if (stillDuration >= thresholdMs) {
                stop() // stop tracking so it doesn't fire repeatedly
                onStillTooLong() // this should call triggerEmergency() directly
            } else {
                checkHandler.postDelayed(this, 10_000)
            }
        }
    }

    fun start() {
        isTracking = true
        lastMovementTime = System.currentTimeMillis()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        checkHandler.removeCallbacksAndMessages(null)
        checkHandler.postDelayed(checkRunnable, 10_000)
    }

    fun stop() {
        isTracking = false
        sensorManager.unregisterListener(this)
        checkHandler.removeCallbacks(checkRunnable)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val delta = sqrt(
            (x - lastX) * (x - lastX) +
                    (y - lastY) * (y - lastY) +
                    (z - lastZ) * (z - lastZ)
        )

        if (delta > movementThreshold) {
            lastMovementTime = System.currentTimeMillis()
        }

        lastX = x
        lastY = y
        lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}