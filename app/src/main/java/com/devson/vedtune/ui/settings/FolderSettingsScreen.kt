package com.devson.vedtune.ui.settings

import android.provider.MediaStore
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.FolderFilterMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val filterMode by viewModel.folderFilterMode.collectAsState()
    val blacklistedFolders by viewModel.blacklistedFolders.collectAsState()
    val whitelistedFolders by viewModel.whitelistedFolders.collectAsState()

    // Tab: 0 = Whitelist, 1 = Blacklist
    var selectedTab by remember { mutableIntStateOf(if (filterMode == FolderFilterMode.WHITELIST) 0 else 1) }
    var showPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Folder Visibility",
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
                actions = {
                    val currentList = if (selectedTab == 0) whitelistedFolders else blacklistedFolders
                    if (currentList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedTab == 0) viewModel.clearWhitelist()
                                else viewModel.clearBlacklist()
                            }
                        ) {
                            Text(
                                text = "Clear All",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = filterMode != FolderFilterMode.NONE,
                enter = scaleIn(spring()) + fadeIn(),
                exit = scaleOut(spring()) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showPicker = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add Folder", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            //  Filter Mode Section 
            FolderFilterModeSection(
                currentMode = filterMode,
                onModeChanged = { mode ->
                    viewModel.setFolderFilterMode(mode)
                    when (mode) {
                        FolderFilterMode.WHITELIST -> selectedTab = 0
                        FolderFilterMode.BLACKLIST -> selectedTab = 1
                        FolderFilterMode.NONE -> Unit
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            //  Tab Row 
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Whitelist (${whitelistedFolders.size})")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Blacklist (${blacklistedFolders.size})")
                        }
                    }
                )
            }

            //  Content 
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                label = "tab_content"
            ) { tab ->
                val list = if (tab == 0) whitelistedFolders.sorted() else blacklistedFolders.sorted()
                val modeActive = when (tab) {
                    0 -> filterMode == FolderFilterMode.WHITELIST
                    else -> filterMode == FolderFilterMode.BLACKLIST
                }
                val icon: ImageVector = if (tab == 0) Icons.Filled.CheckCircle else Icons.Filled.Block
                val emptyTitle = if (tab == 0) "No whitelisted folders" else "No blacklisted folders"
                val emptyBody = if (tab == 0)
                    "Add folders to the whitelist. Only audio from these folders will appear when Whitelist mode is active. An empty whitelist hides every song."
                else
                    "Add folders to the blacklist. Audio from these folders will be hidden when Blacklist mode is active."

                if (list.isEmpty()) {
                    FolderEmptyState(
                        icon = icon,
                        title = emptyTitle,
                        body = emptyBody,
                        modeActive = modeActive,
                        onAddClick = if (filterMode != FolderFilterMode.NONE) ({ showPicker = true }) else null
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = list,
                            key = { it },
                            contentType = { "folder_path" }
                        ) { path ->
                            FolderPathRow(
                                path = path,
                                onRemove = {
                                    if (tab == 0) viewModel.removeFromWhitelist(path)
                                    else viewModel.removeFromBlacklist(path)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    //  Folder Picker Bottom Sheet 
    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FolderPickerContent(
                alreadyAdded = if (selectedTab == 0) whitelistedFolders else blacklistedFolders,
                onFolderSelected = { path ->
                    showPicker = false
                    if (selectedTab == 0) viewModel.addToWhitelist(path)
                    else viewModel.addToBlacklist(path)
                },
                onDismiss = { showPicker = false }
            )
        }
    }
}

// 
// Filter Mode Selector
// 

@Composable
private fun FolderFilterModeSection(
    currentMode: FolderFilterMode,
    onModeChanged: (FolderFilterMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Filter Mode",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FolderFilterModeChip(
                label = "None",
                description = "Show all",
                icon = Icons.Filled.Public,
                selected = currentMode == FolderFilterMode.NONE,
                onClick = { onModeChanged(FolderFilterMode.NONE) },
                modifier = Modifier.weight(1f)
            )
            FolderFilterModeChip(
                label = "Whitelist",
                description = "Only listed",
                icon = Icons.Filled.CheckCircle,
                selected = currentMode == FolderFilterMode.WHITELIST,
                onClick = { onModeChanged(FolderFilterMode.WHITELIST) },
                modifier = Modifier.weight(1f),
                activeColor = MaterialTheme.colorScheme.primaryContainer
            )
            FolderFilterModeChip(
                label = "Blacklist",
                description = "Hide listed",
                icon = Icons.Filled.Block,
                selected = currentMode == FolderFilterMode.BLACKLIST,
                onClick = { onModeChanged(FolderFilterMode.BLACKLIST) },
                modifier = Modifier.weight(1f),
                activeColor = MaterialTheme.colorScheme.errorContainer
            )
        }

        // Active mode hint
        AnimatedContent(
            targetState = currentMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "mode_hint"
        ) { mode ->
            val (text, color) = when (mode) {
                FolderFilterMode.NONE ->
                    "All audio files are visible. No folder filtering is applied." to
                            MaterialTheme.colorScheme.onSurfaceVariant
                FolderFilterMode.WHITELIST ->
                    "Only songs in whitelisted folders are shown. An empty whitelist hides everything." to
                            MaterialTheme.colorScheme.primary
                FolderFilterMode.BLACKLIST ->
                    "Songs in blacklisted folders are hidden. All other audio remains visible." to
                            MaterialTheme.colorScheme.error
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderFilterModeChip(
    label: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer
) {
    val containerColor = if (selected) activeColor else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 
// Empty state
// 

@Composable
private fun FolderEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modeActive: Boolean,
    onAddClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        if (!modeActive) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Enable this mode above to activate folder filtering.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        if (onAddClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Add a Folder",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// 
// Folder path row
// 

@Composable
private fun FolderPathRow(
    path: String,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val folderName = remember(path) {
                    path.substringAfterLast('/', path.substringAfterLast('\\', "Folder"))
                }
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove folder"
                )
            }
        }
    }
}

// 
// Folder Picker — reads distinct DATA folder paths from MediaStore (audio only)
// 

@Composable
private fun FolderPickerContent(
    alreadyAdded: Set<String>,
    onFolderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Discover all unique folder paths from MediaStore audio
    val availableFolders by produceState(initialValue = emptyList<String>()) {
        value = withContext(Dispatchers.IO) {
            val folders = mutableSetOf<String>()
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (cursor.moveToNext()) {
                    val filePath = cursor.getString(dataCol) ?: continue
                    val folder = filePath.substringBeforeLast('/')
                    if (folder.isNotBlank()) folders.add(folder)
                }
            }
            folders.sorted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Sheet header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Select a Folder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close"
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(4.dp))

        if (availableFolders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No audio folders found on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = availableFolders,
                    key = { it },
                    contentType = { "picker_folder" }
                ) { folderPath ->
                    val isAlreadyAdded = folderPath in alreadyAdded
                    PickerFolderRow(
                        path = folderPath,
                        isAlreadyAdded = isAlreadyAdded,
                        onClick = {
                            if (!isAlreadyAdded) onFolderSelected(folderPath)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerFolderRow(
    path: String,
    isAlreadyAdded: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isAlreadyAdded)
        MaterialTheme.colorScheme.surfaceContainerHigh
    else
        MaterialTheme.colorScheme.surfaceContainer

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isAlreadyAdded, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = if (isAlreadyAdded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                val folderName = remember(path) {
                    path.substringAfterLast('/', path.substringAfterLast('\\', "Folder"))
                }
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAlreadyAdded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isAlreadyAdded) 0.4f else 0.8f
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isAlreadyAdded) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Already added",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
