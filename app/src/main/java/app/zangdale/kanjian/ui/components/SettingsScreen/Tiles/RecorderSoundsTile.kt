package app.zangdale.kanjian.ui.components.SettingsScreen.Tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.zangdale.kanjian.R
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.ui.components.atoms.SettingsTile
import kotlinx.coroutines.launch

@Composable
fun RecorderSoundsTile(
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val dataStore = LocalContext.current.dataStore

    SettingsTile(
        title = stringResource(R.string.ui_settings_option_recorderSounds_title),
        description = stringResource(R.string.ui_settings_option_recorderSounds_description),
        leading = {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
            )
        },
        trailing = {
            Switch(
                checked = settings.enableRecorderSounds,
                onCheckedChange = {
                    scope.launch {
                        dataStore.updateData {
                            it.setEnableRecorderSounds(it.enableRecorderSounds.not())
                        }
                    }
                }
            )
        }
    )
}
