package app.zangdale.kanjian.ui.components.RecorderScreen.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.helpers.BatchesFolder
import app.zangdale.kanjian.helpers.VideoBatchesFolder
import app.zangdale.kanjian.ui.components.atoms.MessageBox
import app.zangdale.kanjian.ui.components.atoms.MessageType
import app.zangdale.kanjian.ui.components.atoms.VisualDensity

@Composable
fun LowStorageInfo(
    modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    appSettings: AppSettings,
) {
    val context = LocalContext.current
    val availableBytes =
        VideoBatchesFolder.importFromFolder(appSettings.saveFolder, context).getAvailableBytes()

    if (availableBytes == null) {
        return
    }

    val bytesPerMinute = BatchesFolder.requiredBytesForOneMinuteOfRecording(appSettings)
    val requiredBytes = appSettings.effectiveRetentionMs() / 1000 / 60 * bytesPerMinute

    // Allow for a 10% margin of error
    val isLowOnStorage = availableBytes < requiredBytes * 1.1
    println("LowStorageInfo: availableBytes: $availableBytes, requiredBytes: $requiredBytes, isLowOnStorage: $isLowOnStorage")

    if (isLowOnStorage)
        Box(modifier = modifier) {
            BoxWithConstraints {
                val isLarge = maxHeight > 600.dp;

                MessageBox(
                    type = MessageType.WARNING,
                    message = stringResource(R.string.ui_recorder_lowOnStorage_hint),
                    density = if (isLarge) VisualDensity.COMFORTABLE else VisualDensity.COMPACT
                )
            }
        }
}
