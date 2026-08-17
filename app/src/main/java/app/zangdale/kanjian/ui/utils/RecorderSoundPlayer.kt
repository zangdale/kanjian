package app.zangdale.kanjian.ui.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import app.zangdale.kanjian.R

object RecorderSoundPlayer {
    private var startPlayer: MediaPlayer? = null
    private var pausePlayer: MediaPlayer? = null
    private var fragmentPlayer: MediaPlayer? = null

    fun playStart(context: Context, customUri: String? = null) {
        play(context, customUri, R.raw.start) { startPlayer = it }
    }

    fun playPause(context: Context, customUri: String? = null) {
        play(context, customUri, R.raw.purse) { pausePlayer = it }
    }

    fun playFragment(context: Context, customUri: String? = null) {
        play(context, customUri, R.raw.fragment) { fragmentPlayer = it }
    }

    private fun play(
        context: Context,
        customUri: String?,
        fallbackResourceId: Int,
        storePlayer: (MediaPlayer?) -> Unit,
    ) {
        storePlayer(null)

        runCatching {
            val appContext = context.applicationContext
            val player = createPlayer(appContext, customUri, fallbackResourceId) ?: return@runCatching

            player.apply {
                setOnCompletionListener {
                    release()
                    storePlayer(null)
                }
                setOnErrorListener { _, _, _ ->
                    release()
                    storePlayer(null)
                    true
                }
                storePlayer(this)
                start()
            }
        }
    }

    private fun createPlayer(
        context: Context,
        customUri: String?,
        fallbackResourceId: Int,
    ): MediaPlayer? {
        if (!customUri.isNullOrBlank()) {
            val customPlayer = runCatching {
                MediaPlayer.create(context, Uri.parse(customUri))
            }.getOrNull()

            if (customPlayer != null) {
                return customPlayer
            }
        }

        return MediaPlayer.create(context, fallbackResourceId)
    }
}
