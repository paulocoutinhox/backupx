package com.backupx.app.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupConfig(
    val items: List<BackupItem> = emptyList()
)
