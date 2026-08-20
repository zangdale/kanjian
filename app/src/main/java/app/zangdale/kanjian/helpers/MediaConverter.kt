package app.zangdale.kanjian.helpers

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.util.UUID

class MediaConverter {
    companion object {
        private fun createTempFile(content: String): File {
            val id = UUID.randomUUID().toString()

            return File.createTempFile(".temp-ffmpeg-files-$id", ".txt").apply {
                writeText(content)
            }
        }

        fun concatenateVideoFiles(
            inputFiles: Iterable<String>,
            outputFile: String,
            extraCommand: String = "",
            onProgress: (Int) -> Unit = { },
        ): CompletableDeferred<Unit> {
            val completer = CompletableDeferred<Unit>()

            val listFile = createTempFile(inputFiles.joinToString("\n") { "file '$it'" })

            val command =
                "-protocol_whitelist saf,concat,content,file,subfile" +
                        " -safe 0" +
                        " -strict normal" +
                        " -f concat" +
                        " -i ${listFile.absolutePath}" +
                        extraCommand +
                        " -y" +
                        " $outputFile"

            FFmpegKit.executeAsync(
                command,
                { session ->
                    runCatching {
                        listFile.delete()
                    }

                    if (ReturnCode.isSuccess(session!!.returnCode)) {
                        completer.complete(Unit)
                    } else {
                        Log.i(
                            "Video Concatenation",
                            String.format(
                                "Command failed with state %s and rc %s.%s",
                                session.state,
                                session.returnCode,
                                session.failStackTrace,
                            )
                        )

                        completer.completeExceptionally(FFmpegException("Failed to concatenate videos"))
                    }
                },
                {},
                { statistics ->
                    onProgress(statistics.time.toInt())
                }
            )

            return completer
        }
    }

    class FFmpegException(message: String) : Exception(message)
}
