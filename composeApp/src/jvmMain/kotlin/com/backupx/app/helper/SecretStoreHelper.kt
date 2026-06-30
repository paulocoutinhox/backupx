package com.backupx.app.helper

/**
 * Stores sensitive values in the operating system secret store
 * Delegates to the native secret backend of each platform so secrets never touch the config file
 */
object SecretStoreHelper {

    private val osName = System.getProperty("os.name").lowercase()
    private val isWindows = osName.contains("win")
    private val isMacOS = osName.contains("mac") || osName.contains("darwin")
    private val isUnix = osName.contains("nix") || osName.contains("nux") || isMacOS

    fun store(account: String, secret: String): Boolean {
        return when {
            isMacOS -> MacosHelper.storeSecret(account, secret)
            isWindows -> WindowsHelper.storeSecret(account, secret)
            isUnix -> UnixHelper.storeSecret(account, secret)
            else -> false
        }
    }

    fun load(account: String): String? {
        return when {
            isMacOS -> MacosHelper.loadSecret(account)
            isWindows -> WindowsHelper.loadSecret(account)
            isUnix -> UnixHelper.loadSecret(account)
            else -> null
        }
    }

    fun delete(account: String) {
        when {
            isMacOS -> MacosHelper.deleteSecret(account)
            isWindows -> WindowsHelper.deleteSecret(account)
            isUnix -> UnixHelper.deleteSecret(account)
        }
    }
}
