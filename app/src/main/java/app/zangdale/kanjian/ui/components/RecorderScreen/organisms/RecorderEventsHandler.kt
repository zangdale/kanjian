package app.zangdale.kanjian.ui.components.RecorderScreen.organisms

import android.net.Uri
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.zangdale.kanjian.R
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.db.RecordingInformation
import app.zangdale.kanjian.helpers.AudioBatchesFolder
import app.zangdale.kanjian.helpers.BatchesFolder
import app.zangdale.kanjian.helpers.VideoBatchesFolder
import app.zangdale.kanjian.services.IntervalRecorderService
import app.zangdale.kanjian.ui.RECORDER_MEDIA_SELECTED_VALUE
import app.zangdale.kanjian.ui.components.RecorderScreen.atoms.BatchesInaccessibleDialog
import app.zangdale.kanjian.ui.components.RecorderScreen.atoms.RecorderErrorDialog
import app.zangdale.kanjian.ui.components.RecorderScreen.atoms.RecorderProcessingDialog
import app.zangdale.kanjian.ui.effects.rememberOpenUri
import app.zangdale.kanjian.ui.models.AudioRecorderModel
import app.zangdale.kanjian.ui.models.BaseRecorderModel
import app.zangdale.kanjian.ui.models.VideoRecorderModel
import app.zangdale.kanjian.ui.utils.rememberFileSaverDialog
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

typealias RecorderModel = BaseRecorderModel<
        RecordingInformation,
        BatchesFolder,
        IntervalRecorderService<RecordingInformation, BatchesFolder>,
        >

