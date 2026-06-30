package com.backupx.app.helper

import java.util.Base64

/**
 * MacOS-specific helper functions
 */
object MacosHelper {

    private const val SERVICE = "BackupX"

    /**
     * Open a file or folder in Finder (macOS)
     */
    fun openInFinder(path: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("open", path))
        } catch (e: Exception) {
            println("Error opening in Finder: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Store a secret in the login keychain, replacing any existing entry for the account
     */
    fun storeSecret(account: String, secret: String): Boolean {
        return try {
            // store base64 so the value stays ascii and the keychain returns it verbatim
            val encoded = Base64.getEncoder().encodeToString(secret.toByteArray(Charsets.UTF_8))
            // -U updates the item in place when the service and account already exist
            val process = ProcessBuilder("security", "add-generic-password", "-U", "-s", SERVICE, "-a", account, "-w", encoded)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().readText()
            process.waitFor() == 0
        } catch (e: Exception) {
            println("Error storing secret: ${e.message}")
            false
        }
    }

    /**
     * Read a secret from the login keychain, returning null when it is absent
     */
    fun loadSecret(account: String): String? {
        return try {
            val process = ProcessBuilder("security", "find-generic-password", "-s", SERVICE, "-a", account, "-w").start()
            val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText().trim()
            if (process.waitFor() != 0 || output.isEmpty()) return null
            String(Base64.getDecoder().decode(output), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Remove a secret from the login keychain
     */
    fun deleteSecret(account: String) {
        try {
            ProcessBuilder("security", "delete-generic-password", "-s", SERVICE, "-a", account)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        } catch (e: Exception) {
            println("Error deleting secret: ${e.message}")
        }
    }
}
