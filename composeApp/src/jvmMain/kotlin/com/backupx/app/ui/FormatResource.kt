package com.backupx.app.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

/**
 * Resolves a string resource and applies its placeholders for the current locale
 * Formatting is done here so arguments resolve in every language, not only the default one
 */
@Composable
fun formatResource(resource: StringResource, vararg args: Any): String {
    return String.format(Locale.getDefault(), stringResource(resource), *args)
}
