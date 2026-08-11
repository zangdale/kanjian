package app.zangdale.kanjian.ui.components.RecorderScreen.atoms

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.zangdale.kanjian.R

@Composable
fun DeleteButton(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmDeletionDialog(
            onDismiss = {
                showDeleteDialog = false
            },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
        )
    }
    val label = stringResource(R.string.ui_recorder_action_delete_label)
    FloatingActionButton(
        modifier = Modifier
            .size(64.dp)
            .semantics {
                contentDescription = label
            }
            .then(modifier),
        onClick = {
            showDeleteDialog = true
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
        ),
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
    }
}
