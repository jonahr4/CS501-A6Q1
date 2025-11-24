package com.example.a6q1

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.pow

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var altitudeText: TextView
    private lateinit var pressureText: TextView
    private lateinit var statusText: TextView

    private val p0 = 1013.25f
    private val handler = Handler(Looper.getMainLooper())
    private var fakePressure = p0

    private val simulatePressure = object : Runnable {
        override fun run() {
            // fake sensor gently drifts so the UI still moves
            fakePressure -= 0.8f
            if (fakePressure < 980f) {
                fakePressure = p0
            }
            updateReadings(fakePressure, false)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.main_root)
        altitudeText = findViewById(R.id.altitude_value)
        pressureText = findViewById(R.id.pressure_value)
        statusText = findViewById(R.id.status_value)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor != null) {
            statusText.text = getString(R.string.status_live)
        } else {
            statusText.text = getString(R.string.status_fake)
            handler.post(simulatePressure)
        }
    }

    override fun onResume() {
        super.onResume()
        pressureSensor?.also {
            // register when activity is visible
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: handler.post(simulatePressure)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(simulatePressure)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val pressure = event?.values?.firstOrNull() ?: return
        updateReadings(pressure, true)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // not needed for this simple demo
    }

    private fun updateReadings(pressure: Float, live: Boolean) {
        val altitude = pressureToAltitude(pressure)
        altitudeText.text = getString(R.string.altitude_format, altitude)
        pressureText.text = getString(R.string.pressure_format, pressure)
        if (!live) {
            statusText.text = getString(R.string.status_fake)
        }
        applyBackground(altitude)
    }

    private fun pressureToAltitude(pressure: Float): Double {
        val ratio = (pressure / p0).toDouble()
        return 44330 * (1 - ratio.pow(1 / 5.255))
    }

    private fun applyBackground(altitude: Double) {
        // higher altitude -> darker sky color
        val clamped = altitude.coerceIn(0.0, 4000.0)
        val t = clamped / 4000.0
        val baseRed = 0xBB
        val baseGreen = 0xDC
        val baseBlue = 0xFE
        val drop = (t * 140).toInt()
        val color = Color.rgb(
            (baseRed - drop).coerceAtLeast(0),
            (baseGreen - drop).coerceAtLeast(0),
            (baseBlue - drop * 2).coerceAtLeast(0)
        )
        rootLayout.setBackgroundColor(color)
    }
}
