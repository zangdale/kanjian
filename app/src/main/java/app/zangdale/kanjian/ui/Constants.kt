package app.zangdale.kanjian.ui

import android.os.Build
import androidx.compose.ui.unit.dp
import java.util.Base64

val BIG_PRIMARY_BUTTON_SIZE = 64.dp
val BIG_PRIMARY_BUTTON_MAX_WIDTH = 450.dp

val SHEET_BOTTOM_OFFSET = 24.dp
val MAX_AMPLITUDE = 20000
val SUPPORTS_DARK_MODE_NATIVELY = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

val MEDIA_SUBFOLDER_NAME = "kanjian"

val SUPPORTS_SCOPED_STORAGE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
val SUPPORTS_SAVING_VIDEOS_IN_CUSTOM_FOLDERS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
val MEDIA_RECORDINGS_PREFIX = "kanjian-recording-"
val RECORDER_MEDIA_SELECTED_VALUE = "_'media"
val RECORDER_INTERNAL_SELECTED_VALUE = "_'internal"

val VIDEO_RECORDING_BATCHES_SUBFOLDER_NAME = ".video_recordings"
val AUDIO_RECORDING_BATCHES_SUBFOLDER_NAME = ".audio_recordings"

const val REPO_URL = "https://github.com/zangdale/kanjian"
const val TRANSLATION_HELP_URL = "https://github.com/zangdale/kanjian"
const val GITHUB_SPONSORS_URL = "https://github.com/sponsors/zangdale"
const val PUBLIC_KEY = ""
const val PUBLIC_KEY_FINGERPRINT = ""
val CRYPTO_DONATIONS = emptyMap<String, String>()

// Base64encoding these values so that bots can't easily scrape them.
val b64d = Base64.getDecoder()
val CONTACT_METHODS = mapOf<String, String>(
    "GitHub" to String(
        b64d.decode(
            "aHR0cHM6Ly9naXRodW" +
                    "IuY29tL3phbmdkYWxl"
        )
    ).trim(),
)
