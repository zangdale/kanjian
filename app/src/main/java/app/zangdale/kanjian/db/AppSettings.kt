package app.zangdale.kanjian.db

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import app.zangdale.kanjian.R
import app.zangdale.kanjian.helpers.VideoBatchesFolder
import app.zangdale.kanjian.ui.RECORDER_MEDIA_SELECTED_VALUE
import app.zangdale.kanjian.ui.SUPPORTS_SCOPED_STORAGE
import app.zangdale.kanjian.ui.components.RecorderScreen.organisms.RecorderModel
import app.zangdale.kanjian.ui.utils.PermissionHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

@Serializable
data class AppSettings(
    val videoRecorderSettings: VideoRecorderSettings = VideoRecorderSettings.getDefaultInstance(),

    val appLockSettings: AppLockSettings? = null,

    val hasSeenOnboarding: Boolean = false,
    val showAdvancedSettings: Boolean = false,
    val theme: Theme = Theme.SYSTEM,
    val lastRecording: RecordingInformation? = null,

    val filenameFormat: FilenameFormat = FilenameFormat.DATETIME_RELATIVE_START,

    /// Recording information
    // Kept for backwards-compatible settings JSON; prefer maxSegments.
    @Deprecated("Use maxSegments")
    val maxDuration: Long = 15 * 60 * 1000L,
    // How many permanent segments to keep before overwriting the oldest
    val maxSegments: Int = 15,
    // 60 seconds
    val intervalDuration: Long = 60 * 1000L,

    val notificationSettings: NotificationSettings? = null,
    @Deprecated("Unused after permanent segment auto-save")
    val deleteRecordingsImmediately: Boolean = false,
    val enableRecorderSounds: Boolean = true,
    val saveFolder: String? = RECORDER_MEDIA_SELECTED_VALUE,
    // When enabled, plugging in a charging cable starts video recording and
    // unplugging stops (and saves) the recording automatically.
    val autoRecordOnCharge: Boolean = false,
    // Optional custom URIs for recorder cue sounds. Null keeps the built-in defaults.
    val customStartSoundUri: String? = null,
    val customPauseSoundUri: String? = null,
    val customFragmentSoundUri: String? = null,
) {
    fun setShowAdvancedSettings(showAdvancedSettings: Boolean): AppSettings {
        return copy(showAdvancedSettings = showAdvancedSettings)
    }

    fun setVideoRecorderSettings(videoRecorderSettings: VideoRecorderSettings): AppSettings {
        return copy(videoRecorderSettings = videoRecorderSettings)
    }

    fun setNotificationSettings(notificationSettings: NotificationSettings?): AppSettings {
        return copy(notificationSettings = notificationSettings)
    }

    fun setHasSeenOnboarding(hasSeenOnboarding: Boolean): AppSettings {
        return copy(hasSeenOnboarding = hasSeenOnboarding)
    }

    fun setTheme(theme: Theme): AppSettings {
        return copy(theme = theme)
    }

    fun setLastRecording(lastRecording: RecordingInformation?): AppSettings {
        return copy(lastRecording = lastRecording)
    }

    fun setFilenameFormat(filenameFormat: FilenameFormat): AppSettings {
        return copy(filenameFormat = filenameFormat)
    }

    fun setMaxDuration(duration: Long): AppSettings {
        if (duration < 60 * 1000L || duration > 10 * 24 * 60 * 60 * 1000L) {
            throw Exception("Max duration must be between 1 minute and 10 days")
        }

        val segments = (duration / intervalDuration).toInt().coerceAtLeast(1)
        return copy(maxDuration = duration, maxSegments = segments)
    }

    fun setMaxSegments(segments: Int): AppSettings {
        if (segments < 1 || segments > 10_000) {
            throw Exception("Max segments must be between 1 and 10000")
        }

        return copy(
            maxSegments = segments,
            maxDuration = segments.toLong() * intervalDuration,
        )
    }

    fun setIntervalDuration(duration: Long): AppSettings {
        if (duration < 10 * 1000L || duration > 60 * 60 * 1000L) {
            throw Exception("Interval duration must be between 10 seconds and 1 hour")
        }

        return copy(
            intervalDuration = duration,
            maxDuration = maxSegments.toLong() * duration,
        )
    }

    fun effectiveRetentionMs(): Long = maxSegments.toLong() * intervalDuration

    fun setDeleteRecordingsImmediately(deleteRecordingsImmediately: Boolean): AppSettings {
        return copy(deleteRecordingsImmediately = deleteRecordingsImmediately)
    }

    fun setEnableRecorderSounds(enableRecorderSounds: Boolean): AppSettings {
        return copy(enableRecorderSounds = enableRecorderSounds)
    }

    fun setCustomStartSoundUri(customStartSoundUri: String?): AppSettings {
        return copy(customStartSoundUri = customStartSoundUri)
    }

    fun setCustomPauseSoundUri(customPauseSoundUri: String?): AppSettings {
        return copy(customPauseSoundUri = customPauseSoundUri)
    }

    fun setCustomFragmentSoundUri(customFragmentSoundUri: String?): AppSettings {
        return copy(customFragmentSoundUri = customFragmentSoundUri)
    }

    fun setSaveFolder(saveFolder: String?): AppSettings {
        return copy(saveFolder = saveFolder)
    }

    fun setAutoRecordOnCharge(autoRecordOnCharge: Boolean): AppSettings {
        return copy(autoRecordOnCharge = autoRecordOnCharge)
    }

    fun setAppLockSettings(appLockSettings: AppLockSettings?): AppSettings {
        return copy(appLockSettings = appLockSettings)
    }

    fun saveLastRecording(recorder: RecorderModel): AppSettings {
        return setLastRecording(
            recorder.recorderService!!.getRecordingInformation()
        )
    }

    // If the object is present, biometric authentication is enabled.
    // To disable biometric authentication, set the instance to null.
    fun isAppLockEnabled() = appLockSettings != null

    fun requiresExternalStoragePermission(context: Context): Boolean {
        return !SUPPORTS_SCOPED_STORAGE && (saveFolder == RECORDER_MEDIA_SELECTED_VALUE && !PermissionHelper.hasGranted(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ))
    }

    fun exportToString(): String {
        return Json.encodeToString(serializer(), this)
    }

    enum class Theme {
        SYSTEM,
        LIGHT,
        DARK,
    }

    enum class FilenameFormat {
        DATETIME_ABSOLUTE_START,
        DATETIME_RELATIVE_START,
        DATETIME_NOW,
    }

    companion object {
        fun getDefaultInstance(): AppSettings = AppSettings()

        fun fromExportedString(data: String): AppSettings {
            return Json.decodeFromString(
                serializer(),
                data,
            )
        }

        val EXAMPLE_MAX_SEGMENTS = listOf(5, 15, 30, 60, 120)
        val EXAMPLE_DURATION_TIMES = listOf(
            60 * 1000L,
            60 * 2 * 1000L,
            60 * 5 * 1000L,
            60 * 10 * 1000L,
            60 * 15 * 1000L,
        )
    }
}

