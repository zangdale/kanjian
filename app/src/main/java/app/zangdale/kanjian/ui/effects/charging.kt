package app.zangdale.kanjian.ui.effects

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

fun Context.isChargingCableConnected(): Boolean {
    val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
            plugged == BatteryManager.BATTERY_PLUGGED_USB
}

@Composable
fun rememberIsChargingCableConnected(): State<Boolean> {
    val context = LocalContext.current
    val isConnected = remember {
        mutableStateOf(context.isChargingCableConnected())
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                isConnected.value = context.isChargingCableConnected()
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        isConnected.value = context.isChargingCableConnected()

        onDispose {
            runCatching {
                context.unregisterReceiver(receiver)
            }
        }
    }

    return isConnected
}
