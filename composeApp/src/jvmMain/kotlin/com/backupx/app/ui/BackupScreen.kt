package com.backupx.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.backupx.app.helper.FileHelper
import com.backupx.app.model.BackupItem
import com.backupx.app.model.BackupProviderType
import com.backupx.app.model.FileSystemSettings
import com.backupx.app.model.S3Settings
import com.backupx.app.viewmodel.BackupRunStatus
import com.backupx.app.viewmodel.BackupViewModel
import com.backupx.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupScreen(viewModel: BackupViewModel) {
    val items by viewModel.items.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val isRunningAll by viewModel.isRunningAll.collectAsState()

    var editorItem by remember { mutableStateOf<BackupItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRunAllDialog by remember { mutableStateOf(false) }

    val selectedItem = items.find { it.id == selectedId }
    val anyRunning = isRunningAll || statuses.values.any { it is BackupRunStatus.Running }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                count = items.size,
                hasSelection = selectedItem != null,
                canRunAll = items.isNotEmpty() && !anyRunning,
                onRunAll = { showRunAllDialog = true },
                onAdd = {
                    editorItem = null
                    showEditor = true
                },
                onEdit = {
                    editorItem = selectedItem
                    showEditor = true
                },
                onDelete = { showDeleteDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (items.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        BackupCard(
                            item = item,
                            selected = item.id == selectedId,
                            status = statuses[item.id] ?: BackupRunStatus.Idle,
                            enabled = !isRunningAll,
                            onSelect = { viewModel.selectItem(item.id) },
                            onRun = { viewModel.runItem(item.id) },
                            onOpenDestination = { (item.settings as? FileSystemSettings)?.let { openDestination(it) } }
                        )
                    }
                }
            }
        }

        if (showEditor) {
            BackupEditorDialog(
                item = editorItem,
                newId = { viewModel.newItemId() },
                loadSecret = { viewModel.loadSecret(it) },
                onTest = { settings, secret -> viewModel.testS3(settings, secret) },
                onSave = { item, secret ->
                    viewModel.saveItem(item, secret)
                    showEditor = false
                },
                onDismiss = { showEditor = false }
            )
        }

        if (showDeleteDialog && selectedItem != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(Res.string.dialog_delete_title)) },
                text = { Text(formatResource(Res.string.dialog_delete_message, selectedItem.name)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteItem(selectedItem.id)
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(Res.string.action_delete))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                }
            )
        }

        if (showRunAllDialog) {
            AlertDialog(
                onDismissRequest = { showRunAllDialog = false },
                title = { Text(stringResource(Res.string.dialog_run_all_title)) },
                text = { Text(stringResource(Res.string.dialog_run_all_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.runAll()
                            showRunAllDialog = false
                        }
                    ) {
                        Text(stringResource(Res.string.action_run))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRunAllDialog = false }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun Header(
    count: Int,
    hasSelection: Boolean,
    canRunAll: Boolean,
    onRunAll: () -> Unit,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(Res.string.title_backups),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = formatResource(Res.string.configured_count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onEdit, enabled = hasSelection) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.action_edit))
            }

            OutlinedButton(
                onClick = onDelete,
                enabled = hasSelection,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.action_delete))
            }

            OutlinedButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.action_add))
            }

            Button(onClick = onRunAll, enabled = canRunAll) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.action_run_all))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun BackupCard(
    item: BackupItem,
    selected: Boolean,
    status: BackupRunStatus,
    enabled: Boolean,
    onSelect: () -> Unit,
    onRun: () -> Unit,
    onOpenDestination: () -> Unit
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ProviderBadge(item.settings.type)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LabeledPath(label = stringResource(Res.string.label_source), value = item.sourcePath)
                    Spacer(modifier = Modifier.height(4.dp))
                    LabeledPath(label = stringResource(Res.string.label_destination), value = destinationOf(item))
                }

                TooltipIconButton(
                    tooltip = stringResource(Res.string.action_run),
                    enabled = enabled && status !is BackupRunStatus.Running,
                    onClick = onRun
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(Res.string.action_run),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                // opening a local folder only makes sense for the file system provider
                if (item.settings is FileSystemSettings) {
                    TooltipIconButton(
                        tooltip = stringResource(Res.string.cd_open_destination),
                        onClick = onOpenDestination
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = stringResource(Res.string.cd_open_destination))
                    }
                }
            }

            StatusRow(status)
        }
    }
}

@Composable
private fun StatusRow(status: BackupRunStatus) {
    when (status) {
        is BackupRunStatus.Idle -> Unit

        is BackupRunStatus.Running -> {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatResource(Res.string.status_backing_up, (status.progress * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        is BackupRunStatus.Success -> {
            Spacer(modifier = Modifier.height(12.dp))
            StatusLine(
                icon = Icons.Default.CheckCircle,
                text = stringResource(Res.string.status_completed),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        is BackupRunStatus.Failed -> {
            Spacer(modifier = Modifier.height(12.dp))
            StatusLine(
                icon = Icons.Default.Error,
                text = status.message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun LabeledPath(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(86.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProviderBadge(type: BackupProviderType) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = providerIcon(type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = providerShortLabel(type),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun StatusLine(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick, enabled = enabled) { content() }
    }
}

private fun destinationOf(item: BackupItem): String {
    return when (val settings = item.settings) {
        is FileSystemSettings -> settings.destinationPath
        is S3Settings -> {
            val base = settings.basePath.trim().trim('/')
            if (base.isEmpty()) "s3://${settings.bucket}" else "s3://${settings.bucket}/$base"
        }
    }
}

private fun openDestination(settings: FileSystemSettings) {
    FileHelper.openInFileManager(settings.destinationPath)
}
