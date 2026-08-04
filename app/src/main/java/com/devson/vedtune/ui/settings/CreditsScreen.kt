package com.devson.vedtune.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var animateTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Open Source Credits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))

            CreditsHeroCard()

            AnimatedVisibility(
                visible = animateTrigger,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Libraries & Dependencies",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    val libraries = remember { getOpenSourceLibraries() }
                    libraries.forEach { library ->
                        LibraryCard(context = context, library = library)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CreditsHeroCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(
                text = "Open Source Acknowledgements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "VedTune is proudly built on top of amazing open source technologies and community libraries. We are deeply grateful to all maintainers for making these tools freely available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun LibraryCard(
    context: Context,
    library: OpenSourceLibrary
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { openUrl(context, library.url) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = library.version,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = library.license,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = "Open Website",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = library.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}

private data class OpenSourceLibrary(
    val name: String,
    val version: String,
    val license: String,
    val description: String,
    val url: String
)

private fun getOpenSourceLibraries(): List<OpenSourceLibrary> {
    return listOf(
        OpenSourceLibrary(
            name = "LRCLIB Lyrics API",
            version = "v1 API",
            license = "MIT",
            description = "Open-source synchronized lyrics provider service used for online search, fetching, and matching LRC lyrics.",
            url = "https://github.com/tranxuanthang/lrclib"
        ),
        OpenSourceLibrary(
            name = "AndroidX Media3 ExoPlayer",
            version = "1.4.1",
            license = "Apache-2.0",
            description = "Next-generation media playback engine and MediaSession background service backend for Android audio rendering.",
            url = "https://developer.android.com/media/media3"
        ),
        OpenSourceLibrary(
            name = "Jetpack Compose",
            version = "1.4.0-alpha04",
            license = "Apache-2.0",
            description = "Android's modern declarative toolkit for building performant, native Material Design 3 user interfaces.",
            url = "https://developer.android.com/jetpack/compose"
        ),
        OpenSourceLibrary(
            name = "jaudiotagger (libsrc)",
            version = "2.3.3",
            license = "LGPL-3.0",
            description = "Powerful audio metadata library for reading and safely editing ID3v1, ID3v2, Vorbis, and FLAC tags.",
            url = "https://github.com/Adonai/jaudiotagger"
        ),
        OpenSourceLibrary(
            name = "Coil Image Loader",
            version = "2.7.0",
            license = "Apache-2.0",
            description = "Fast, lightweight Kotlin Coroutines-backed image loading library for Jetpack Compose album artwork rendering.",
            url = "https://github.com/coil-kt/coil"
        ),
        OpenSourceLibrary(
            name = "Dagger Hilt",
            version = "2.52",
            license = "Apache-2.0",
            description = "Dependency injection framework for Android that simplifies architectural state management and dependency graphs.",
            url = "https://dagger.dev/hilt/"
        ),
        OpenSourceLibrary(
            name = "Room Database",
            version = "2.6.1",
            license = "Apache-2.0",
            description = "Persistence library providing a robust abstraction layer over SQLite for local database caching.",
            url = "https://developer.android.com/training/data-storage/room"
        ),
        OpenSourceLibrary(
            name = "Retrofit",
            version = "2.11.0",
            license = "Apache-2.0",
            description = "Type-safe HTTP client for Android and Kotlin to interface with REST APIs seamlessly.",
            url = "https://github.com/square/retrofit"
        ),
        OpenSourceLibrary(
            name = "OkHttp",
            version = "4.12.0",
            license = "Apache-2.0",
            description = "Efficient HTTP & HTTP/2 client for Android with support for connection pooling, logging, and caching.",
            url = "https://github.com/square/okhttp"
        ),
        OpenSourceLibrary(
            name = "Reorderable",
            version = "3.1.0",
            license = "MIT",
            description = "Drag-and-drop list reordering library built specifically for smooth queue management in Jetpack Compose.",
            url = "https://github.com/Calvin-Sh/Reorderable"
        ),
        OpenSourceLibrary(
            name = "Gson",
            version = "2.11.0",
            license = "Apache-2.0",
            description = "JSON serialization and deserialization library for converting data objects to and from JSON format.",
            url = "https://github.com/google/gson"
        ),
        OpenSourceLibrary(
            name = "Preferences DataStore",
            version = "1.1.1",
            license = "Apache-2.0",
            description = "Asynchronous key-value pair data storage solution for Android backed by Kotlin Coroutines and Flow.",
            url = "https://developer.android.com/topic/libraries/architecture/datastore"
        )
    )
}
