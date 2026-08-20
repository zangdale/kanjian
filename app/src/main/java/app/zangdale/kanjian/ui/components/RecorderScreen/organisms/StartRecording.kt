package app.zangdale.kanjian.ui.components.RecorderScreen.organisms

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.ui.BIG_PRIMARY_BUTTON_MAX_WIDTH
import app.zangdale.kanjian.ui.BIG_PRIMARY_BUTTON_SIZE
import app.zangdale.kanjian.ui.components.RecorderScreen.atoms.ChargingAutoRecordSwitch
import app.zangdale.kanjian.ui.components.RecorderScreen.atoms.LowStorageInfo
import app.zangdale.kanjian.ui.components.RecorderScreen.molecules.QuickMaxDurationSelector
import app.zangdale.kanjian.ui.components.RecorderScreen.molecules.VideoRecordingStart
import app.zangdale.kanjian.ui.effects.rememberForceUpdateOnLifeCycleChange
import app.zangdale.kanjian.ui.models.VideoRecorderModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StartRecording(
    videoRecorder: VideoRecorderModel,
    // Loading this from parent, because if we load it ourselves
    // and permissions have already been granted, initial
    // settings will be used, instead of the actual settings.
    appSettings: AppSettings,
    onSaveLastRecording: () -> Unit,
    onHideTopBar: () -> Unit,
    onShowTopBar: () -> Unit,
    showVideoPreviewFullscreen: Boolean,
) {
    val orientation = LocalConfiguration.current.orientation
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

    val label = stringResource(
        R.string.ui_recorder_action_start_description_2,
        appSettings.maxSegments
    )
    val annotatedDescription = buildAnnotatedString {
        append(stringResource(R.string.ui_recorder_action_start_description_1))

        withStyle(SpanStyle(background = MaterialTheme.colorScheme.surfaceVariant)) {
            pushStringAnnotation(
                tag = "minutes",
                annotation = label,
            )
            append(label)
        }

        append(stringResource(R.string.ui_recorder_action_start_description_3))
    }

    var showQuickMaxDurationSelector by rememberSaveable {
        mutableStateOf(false)
    }

    if (showQuickMaxDurationSelector) {
        QuickMaxDurationSelector(
            onDismiss = {
                showQuickMaxDurationSelector = false
            },
        )
    }

    BoxWithConstraints {
        val isLargeDisplay =
            maxWidth > 250.dp && maxHeight > 600.dp && orientation == Configuration.ORIENTATION_PORTRAIT

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    VideoRecordingStart(
                        videoRecorder = videoRecorder,
                        appSettings = appSettings,
                        onHideAudioRecording = onHideTopBar,
                        onShowAudioRecording = onShowTopBar,
                        showPreview = showVideoPreviewFullscreen,
                        useLargeButtons = false,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .then(rememberForceUpdateOnLifeCycleChange()),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!showVideoPreviewFullscreen) {
                        ChargingAutoRecordSwitch(
                            settings = appSettings,
                            showDescription = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp),
                        )
                    }

                    _StartRecordingFooter(
                        appSettings = appSettings,
                        annotatedDescription = annotatedDescription,
                        onSaveLastRecording = onSaveLastRecording,
                        onShowQuickMaxDurationSelector = {
                            showQuickMaxDurationSelector = true
                        },
                        compact = true,
                    )

                    LowStorageInfo(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        appSettings = appSettings,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                VideoRecordingStart(
                    videoRecorder = videoRecorder,
                    appSettings = appSettings,
                    onHideAudioRecording = onHideTopBar,
                    onShowAudioRecording = onShowTopBar,
                    showPreview = showVideoPreviewFullscreen,
                    useLargeButtons = isLargeDisplay,
                )

                if (!showVideoPreviewFullscreen) {
                    ChargingAutoRecordSwitch(
                        settings = appSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                            .padding(top = 8.dp),
                    )
                }

                val forceUpdate = rememberForceUpdateOnLifeCycleChange()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(forceUpdate),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    _StartRecordingFooter(
                        appSettings = appSettings,
                        annotatedDescription = annotatedDescription,
                        onSaveLastRecording = onSaveLastRecording,
                        onShowQuickMaxDurationSelector = {
                            showQuickMaxDurationSelector = true
                        },
                        compact = false,
                    )
                }

                LowStorageInfo(
                    modifier = if (isLargeDisplay) Modifier
                        .padding(16.dp)
                        .widthIn(max = 400.dp) else Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    appSettings = appSettings,
                )
            }
        }
    }
}

@Composable
private fun _StartRecordingFooter(
    appSettings: AppSettings,
    annotatedDescription: AnnotatedString,
    onSaveLastRecording: () -> Unit,
    onShowQuickMaxDurationSelector: () -> Unit,
    compact: Boolean,
) {
    val context = LocalContext.current

    if (appSettings.lastRecording?.hasRecordingsAvailable(context) == true) {
        val saveLabel = stringResource(
            R.string.ui_recorder_action_saveOldRecording_label,
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .format(appSettings.lastRecording.recordingStart),
        )
        TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .requiredWidthIn(max = BIG_PRIMARY_BUTTON_MAX_WIDTH)
                .height(if (compact) 48.dp else BIG_PRIMARY_BUTTON_SIZE)
                .semantics {
                    contentDescription = saveLabel
                },
            onClick = onSaveLastRecording,
            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(saveLabel)
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 0.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.launcher_monochrome_noopacity),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )

            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))

            ClickableText(
                text = annotatedDescription,
                onClick = { textIndex ->
                    if (annotatedDescription.getStringAnnotations(textIndex, textIndex)
                            .firstOrNull()?.tag == "minutes"
                    ) {
                        onShowQuickMaxDurationSelector()
                    }
                },
                modifier = Modifier
                    .widthIn(max = if (compact) 360.dp else 300.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
