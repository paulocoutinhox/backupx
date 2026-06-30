package com.backupx.app.provider

import com.backupx.app.helper.BookmarkHelper
import com.backupx.app.helper.FileHelper
import com.backupx.app.model.BackupItem
import com.backupx.app.model.BackupProviderType
import com.backupx.app.model.FileSystemSettings
import com.backupx.composeapp.generated.resources.Res
import com.backupx.composeapp.generated.resources.error_create_dir
import com.backupx.composeapp.generated.resources.error_destination_inside_source
import com.backupx.composeapp.generated.resources.error_destination_not_set
import com.backupx.composeapp.generated.resources.error_same_path
import com.backupx.composeapp.generated.resources.error_source_missing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Backup provider that copies a local file or folder into a destination directory
 * The destination entry is replaced on every run so it mirrors the current source
 */
class FileSystemBackupProvider : BackupProvider {

    override val type = BackupProviderType.filesystem

    override suspend fun execute(item: BackupItem, onProgress: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            val settings = item.settings as FileSystemSettings

            // resolve the macOS bookmarks so saved paths stay accessible under the sandbox
            val sourcePath = BookmarkHelper.resolve(item.sourceBookmark, item.sourcePath)
            val destinationPath = BookmarkHelper.resolve(settings.destinationBookmark, settings.destinationPath)

            // validate inputs before touching the disk
            val source = File(FileHelper.expandPath(sourcePath))
            if (item.sourcePath.isBlank() || !source.exists()) throw BackupException(Res.string.error_source_missing)
            if (settings.destinationPath.isBlank()) throw BackupException(Res.string.error_destination_not_set)

            // make sure the destination folder is ready to receive the copy
            val destinationRoot = File(FileHelper.expandPath(destinationPath))
            if (!destinationRoot.exists() && !destinationRoot.mkdirs()) throw BackupException(Res.string.error_create_dir)

            // replace the matching entry inside the destination with the current source
            val target = File(destinationRoot, source.name)

            // guard against copying a folder into itself, which would recurse and destroy data
            val sourceCanonical = source.canonicalFile.toPath()
            val targetCanonical = target.canonicalFile.toPath()
            if (sourceCanonical == targetCanonical) throw BackupException(Res.string.error_same_path)
            if (targetCanonical.startsWith(sourceCanonical)) throw BackupException(Res.string.error_destination_inside_source)

            FileHelper.copyReplacing(source, target, onProgress)
        }
    }
}
