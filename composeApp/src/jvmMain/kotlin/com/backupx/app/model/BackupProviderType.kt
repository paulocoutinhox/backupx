package com.backupx.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class BackupProviderType {
    filesystem,
    s3
}