@Serializable
data class RecordingInformation(
    val folderPath: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val recordingStart: LocalDateTime,
    val batchesAmount: Int,
    val maxDuration: Long,
    val intervalDuration: Long,
    val fileExtension: String,
    val watermarkLatitude: Double? = null,
    val watermarkLongitude: Double? = null,
) {
    fun hasRecordingsAvailable(context: Context): Boolean =
        VideoBatchesFolder.importFromFolder(folderPath, context)
            .hasRecordingsAvailable()

    fun getStartDateForFilename(filenameFormat: AppSettings.FilenameFormat): LocalDateTime {
        return when (filenameFormat) {
            AppSettings.FilenameFormat.DATETIME_ABSOLUTE_START -> recordingStart
            AppSettings.FilenameFormat.DATETIME_RELATIVE_START -> LocalDateTime.now().minusSeconds(
                getFullDuration() / 1000
            )

            AppSettings.FilenameFormat.DATETIME_NOW -> LocalDateTime.now()
        }
    }

    fun getFullDuration(): Long {
        // This is not accurate, since the last batch may be shorter than the others
        // but it's good enough
        return intervalDuration * batchesAmount - (intervalDuration * 0.5).toLong()
    }
}

@Serializable
data class VideoRecorderSettings(
    val targetedVideoBitRate: Int? = null,
    val quality: String? = null,
    val targetFrameRate: Int? = null,
    val enableWatermark: Boolean = false,
    val includeLocationInWatermark: Boolean = true,
    val includeGyroscopeInWatermark: Boolean = true,
    val includeAccelerometerInWatermark: Boolean = true,
    val includeOrientationInWatermark: Boolean = true,
    val includeBarometerInWatermark: Boolean = true,
    val includeLinearAccelerationInWatermark: Boolean = true,
    val watermarkRotationDegrees: Int = 0,
    /** Left inset as percent of visible width (0–90). */
    val watermarkHorizontalMarginPercent: Int = 2,
    /** Top inset as percent of visible height (0–90). */
    val watermarkVerticalMarginPercent: Int = 80,
    val watermarkFooterText: String = DEFAULT_WATERMARK_FOOTER,
) {
    fun setTargetedVideoBitRate(bitRate: Int?): VideoRecorderSettings {
        return copy(targetedVideoBitRate = bitRate)
    }

    fun setQuality(quality: Quality?): VideoRecorderSettings {
        val invertedMap = QUALITY_NAME_QUALITY_MAP.entries.associateBy({ it.value }, { it.key })

        return copy(quality = quality?.let { invertedMap[it] })
    }

    fun setTargetFrameRate(frameRate: Int?): VideoRecorderSettings {
        return copy(targetFrameRate = frameRate)
    }

    fun setEnableWatermark(enableWatermark: Boolean): VideoRecorderSettings {
        return copy(enableWatermark = enableWatermark)
    }

    fun setIncludeLocationInWatermark(includeLocationInWatermark: Boolean): VideoRecorderSettings {
        return copy(includeLocationInWatermark = includeLocationInWatermark)
    }

    fun setIncludeGyroscopeInWatermark(includeGyroscopeInWatermark: Boolean): VideoRecorderSettings {
        return copy(includeGyroscopeInWatermark = includeGyroscopeInWatermark)
    }

    fun setIncludeAccelerometerInWatermark(includeAccelerometerInWatermark: Boolean): VideoRecorderSettings {
        return copy(includeAccelerometerInWatermark = includeAccelerometerInWatermark)
    }

    fun setIncludeOrientationInWatermark(includeOrientationInWatermark: Boolean): VideoRecorderSettings {
        return copy(includeOrientationInWatermark = includeOrientationInWatermark)
    }

    fun setIncludeBarometerInWatermark(includeBarometerInWatermark: Boolean): VideoRecorderSettings {
        return copy(includeBarometerInWatermark = includeBarometerInWatermark)
    }

    fun setIncludeLinearAccelerationInWatermark(includeLinearAccelerationInWatermark: Boolean): VideoRecorderSettings {
        return copy(includeLinearAccelerationInWatermark = includeLinearAccelerationInWatermark)
    }

    fun setWatermarkRotationDegrees(watermarkRotationDegrees: Int): VideoRecorderSettings {
        return copy(watermarkRotationDegrees = watermarkRotationDegrees)
    }

    fun setWatermarkHorizontalMarginPercent(watermarkHorizontalMarginPercent: Int): VideoRecorderSettings {
        return copy(
            watermarkHorizontalMarginPercent = watermarkHorizontalMarginPercent.coerceIn(
                WATERMARK_MARGIN_MIN,
                WATERMARK_MARGIN_MAX,
            )
        )
    }

    fun setWatermarkVerticalMarginPercent(watermarkVerticalMarginPercent: Int): VideoRecorderSettings {
        return copy(
            watermarkVerticalMarginPercent = watermarkVerticalMarginPercent.coerceIn(
                WATERMARK_MARGIN_MIN,
                WATERMARK_MARGIN_MAX,
            )
        )
    }

    fun setWatermarkFooterText(watermarkFooterText: String): VideoRecorderSettings {
        return copy(watermarkFooterText = watermarkFooterText)
    }

    fun getQuality(): Quality? =
        quality?.let {
            QUALITY_NAME_QUALITY_MAP[it]!!
        }

    fun getQualitySelector(): QualitySelector? =
        quality?.let {
            QualitySelector.from(
                QUALITY_NAME_QUALITY_MAP[it]!!
            )
        }

    fun getMimeType() = "video/$fileExtension"

    val fileExtension
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "mp4" else "3gp"

    companion object {
        fun getDefaultInstance() = VideoRecorderSettings()

        val WATERMARK_ROTATION_OPTIONS = listOf(0, 90, 180, 270, 360)
        const val WATERMARK_MARGIN_MIN = 0
        const val WATERMARK_MARGIN_MAX = 90
        const val WATERMARK_HORIZONTAL_MARGIN_MIN = WATERMARK_MARGIN_MIN
        const val WATERMARK_HORIZONTAL_MARGIN_MAX = WATERMARK_MARGIN_MAX
        val WATERMARK_MARGIN_EXAMPLES = listOf(0, 2, 5, 10, 20, 40, 60, 80)
        val WATERMARK_HORIZONTAL_MARGIN_EXAMPLES = WATERMARK_MARGIN_EXAMPLES
        const val DEFAULT_WATERMARK_FOOTER = "kanjian记录仪"

        val QUALITY_NAME_QUALITY_MAP: Map<String, Quality> = mapOf(
            "LOWEST" to Quality.LOWEST,
            "HIGHEST" to Quality.HIGHEST,
            "SD" to Quality.SD,
            "HD" to Quality.HD,
            "FHD" to Quality.FHD,
            "UHD" to Quality.UHD,
        )

        val EXAMPLE_BITRATE_VALUES = listOf(
            null,
            500 * 1000,
            // 1 Mbps
            1 * 1000 * 1000,
            2 * 1000 * 1000,
            4 * 1000 * 1000,
            8 * 1000 * 1000,
            16 * 1000 * 1000,
            32 * 1000 * 1000,
            50 * 1000 * 1000,
            100 * 1000 * 1000,
        )

        val EXAMPLE_FRAME_RATE_VALUES = listOf(
            null,
            24,
            30,
            60,
            120,
            240,
        )

        val AVAILABLE_QUALITIES = listOf(
            Quality.HIGHEST,
            Quality.UHD,
            Quality.FHD,
            Quality.HD,
            Quality.SD,
            Quality.LOWEST,
        )

        val EXAMPLE_QUALITY_VALUES = listOf(
            null,
        ) + AVAILABLE_QUALITIES
    }
}

