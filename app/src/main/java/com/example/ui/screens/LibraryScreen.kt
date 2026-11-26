package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LinkedFolderEntity
import com.example.data.model.MediaTrack
import com.example.player.PlaybackUiState
import com.example.ui.components.MediaTrackRow
import com.example.viewmodel.LibraryFilter
import com.example.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    playbackState: PlaybackUiState,
    onSelectFilter: (LibraryFilter) -> Unit,
    onFolderPicked: (Uri) -> Unit,
    onFilesPicked: (List<Uri>) -> Unit,
    onAddDirectUrl: (String, String?) -> Unit,
    onRescanFolders: () -> Unit,
    onRemoveFolder: (LinkedFolderEntity) -> Unit,
    onPlayTracks: (List<MediaTrack>, Int) -> Unit,
    onAddToQueue: (MediaTrack) -> Unit,
    onToggleFavorite: (MediaTrack) -> Unit,
    onDismissStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUrlDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri -> if (uri != null) onFolderPicked(uri) }
    )

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris -> if (uris.isNotEmpty()) onFilesPicked(uris) }
    )

    val photoPickerVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> if (uris.isNotEmpty()) onFilesPicked(uris) }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_studio),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.library_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.library_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (state.linkedFolders.isNotEmpty()) {
                            IconButton(
                                onClick = onRescanFolders,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("rescan_folders_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Pindai ulang folder",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${state.audioCount} Musik",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${state.videoCount} Video",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (state.isScanning) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.scanning_folders),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (!state.statusMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissStatus,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Tutup pesan"
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Source Buttons
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.testTag("scan_folder_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.scan_folder_button))
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            multiFilePickerLauncher.launch(
                                arrayOf("audio/*", "video/*", "application/ogg", "application/octet-stream")
                            )
                        },
                        modifier = Modifier.testTag("pick_files_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.pick_files_button))
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            photoPickerVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.testTag("pick_video_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.pick_video_button))
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { showUrlDialog = true },
                        modifier = Modifier.testTag("play_url_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.play_url_button))
                    }
                }
            }
        }

        // Linked Folders Strip
        if (state.linkedFolders.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.linkedFolders, key = { it.uri }) { folder ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folder.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { onRemoveFolder(folder) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Hapus folder ${folder.displayName}",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips + Play All / Shuffle
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LibraryFilter.entries) { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { onSelectFilter(filter) },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                if (state.tracks.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onPlayTracks(state.tracks, 0) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("library_play_all_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.play_all_button))
                        }
                        OutlinedButton(
                            onClick = { onPlayTracks(state.tracks.shuffled(), 0) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("library_shuffle_button")
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.shuffle_button))
                        }
                    }
                }
            }
        }

        if (state.tracks.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_library_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.empty_library_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.testTag("empty_scan_folder_button")
                        ) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.scan_folder_button))
                        }
                    }
                }
            }
        } else {
            itemsIndexed(state.tracks, key = { _, item -> item.id }) { index, track ->
                MediaTrackRow(
                    track = track,
                    isActive = playbackState.currentTrack?.id == track.id,
                    isPlaying = playbackState.isPlaying,
                    onPlayClick = { onPlayTracks(state.tracks, index) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToQueue = { onAddToQueue(track) }
                )
            }
        }
    }

    if (showUrlDialog) {
        DirectUrlDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { url, title ->
                showUrlDialog = false
                onAddDirectUrl(url, title)
            }
        )
    }
}

@Composable
private fun DirectUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Putar Stream / URL Media") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mendukung MP3, FLAC, MP4, MKV, HLS (.m3u8), DASH (.mpd), dan RTSP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL Media (https://...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_url_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url, title.takeIf { it.isNotBlank() }) },
                enabled = url.trim().startsWith("http", ignoreCase = true) ||
                    url.trim().startsWith("rtsp", ignoreCase = true),
                modifier = Modifier.testTag("confirm_direct_url_button")
            ) {
                Text("Putar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
