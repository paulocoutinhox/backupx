package com.backupx.app.helper

/**
 * MacOS-specific helper functions
 * Delegates to the native bridge so it stays sandbox-friendly (no subprocesses)
 */
object MacosHelper {

    fun openInFinder(path: String) {
        try {
            MacNativeBridge.openInFinder(path)
        } catch (e: Exception) {
            println("Error opening in Finder: ${e.message}")
        }
    }

    /**
     * Store a secret in the login keychain, replacing any existing entry for the account
     */
    fun storeSecret(account: String, secret: String): Boolean {
        return try {
            MacNativeBridge.keychainStore(account, secret)
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
            MacNativeBridge.keychainLoad(account)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Remove a secret from the login keychain
     */
    fun deleteSecret(account: String) {
        try {
            MacNativeBridge.keychainDelete(account)
        } catch (e: Exception) {
            println("Error deleting secret: ${e.message}")
        }
    }
}
