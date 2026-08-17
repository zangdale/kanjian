package app.zangdale.kanjian.ui.components.SettingsScreen.Tiles

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import app.zangdale.kanjian.R
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.ui.components.atoms.SettingsTile
import app.zangdale.kanjian.ui.utils.RecorderSoundPlayer
import kotlinx.coroutines.launch

private enum class CustomRecorderSoundType {
    START,
    PAUSE,
    FRAGMENT,
}

@Composable
fun RecorderSoundsTile(
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataStore = context.dataStore

    val pendingTypeHolder = remember {
        object {
            var value: CustomRecorderSoundType? = null
        }
    }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val type = pendingTypeHolder.value ?: return@rememberLauncherForActivityResult
        pendingTypeHolder.value = null
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        scope.launch {
            dataStore.updateData { current ->
                when (type) {
                    CustomRecorderSoundType.START ->
                        current.setCustomStartSoundUri(uri.toString())

                    CustomRecorderSoundType.PAUSE ->
                        current.setCustomPauseSoundUri(uri.toString())

                    CustomRecorderSoundType.FRAGMENT ->
                        current.setCustomFragmentSoundUri(uri.toString())
                }
            }
        }
    }

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
        },
        extra = {
            AnimatedVisibility(visible = settings.enableRecorderSounds) {
                Column {
                    Text(
                        text = stringResource(R.string.ui_settings_option_recorderSounds_custom_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    CustomSoundRow(
                        title = stringResource(R.string.ui_settings_option_recorderSounds_custom_start),
                        uri = settings.customStartSoundUri,
                        onPick = {
                            pendingTypeHolder.value = CustomRecorderSoundType.START
                            pickAudioLauncher.launch(
                                arrayOf(
                                    "audio/mpeg",
                                    "audio/mp3",
                                    "audio/x-mpeg",
                                )
                            )
                        },
                        onReset = {
                            scope.launch {
                                dataStore.updateData { it.setCustomStartSoundUri(null) }
                            }
                        },
                        onPreview = {
                            RecorderSoundPlayer.playStart(context, settings.customStartSoundUri)
                        },
                    )

                    CustomSoundRow(
                        title = stringResource(R.string.ui_settings_option_recorderSounds_custom_pause),
                        uri = settings.customPauseSoundUri,
                        onPick = {
                            pendingTypeHolder.value = CustomRecorderSoundType.PAUSE
                            pickAudioLauncher.launch(
                                arrayOf(
                                    "audio/mpeg",
                                    "audio/mp3",
                                    "audio/x-mpeg",
                                )
                            )
                        },
                        onReset = {
                            scope.launch {
                                dataStore.updateData { it.setCustomPauseSoundUri(null) }
                            }
                        },
                        onPreview = {
                            RecorderSoundPlayer.playPause(context, settings.customPauseSoundUri)
                        },
                    )

                    CustomSoundRow(
                        title = stringResource(R.string.ui_settings_option_recorderSounds_custom_fragment),
                        uri = settings.customFragmentSoundUri,
                        onPick = {
                            pendingTypeHolder.value = CustomRecorderSoundType.FRAGMENT
                            pickAudioLauncher.launch(
                                arrayOf(
                                    "audio/mpeg",
                                    "audio/mp3",
                                    "audio/x-mpeg",
                                )
                            )
                        },
                        onReset = {
                            scope.launch {
                                dataStore.updateData { it.setCustomFragmentSoundUri(null) }
                            }
                        },
                        onPreview = {
                            RecorderSoundPlayer.playFragment(context, settings.customFragmentSoundUri)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun CustomSoundRow(
    title: String,
    uri: String?,
    onPick: () -> Unit,
    onReset: () -> Unit,
    onPreview: () -> Unit,
) {
    val context = LocalContext.current
    val displayName = uri?.let {
        runCatching {
            DocumentFile.fromSingleUri(context, Uri.parse(it))?.name
        }.getOrNull() ?: stringResource(R.string.ui_settings_option_recorderSounds_custom_selected)
    } ?: stringResource(R.string.ui_settings_option_recorderSounds_custom_default)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AudioFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onPreview) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.ui_settings_option_recorderSounds_custom_preview),
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPick) {
                Text(stringResource(R.string.ui_settings_option_recorderSounds_custom_select))
            }
            if (uri != null) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.ui_settings_option_recorderSounds_custom_reset))
                }
            }
        }
    }
}
