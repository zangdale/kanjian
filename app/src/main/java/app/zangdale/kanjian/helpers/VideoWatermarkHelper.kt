package app.zangdale.kanjian.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import android.util.Size
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object VideoWatermarkHelper {
    private const val ASSET_FONT_PATH = "fonts/HarmonyOS_Sans_Regular.ttf"
    private const val CACHED_FONT_NAME = "HarmonyOS_Sans_Regular.ttf"

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private var cachedTypeface: Typeface? = null

    fun resolveFontPath(context: Context): String? {
        val cached = File(File(context.filesDir, "fonts"), CACHED_FONT_NAME)
        if (!cached.exists() || cached.length() == 0L) {
            runCatching {
                cached.parentFile?.mkdirs()
                context.assets.open(ASSET_FONT_PATH).use { input ->
                    cached.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }.onFailure {
                Log.e("VideoWatermark", "Failed to copy bundled HarmonyOS font", it)
            }
        }

        return cached.takeIf { it.exists() && it.length() > 0L }?.absolutePath
    }

    fun resolveTypeface(context: Context): Typeface {
        cachedTypeface?.let { return it }

        val path = resolveFontPath(context)
        val typeface = if (path != null) {
            runCatching { Typeface.createFromFile(path) }.getOrNull()
        } else {
            null
        } ?: Typeface.DEFAULT

        cachedTypeface = typeface
        return typeface
    }

    fun formatLiveText(
        includeLocation: Boolean,
        latitude: Double?,
        longitude: Double?,
        includeGyroscope: Boolean = false,
        gyroscope: GyroscopeTracker.Reading? = null,
        includeAccelerometer: Boolean = false,
        accelerometer: AccelerometerTracker.Reading? = null,
        includeOrientation: Boolean = false,
        orientation: OrientationTracker.Reading? = null,
        includeBarometer: Boolean = false,
        barometer: BarometerTracker.Reading? = null,
        includeLinearAcceleration: Boolean = false,
        linearAcceleration: LinearAccelerationTracker.Reading? = null,
        footerText: String = "",
        now: LocalDateTime = LocalDateTime.now(),
    ): String {
        val lines = mutableListOf(now.format(timeFormatter))

        if (includeLocation && latitude != null && longitude != null) {
            lines += String.format(
                Locale.US,
                "GPS: %.6f, %.6f",
                latitude,
                longitude,
            )
        }

        if (includeGyroscope && gyroscope != null) {
            lines += String.format(
                Locale.US,
                "GYRO: X:%.2f Y:%.2f Z:%.2f °/s",
                gyroscope.xDegPerSec,
                gyroscope.yDegPerSec,
                gyroscope.zDegPerSec,
            )
        }

        if (includeAccelerometer && accelerometer != null) {
            lines += String.format(
                Locale.US,
                "ACC: X:%.2f Y:%.2f Z:%.2f m/s²",
                accelerometer.xMetersPerSec2,
                accelerometer.yMetersPerSec2,
                accelerometer.zMetersPerSec2,
            )
        }

        if (includeOrientation && orientation != null) {
            lines += String.format(
                Locale.US,
                "ORI: P:%.1f R:%.1f Y:%.1f °",
                orientation.pitchDeg,
                orientation.rollDeg,
                orientation.azimuthDeg,
            )
        }

        if (includeBarometer && barometer != null) {
            lines += String.format(
                Locale.US,
                "BARO: %.1f hPa  ALT: %.1f m",
                barometer.pressureHpa,
                barometer.altitudeMeters,
            )
        }

        if (includeLinearAcceleration && linearAcceleration != null) {
            lines += String.format(
                Locale.US,
                "LIN: X:%.2f Y:%.2f Z:%.2f m/s²",
                linearAcceleration.xMetersPerSec2,
                linearAcceleration.yMetersPerSec2,
                linearAcceleration.zMetersPerSec2,
            )
        }

        val footer = footerText.trim()
        if (footer.isNotEmpty()) {
            lines += footer
        }

        return lines.joinToString("\n")
    }

    fun drawWatermark(
        canvas: Canvas,
        text: String,
        typeface: Typeface,
        frameSize: Size,
        cropRect: Rect = Rect(0, 0, frameSize.width, frameSize.height),
        rotationDegrees: Int = 0,
        horizontalMarginPercent: Int = 2,
        verticalMarginPercent: Int = 80,
    ) {
        if (text.isBlank()) {
            return
        }

        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return
        }

        // Prevent ghosting from previous frames on some devices.
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Visible video area may be smaller than the full buffer.
        val visible = if (cropRect.isEmpty) {
            Rect(0, 0, frameSize.width, frameSize.height)
        } else {
            Rect(cropRect)
        }

        val visibleWidth = visible.width().coerceAtLeast(1)
        val visibleHeight = visible.height().coerceAtLeast(1)
        val scale = visibleWidth.coerceAtMost(visibleHeight) / 1080f
        val fontSize = (34f * scale).coerceAtLeast(22f)
        val lineGap = (6f * scale).coerceAtLeast(3f)
        val strokeWidth = (3.5f * scale).coerceAtLeast(2.5f)

        val clampedHorizontalMarginPercent = horizontalMarginPercent.coerceIn(0, 90)
        val clampedVerticalMarginPercent = verticalMarginPercent.coerceIn(0, 90)
        val maxHorizontalPadding = (visibleWidth - 1f).coerceAtLeast(0f)
        val maxVerticalPadding = (visibleHeight - 1f).coerceAtLeast(0f)
        val horizontalPadding = (visibleWidth * clampedHorizontalMarginPercent / 100f)
            .coerceIn(0f, maxHorizontalPadding)
        val verticalPadding = (visibleHeight * clampedVerticalMarginPercent / 100f)
            .coerceIn(0f, maxVerticalPadding)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = fontSize
            color = Color.WHITE
            style = Paint.Style.FILL
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = fontSize
            color = Color.BLACK
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeJoin = Paint.Join.ROUND
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }

        val fm = fillPaint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) + lineGap
        val pivotX = visible.left + horizontalPadding
        val pivotY = visible.top + verticalPadding

        // Manual rotation only — do not auto-follow device/camera orientation.
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360

        canvas.save()
        canvas.clipRect(visible)
        canvas.translate(pivotX, pivotY)
        if (normalizedRotation != 0) {
            canvas.rotate(normalizedRotation.toFloat())
        }

        var baseline = -fm.ascent
        for (line in lines) {
            canvas.drawText(line, 0f, baseline, strokePaint)
            canvas.drawText(line, 0f, baseline, fillPaint)
            baseline += lineHeight
        }
        canvas.restore()
    }
}
