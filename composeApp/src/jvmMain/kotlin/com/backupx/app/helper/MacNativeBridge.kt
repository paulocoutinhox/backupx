package com.backupx.app.helper

import java.io.File

/**
 * Loads the macOS native library shipped inside the app bundle and exposes its JNI entry points
 * The library is signed together with the app, so it loads under the sandbox and library validation
 */
object MacNativeBridge {

    init {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
            ?: throw IllegalStateException("Application resources directory is not available")
        System.load(File(resourcesDir, "macbridge.dylib").absolutePath)
    }

    external fun keychainStore(account: String, secret: String): Boolean
    external fun keychainLoad(account: String): String?
    external fun keychainDelete(account: String)
    external fun openInFinder(path: String)
    external fun bookmarkCreate(path: String): String?
    external fun bookmarkResolve(bookmark: String): String?
}
