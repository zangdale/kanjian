package app.zangdale.kanjian.helpers

import android.content.Context
import app.zangdale.kanjian.db.VideoRecorderSettings

class WatermarkPreviewSensors(
    context: Context,
    private val settings: VideoRecorderSettings,
) {
    private val locationTracker =
        if (settings.includeLocationInWatermark) LocationTracker(context) else null
    private val gyroscopeTracker =
        if (settings.includeGyroscopeInWatermark) GyroscopeTracker(context) else null
    private val accelerometerTracker =
        if (settings.includeAccelerometerInWatermark) AccelerometerTracker(context) else null
    private val orientationTracker =
        if (settings.includeOrientationInWatermark) OrientationTracker(context) else null
    private val barometerTracker =
        if (settings.includeBarometerInWatermark) BarometerTracker(context) else null
    private val linearAccelerationTracker =
        if (settings.includeLinearAccelerationInWatermark) LinearAccelerationTracker(context) else null

    fun start() {
        locationTracker?.start()
        gyroscopeTracker?.start()
        accelerometerTracker?.start()
        orientationTracker?.start()
        barometerTracker?.start()
        linearAccelerationTracker?.start()
    }

    fun stop() {
        locationTracker?.stop()
        gyroscopeTracker?.stop()
        accelerometerTracker?.stop()
        orientationTracker?.stop()
        barometerTracker?.stop()
        linearAccelerationTracker?.stop()
    }

    fun formatLiveText(): String {
        return VideoWatermarkHelper.formatLiveText(
            includeLocation = settings.includeLocationInWatermark,
            latitude = locationTracker?.lastLocation?.latitude,
            longitude = locationTracker?.lastLocation?.longitude,
            includeGyroscope = settings.includeGyroscopeInWatermark,
            gyroscope = gyroscopeTracker?.lastReading,
            includeAccelerometer = settings.includeAccelerometerInWatermark,
            accelerometer = accelerometerTracker?.lastReading,
            includeOrientation = settings.includeOrientationInWatermark,
            orientation = orientationTracker?.lastReading,
            includeBarometer = settings.includeBarometerInWatermark,
            barometer = barometerTracker?.lastReading,
            includeLinearAcceleration = settings.includeLinearAccelerationInWatermark,
            linearAcceleration = linearAccelerationTracker?.lastReading,
            footerText = settings.watermarkFooterText,
        )
    }
}
