package com.backupx.app.repository

import com.backupx.app.model.BackupConfig
import kotlinx.serialization.json.Json
import java.io.File

class BackupRepositoryImpl : BackupRepository {

    private val configFile = File(System.getProperty("user.home"), ".backupx/config.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun load(): BackupConfig {
        if (!configFile.exists()) return BackupConfig()
        return json.decodeFromString(BackupConfig.serializer(), configFile.readText())
    }

    override fun save(config: BackupConfig) {
        configFile.parentFile?.mkdirs()
        configFile.writeText(json.encodeToString(BackupConfig.serializer(), config))
    }
}
