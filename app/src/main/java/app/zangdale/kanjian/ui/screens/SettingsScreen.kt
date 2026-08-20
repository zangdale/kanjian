package app.zangdale.kanjian.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.ui.SUPPORTS_DARK_MODE_NATIVELY
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.AboutTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.CustomNotificationTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.DividerTitle
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.EnableAppLockTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.FilenameFormatTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.ImportExport
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.IntervalDurationTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.MaxDurationTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.RecorderSoundsTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.SaveFolderTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.VideoRecorderBitrateTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.VideoRecorderFrameRateTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.VideoRecorderQualityTile
import app.zangdale.kanjian.ui.components.SettingsScreen.Tiles.VideoRecorderWatermarkTile
import app.zangdale.kanjian.ui.components.SettingsScreen.atoms.InAppLanguagePicker
import app.zangdale.kanjian.ui.components.SettingsScreen.atoms.ThemeSelector
import app.zangdale.kanjian.ui.components.atoms.GlobalSwitch
import app.zangdale.kanjian.ui.components.atoms.MessageBox
import app.zangdale.kanjian.ui.components.atoms.MessageType
import app.zangdale.kanjian.ui.effects.rememberSettings
import app.zangdale.kanjian.ui.models.VideoRecorderModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackNavigate: () -> Unit,
    onNavigateToCustomRecordingNotifications: () -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    videoRecorder: VideoRecorderModel,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        dismissActionContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            )
        },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.ui_settings_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBackNavigate) {
                        val label = stringResource(R.string.goBack)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = label,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val scope = rememberCoroutineScope()
            val dataStore = LocalContext.current.dataStore
            val settings = rememberSettings()

            // Show alert
            if (videoRecorder.isInRecording) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    MessageBox(
                        type = MessageType.WARNING,
                        title = stringResource(R.string.ui_settings_hint_recordingActive_title),
                        message = stringResource(R.string.ui_settings_hint_recordingActive_message),
                    )
                }
            }
            if (!SUPPORTS_DARK_MODE_NATIVELY) {
                ThemeSelector()
            }
            MaxDurationTile(settings = settings)
            IntervalDurationTile(settings = settings)
            InAppLanguagePicker()
            RecorderSoundsTile(settings = settings)
            CustomNotificationTile(onNavigateToCustomRecordingNotifications, settings = settings)
            EnableAppLockTile(settings = settings)
            FilenameFormatTile(settings = settings, snackbarHostState = snackbarHostState)
            SaveFolderTile(
                settings = settings,
                snackbarHostState = snackbarHostState,
            )
            VideoRecorderWatermarkTile(settings = settings)
            GlobalSwitch(
                label = stringResource(R.string.ui_settings_advancedSettings_label),
                checked = settings.showAdvancedSettings,
                onCheckedChange = {
                    scope.launch {
                        dataStore.updateData {
                            it.setShowAdvancedSettings(it.showAdvancedSettings.not())
                        }
                    }
                }
            )
            AnimatedVisibility(visible = settings.showAdvancedSettings) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Column {
                        DividerTitle(
                            title = stringResource(R.string.ui_settings_sections_video_title),
                            description = stringResource(R.string.ui_settings_sections_video_description),
                        )
                        VideoRecorderQualityTile(settings = settings)
                        VideoRecorderBitrateTile(settings = settings)
                        VideoRecorderFrameRateTile(settings = settings)
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                    )
                    ImportExport(snackbarHostState = snackbarHostState)
                }
            }
            AboutTile(onNavigateToAboutScreen)
        }
    }
}
