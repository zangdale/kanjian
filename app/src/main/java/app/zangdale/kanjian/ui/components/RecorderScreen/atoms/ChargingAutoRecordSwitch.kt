package app.zangdale.kanjian.ui.components.RecorderScreen.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.db.AppSettings
import app.zangdale.kanjian.ui.components.atoms.GlobalSwitch
import kotlinx.coroutines.launch

@Composable
fun ChargingAutoRecordSwitch(
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val dataStore = LocalContext.current.dataStore

    Column(modifier = modifier) {
        GlobalSwitch(
            label = stringResource(R.string.ui_recorder_option_autoRecordOnCharge_title),
            checked = settings.autoRecordOnCharge,
            onCheckedChange = { enabled ->
                scope.launch {
                    dataStore.updateData {
                        it.setAutoRecordOnCharge(enabled)
                    }
                }
            },
        )
        Text(
            text = stringResource(R.string.ui_recorder_option_autoRecordOnCharge_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
