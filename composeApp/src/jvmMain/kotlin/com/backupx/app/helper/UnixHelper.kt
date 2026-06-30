package com.backupx.app.helper

/**
 * Unix/Linux-specific helper functions
 */
object UnixHelper {

    private const val SERVICE = "BackupX"

    /**
     * Open a file or folder in the default file manager (Linux)
     * Uses xdg-open which works across most Linux desktop environments
     */
    fun openInFileManager(path: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("xdg-open", path))
        } catch (e: Exception) {
            println("Error opening in file manager: ${e.message}")
        }
    }

    /**
     * Show the native folder chooser of the running desktop and return the selected path
     * Tries the common GTK and KDE dialogs, returns null when none is available or the user cancels
     */
    fun pickFolder(title: String): String? {
        val home = System.getProperty("user.home")
        val gtk = runFolderChooser(listOf("zenity", "--file-selection", "--directory", "--title=$title"))
        if (gtk != null) return gtk.ifBlank { null }

        val kde = runFolderChooser(listOf("kdialog", "--getexistingdirectory", home))
        if (kde != null) return kde.ifBlank { null }

        return null
    }

    private fun runFolderChooser(command: List<String>): String? {
        return try {
            val process = ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

            // read stdout before waitFor to prevent buffer deadlock
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            // an empty string still means the tool ran and the user cancelled
            output
        } catch (e: Exception) {
            // the tool is not installed, let the caller try the next one
            null
        }
    }

    /**
     * Store a secret in the desktop secret service, reading the value from stdin to avoid exposing it
     */
    fun storeSecret(account: String, secret: String): Boolean {
        return try {
            val process = ProcessBuilder("secret-tool", "store", "--label=$SERVICE", "service", SERVICE, "account", account)
                .redirectErrorStream(true)
                .start()
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(secret) }
            process.inputStream.bufferedReader().readText()
            process.waitFor() == 0
        } catch (e: Exception) {
            println("Error storing secret: ${e.message}")
            false
        }
    }

    /**
     * Read a secret from the desktop secret service, returning null when it is absent
     */
    fun loadSecret(account: String): String? {
        return try {
            val process = ProcessBuilder("secret-tool", "lookup", "service", SERVICE, "account", account).start()
            val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText().trim()
            if (process.waitFor() == 0 && output.isNotEmpty()) output else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Remove a secret from the desktop secret service
     */
    fun deleteSecret(account: String) {
        try {
            ProcessBuilder("secret-tool", "clear", "service", SERVICE, "account", account).start().waitFor()
        } catch (e: Exception) {
            println("Error deleting secret: ${e.message}")
        }
    }
}
