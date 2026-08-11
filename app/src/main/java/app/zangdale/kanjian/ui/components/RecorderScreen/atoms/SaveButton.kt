package app.zangdale.kanjian.ui.components.RecorderScreen.atoms

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SaveButton(
    modifier: Modifier = Modifier,
    onSave: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val label = stringResource(R.string.ui_recorder_action_save_label)
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .size(64.dp)
            .semantics {
                contentDescription = label
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onSave,
                onLongClick = onLongClick,
            )
            .then(modifier),
        shape = FloatingActionButtonDefaults.shape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
