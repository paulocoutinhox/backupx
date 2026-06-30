package com.backupx.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Settings for the object storage provider
 * The secret key is never stored here, it lives in the operating system secret store keyed by the backup id
 * An empty endpoint means the default endpoint is derived from the region
 */
@Serializable
@SerialName("s3")
data class S3Settings(
    val bucket: String = "",
    val basePath: String = "",
    val region: String = "us-east-1",
    val accessKeyId: String = "",
    val endpoint: String = ""
) : ProviderSettings {
    override val type: BackupProviderType get() = BackupProviderType.s3
}
