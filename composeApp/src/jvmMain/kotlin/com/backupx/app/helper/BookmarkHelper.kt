package com.backupx.app.helper

/**
 * Security-scoped bookmarks on macOS, so chosen paths stay accessible under the sandbox
 * Bookmarks are a macOS sandbox concept, on other systems the plain path is used directly
 */
object BookmarkHelper {

    private val isMacOS = System.getProperty("os.name").lowercase().let {
        it.contains("mac") || it.contains("darwin")
    }

    fun create(path: String): String? {
        if (!isMacOS) return null
        return MacNativeBridge.bookmarkCreate(path)
    }

    fun resolve(bookmark: String?, path: String): String {
        if (!isMacOS || bookmark.isNullOrBlank()) return path
        return MacNativeBridge.bookmarkResolve(bookmark) ?: path
    }
}
