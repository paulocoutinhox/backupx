package com.backupx.app.provider

import com.backupx.app.model.BackupItem
import com.backupx.app.model.BackupProviderType

/**
 * Contract implemented by every backup destination
 * Each provider knows how to persist a backup item and reports its progress
 */
interface BackupProvider {

    val type: BackupProviderType

    /**
     * Run the backup for the given item, reporting progress between 0 and 1
     * Throws when the backup cannot be completed so the caller can surface the reason
     */
    suspend fun execute(item: BackupItem, onProgress: (Float) -> Unit)
}
