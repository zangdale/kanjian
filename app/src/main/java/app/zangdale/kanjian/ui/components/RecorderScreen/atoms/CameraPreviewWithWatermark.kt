package app.zangdale.kanjian.ui.components.RecorderScreen.atoms

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import app.zangdale.kanjian.db.VideoRecorderSettings
import app.zangdale.kanjian.helpers.VideoWatermarkHelper
import app.zangdale.kanjian.helpers.WatermarkPreviewSensors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun CameraPreviewWithWatermark(
    modifier: Modifier,
    cameraSelector: CameraSelector,
    settings: VideoRecorderSettings,
) {
    Box(modifier = modifier) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraSelector = cameraSelector,
        )
        if (settings.enableWatermark) {
            WatermarkPreviewOverlay(
                settings = settings,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun WatermarkPreviewOverlay(
    settings: VideoRecorderSettings,
    modifier: Modifier = Modifier,
) {
    val text = rememberWatermarkPreviewText(settings)
    val context = LocalContext.current
    val typeface = remember { VideoWatermarkHelper.resolveTypeface(context) }
    val fontFamily = remember(typeface) { FontFamily(typeface) }

    BoxWithConstraints(modifier = modifier) {
        val scale = minOf(maxWidth.value, maxHeight.value) / 1080f
        val fontSize = (34f * scale).coerceAtLeast(12f).sp
        val xPx = constraints.maxWidth * settings.watermarkHorizontalMarginPercent / 100f
        val yPx = constraints.maxHeight * settings.watermarkVerticalMarginPercent / 100f
        val rotation = ((settings.watermarkRotationDegrees % 360) + 360) % 360

        Text(
            text = text,
            color = ComposeColor.White,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            lineHeight = (40f * scale).coerceAtLeast(14f).sp,
            style = TextStyle(
                shadow = Shadow(
                    color = ComposeColor.Black,
                    blurRadius = (3.5f * scale).coerceAtLeast(2f),
                ),
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(xPx.toInt(), yPx.toInt()) }
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        )
    }
}

@Composable
private fun rememberWatermarkPreviewText(settings: VideoRecorderSettings): String {
    val context = LocalContext.current
    val sensors = remember(
        settings.enableWatermark,
        settings.includeLocationInWatermark,
        settings.includeGyroscopeInWatermark,
        settings.includeAccelerometerInWatermark,
        settings.includeOrientationInWatermark,
        settings.includeBarometerInWatermark,
        settings.includeLinearAccelerationInWatermark,
        settings.watermarkFooterText,
    ) {
        WatermarkPreviewSensors(context, settings)
    }

    DisposableEffect(sensors) {
        sensors.start()
        onDispose { sensors.stop() }
    }

    var text by remember { mutableStateOf(sensors.formatLiveText()) }

    LaunchedEffect(sensors) {
        while (isActive) {
            text = sensors.formatLiveText()
            delay(200)
        }
    }

    return text
}
