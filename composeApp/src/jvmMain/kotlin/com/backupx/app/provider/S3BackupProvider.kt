package com.backupx.app.provider

import com.backupx.app.helper.BookmarkHelper
import com.backupx.app.helper.FileHelper
import com.backupx.app.helper.SecretStoreHelper
import com.backupx.app.model.BackupItem
import com.backupx.app.model.BackupProviderType
import com.backupx.app.model.S3Settings
import com.backupx.composeapp.generated.resources.Res
import com.backupx.composeapp.generated.resources.error_bucket_not_set
import com.backupx.composeapp.generated.resources.error_secret_missing
import com.backupx.composeapp.generated.resources.error_source_missing
import com.backupx.composeapp.generated.resources.test_bucket_not_found
import com.backupx.composeapp.generated.resources.test_failed
import io.minio.BucketExistsArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Backup provider that uploads a local file or folder to an object storage bucket
 * Each run is written under a new timestamped folder so previous backups are preserved
 */
class S3BackupProvider : BackupProvider {

    override val type = BackupProviderType.s3

    private val timestampFormat = DateTimeFormatter
        .ofPattern("yyyy-MM-dd-HH-mm-ss")
        .withZone(ZoneOffset.UTC)

    override suspend fun execute(item: BackupItem, onProgress: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            val settings = item.settings as S3Settings

            // validate inputs before opening a connection
            val sourcePath = BookmarkHelper.resolve(item.sourceBookmark, item.sourcePath)
            val source = File(FileHelper.expandPath(sourcePath))
            if (item.sourcePath.isBlank() || !source.exists()) throw BackupException(Res.string.error_source_missing)
            if (settings.bucket.isBlank()) throw BackupException(Res.string.error_bucket_not_set)

            // the secret never lives in the config, it is read from the system secret store
            val secret = SecretStoreHelper.load(item.id)
                ?: throw BackupException(Res.string.error_secret_missing)

            val client = buildClient(settings, secret)

            // every object of this run shares the same timestamped prefix
            val prefix = buildPrefix(settings.basePath, timestampFormat.format(Instant.now()))
            uploadSource(client, settings.bucket, prefix, source, onProgress)
        }
    }

    suspend fun test(settings: S3Settings, secret: String): ConnectionTestResult {
        if (settings.bucket.isBlank()) return ConnectionTestResult(false, getString(Res.string.error_bucket_not_set))
        return withContext(Dispatchers.IO) {
            try {
                val client = buildClient(settings, secret)
                val exists = client.bucketExists(BucketExistsArgs.builder().bucket(settings.bucket).build())
                // the success text is shown by the caller, only the failure detail is carried here
                if (exists) {
                    ConnectionTestResult(true, "")
                } else {
                    ConnectionTestResult(false, getString(Res.string.test_bucket_not_found))
                }
            } catch (e: Exception) {
                ConnectionTestResult(false, e.message ?: getString(Res.string.test_failed))
            }
        }
    }

    private fun buildClient(settings: S3Settings, secret: String): MinioClient {
        val endpoint = resolveEndpoint(settings)
        return MinioClient.builder()
            .endpoint(endpoint)
            .region(settings.region)
            .credentials(settings.accessKeyId, secret)
            .build()
    }

    private fun resolveEndpoint(settings: S3Settings): String {
        if (settings.endpoint.isBlank()) {
            return "https://s3.${settings.region}.amazonaws.com"
        }
        if (settings.endpoint.startsWith("http://") || settings.endpoint.startsWith("https://")) {
            return settings.endpoint
        }
        return "https://${settings.endpoint}"
    }

    private fun uploadSource(client: MinioClient, bucket: String, prefix: String, source: File, onProgress: (Float) -> Unit) {
        if (source.isFile) {
            uploadFile(client, bucket, "$prefix/${source.name}", source)
            onProgress(1f)
            return
        }

        // walk the tree once so progress reflects the real file count
        val files = source.walkTopDown().filter { it.isFile }.toList()
        val total = files.size.coerceAtLeast(1)

        files.forEachIndexed { index, file ->
            val relative = file.relativeTo(source).path.replace(File.separatorChar, '/')
            uploadFile(client, bucket, "$prefix/${source.name}/$relative", file)
            onProgress((index + 1).toFloat() / total)
        }

        onProgress(1f)
    }

    private fun uploadFile(client: MinioClient, bucket: String, key: String, file: File) {
        file.inputStream().use { stream ->
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(key)
                    .stream(stream, file.length(), -1)
                    .build()
            )
        }
    }

    private fun buildPrefix(basePath: String, timestamp: String): String {
        val base = basePath.trim().trim('/')
        return if (base.isEmpty()) timestamp else "$base/$timestamp"
    }
}
