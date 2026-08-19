package app.zangdale.kanjian.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class GyroscopeTracker(
    context: Context,
) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gyroscope: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    @Volatile
    var lastReading: Reading? = null
        private set

    val isAvailable: Boolean
        get() = gyroscope != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_GYROSCOPE || event.values.size < 3) {
                return
            }

            lastReading = Reading(
                xRadPerSec = event.values[0],
                yRadPerSec = event.values[1],
                zRadPerSec = event.values[2],
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        val sensor = gyroscope ?: return
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
        val xRadPerSec: Float,
        val yRadPerSec: Float,
        val zRadPerSec: Float,
    ) {
        val xDegPerSec: Float
            get() = Math.toDegrees(xRadPerSec.toDouble()).toFloat()

        val yDegPerSec: Float
            get() = Math.toDegrees(yRadPerSec.toDouble()).toFloat()

        val zDegPerSec: Float
            get() = Math.toDegrees(zRadPerSec.toDouble()).toFloat()
    }
}
