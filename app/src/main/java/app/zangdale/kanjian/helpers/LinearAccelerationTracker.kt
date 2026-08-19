package app.zangdale.kanjian.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class LinearAccelerationTracker(
    context: Context,
) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val linearAcceleration: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    @Volatile
    var lastReading: Reading? = null
        private set

    val isAvailable: Boolean
        get() = linearAcceleration != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION || event.values.size < 3) {
                return
            }

            lastReading = Reading(
                xMetersPerSec2 = event.values[0],
                yMetersPerSec2 = event.values[1],
                zMetersPerSec2 = event.values[2],
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        val sensor = linearAcceleration ?: return
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_GAME,
        )
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        lastReading = null
    }

    data class Reading(
        val xMetersPerSec2: Float,
        val yMetersPerSec2: Float,
        val zMetersPerSec2: Float,
    )
}
