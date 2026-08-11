package app.zangdale.kanjian.services

import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.helpers.BatchesFolder
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

abstract class IntervalRecorderService<I, B : BatchesFolder> :
    RecorderService() {
    protected var counter = 0L
        private set

    protected var currentSegmentStart: LocalDateTime = LocalDateTime.now()
        private set

    protected var currentSegmentFileName: String? = null
        private set

    lateinit var settings: AppSettings

    private lateinit var cycleTimer: ScheduledExecutorService

    abstract var batchesFolder: B

    var onBatchesFolderNotAccessible: () -> Unit = {}

    abstract fun getRecordingInformation(): I

    abstract fun getSegmentFileExtension(): String

    override fun shouldPlayRecorderSounds(): Boolean =
        ::settings.isInitialized && settings.enableRecorderSounds

    // Kept for compatibility with legacy merge/export flows.
    fun lockFiles() {}

    fun unlockFiles(cleanupFiles: Boolean = false) {}

    protected fun beginNextSegment() {
        counter += 1
        currentSegmentStart = LocalDateTime.now()
        currentSegmentFileName = batchesFolder.getUniqueSegmentName(
            currentSegmentStart,
            getSegmentFileExtension(),
        )
    }

    protected fun enforceMaxSegments(excludeCurrent: Boolean = true) {
        runCatching {
            batchesFolder.trimPermanentSegments(
                maxSegments = settings.maxSegments,
                excludeFileName = if (excludeCurrent) currentSegmentFileName else null,
            )
        }
    }

    // Make overrideable
    open fun startNewCycle() {
        beginNextSegment()
    }

    private fun createTimer() {
        cycleTimer = Executors.newSingleThreadScheduledExecutor().also {
            it.scheduleAtFixedRate(
                ::startNewCycle,
                0,
                settings.intervalDuration,
                TimeUnit.MILLISECONDS
            )
        }
    }

    override fun start() {
        super.start()

        batchesFolder.initFolders()

        if (!batchesFolder.checkIfFolderIsAccessible()) {
            onBatchesFolderNotAccessible()

            throw AvoidErrorDialogError()
        }

        createTimer()
    }

    override fun pause() {
        super.pause()
        cycleTimer.shutdown()
    }

    override fun resume() {
        super.resume()
        createTimer()
    }

    override suspend fun stop() {
        cycleTimer.shutdown()
        batchesFolder.cleanup()
        super.stop()
    }

    fun clearAllRecordings() {
        // Permanent auto-saved segments must not be deleted when starting a new recording.
        // Only clean leftover temporary batch folders from older app versions.
        runCatching {
            batchesFolder.deleteRecordings()
        }
    }
}
