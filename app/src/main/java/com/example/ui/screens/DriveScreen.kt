package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.rounded.AddToDrive
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaTrack
import com.example.player.PlaybackUiState
import com.example.ui.components.MediaTrackRow
import com.example.viewmodel.DriveFilter
import com.example.viewmodel.DriveUiState

@Composable
fun DriveScreen(
    state: DriveUiState,
    playbackState: PlaybackUiState,
    onConnectClick: (Activity) -> Unit,
    onResolutionConsumed: () -> Unit,
    onResolutionResult: (Intent?) -> Unit,
    onConnectManualToken: (String) -> Unit,
    onSyncClick: () -> Unit,
    onAutoPlayAllClick: () -> Unit,
    onSetAutoPlay: (Boolean) -> Unit,
    onSetFilter: (DriveFilter) -> Unit,
    onDisconnectClick: () -> Unit,
    onPlayTracks: (List<MediaTrack>, Int) -> Unit,
    onAddToQueue: (MediaTrack) -> Unit,
    onToggleFavorite: (MediaTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showTokenDialog by remember { mutableStateOf(false) }

    val authResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            onResolutionResult(result.data)
        }
    )

    val pendingReq = state.pendingAuthResolution
    LaunchedEffect(pendingReq) {
        if (pendingReq != null) {
            onResolutionConsumed()
            authResolutionLauncher.launch(pendingReq)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("drive_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_drive_banner),
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
                                    Color.Black.copy(alpha = 0.25f),
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
                                text = stringResource(R.string.drive_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.drive_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (state.isConnected) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.drive_connected_badge),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }

                    if (state.isConnected && state.accountInfo != null) {
                        Text(
                            text = "${state.accountInfo.displayName} (${state.accountInfo.email})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        if (state.isSyncing) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (!state.errorMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { showTokenDialog = true },
                            modifier = Modifier.testTag("drive_manual_token_fallback_button")
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.drive_paste_token_button))
                        }
                    }
                }
            }
        }

        if (!state.isConnected) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddToDrive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = stringResource(R.string.drive_not_connected_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.drive_not_connected_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { if (activity != null) onConnectClick(activity) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connect_drive_button")
                        ) {
                            Icon(Icons.Rounded.AddToDrive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.drive_connect_button))
                        }
                        TextButton(
                            onClick = { showTokenDialog = true },
                            modifier = Modifier.testTag("open_manual_token_button")
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.drive_paste_token_button))
                        }
                    }
                }
            }
        } else {
            // Connected controls
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Putar Otomatis Saat Sinkron",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Otomatis antrekan & putar file Drive berurutan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.autoPlayEnabled,
                                onCheckedChange = onSetAutoPlay,
                                modifier = Modifier.testTag("drive_autoplay_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAutoPlayAllClick,
                                enabled = state.tracks.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("drive_autoplay_all_button")
                            ) {
                                Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.drive_autoplay_all))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSyncClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("drive_sync_button")
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.drive_sync_button))
                            }
                            OutlinedButton(
                                onClick = onDisconnectClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("drive_disconnect_button")
                            ) {
                                Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.drive_disconnect_button))
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DriveFilter.entries) { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { onSetFilter(filter) },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("drive_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (state.tracks.isEmpty() && !state.isSyncing) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.drive_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(state.tracks, key = { _, t -> t.id }) { idx, track ->
                    MediaTrackRow(
                        track = track,
                        isActive = playbackState.currentTrack?.id == track.id,
                        isPlaying = playbackState.isPlaying,
                        onPlayClick = { onPlayTracks(state.tracks, idx) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToQueue = { onAddToQueue(track) }
                    )
                }
            }
        }
    }

    if (showTokenDialog) {
        ManualDriveTokenDialog(
            onDismiss = { showTokenDialog = false },
            onConfirm = { token ->
                showTokenDialog = false
                onConnectManualToken(token)
            }
        )
    }
}

@Composable
private fun ManualDriveTokenDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var token by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Token Akses OAuth Drive") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Masukkan Google OAuth 2.0 Bearer token dengan scope drive.readonly untuk langsung memindai dan memutar media Drive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Access Token (ya29....)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_drive_token_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(token) },
                enabled = token.trim().length > 10,
                modifier = Modifier.testTag("confirm_manual_token_button")
            ) {
                Text("Hubungkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
