package app.zangdale.kanjian.ui.components.RecorderScreen.organisms

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.ui.effects.rememberIsChargingCableConnected
import app.zangdale.kanjian.ui.models.VideoRecorderModel
import app.zangdale.kanjian.ui.utils.PermissionHelper
import kotlinx.coroutines.launch

private fun canAutoStartVideoRecording(context: Context, settings: AppSettings): Boolean {
    if (settings.requiresExternalStoragePermission(context)) {
        return false
    }

    return PermissionHelper.hasGranted(context, Manifest.permission.CAMERA) &&
            PermissionHelper.hasGranted(context, Manifest.permission.RECORD_AUDIO)
}

@Composable
fun ChargingAutoRecordHandler(
    settings: AppSettings,
    videoRecorder: VideoRecorderModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isCharging by rememberIsChargingCableConnected()
    val settingsState = rememberUpdatedState(settings)
    val videoRecorderState = rememberUpdatedState(videoRecorder)
    var pendingAutoStart by remember { mutableStateOf(false) }
    var pendingAutoStop by remember { mutableStateOf(false) }

    LaunchedEffect(
        settings.autoRecordOnCharge,
        isCharging,
        videoRecorder.isInRecording,
        pendingAutoStart,
        pendingAutoStop,
    ) {
        if (!settings.autoRecordOnCharge) {
            pendingAutoStart = false
            pendingAutoStop = false
            return@LaunchedEffect
        }

        val currentSettings = settingsState.value
        val recorder = videoRecorderState.value

        if (recorder.isInRecording) {
            pendingAutoStart = false
        }

        if (isCharging) {
            pendingAutoStop = false
            if (!recorder.isInRecording &&
                !pendingAutoStart &&
                canAutoStartVideoRecording(context, currentSettings)
            ) {
                Log.i("Kanjian", "Charging cable connected — auto-starting video recording")
                pendingAutoStart = true
                recorder.startRecording(context, currentSettings)
            }
        } else if (recorder.isInRecording && !pendingAutoStop) {
            Log.i("Kanjian", "Charging cable disconnected — auto-stopping video recording")
            pendingAutoStop = true
            scope.launch {
                try {
                    runCatching {
                        recorder.stopRecording(context)
                    }
                    runCatching {
                        recorder.destroyService(context)
                    }
                    runCatching {
                        recorder.onRecordingSave(false)
                    }
                } finally {
                    pendingAutoStop = false
                }
            }
        }
    }
}
