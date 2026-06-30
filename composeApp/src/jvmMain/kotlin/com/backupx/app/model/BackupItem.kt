package com.backupx.app.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupItem(
    val id: String,
    val name: String,
    val sourcePath: String,
    // macOS security-scoped bookmark for the source, used to keep access under the sandbox
    val sourceBookmark: String? = null,
    val settings: ProviderSettings
)
