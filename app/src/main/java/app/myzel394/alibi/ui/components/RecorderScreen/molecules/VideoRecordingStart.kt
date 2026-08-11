package app.myzel394.alibi.ui.components.RecorderScreen.molecules

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.myzel394.alibi.R
import app.myzel394.alibi.db.AppSettings
import app.myzel394.alibi.ui.components.RecorderScreen.atoms.BigButton
import app.myzel394.alibi.ui.components.atoms.PermissionRequester
import app.myzel394.alibi.ui.models.VideoRecorderModel
import app.myzel394.alibi.ui.utils.PermissionHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoRecordingStart(
    videoRecorder: VideoRecorderModel,
    appSettings: AppSettings,
    onHideAudioRecording: () -> Unit,
    onShowAudioRecording: () -> Unit,
    showPreview: Boolean,
    useLargeButtons: Boolean? = null,
) {
    val context = LocalContext.current

    var showSheet by rememberSaveable {
        mutableStateOf(false)
    }
    var pendingStart by rememberSaveable {
        mutableStateOf(false)
    }

    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            if (pendingStart) {
                pendingStart = false
                videoRecorder.startRecording(context, appSettings)
            }
        },
    )

    fun startVideoRecording() {
        val needsLocationPermission =
            appSettings.videoRecorderSettings.enableWatermark &&
                    appSettings.videoRecorderSettings.includeLocationInWatermark &&
                    !PermissionHelper.hasGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
                    !PermissionHelper.hasGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (needsLocationPermission) {
            pendingStart = true
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            videoRecorder.startRecording(context, appSettings)
        }
    }

    if (showSheet) {
        VideoRecorderPreparationSheet(
            showPreview = showPreview,
            videoSettings = videoRecorder,
            onDismiss = {
                showSheet = false
            },
            onPreviewVisible = onHideAudioRecording,
            onPreviewHidden = onShowAudioRecording,
            onStartRecording = ::startVideoRecording,
        )
    }

    PermissionRequester(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
        onPermissionAvailable = {
            showSheet = true
        }
    ) { triggerExternalStorage ->
        BigButton(
            label = stringResource(R.string.ui_videoRecorder_action_start_label),
            description = stringResource(R.string.ui_videoRecorder_action_configure_label),
            icon = Icons.Default.CameraAlt,
            onLongClick = {
                if (appSettings.requiresExternalStoragePermission(context)) {
                    triggerExternalStorage()
                    return@BigButton
                }

                showSheet = true
            },
            onClick = {
                if (appSettings.requiresExternalStoragePermission(context)) {
                    triggerExternalStorage()
                    return@BigButton
                }

                if (PermissionHelper.hasGranted(
                        context,
                        Manifest.permission.CAMERA
                    ) && PermissionHelper.hasGranted(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    )
                ) {
                    startVideoRecording()
                } else {
                    showSheet = true
                }
            },
            isBig = useLargeButtons,
        )
    }
}
