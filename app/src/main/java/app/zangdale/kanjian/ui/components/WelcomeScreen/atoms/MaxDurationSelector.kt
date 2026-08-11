package app.zangdale.kanjian.ui.components.WelcomeScreen.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.db.AudioRecorderSettings
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
fun MaxDurationSelector(
    modifier: Modifier = Modifier,
) {
    val OPTIONS = AudioRecorderSettings.EXAMPLE_MAX_SEGMENTS

    val scope = rememberCoroutineScope()
    val dataStore = LocalContext.current.dataStore

    var selectedSegments by rememberSaveable { mutableIntStateOf(15) }

    LaunchedEffect(selectedSegments) {
        scope.launch {
            dataStore.updateData {
                it.setMaxSegments(selectedSegments)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(modifier),
        verticalArrangement = Arrangement.Center,
    ) {
        for (segments in OPTIONS) {
            val label = stringResource(
                R.string.ui_settings_option_maxDuration_value,
                segments,
            )
            val a11yLabel = stringResource(
                R.string.a11y_selectValue,
                label
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .semantics {
                        contentDescription = a11yLabel
                    }
                    .clickable {
                        selectedSegments = segments
                    }
                    .padding(16.dp)
            ) {
                RadioButton(
                    selected = selectedSegments == segments,
                    onClick = { selectedSegments = segments },
                )
                Text(label)
            }
        }

        let {
            val showDialog = rememberUseCaseState()
            val label = stringResource(R.string.ui_welcome_timeSettings_values_custom)
            val selected = selectedSegments !in OPTIONS
            val notNumberLabel = stringResource(R.string.form_error_type_notNumber)
            val notInRangeLabel = stringResource(R.string.form_error_value_notInRange, 1, 10000)

            InputDialog(
                state = showDialog,
                header = Header.Default(
                    title = stringResource(R.string.ui_settings_option_maxDuration_title),
                    icon = IconSource(
                        painter = IconResource.fromImageVector(Icons.Default.FilterNone)
                            .asPainterResource(),
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
                            text = selectedSegments.toString(),
                            validationListener = { text ->
                                val value = text?.toIntOrNull()
                                if (value == null) {
                                    return@InputTextField ValidationResult.Invalid(notNumberLabel)
                                }
                                if (value !in 1..10_000) {
                                    return@InputTextField ValidationResult.Invalid(notInRangeLabel)
                                }
                                ValidationResult.Valid
                            },
                            key = "maxSegments",
                        )
                    ),
                ) { result ->
                    selectedSegments = result.getString("maxSegments")?.toIntOrNull() ?: selectedSegments
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = label
                    }
                    .clickable {
                        showDialog.show()
                    }
                    .clip(MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .padding(2.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else contentColorFor(
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                if (selected) {
                    Text(
                        text = stringResource(
                            R.string.ui_settings_option_maxDuration_value,
                            selectedSegments,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.ui_welcome_timeSettings_values_custom),
                    )
                }
            }
        }
    }
}
