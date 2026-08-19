package app.zangdale.kanjian.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class OrientationTracker(
    context: Context,
) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVector: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    @Volatile
    var lastReading: Reading? = null
        private set

    val isAvailable: Boolean
        get() = rotationVector != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val type = event.sensor.type
            if (
                type != Sensor.TYPE_ROTATION_VECTOR &&
                type != Sensor.TYPE_GAME_ROTATION_VECTOR
            ) {
                return
            }

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            lastReading = Reading(
                azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat(),
                pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
                rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat(),
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        val sensor = rotationVector ?: return
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
        /** Compass heading around Z, degrees [-180, 180]. */
        val azimuthDeg: Float,
        /** Pitch around X, degrees [-90, 90]. */
        val pitchDeg: Float,
        /** Roll around Y, degrees [-180, 180]. */
        val rollDeg: Float,
    )
}
