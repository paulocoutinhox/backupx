package com.backupx.app.provider

import org.jetbrains.compose.resources.StringResource

/**
 * Backup failure that carries a localizable message resource
 * The caller resolves the resource for the current locale when showing the error
 */
class BackupException(val resource: StringResource) : Exception()
