package com.backupx.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.backupx.app.provider.BackupProviderFactory
import com.backupx.app.repository.BackupRepositoryImpl
import com.backupx.app.ui.AboutScreen
import com.backupx.app.ui.BackupScreen
import com.backupx.app.ui.theme.AppTheme
import com.backupx.app.viewmodel.BackupViewModel
import com.backupx.composeapp.generated.resources.Res
import com.backupx.composeapp.generated.resources.app_name
import com.backupx.composeapp.generated.resources.logo_dark
import com.backupx.composeapp.generated.resources.nav_about
import com.backupx.composeapp.generated.resources.nav_backups
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
@Preview
fun App() {
    AppTheme {
        val backupViewModel = viewModel { BackupViewModel(BackupRepositoryImpl(), BackupProviderFactory()) }
        var selectedScreen by remember { mutableStateOf(Screen.BACKUPS) }

        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight().width(160.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                Image(
                    painter = painterResource(Res.drawable.logo_dark),
                    contentDescription = stringResource(Res.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                NavigationRailItem(
                    icon = { Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(28.dp)) },
                    label = { Text(stringResource(Res.string.nav_backups), style = MaterialTheme.typography.titleSmall) },
                    selected = selectedScreen == Screen.BACKUPS,
                    onClick = { selectedScreen = Screen.BACKUPS },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                NavigationRailItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(28.dp)) },
                    label = { Text(stringResource(Res.string.nav_about), style = MaterialTheme.typography.titleSmall) },
                    selected = selectedScreen == Screen.ABOUT,
                    onClick = { selectedScreen = Screen.ABOUT },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (selectedScreen) {
                    Screen.BACKUPS -> BackupScreen(viewModel = backupViewModel)
                    Screen.ABOUT -> AboutScreen()
                }
            }
        }
    }
}
