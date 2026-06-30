package com.backupx.app.provider

import com.backupx.app.model.BackupProviderType

/**
 * Resolves the concrete provider responsible for a given provider type
 * New providers are registered here as they become available
 */
class BackupProviderFactory {

    private val providers: List<BackupProvider> = listOf(
        FileSystemBackupProvider(),
        S3BackupProvider()
    )

    fun resolve(type: BackupProviderType): BackupProvider {
        return providers.first { it.type == type }
    }
}
