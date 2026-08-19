package app.zangdale.kanjian.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class BarometerTracker(
    context: Context,
) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val pressureSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    @Volatile
    var lastReading: Reading? = null
        private set

    val isAvailable: Boolean
        get() = pressureSensor != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_PRESSURE || event.values.isEmpty()) {
                return
            }

            val pressureHpa = event.values[0]
            lastReading = Reading(
                pressureHpa = pressureHpa,
                altitudeMeters = SensorManager.getAltitude(
                    SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                    pressureHpa,
                ),
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        val sensor = pressureSensor ?: return
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_UI,
        )
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        lastReading = null
    }

    data class Reading(
        val pressureHpa: Float,
        /** Estimated altitude relative to standard atmosphere (1013.25 hPa). */
        val altitudeMeters: Float,
    )
}
