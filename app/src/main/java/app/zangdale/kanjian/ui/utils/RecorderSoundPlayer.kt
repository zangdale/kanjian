package app.zangdale.kanjian.ui.utils

import android.content.Context
import android.media.MediaPlayer
import app.zangdale.kanjian.R

object RecorderSoundPlayer {
    private var startPlayer: MediaPlayer? = null
    private var pausePlayer: MediaPlayer? = null
    private var fragmentPlayer: MediaPlayer? = null

    fun playStart(context: Context) {
        play(context, R.raw.start) { startPlayer = it }
    }

    fun playPause(context: Context) {
        play(context, R.raw.purse) { pausePlayer = it }
    }

    fun playFragment(context: Context) {
        play(context, R.raw.fragment) { fragmentPlayer = it }
    }

    private fun play(
        context: Context,
        resourceId: Int,
        storePlayer: (MediaPlayer?) -> Unit,
    ) {
        storePlayer(null)

        runCatching {
            MediaPlayer.create(context.applicationContext, resourceId)?.apply {
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
}
