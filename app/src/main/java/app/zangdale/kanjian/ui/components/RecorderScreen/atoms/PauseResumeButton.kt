package app.zangdale.kanjian.ui.components.RecorderScreen.atoms

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R

@Composable
fun PauseResumeButton(
    modifier: Modifier = Modifier,
    isPaused: Boolean,
    onChange: () -> Unit,
) {
    val pauseLabel = stringResource(R.string.ui_recorder_action_pause_label)
    val resumeLabel = stringResource(R.string.ui_recorder_action_resume_label)

    FloatingActionButton(
        modifier = Modifier
            .size(64.dp)
            .semantics {
                contentDescription = if (isPaused) resumeLabel else pauseLabel
            }
            .then(modifier),
        onClick = onChange,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
        ),
    ) {
        Icon(
            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
    }
}
