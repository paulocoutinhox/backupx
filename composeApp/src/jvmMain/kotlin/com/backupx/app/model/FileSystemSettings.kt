package com.backupx.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("filesystem")
data class FileSystemSettings(
    val destinationPath: String = "",
    // macOS security-scoped bookmark for the destination, used to keep access under the sandbox
    val destinationBookmark: String? = null
) : ProviderSettings {
    override val type: BackupProviderType get() = BackupProviderType.filesystem
}
