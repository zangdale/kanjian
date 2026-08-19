package app.zangdale.kanjian.ui.components.SettingsScreen.Tiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.db.VideoRecorderSettings
import app.zangdale.kanjian.ui.components.atoms.ExampleListRoulette
import app.zangdale.kanjian.ui.components.atoms.SettingsTile
import app.zangdale.kanjian.ui.utils.IconResource
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.IconSource
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.input.InputDialog
import com.maxkeppeler.sheets.input.models.InputHeader
import com.maxkeppeler.sheets.input.models.InputSelection
import com.maxkeppeler.sheets.input.models.InputTextField
import com.maxkeppeler.sheets.input.models.InputTextFieldType
import com.maxkeppeler.sheets.input.models.ValidationResult
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoRecorderWatermarkTile(
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val dataStore = LocalContext.current.dataStore
    val rotationDialog = rememberUseCaseState()
    val horizontalMarginDialog = rememberUseCaseState()
    val verticalMarginDialog = rememberUseCaseState()
    val footerDialog = rememberUseCaseState()

    val rotations = VideoRecorderSettings.WATERMARK_ROTATION_OPTIONS
    val horizontalMarginRangeError = stringResource(
        R.string.ui_settings_option_videoWatermarkHorizontalMargin_rangeError,
    )
    val verticalMarginRangeError = stringResource(
        R.string.ui_settings_option_videoWatermarkVerticalMargin_rangeError,
    )

    fun updateWatermark(transform: (VideoRecorderSettings) -> VideoRecorderSettings) {
        scope.launch {
            dataStore.updateData {
                it.setVideoRecorderSettings(transform(it.videoRecorderSettings))
            }
        }
    }

    ListDialog(
        state = rotationDialog,
        header = Header.Default(
            title = stringResource(R.string.ui_settings_option_videoWatermarkRotation_title),
            icon = IconSource(
                painter = IconResource.fromImageVector(Icons.Default.Rotate90DegreesCcw)
                    .asPainterResource(),
                contentDescription = null,
            ),
        ),
        selection = ListSelection.Single(
            showRadioButtons = true,
            options = rotations.map { degrees ->
                ListOption(
                    titleText = stringResource(
                        R.string.ui_settings_option_videoWatermarkRotation_degrees,
                        degrees,
                    ),
                    selected = settings.videoRecorderSettings.watermarkRotationDegrees == degrees,
                )
            },
        ) { index, _ ->
            updateWatermark {
                it.setWatermarkRotationDegrees(rotations[index])
            }
        },
    )

    InputDialog(
        state = horizontalMarginDialog,
        header = Header.Default(
            title = stringResource(R.string.ui_settings_option_videoWatermarkHorizontalMargin_title),
            icon = IconSource(
                painter = IconResource.fromImageVector(Icons.Default.SpaceBar).asPainterResource(),
                contentDescription = null,
            ),
        ),
        selection = InputSelection(
            input = listOf(
                InputTextField(
                    header = InputHeader(
                        title = stringResource(
                            R.string.ui_settings_option_videoWatermarkHorizontalMargin_input,
                        ),
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    type = InputTextFieldType.OUTLINED,
                    text = settings.videoRecorderSettings.watermarkHorizontalMarginPercent.toString(),
                    validationListener = { text ->
                        val value = text?.toIntOrNull()
                        if (
                            value == null ||
                            value < VideoRecorderSettings.WATERMARK_MARGIN_MIN ||
                            value > VideoRecorderSettings.WATERMARK_MARGIN_MAX
                        ) {
                            return@InputTextField ValidationResult.Invalid(horizontalMarginRangeError)
                        }
                        ValidationResult.Valid
                    },
                    key = "horizontal_margin",
                )
            ),
        ) { result ->
            val value = result.getString("horizontal_margin")?.toIntOrNull() ?: return@InputSelection
            updateWatermark {
                it.setWatermarkHorizontalMarginPercent(value)
            }
        },
    )

    InputDialog(
        state = verticalMarginDialog,
        header = Header.Default(
            title = stringResource(R.string.ui_settings_option_videoWatermarkVerticalMargin_title),
            icon = IconSource(
                painter = IconResource.fromImageVector(Icons.Default.Height).asPainterResource(),
                contentDescription = null,
            ),
        ),
        selection = InputSelection(
            input = listOf(
                InputTextField(
                    header = InputHeader(
                        title = stringResource(
                            R.string.ui_settings_option_videoWatermarkVerticalMargin_input,
                        ),
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    type = InputTextFieldType.OUTLINED,
                    text = settings.videoRecorderSettings.watermarkVerticalMarginPercent.toString(),
                    validationListener = { text ->
                        val value = text?.toIntOrNull()
                        if (
                            value == null ||
                            value < VideoRecorderSettings.WATERMARK_MARGIN_MIN ||
                            value > VideoRecorderSettings.WATERMARK_MARGIN_MAX
                        ) {
                            return@InputTextField ValidationResult.Invalid(verticalMarginRangeError)
                        }
                        ValidationResult.Valid
                    },
                    key = "vertical_margin",
                )
            ),
        ) { result ->
            val value = result.getString("vertical_margin")?.toIntOrNull() ?: return@InputSelection
            updateWatermark {
                it.setWatermarkVerticalMarginPercent(value)
            }
        },
    )

    InputDialog(
        state = footerDialog,
        header = Header.Default(
            title = stringResource(R.string.ui_settings_option_videoWatermarkFooter_title),
            icon = IconSource(
                painter = IconResource.fromImageVector(Icons.Default.ShortText).asPainterResource(),
                contentDescription = null,
            ),
        ),
        selection = InputSelection(
            input = listOf(
                InputTextField(
                    header = InputHeader(
                        title = stringResource(
                            R.string.ui_settings_option_videoWatermarkFooter_input,
                        ),
                    ),
                    type = InputTextFieldType.OUTLINED,
                    text = settings.videoRecorderSettings.watermarkFooterText,
                    key = "footer",
                )
            ),
        ) { result ->
            updateWatermark {
                it.setWatermarkFooterText(result.getString("footer").orEmpty())
            }
        },
    )

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
                    updateWatermark {
                        it.setEnableWatermark(!it.enableWatermark)
                    }
                }
            )
        },
        extra = {
            if (settings.videoRecorderSettings.enableWatermark) {
                Column {
                    SettingsTile(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkRotation_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkRotation_description),
                        leading = {
                            Icon(
                                Icons.Default.Rotate90DegreesCcw,
                                contentDescription = null,
                            )
                        },
                        trailing = {
                            Button(
                                onClick = rotationDialog::show,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    stringResource(
                                        R.string.ui_settings_option_videoWatermarkRotation_degrees,
                                        settings.videoRecorderSettings.watermarkRotationDegrees,
                                    )
                                )
                            }
                        },
                    )

                    SettingsTile(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkHorizontalMargin_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkHorizontalMargin_description),
                        leading = {
                            Icon(
                                Icons.Default.SpaceBar,
                                contentDescription = null,
                            )
                        },
                        trailing = {
                            Button(
                                onClick = horizontalMarginDialog::show,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    stringResource(
                                        R.string.ui_settings_option_videoWatermarkHorizontalMargin_value,
                                        settings.videoRecorderSettings.watermarkHorizontalMarginPercent,
                                    )
                                )
                            }
                        },
                        extra = {
                            ExampleListRoulette(
                                items = VideoRecorderSettings.WATERMARK_MARGIN_EXAMPLES,
                                onItemSelected = { value ->
                                    updateWatermark {
                                        it.setWatermarkHorizontalMarginPercent(value)
                                    }
                                },
                            ) { value ->
                                Text(
                                    stringResource(
                                        R.string.ui_settings_option_videoWatermarkHorizontalMargin_value,
                                        value,
                                    )
                                )
                            }
                        },
                    )

                    SettingsTile(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkVerticalMargin_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkVerticalMargin_description),
                        leading = {
                            Icon(
                                Icons.Default.Height,
                                contentDescription = null,
                            )
                        },
                        trailing = {
                            Button(
                                onClick = verticalMarginDialog::show,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    stringResource(
                                        R.string.ui_settings_option_videoWatermarkVerticalMargin_value,
                                        settings.videoRecorderSettings.watermarkVerticalMarginPercent,
                                    )
                                )
                            }
                        },
                        extra = {
                            ExampleListRoulette(
                                items = VideoRecorderSettings.WATERMARK_MARGIN_EXAMPLES,
                                onItemSelected = { value ->
                                    updateWatermark {
                                        it.setWatermarkVerticalMarginPercent(value)
                                    }
                                },
                            ) { value ->
                                Text(
                                    stringResource(
                                        R.string.ui_settings_option_videoWatermarkVerticalMargin_value,
                                        value,
                                    )
                                )
                            }
                        },
                    )

                    SettingsTile(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkFooter_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkFooter_description),
                        leading = {
                            Icon(
                                Icons.Default.ShortText,
                                contentDescription = null,
                            )
                        },
                        trailing = {
                            Button(
                                onClick = footerDialog::show,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                val footer = settings.videoRecorderSettings.watermarkFooterText
                                Text(
                                    if (footer.isBlank()) {
                                        stringResource(R.string.ui_settings_option_videoWatermarkFooter_empty)
                                    } else {
                                        footer
                                    }
                                )
                            }
                        },
                        extra = {
                            ExampleListRoulette(
                                items = listOf(
                                    VideoRecorderSettings.DEFAULT_WATERMARK_FOOTER,
                                    "",
                                ),
                                onItemSelected = { value ->
                                    updateWatermark {
                                        it.setWatermarkFooterText(value)
                                    }
                                },
                            ) { value ->
                                Text(
                                    if (value.isBlank()) {
                                        stringResource(R.string.ui_settings_option_videoWatermarkFooter_empty)
                                    } else {
                                        value
                                    }
                                )
                            }
                        },
                    )

                    WatermarkOptionSwitch(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkLocation_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkLocation_description),
                        icon = Icons.Default.LocationOn,
                        checked = settings.videoRecorderSettings.includeLocationInWatermark,
                        onToggle = {
                            updateWatermark {
                                it.setIncludeLocationInWatermark(!it.includeLocationInWatermark)
                            }
                        },
                    )

                    WatermarkOptionSwitch(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkGyroscope_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkGyroscope_description),
                        icon = Icons.Default.ScreenRotation,
                        checked = settings.videoRecorderSettings.includeGyroscopeInWatermark,
                        onToggle = {
                            updateWatermark {
                                it.setIncludeGyroscopeInWatermark(!it.includeGyroscopeInWatermark)
                            }
                        },
                    )

                    WatermarkOptionSwitch(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkAccelerometer_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkAccelerometer_description),
                        icon = Icons.Default.Speed,
                        checked = settings.videoRecorderSettings.includeAccelerometerInWatermark,
                        onToggle = {
                            updateWatermark {
                                it.setIncludeAccelerometerInWatermark(!it.includeAccelerometerInWatermark)
                            }
                        },
                    )

                    WatermarkOptionSwitch(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkOrientation_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkOrientation_description),
                        icon = Icons.Default.Explore,
                        checked = settings.videoRecorderSettings.includeOrientationInWatermark,
                        onToggle = {
                            updateWatermark {
                                it.setIncludeOrientationInWatermark(!it.includeOrientationInWatermark)
                            }
                        },
                    )

                    WatermarkOptionSwitch(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkBarometer_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkBarometer_description),
                        icon = Icons.Default.Compress,
                        checked = settings.videoRecorderSettings.includeBarometerInWatermark,
                        onToggle = {
                            updateWatermark {
                                it.setIncludeBarometerInWatermark(!it.includeBarometerInWatermark)
                            }
                        },
                    )

                    WatermarkOptionSwitch(
                        title = stringResource(R.string.ui_settings_option_videoWatermarkLinearAcceleration_title),
                        description = stringResource(R.string.ui_settings_option_videoWatermarkLinearAcceleration_description),
                        icon = Icons.Default.DirectionsRun,
                        checked = settings.videoRecorderSettings.includeLinearAccelerationInWatermark,
                        onToggle = {
                            updateWatermark {
                                it.setIncludeLinearAccelerationInWatermark(
                                    !it.includeLinearAccelerationInWatermark
                                )
                            }
                        },
                        showHint = true,
                    )
                }
            }
        },
    )
}

@Composable
private fun WatermarkOptionSwitch(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit,
    showHint: Boolean = false,
) {
    SettingsTile(
        title = title,
        description = description,
        leading = {
            Icon(
                icon,
                contentDescription = null,
            )
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
            )
        },
        tertiaryLine = {
            if (showHint) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.ui_settings_option_videoWatermark_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
