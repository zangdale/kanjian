package app.zangdale.kanjian.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import app.zangdale.kanjian.dataStore
import app.zangdale.kanjian.db.AppSettings

@Composable
fun rememberSettings(): AppSettings {
    return LocalContext.current.dataStore.data.collectAsState(initial = AppSettings.getDefaultInstance()).value
}
