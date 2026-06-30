package com.backupx.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.backupx.app.helper.NativeDialogHelper
import com.backupx.app.model.BackupItem
import com.backupx.app.model.BackupProviderType
import com.backupx.app.model.FileSystemSettings
import com.backupx.app.model.ProviderSettings
import com.backupx.app.model.S3Settings
import com.backupx.app.provider.ConnectionTestResult
import com.backupx.composeapp.generated.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupEditorDialog(
    item: BackupItem?,
    newId: () -> String,
    loadSecret: (String) -> String?,
    onTest: suspend (S3Settings, String) -> ConnectionTestResult,
    onSave: (BackupItem, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var sourcePath by remember { mutableStateOf(item?.sourcePath ?: "") }
    var provider by remember { mutableStateOf(item?.settings?.type ?: BackupProviderType.filesystem) }

    // the options follow the selected provider, so switching it swaps the fields
    var settings by remember(provider) { mutableStateOf(defaultSettingsFor(provider, item)) }

    // the secret comes from the system store when editing an existing object storage backup
    var secret by remember(provider) { mutableStateOf("") }
    LaunchedEffect(item?.id, provider) {
        secret = if (item != null && provider == BackupProviderType.s3) {
            withContext(Dispatchers.IO) { loadSecret(item.id) ?: "" }
        } else {
            ""
        }
    }

    val secretRequired = provider == BackupProviderType.s3
    val isValid = name.isNotBlank() && sourcePath.isNotBlank() && isSettingsValid(settings) &&
        (!secretRequired || secret.isNotBlank())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(540.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(if (item == null) Res.string.editor_new else Res.string.editor_edit),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProviderSelector(selected = provider, onSelect = { provider = it })

                Spacer(modifier = Modifier.height(16.dp))

                // common to every provider: what should be backed up
                val pickFileTitle = stringResource(Res.string.dialog_pick_source_file)
                val pickSourceFolderTitle = stringResource(Res.string.dialog_pick_source_folder)
                PathPicker(
                    label = stringResource(Res.string.label_source),
                    value = sourcePath,
                    placeholder = stringResource(Res.string.placeholder_source)
                ) {
                    BrowseButton(label = stringResource(Res.string.browse_file), icon = Icons.AutoMirrored.Filled.InsertDriveFile) {
                        NativeDialogHelper.openFile(pickFileTitle)?.let { sourcePath = it }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    BrowseButton(label = stringResource(Res.string.browse_folder), icon = Icons.Default.FolderOpen) {
                        NativeDialogHelper.openFolder(pickSourceFolderTitle)?.let { sourcePath = it }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // everything below this divider belongs to the selected provider
                ProviderSectionHeader(provider)

                Spacer(modifier = Modifier.height(16.dp))

                ProviderOptions(
                    settings = settings,
                    secret = secret,
                    onChange = { settings = it },
                    onSecretChange = { secret = it },
                    onTest = onTest
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            onSave(
                                BackupItem(
                                    id = item?.id ?: newId(),
                                    name = name.trim(),
                                    sourcePath = sourcePath.trim(),
                                    settings = settings
                                ),
                                if (secretRequired) secret else null
                            )
                        },
                        enabled = isValid
                    ) {
                        Text(stringResource(Res.string.action_save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSelector(selected: BackupProviderType, onSelect: (BackupProviderType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = providerLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.field_provider)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BackupProviderType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(providerLabel(type)) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ProviderSectionHeader(provider: BackupProviderType) {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
    Spacer(modifier = Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = providerIcon(provider),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatResource(Res.string.provider_options, providerLabel(provider)),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun ProviderOptions(
    settings: ProviderSettings,
    secret: String,
    onChange: (ProviderSettings) -> Unit,
    onSecretChange: (String) -> Unit,
    onTest: suspend (S3Settings, String) -> ConnectionTestResult
) {
    when (settings) {
        is FileSystemSettings -> {
            val pickDestinationTitle = stringResource(Res.string.dialog_pick_destination)
            PathPicker(
                label = stringResource(Res.string.label_destination),
                value = settings.destinationPath,
                placeholder = stringResource(Res.string.placeholder_destination)
            ) {
                BrowseButton(label = stringResource(Res.string.action_browse), icon = Icons.Default.FolderOpen) {
                    NativeDialogHelper.openFolder(pickDestinationTitle)?.let {
                        onChange(settings.copy(destinationPath = it))
                    }
                }
            }
        }

        is S3Settings -> S3Options(
            settings = settings,
            secret = secret,
            onChange = onChange,
            onSecretChange = onSecretChange,
            onTest = onTest
        )
    }
}

@Composable
private fun S3Options(
    settings: S3Settings,
    secret: String,
    onChange: (S3Settings) -> Unit,
    onSecretChange: (String) -> Unit,
    onTest: suspend (S3Settings, String) -> ConnectionTestResult
) {
    val scope = rememberCoroutineScope()
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ConnectionTestResult?>(null) }

    val canTest = settings.bucket.isNotBlank() && settings.region.isNotBlank() &&
        settings.accessKeyId.isNotBlank() && secret.isNotBlank() && !testing

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = settings.bucket,
            onValueChange = { onChange(settings.copy(bucket = it)) },
            label = { Text(stringResource(Res.string.field_bucket)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = settings.basePath,
            onValueChange = { onChange(settings.copy(basePath = it)) },
            label = { Text(stringResource(Res.string.field_base_path)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = settings.region,
            onValueChange = { onChange(settings.copy(region = it)) },
            label = { Text(stringResource(Res.string.field_region)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = settings.accessKeyId,
            onValueChange = { onChange(settings.copy(accessKeyId = it)) },
            label = { Text(stringResource(Res.string.field_access_key)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = secret,
            onValueChange = onSecretChange,
            label = { Text(stringResource(Res.string.field_secret_key)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = settings.endpoint,
            onValueChange = { onChange(settings.copy(endpoint = it)) },
            label = { Text(stringResource(Res.string.field_endpoint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val successText = stringResource(Res.string.test_success)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        testing = true
                        result = onTest(settings, secret)
                        testing = false
                    }
                },
                enabled = canTest
            ) {
                if (testing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.action_test))
                }
            }

            result?.let {
                Spacer(modifier = Modifier.width(12.dp))
                val color = if (it.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                Icon(
                    imageVector = if (it.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (it.success) successText else it.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun PathPicker(
    label: String,
    value: String,
    placeholder: String,
    actions: @Composable RowScope.() -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank()) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            actions()
        }
    }
}

@Composable
private fun BrowseButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

private fun defaultSettingsFor(provider: BackupProviderType, item: BackupItem?): ProviderSettings {
    // keep the saved options when editing the same provider, otherwise start clean
    if (item != null && item.settings.type == provider) return item.settings

    return when (provider) {
        BackupProviderType.filesystem -> FileSystemSettings()
        BackupProviderType.s3 -> S3Settings()
    }
}

private fun isSettingsValid(settings: ProviderSettings): Boolean {
    return when (settings) {
        is FileSystemSettings -> settings.destinationPath.isNotBlank()
        is S3Settings -> settings.bucket.isNotBlank() && settings.region.isNotBlank() && settings.accessKeyId.isNotBlank()
    }
}
