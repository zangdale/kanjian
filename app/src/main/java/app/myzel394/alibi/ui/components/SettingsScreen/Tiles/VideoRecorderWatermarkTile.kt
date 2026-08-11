package app.myzel394.alibi.ui.components.SettingsScreen.Tiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.myzel394.alibi.R
import app.myzel394.alibi.dataStore
import app.myzel394.alibi.db.AppSettings
import app.myzel394.alibi.ui.components.atoms.SettingsTile
import kotlinx.coroutines.launch

@Composable
fun VideoRecorderWatermarkTile(
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val dataStore = LocalContext.current.dataStore

    SettingsTile(
        title = stringResource(R.string.ui_settings_option_videoWatermark_title),
        description = stringResource(R.string.ui_settings_option_videoWatermark_description),
        leading = {
            Icon(
                Icons.Default.WaterDrop,
                contentDescription = null,
            )
        },
        trailing = {
            Switch(
                checked = settings.videoRecorderSettings.enableWatermark,
                onCheckedChange = {
                    scope.launch {
                        dataStore.updateData {
                            it.setVideoRecorderSettings(
                                it.videoRecorderSettings.setEnableWatermark(
                                    !it.videoRecorderSettings.enableWatermark
                                )
                            )
                        }
                    }
                }
            )
        },
        extra = {
            if (settings.videoRecorderSettings.enableWatermark) {
                SettingsTile(
                    title = stringResource(R.string.ui_settings_option_videoWatermarkLocation_title),
                    description = stringResource(R.string.ui_settings_option_videoWatermarkLocation_description),
                    leading = {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                        )
                    },
                    trailing = {
                        Switch(
                            checked = settings.videoRecorderSettings.includeLocationInWatermark,
                            onCheckedChange = {
                                scope.launch {
                                    dataStore.updateData {
                                        it.setVideoRecorderSettings(
                                            it.videoRecorderSettings.setIncludeLocationInWatermark(
                                                !it.videoRecorderSettings.includeLocationInWatermark
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    },
                    tertiaryLine = {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.ui_settings_option_videoWatermark_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        },
    )
}
