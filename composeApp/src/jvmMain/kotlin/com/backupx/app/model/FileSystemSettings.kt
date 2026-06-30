package com.backupx.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("filesystem")
data class FileSystemSettings(
    val destinationPath: String = ""
) : ProviderSettings {
    override val type: BackupProviderType get() = BackupProviderType.filesystem
}
