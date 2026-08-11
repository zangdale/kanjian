package app.zangdale.kanjian.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
        now: LocalDateTime = LocalDateTime.now(),
    ): String {
        val timePart = now.format(timeFormatter)
        if (!includeLocation || latitude == null || longitude == null) {
            return timePart
        }

        return String.format(
            Locale.US,
            "%s  GPS: %.6f, %.6f",
            timePart,
            latitude,
            longitude,
        )
    }

    fun drawWatermark(
        canvas: Canvas,
        text: String,
        typeface: Typeface,
        frameSize: Size,
    ) {
        if (text.isBlank()) {
            return
        }

        val scale = frameSize.width.coerceAtMost(frameSize.height) / 1080f
        val fontSize = (28f * scale).coerceAtLeast(18f)
        val padding = (16f * scale).coerceAtLeast(10f)
        val boxPadX = (8f * scale).coerceAtLeast(6f)
        val boxPadY = (6f * scale).coerceAtLeast(4f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = fontSize
            color = Color.WHITE
            setShadowLayer(2f * scale, 0f, 1f * scale, Color.BLACK)
        }

        val textWidth = paint.measureText(text)
        val fm = paint.fontMetrics
        val left = padding
        val baseline = frameSize.height - padding - fm.descent
        val right = left + textWidth

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(115, 0, 0, 0)
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(
            RectF(
                left - boxPadX,
                baseline + fm.ascent - boxPadY,
                right + boxPadX,
                baseline + fm.descent + boxPadY,
            ),
            6f * scale,
            6f * scale,
            boxPaint,
        )

        canvas.drawText(text, left, baseline, paint)
    }
}