@Composable
fun RecorderEventsHandler(
    settings: AppSettings,
    snackbarHostState: SnackbarHostState,
    audioRecorder: AudioRecorderModel,
    videoRecorder: VideoRecorderModel,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataStore = context.dataStore

    var isProcessing by remember { mutableStateOf(false) }
    var showRecorderError by remember { mutableStateOf(false) }
    var showBatchesInaccessibleError by remember { mutableStateOf(false) }

    var processingProgress by remember { mutableStateOf<Float?>(null) }

    val saveAudioFile = rememberFileSaverDialog(settings.audioRecorderSettings.getMimeType()) {
        if (audioRecorder.batchesFolder?.hasRecordingsAvailable() == false) {
            scope.launch {
                dataStore.updateData {
                    it.setLastRecording(null)
                }
            }
        }
    }
    val saveVideoFile = rememberFileSaverDialog(settings.videoRecorderSettings.getMimeType()) {
        if (videoRecorder.batchesFolder?.hasRecordingsAvailable() == false) {
            scope.launch {
                dataStore.updateData {
                    it.setLastRecording(null)
                }
            }
        }
    }

    suspend fun saveAsLastRecording(
        recorder: RecorderModel
    ) {
        val information = recorder.recorderService?.getRecordingInformation()

        if (information == null) {
            Log.e("RecorderEventsHandler", "Recording information is null")
            return
        }

        dataStore.updateData {
            it.setLastRecording(
                information
            )
        }
    }

    val successMessage = stringResource(R.string.ui_recorder_action_save_success)
    val openMessage = stringResource(R.string.ui_recorder_action_save_openFolder)

    val openFolder = rememberOpenUri()

    fun showSnackbar() {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = successMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    fun showSnackbar(uri: Uri) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = successMessage,
                actionLabel = openMessage,
                duration = SnackbarDuration.Short,
            )

            if (result == SnackbarResult.ActionPerformed) {
                openFolder(uri)
            }
        }
    }

    fun notifySegmentSaved(): CompletableDeferred<Unit> {
        val completer = CompletableDeferred<Unit>()

        when (settings.saveFolder) {
            null, RECORDER_MEDIA_SELECTED_VALUE -> showSnackbar()
            else -> {
                runCatching {
                    showSnackbar(
                        DocumentFile.fromTreeUri(
                            context,
                            android.net.Uri.parse(settings.saveFolder)
                        )!!.uri
                    )
                }.onFailure {
                    showSnackbar()
                }
            }
        }

        completer.complete(Unit)
        return completer
    }

    fun saveRecording(
        recorder: RecorderModel,
        cleanupOldFiles: Boolean = false
    ): CompletableDeferred<Unit> {
        // Legacy path for leftover counter-based batches from older app versions.
        if (settings.lastRecording?.hasRecordingsAvailable(context) != true &&
            recorder.recorderService == null
        ) {
            return notifySegmentSaved()
        }

        val completer = CompletableDeferred<Unit>()

        // If processing takes this short, don't show the processing dialog
        val timer = Timer().schedule(250L) {
            isProcessing = true
        }

        thread {
            runBlocking {
                try {
                    if (recorder.isCurrentlyActivelyRecording) {
                        recorder.recorderService?.lockFiles()
                    }

                    val recording =
                        // When new recording created
                        recorder.recorderService?.getRecordingInformation()
                        // When recording is loaded from lastRecording
                            ?: settings.lastRecording
                            ?: throw Exception("No recording information available")

                    val batchesFolder = when (recorder.javaClass) {
                        AudioRecorderModel::class.java -> AudioBatchesFolder.importFromFolder(
                            recording.folderPath,
                            context
                        )

                        VideoRecorderModel::class.java -> VideoBatchesFolder.importFromFolder(
                            recording.folderPath,
                            context
                        )

                        else -> throw Exception("Unknown recorder type")
                    }

                    // No leftover counter batches: segments were already auto-saved.
                    if (!batchesFolder.hasRecordingsAvailable()) {
                        showSnackbar()
                        return@runBlocking
                    }

                    val fileName = batchesFolder.getName(
                        recording.recordingStart,
                        recording.fileExtension,
                    )

                    batchesFolder.concatenate(
                        recording,
                        filenameFormat = settings.filenameFormat,
                        fileName = fileName,
                        onProgress = { percentage ->
                            processingProgress = percentage
                        }
                    )

                    // Save file
                    when (batchesFolder.type) {
                        BatchesFolder.BatchType.INTERNAL -> {
                            when (batchesFolder) {
                                is AudioBatchesFolder -> {
                                    saveAudioFile(
                                        batchesFolder.asInternalGetOutputFile(fileName), fileName
                                    )
                                }

                                is VideoBatchesFolder -> {
                                    saveVideoFile(
                                        batchesFolder.asInternalGetOutputFile(fileName), fileName
                                    )
                                }
                            }
                        }

                        BatchesFolder.BatchType.CUSTOM -> {
                            showSnackbar(batchesFolder.customFolder!!.uri)
                        }

                        BatchesFolder.BatchType.MEDIA -> {
                            showSnackbar()
                        }
                    }
                } catch (error: Exception) {
                    Log.getStackTraceString(error)
                    showSnackbar()
                } finally {
                    if (recorder.isCurrentlyActivelyRecording) {
                        recorder.recorderService?.unlockFiles(cleanupOldFiles)
                    }
                    timer.cancel()
                    isProcessing = false
                    processingProgress = null
                    completer.complete(Unit)
                }
            }
        }

        return completer
    }

    // Register audio recorder events
    // Absolutely no idea, but somehow on some devices the `DisposableEffect`
    // is registered twice, and THEN disposed once (AFTER being called twice),
    // which then causes the `onRecordingSave` to be in a weird state.
    // This variable is a workaround to prevent this from happening.
    var previousAudioSettings: AppSettings? = null
    DisposableEffect(settings) {
        if (previousAudioSettings == settings) {
            onDispose { }
        } else {
            previousAudioSettings = settings
            audioRecorder.onRecordingSave = { cleanupOldFiles ->
                if (audioRecorder.recorderService == null &&
                    settings.lastRecording?.hasRecordingsAvailable(context) == true
                ) {
                    saveRecording(audioRecorder as RecorderModel, cleanupOldFiles)
                } else {
                    notifySegmentSaved()
                }
            }
            audioRecorder.onRecordingStart = {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            audioRecorder.onError = {
                scope.launch {
                    saveAsLastRecording(audioRecorder as RecorderModel)

                    runCatching {
                        audioRecorder.stopRecording(context)
                    }
                    runCatching {
                        audioRecorder.destroyService(context)
                    }

                    showRecorderError = true
                }
            }
            audioRecorder.onBatchesFolderNotAccessible = {
                scope.launch {
                    showBatchesInaccessibleError = true

                    runCatching {
                        audioRecorder.stopRecording(context)
                    }
                    runCatching {
                        audioRecorder.destroyService(context)
                    }
                }
            }

            onDispose {
                audioRecorder.onRecordingSave = {
                    throw NotImplementedError("onRecordingSave should not be called now")
                }
                audioRecorder.onError = {}
            }
        }
    }

    // Register video recorder events
    var previousVideoSettings: AppSettings? = null
    DisposableEffect(settings) {
        if (previousVideoSettings == settings) {
            onDispose { }
        } else {
            previousVideoSettings = settings
            Log.i("Kanjian", "===== Registering videoRecorder events $videoRecorder")
            videoRecorder.onRecordingSave = { cleanupOldFiles ->
                if (videoRecorder.recorderService == null &&
                    settings.lastRecording?.hasRecordingsAvailable(context) == true
                ) {
                    saveRecording(videoRecorder as RecorderModel, cleanupOldFiles)
                } else {
                    notifySegmentSaved()
                }
            }
            videoRecorder.onRecordingStart = {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            videoRecorder.onError = {
                scope.launch {
                    saveAsLastRecording(videoRecorder as RecorderModel)

                    runCatching {
                        videoRecorder.stopRecording(context)
                    }
                    runCatching {
                        videoRecorder.destroyService(context)
                    }

                    showRecorderError = true
                }
            }
            videoRecorder.onBatchesFolderNotAccessible = {
                scope.launch {
                    showBatchesInaccessibleError = true

                    runCatching {
                        videoRecorder.stopRecording(context)
                    }
                    runCatching {
                        videoRecorder.destroyService(context)
                    }
                }
            }

            onDispose {
                Log.i("Kanjian", "===== Disposing videoRecorder events")
                videoRecorder.onRecordingSave = {
                    throw NotImplementedError("onRecordingSave should not be called now")
                }
                videoRecorder.onError = {}
            }
        }
    }

    if (isProcessing)
        RecorderProcessingDialog(
            progress = processingProgress,
        )

    if (showBatchesInaccessibleError)
        BatchesInaccessibleDialog(
            onClose = {
                showBatchesInaccessibleError = false
            },
        )
    else if (showRecorderError)
        RecorderErrorDialog(
            onClose = {
                showRecorderError = false
            },
        )
}