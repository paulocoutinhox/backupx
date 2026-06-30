package com.backupx.app.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface ProviderSettings {
    val type: BackupProviderType
}
