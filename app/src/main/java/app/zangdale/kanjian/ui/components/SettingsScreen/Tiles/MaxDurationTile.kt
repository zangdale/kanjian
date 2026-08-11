package app.zangdale.kanjian.ui.components.SettingsScreen.Tiles

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import app.zangdale.kanjian.R
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.db.AudioRecorderSettings
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxDurationTile(
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val showDialog = rememberUseCaseState()
    val dataStore = LocalContext.current.dataStore

    fun updateValue(maxSegments: Int) {
        scope.launch {
            dataStore.updateData {
                it.setMaxSegments(maxSegments)
            }
        }
    }

    val notNumberLabel = stringResource(R.string.form_error_type_notNumber)
    val notInRangeLabel = stringResource(R.string.form_error_value_notInRange, 1, 10000)
    InputDialog(
        state = showDialog,
        header = Header.Default(
            title = stringResource(R.string.ui_settings_option_maxDuration_title),
            icon = IconSource(
                painter = IconResource.fromImageVector(Icons.Default.FilterNone).asPainterResource(),
                contentDescription = null,
            )
        ),
        selection = InputSelection(
            input = listOf(
                InputTextField(
                    header = InputHeader(
                        title = stringResource(R.string.ui_settings_option_maxDuration_explanation),
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    type = InputTextFieldType.OUTLINED,
                    text = settings.maxSegments.toString(),
                    validationListener = { text ->
                        val segments = text?.toIntOrNull()

                        if (segments == null) {
                            return@InputTextField ValidationResult.Invalid(notNumberLabel)
                        }

                        if (segments !in 1..10_000) {
                            return@InputTextField ValidationResult.Invalid(notInRangeLabel)
                        }

                        ValidationResult.Valid
                    },
                    key = "maxSegments",
                )
            ),
        ) { result ->
            val segments = result.getString("maxSegments")?.toIntOrNull()
                ?: throw IllegalStateException("maxSegments is null")
            updateValue(segments)
        }
    )
    SettingsTile(
        title = stringResource(R.string.ui_settings_option_maxDuration_title),
        description = stringResource(R.string.ui_settings_option_maxDuration_description),
        leading = {
            Icon(
                Icons.Default.FilterNone,
                contentDescription = null,
            )
        },
        trailing = {
            Button(
                onClick = showDialog::show,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    stringResource(
                        R.string.ui_settings_option_maxDuration_value,
                        settings.maxSegments,
                    )
                )
            }
        },
        extra = {
            ExampleListRoulette(
                items = AudioRecorderSettings.EXAMPLE_MAX_SEGMENTS,
                onItemSelected = ::updateValue,
            ) { maxSegments ->
                Text(
                    stringResource(
                        R.string.ui_settings_option_maxDuration_value,
                        maxSegments,
                    )
                )
            }
        }
    )
}
