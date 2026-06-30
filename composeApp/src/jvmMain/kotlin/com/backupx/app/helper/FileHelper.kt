package com.backupx.app.helper

import java.io.File

/**
 * Generic file helper that coordinates file operations across different operating systems
 * Delegates OS-specific operations to appropriate helpers
 */
object FileHelper {

    private val osName = System.getProperty("os.name").lowercase()
    private val isWindows = osName.contains("win")
    private val isMacOS = osName.contains("mac") || osName.contains("darwin")
    private val isUnix = osName.contains("nix") || osName.contains("nux") || isMacOS

    /**
     * Expand a leading tilde (~) in a path to the user's home directory
     * Only the home shortcut is expanded, tildes elsewhere in the path are left untouched
     */
    fun expandPath(path: String): String {
        val home = System.getProperty("user.home")
        return when {
            path == "~" -> home
            path.startsWith("~/") -> home + path.substring(1)
            else -> path
        }
    }

    /**
     * Format byte size to human-readable format (B, KB, MB, GB, TB)
     */
    fun formatSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }

    /**
     * Ensure a directory exists, creating the full path when missing
     */
    fun ensureDirectory(directory: File) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Unable to create directory: ${directory.absolutePath}")
        }
    }

    /**
     * Copy a file or directory into the target path, replacing any existing content
     * Reports progress between 0 and 1 as each file is copied
     */
    fun copyReplacing(source: File, target: File, onProgress: (Float) -> Unit) {
        if (target.exists()) target.deleteRecursively()

        if (source.isFile) {
            target.parentFile?.let { ensureDirectory(it) }
            source.copyTo(target, overwrite = true)
            onProgress(1f)
            return
        }

        // walk the tree once so progress reflects the real file count
        val files = source.walkTopDown().filter { it.isFile }.toList()
        val total = files.size.coerceAtLeast(1)
        ensureDirectory(target)

        files.forEachIndexed { index, file ->
            val relative = file.relativeTo(source).path
            val destination = File(target, relative)
            destination.parentFile?.let { ensureDirectory(it) }
            file.copyTo(destination, overwrite = true)
            onProgress((index + 1).toFloat() / total)
        }

        onProgress(1f)
    }

    /**
     * Open a file or folder in the system's default file manager
     * Delegates to OS-specific helpers
     */
    fun openInFileManager(path: String) {
        val expandedPath = expandPath(path)

        when {
            isMacOS -> MacosHelper.openInFinder(expandedPath)
            isWindows -> WindowsHelper.openInExplorer(expandedPath)
            isUnix -> UnixHelper.openInFileManager(expandedPath)
            else -> println("Unsupported OS for opening file manager")
        }
    }
}
