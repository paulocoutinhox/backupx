package com.backupx.app.helper

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Opens the operating system's native open dialog
 * File selection uses the native file dialog on every platform, folder selection is resolved per platform
 */
object NativeDialogHelper {

    private val osName = System.getProperty("os.name").lowercase()
    private val isWindows = osName.contains("win")
    private val isMacOS = osName.contains("mac") || osName.contains("darwin")

    fun openFile(title: String): String? {
        return awtDialog(title, directories = false)
    }

    fun openFolder(title: String): String? {
        // only macOS exposes a folder mode in the awt dialog, the others use their native folder chooser
        return when {
            isMacOS -> awtDialog(title, directories = true)
            isWindows -> WindowsHelper.pickFolder(title)
            else -> UnixHelper.pickFolder(title)
        }
    }

    private fun awtDialog(title: String, directories: Boolean): String? {
        // on macOS the native panel only selects folders when this property is enabled
        val previous = System.getProperty("apple.awt.fileDialogForDirectories")
        if (isMacOS) System.setProperty("apple.awt.fileDialogForDirectories", directories.toString())

        try {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            dialog.isMultipleMode = false
            dialog.isVisible = true

            val directory = dialog.directory ?: return null
            val file = dialog.file ?: return null
            return File(directory, file).absolutePath
        } finally {
            if (isMacOS) {
                if (previous == null) {
                    System.clearProperty("apple.awt.fileDialogForDirectories")
                } else {
                    System.setProperty("apple.awt.fileDialogForDirectories", previous)
                }
            }
        }
    }
}