@Serializable
data class NotificationSettings(
    val title: String,
    val message: String,
    val iconID: Int,
    val showOngoing: Boolean,
    val preset: Preset? = null,
) {
    @Serializable
    sealed class Preset(
        val titleID: Int,
        val messageID: Int,
        val showOngoing: Boolean,
        val iconID: Int,
    ) {
        @Serializable
        data object Default : Preset(
            R.string.ui_videoRecorder_state_recording_title,
            R.string.ui_recorder_state_recording_description,
            true,
            R.drawable.launcher_monochrome_noopacity,
        )

        @Serializable
        data object Weather : Preset(
            R.string.ui_recorder_state_recording_fake_weather_title,
            R.string.ui_recorder_state_recording_fake_weather_description,
            false,
            R.drawable.ic_cloud
        )

        @Serializable
        data object Player : Preset(
            R.string.ui_recorder_state_recording_fake_player_title,
            R.string.ui_recorder_state_recording_fake_player_description,
            true,
            R.drawable.ic_note,
        )

        @Serializable
        data object Browser : Preset(
            R.string.ui_recorder_state_recording_fake_browser_title,
            R.string.ui_recorder_state_recording_fake_browser_description,
            true,
            R.drawable.ic_download,
        )

        @Serializable
        data object VPN : Preset(
            R.string.ui_recorder_state_recording_fake_vpn_title,
            R.string.ui_recorder_state_recording_fake_vpn_description,
            false,
            R.drawable.ic_vpn,
        )
    }

    companion object {
        fun fromPreset(preset: Preset): NotificationSettings {
            return NotificationSettings(
                title = "",
                message = "",
                showOngoing = preset.showOngoing,
                iconID = preset.iconID,
                preset = preset,
            )
        }

        val PRESETS = listOf(
            Preset.Default,
            Preset.Weather,
            Preset.Player,
            Preset.Browser,
            Preset.VPN,
        )
    }
}

@Serializable
class AppLockSettings {
    companion object {
        fun getDefaultInstance() = AppLockSettings()
    }
}
