package app.zangdale.kanjian.services

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.zangdale.kanjian.NotificationHelper
import app.zangdale.kanjian.db.RecordingInformation
import app.zangdale.kanjian.enums.RecorderState
import app.zangdale.kanjian.helpers.AccelerometerTracker
import app.zangdale.kanjian.helpers.BarometerTracker
import app.zangdale.kanjian.helpers.BatchesFolder
import app.zangdale.kanjian.helpers.GyroscopeTracker
import app.zangdale.kanjian.helpers.LinearAccelerationTracker
import app.zangdale.kanjian.helpers.LocationTracker
import app.zangdale.kanjian.helpers.OrientationTracker
import app.zangdale.kanjian.helpers.VideoBatchesFolder
import app.zangdale.kanjian.helpers.VideoWatermarkHelper
import app.zangdale.kanjian.ui.SUPPORTS_SAVING_VIDEOS_IN_CUSTOM_FOLDERS
import app.zangdale.kanjian.ui.SUPPORTS_SCOPED_STORAGE
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.properties.Delegates

class VideoRecorderService :
    IntervalRecorderService<RecordingInformation, VideoBatchesFolder>() {
    override var batchesFolder = VideoBatchesFolder.viaInternalFolder(this)

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var locationTracker: LocationTracker? = null
    private var gyroscopeTracker: GyroscopeTracker? = null
    private var accelerometerTracker: AccelerometerTracker? = null
    private var orientationTracker: OrientationTracker? = null
    private var barometerTracker: BarometerTracker? = null
    private var linearAccelerationTracker: LinearAccelerationTracker? = null
    private var overlayEffect: OverlayEffect? = null
    private var overlayHandlerThread: HandlerThread? = null

    // Used to listen and check if the camera is available
    private var _cameraAvailableListener = CompletableDeferred<Unit>()
    private lateinit var _videoFinalizerListener: CompletableDeferred<Unit>;

    // Absolute last completer that can be awaited to ensure that the camera is closed
    private var _cameraCloserListener = CompletableDeferred<Unit>()

    private lateinit var selectedCamera: CameraSelector
    private var enableAudio by Delegates.notNull<Boolean>()

    var onCameraControlAvailable = {}

    var cameraControl: CameraControl? = null
        private set

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "init") {
            selectedCamera = CameraSelector.Builder().requireLensFacing(
                intent.getIntExtra("cameraID", CameraSelector.LENS_FACING_BACK)
            ).build()
            enableAudio = intent.getBooleanExtra("enableAudio", true)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun start() {
        super.start()

        if (settings.videoRecorderSettings.enableWatermark) {
            // Keep location warm even if the GPS toggle is off initially; tracker is cheap.
            if (settings.videoRecorderSettings.includeLocationInWatermark) {
                locationTracker = LocationTracker(this).also { it.start() }
            }
            if (settings.videoRecorderSettings.includeGyroscopeInWatermark) {
                gyroscopeTracker = GyroscopeTracker(this).also { it.start() }
            }
            if (settings.videoRecorderSettings.includeAccelerometerInWatermark) {
                accelerometerTracker = AccelerometerTracker(this).also { it.start() }
            }
            if (settings.videoRecorderSettings.includeOrientationInWatermark) {
                orientationTracker = OrientationTracker(this).also { it.start() }
            }
            if (settings.videoRecorderSettings.includeBarometerInWatermark) {
                barometerTracker = BarometerTracker(this).also { it.start() }
            }
            if (settings.videoRecorderSettings.includeLinearAccelerationInWatermark) {
                linearAccelerationTracker = LinearAccelerationTracker(this).also { it.start() }
            }
        }

        scope.launch {
            openCamera()
        }
    }

    override suspend fun stop() {
        locationTracker?.stop()
        locationTracker = null
        gyroscopeTracker?.stop()
        gyroscopeTracker = null
        accelerometerTracker?.stop()
        accelerometerTracker = null
        orientationTracker?.stop()
        orientationTracker = null
        barometerTracker?.stop()
        barometerTracker = null
        linearAccelerationTracker?.stop()
        linearAccelerationTracker = null

        super.stop()

        stopActiveRecording()

        // Camera can only be closed after the recording has been finalized
        withTimeoutOrNull(CAMERA_CLOSE_TIMEOUT) {
            _videoFinalizerListener.await()
        }

        closeCamera()

        withTimeoutOrNull(CAMERA_CLOSE_TIMEOUT) {
            _cameraCloserListener.await()
        }
    }

    override fun pause() {
        super.pause()

        stopActiveRecording()
    }

    override fun startForegroundService() {
        ServiceCompat.startForeground(
            this,
            NotificationHelper.RECORDER_CHANNEL_NOTIFICATION_ID,
            getNotificationHelper().buildStartingNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (enableAudio)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                else
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            } else {
                0
            },
        )
    }

    @SuppressLint("MissingPermission")
    override fun startNewCycle() {
        beginNextSegment()

        fun action() {
            stopActiveRecording()
            val newRecording = prepareVideoRecording()

            val finalizer = CompletableDeferred<Unit>()
            _videoFinalizerListener = finalizer

            activeRecording = newRecording.start(ContextCompat.getMainExecutor(this)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    scope.launch {
                        try {
                            if (batchesFolder.type == BatchesFolder.BatchType.MEDIA &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            ) {
                                runCatching {
                                    batchesFolder.markPermanentMediaReady(
                                        event.outputResults.outputUri
                                    )
                                }
                            }

                            if (!event.hasError()) {
                                enforceMaxSegments(
                                    excludeCurrent = this@VideoRecorderService.state == RecorderState.RECORDING,
                                )
                            }

                            // Segment finished while recording continues (interval / save-current).
                            if (this@VideoRecorderService.state == RecorderState.RECORDING &&
                                !event.hasError()
                            ) {
                                playFragmentSavedSound()
                            }
                        } finally {
                            if (this@VideoRecorderService.state == RecorderState.STOPPED ||
                                this@VideoRecorderService.state == RecorderState.PAUSED
                            ) {
                                finalizer.complete(Unit)
                            }
                        }
                    }
                }
            }
        }

        if (_cameraAvailableListener.isCompleted) {
            action()
        } else {
            // Race condition of `startNewCycle` being called before `invokeOnCompletion`
            // has been called can be ignored, as the camera usually opens within 5 seconds
            // and the interval can't be set shorter than 10 seconds.
            _cameraAvailableListener.invokeOnCompletion {
                action()
            }
        }
    }

    override fun getSegmentFileExtension(): String =
        settings.videoRecorderSettings.fileExtension

    // Runs a function in the main thread
    private fun runOnMain(callback: () -> Unit) {
        val mainHandler = ContextCompat.getMainExecutor(this)

        mainHandler.execute(callback)
    }

    private fun createWatermarkOverlayEffect(): OverlayEffect? {
        if (!settings.videoRecorderSettings.enableWatermark) {
            return null
        }

        releaseWatermarkOverlay()

        val thread = HandlerThread("WatermarkOverlay").also { it.start() }
        overlayHandlerThread = thread

        val typeface = VideoWatermarkHelper.resolveTypeface(this)
        val includeLocation = settings.videoRecorderSettings.includeLocationInWatermark
        val includeGyroscope = settings.videoRecorderSettings.includeGyroscopeInWatermark
        val includeAccelerometer = settings.videoRecorderSettings.includeAccelerometerInWatermark
        val includeOrientation = settings.videoRecorderSettings.includeOrientationInWatermark
        val includeBarometer = settings.videoRecorderSettings.includeBarometerInWatermark
        val includeLinearAcceleration =
            settings.videoRecorderSettings.includeLinearAccelerationInWatermark
        val watermarkRotationDegrees = settings.videoRecorderSettings.watermarkRotationDegrees
        val watermarkHorizontalMarginPercent =
            settings.videoRecorderSettings.watermarkHorizontalMarginPercent
        val watermarkVerticalMarginPercent =
            settings.videoRecorderSettings.watermarkVerticalMarginPercent
        val watermarkFooterText = settings.videoRecorderSettings.watermarkFooterText

        return OverlayEffect(
            CameraEffect.VIDEO_CAPTURE,
            /* queueDepth = */ 0,
            Handler(thread.looper),
        ) { error ->
            Log.e("VideoWatermark", "OverlayEffect error", error)
        }.also { effect ->
            effect.setOnDrawListener { frame ->
                val text = VideoWatermarkHelper.formatLiveText(
                    includeLocation = includeLocation,
                    latitude = locationTracker?.lastLocation?.latitude,
                    longitude = locationTracker?.lastLocation?.longitude,
                    includeGyroscope = includeGyroscope,
                    gyroscope = gyroscopeTracker?.lastReading,
                    includeAccelerometer = includeAccelerometer,
                    accelerometer = accelerometerTracker?.lastReading,
                    includeOrientation = includeOrientation,
                    orientation = orientationTracker?.lastReading,
                    includeBarometer = includeBarometer,
                    barometer = barometerTracker?.lastReading,
                    includeLinearAcceleration = includeLinearAcceleration,
                    linearAcceleration = linearAccelerationTracker?.lastReading,
                    footerText = watermarkFooterText,
                )
                VideoWatermarkHelper.drawWatermark(
                    canvas = frame.overlayCanvas,
                    text = text,
                    typeface = typeface,
                    frameSize = frame.size,
                    cropRect = frame.cropRect,
                    rotationDegrees = watermarkRotationDegrees,
                    horizontalMarginPercent = watermarkHorizontalMarginPercent,
                    verticalMarginPercent = watermarkVerticalMarginPercent,
                )
                true
            }
            overlayEffect = effect
        }
    }

    private fun releaseWatermarkOverlay() {
        runCatching {
            overlayEffect?.clearOnDrawListener()
            overlayEffect?.close()
        }
        overlayEffect = null

        overlayHandlerThread?.quitSafely()
        overlayHandlerThread = null
    }

    private fun buildRecorder() = Recorder.Builder()
        .setQualitySelector(
            settings.videoRecorderSettings.getQualitySelector()
                ?: QualitySelector.from(Quality.HIGHEST)
        )
        .apply {
            if (settings.videoRecorderSettings.targetedVideoBitRate != null) {
                setTargetVideoEncodingBitRate(settings.videoRecorderSettings.targetedVideoBitRate!!)
            }
        }
        .build()

    private fun buildVideoCapture(recorder: Recorder) = VideoCapture.Builder(recorder)
        .apply {
            val frameRate = settings.videoRecorderSettings.targetFrameRate
            if (frameRate != null) {
                setTargetFrameRate(Range(frameRate, frameRate))
            }
        }
        .build()

    // Open the camera.
    // Used to open it for a longer time, shouldn't be called when pausing / resuming.
    // This should only be called when starting a recording.
    private suspend fun openCamera() {
        cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(this@VideoRecorderService).get()
        }

        val recorder = buildRecorder()
        videoCapture = buildVideoCapture(recorder)
        val watermarkEffect = createWatermarkOverlayEffect()

        runOnMain {
            try {
                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(videoCapture!!)
                    .apply {
                        if (watermarkEffect != null) {
                            addEffect(watermarkEffect)
                        }
                    }
                    .build()

                camera = cameraProvider!!.bindToLifecycle(
                    this,
                    selectedCamera,
                    useCaseGroup,
                )

                cameraControl = CameraControl(camera!!).also {
                    it.init()
                }
                onCameraControlAvailable()

                _cameraAvailableListener.complete(Unit)
            } catch (error: IllegalArgumentException) {
                releaseWatermarkOverlay()
                onError()
            }
        }
    }

    // Close the camera
    // Used to close it finally, shouldn't be called when pausing / resuming.
    // This should only be called after recording has finished.
    private fun closeCamera() {
        runOnMain {
            runCatching {
                cameraProvider?.unbindAll()
            }
            releaseWatermarkOverlay()
            _cameraCloserListener.complete(Unit)

            // Doesn't need to run on main thread, but
            // if it runs outside `runOnMain`, `cameraProvider` is already null
            // before it's unbound
            cameraProvider = null
            videoCapture = null
            camera = null
        }
    }

    // `resume` override not needed as `startNewCycle` is called by `IntervalRecorderService`

    private fun stopActiveRecording() {
        runCatching {
            activeRecording?.stop()
        }
    }

    private fun getNameForMediaFile() =
        currentSegmentFileName
            ?: "${batchesFolder.mediaPrefix}$counter.${settings.videoRecorderSettings.fileExtension}"

    @SuppressLint("MissingPermission", "NewApi")
    private fun prepareVideoRecording() =
        videoCapture!!.output
            .let {
                val fileName = currentSegmentFileName
                    ?: batchesFolder.getUniqueSegmentName(
                        java.time.LocalDateTime.now(),
                        settings.videoRecorderSettings.fileExtension,
                    )

                if (batchesFolder.type == BatchesFolder.BatchType.CUSTOM && SUPPORTS_SAVING_VIDEOS_IN_CUSTOM_FOLDERS) {
                    it.prepareRecording(
                        this,
                        FileDescriptorOutputOptions.Builder(
                            batchesFolder.asCustomGetPermanentParcelFileDescriptor(
                                fileName,
                                settings.videoRecorderSettings.fileExtension
                            )
                        ).build()
                    )
                } else if (batchesFolder.type == BatchesFolder.BatchType.MEDIA) {
                    if (SUPPORTS_SCOPED_STORAGE) {
                        it.prepareRecording(
                            this,
                            MediaStoreOutputOptions
                                .Builder(
                                    contentResolver,
                                    batchesFolder.scopedMediaContentUri,
                                )
                                .setContentValues(
                                    batchesFolder.asMediaGetPermanentScopedStorageContentValues(
                                        fileName
                                    )
                                )
                                .build()
                        )
                    } else {
                        it.prepareRecording(
                            this,
                            FileOutputOptions
                                .Builder(batchesFolder.asMediaGetPermanentLegacyFile(fileName))
                                .build()
                        )
                    }
                } else {
                    it.prepareRecording(
                        this,
                        FileOutputOptions.Builder(
                            batchesFolder.asInternalGetFile(
                                counter,
                                settings.videoRecorderSettings.fileExtension
                            ).apply {
                                createNewFile()
                            }
                        ).build()
                    )
                }
            }
            .run {
                if (enableAudio) {
                    return@run withAudioEnabled()
                }

                this
            }

    override fun getRecordingInformation() =
        RecordingInformation(
            folderPath = batchesFolder.exportFolderForSettings(),
            recordingStart = recordingStart,
            maxDuration = settings.effectiveRetentionMs(),
            batchesAmount = batchesFolder.getBatchesForFFmpeg().size,
            fileExtension = settings.videoRecorderSettings.fileExtension,
            intervalDuration = settings.intervalDuration,
            type = RecordingInformation.Type.VIDEO,
            watermarkLatitude = locationTracker?.lastLocation?.latitude,
            watermarkLongitude = locationTracker?.lastLocation?.longitude,
        )

    companion object {
        const val CAMERA_CLOSE_TIMEOUT = 20000L
    }

    class CameraControl(
        val camera: Camera,
        // Save state for optimistic updates
        var torchEnabled: Boolean = false,
    ) {
        fun init() {
            torchEnabled = camera.cameraInfo.torchState.value == TorchState.ON
        }

        fun enableTorch() {
            torchEnabled = true
            camera.cameraControl.enableTorch(true)
        }

        fun disableTorch() {
            torchEnabled = false
            camera.cameraControl.enableTorch(false)
        }

        fun isHardwareTorchReallyEnabled(): Boolean {
            return camera.cameraInfo.torchState.value == TorchState.ON
        }

        fun hasTorchAvailable() = camera.cameraInfo.hasFlashUnit()
    }
}
