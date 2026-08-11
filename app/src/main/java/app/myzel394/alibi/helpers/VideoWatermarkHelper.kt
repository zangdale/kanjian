package app.myzel394.alibi.helpers

import app.myzel394.alibi.db.AppSettings
import app.myzel394.alibi.db.RecordingInformation
import java.time.ZoneId
import java.util.Locale

object VideoWatermarkHelper {
    private const val FONT_PATH = "/system/fonts/Roboto-Regular.ttf"

    fun buildDrawtextFilter(
        recording: RecordingInformation,
        settings: AppSettings,
    ): String? {
        if (!settings.videoRecorderSettings.enableWatermark) {
            return null
        }

        if (recording.type != RecordingInformation.Type.VIDEO) {
            return null
        }

        val epoch = recording.recordingStart
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()

        val filters = mutableListOf(buildTimestampFilter(epoch))

        if (settings.videoRecorderSettings.includeLocationInWatermark) {
            val latitude = recording.watermarkLatitude
            val longitude = recording.watermarkLongitude
            if (latitude != null && longitude != null) {
                filters.add(buildLocationFilter(latitude, longitude))
            }
        }

        return filters.joinToString(",")
    }

    fun ffmpegParametersWithWatermark(videoFilter: String): Array<String> {
        return arrayOf(
            " -vf \"$videoFilter\" -c:v libx264 -preset ultrafast -crf 23 -c:a copy",
            " -vf \"$videoFilter\" -c:v libx264 -preset ultrafast -crf 23 -c:a aac",
            " -vf \"$videoFilter\" -c:v libx265 -preset ultrafast -crf 28 -c:a copy",
            " -vf \"$videoFilter\" -c:v libx265 -preset ultrafast -crf 28 -c:a aac",
        )
    }

    private fun buildTimestampFilter(epoch: Long): String {
        return "drawtext=fontfile=$FONT_PATH:" +
                "text='%{pts\\\\:localtime\\\\:$epoch}':" +
                "x=20:y=h-th-20:fontsize=28:fontcolor=white:borderw=2:bordercolor=black@0.8:" +
                "box=1:boxcolor=black@0.4:boxborderw=8"
    }

    private fun buildLocationFilter(latitude: Double, longitude: Double): String {
        val text = String.format(
            Locale.US,
            "GPS\\\\: %.6f, %.6f",
            latitude,
            longitude,
        )

        return "drawtext=fontfile=$FONT_PATH:text='$text':" +
                "x=20:y=h-th-60:fontsize=24:fontcolor=white:borderw=2:bordercolor=black@0.8:" +
                "box=1:boxcolor=black@0.4:boxborderw=8"
    }
}
