package app.zangdale.kanjian.ui.components.RecorderScreen.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicExternalOn
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.zangdale.kanjian.ui.utils.MicrophoneInfo

@Composable
fun MicrophoneTypeInfo(
    modifier: Modifier = Modifier,
    type: MicrophoneInfo.MicrophoneType,
) {
    Icon(
        imageVector = when (type) {
            MicrophoneInfo.MicrophoneType.BLUETOOTH -> Icons.Filled.BluetoothAudio
            MicrophoneInfo.MicrophoneType.WIRED -> Icons.Filled.MicExternalOn
            MicrophoneInfo.MicrophoneType.PHONE -> Icons.Filled.Smartphone
            MicrophoneInfo.MicrophoneType.OTHER -> Icons.Filled.Mic
        },
        modifier = modifier,
        contentDescription = null,
    )
}
