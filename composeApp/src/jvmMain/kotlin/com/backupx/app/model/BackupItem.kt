package com.backupx.app.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupItem(
    val id: String,
    val name: String,
    val sourcePath: String,
    val settings: ProviderSettings
)
