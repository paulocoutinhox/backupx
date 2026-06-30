package com.backupx.app.viewmodel

sealed class BackupRunStatus {
    object Idle : BackupRunStatus()
    data class Running(val progress: Float) : BackupRunStatus()
    object Success : BackupRunStatus()
    data class Failed(val message: String) : BackupRunStatus()
}
