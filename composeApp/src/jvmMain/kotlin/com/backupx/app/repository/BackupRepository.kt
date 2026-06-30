package com.backupx.app.repository

import com.backupx.app.model.BackupConfig

interface BackupRepository {
    fun load(): BackupConfig
    fun save(config: BackupConfig)
}
