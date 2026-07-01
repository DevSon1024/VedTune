package com.devson.vedtune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.FolderFilterMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFolderSettings: () -> Unit
) {
    val autoSyncOnStartup by viewModel.autoSyncOnStartup.collectAsState()
    val folderFilterMode by viewModel.folderFilterMode.collectAsState()

    val folderModeLabel = when (folderFilterMode) {
        FolderFilterMode.NONE -> "None"
        FolderFilterMode.WHITELIST -> "Whitelist"
        FolderFilterMode.BLACKLIST -> "Blacklist"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Library & Folders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsCard(
                title = "Library & Folders",
                icon = Icons.Default.Folder
            ) {
                SettingSwitchRow(
                    title = "Auto-sync Library on Startup",
                    description = "Scan and sync device audio files on app launch.",
                    checked = autoSyncOnStartup,
                    onCheckedChange = { viewModel.setAutoSyncOnStartup(it) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SettingsNavigationRow(
                    title = "Folder Visibility",
                    description = "Filter mode: $folderModeLabel",
                    onClick = onNavigateToFolderSettings
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
