package com.backupx.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.backupx.app.model.BackupProviderType
import com.backupx.composeapp.generated.resources.Res
import com.backupx.composeapp.generated.resources.provider_filesystem
import com.backupx.composeapp.generated.resources.provider_s3
import org.jetbrains.compose.resources.stringResource

@Composable
fun providerLabel(type: BackupProviderType): String {
    return when (type) {
        BackupProviderType.filesystem -> stringResource(Res.string.provider_filesystem)
        BackupProviderType.s3 -> stringResource(Res.string.provider_s3)
    }
}

fun providerIcon(type: BackupProviderType): ImageVector {
    return when (type) {
        BackupProviderType.filesystem -> Icons.Default.Folder
        BackupProviderType.s3 -> Icons.Default.Cloud
    }
}

fun providerShortLabel(type: BackupProviderType): String {
    return when (type) {
        BackupProviderType.filesystem -> "FS"
        BackupProviderType.s3 -> "S3"
    }
}
