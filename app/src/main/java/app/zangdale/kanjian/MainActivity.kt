package app.zangdale.kanjian

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import app.zangdale.kanjian.ui.components.RecorderScreen.organisms.RecorderMediaButtonDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.datastore.dataStore
import app.zangdale.kanjian.db.AppSettingsSerializer
import app.zangdale.kanjian.ui.AsLockedApp
import app.zangdale.kanjian.ui.LockedAppHandlers
import app.zangdale.kanjian.ui.Navigation
import app.zangdale.kanjian.ui.theme.KanjianTheme

const val SETTINGS_FILE = "settings.json"
val Context.dataStore by dataStore(
    fileName = SETTINGS_FILE,
    serializer = AppSettingsSerializer()
)

class MainActivity : AppCompatActivity() {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (RecorderMediaButtonDispatcher.dispatchKeyEvent(event)) {
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            KanjianTheme {
                LockedAppHandlers()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background
                        )
                ) {
                    AsLockedApp {
                        Navigation()
                    }
                }
            }
        }
    }
}
