package app.zangdale.kanjian.ui.models

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.db.RecordingInformation
import app.zangdale.kanjian.enums.RecorderState
import app.zangdale.kanjian.helpers.Doctor
import app.zangdale.kanjian.helpers.VideoBatchesFolder
import app.zangdale.kanjian.services.VideoRecorderService
import app.zangdale.kanjian.ui.RECORDER_MEDIA_SELECTED_VALUE
import app.zangdale.kanjian.ui.utils.CameraInfo
import app.zangdale.kanjian.ui.utils.PermissionHelper

class VideoRecorderModel :
    BaseRecorderModel<RecordingInformation, VideoBatchesFolder, VideoRecorderService>() {
    override var batchesFolder: VideoBatchesFolder? = null
    override val intentClass = VideoRecorderService::class.java

    var enableAudio by mutableStateOf(true)
    var cameraID by mutableIntStateOf(CameraInfo.Lens.BACK.androidValue)

    override val isInRecording: Boolean
        get() = super.isInRecording

    var isStartingRecording by mutableStateOf(true)
        private set

    val cameraSelector: CameraSelector
        get() = CameraSelector.Builder().requireLensFacing(cameraID).build()

    fun init(context: Context) {
        enableAudio = PermissionHelper.hasGranted(context, Manifest.permission.RECORD_AUDIO)
        cameraID = CameraInfo.Lens.BACK.androidValue
    }

    override fun startRecording(context: Context, settings: AppSettings) {
        batchesFolder = when (settings.saveFolder) {
            null, RECORDER_MEDIA_SELECTED_VALUE -> VideoBatchesFolder.viaMediaFolder(context)
            else -> VideoBatchesFolder.viaCustomFolder(
                context,
                DocumentFile.fromTreeUri(
                    context,
                    Uri.parse(settings.saveFolder)
                )!!
            )
        }

        super.startRecording(context, settings)
    }

    override fun onServiceConnected(service: VideoRecorderService) {
        // `onServiceConnected` may be called when reconnecting to the service,
        // so we only want to actually start the recording if the service is idle and thus
        // not already recording
        if (service.state == RecorderState.IDLE) {
            isStartingRecording = true

            service.clearAllRecordings()
            service.startRecording()
            onRecordingStart()
        } else {
            isStartingRecording = false
        }

        service.onCameraControlAvailable = {
            isStartingRecording = false
        }

        recorderState = service.state
        recordingTime = service.recordingTime
    }

    override fun handleIntent(intent: Intent) =
        intent.apply {
            putExtra("cameraID", cameraID)
            putExtra("enableAudio", enableAudio)
        }
}