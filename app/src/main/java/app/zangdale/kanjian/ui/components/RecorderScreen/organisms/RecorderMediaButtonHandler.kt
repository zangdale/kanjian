package app.zangdale.kanjian.ui.components.RecorderScreen.organisms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.ui.models.VideoRecorderModel
import app.zangdale.kanjian.ui.utils.PermissionHelper
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

object RecorderMediaButtonDispatcher {
    var onMediaButtonPressed: (() -> Boolean)? = null

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) {
            return false
        }

        if (!isMediaButtonKey(event.keyCode)) {
            return false
        }

        return onMediaButtonPressed?.invoke() == true
    }

    private fun isMediaButtonKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
}

@Composable
fun RecorderMediaButtonHandler(
    settings: AppSettings,
    videoRecorder: VideoRecorderModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsState = rememberUpdatedState(settings)
    val videoRecorderState = rememberUpdatedState(videoRecorder)
    val lastPressTimeMs = remember { AtomicLong(0L) }

    val handleMediaButton = remember(scope, context) {
        {
            val now = System.currentTimeMillis()
            if (now - lastPressTimeMs.get() < 500L) {
                true
            } else {
                lastPressTimeMs.set(now)

                val currentSettings = settingsState.value
                val currentVideoRecorder = videoRecorderState.value

                when {
                    currentVideoRecorder.isInRecording -> {
                        scope.launch {
                            currentVideoRecorder.stopRecording(context)
                            runCatching {
                                currentVideoRecorder.destroyService(context)
                            }
                            currentVideoRecorder.onRecordingSave(false)
                        }
                        true
                    }

                    canStartVideoRecording(context, currentSettings) -> {
                        currentVideoRecorder.startRecording(context, currentSettings)
                        true
                    }

                    else -> false
                }
            }
        }
    }

    DisposableEffect(handleMediaButton) {
        RecorderMediaButtonDispatcher.onMediaButtonPressed = handleMediaButton

        val mediaSession = MediaSession(context, "KanjianRecorder").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    handleMediaButton()
                }

                override fun onPause() {
                    handleMediaButton()
                }

                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val event = mediaButtonIntent.getKeyEventExtra()
                        ?: return super.onMediaButtonEvent(mediaButtonIntent)

                    if (RecorderMediaButtonDispatcher.dispatchKeyEvent(event)) {
                        return true
                    }

                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                                PlaybackState.ACTION_PAUSE or
                                PlaybackState.ACTION_PLAY_PAUSE,
                    )
                    .setState(PlaybackState.STATE_PAUSED, 0, 1f)
                    .build(),
            )
            isActive = true
        }

        onDispose {
            if (RecorderMediaButtonDispatcher.onMediaButtonPressed === handleMediaButton) {
                RecorderMediaButtonDispatcher.onMediaButtonPressed = null
            }
            mediaSession.isActive = false
            mediaSession.release()
        }
    }
}

private fun canStartVideoRecording(context: Context, settings: AppSettings): Boolean {
    if (!PermissionHelper.hasGranted(context, Manifest.permission.CAMERA)) {
        return false
    }

    if (settings.requiresExternalStoragePermission(context)) {
        return false
    }

    return true
}

@Suppress("DEPRECATION")
private fun Intent.getKeyEventExtra(): KeyEvent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_KEY_EVENT)
    }
