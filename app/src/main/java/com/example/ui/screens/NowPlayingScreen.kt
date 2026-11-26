package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MediaType
import com.example.data.model.RepeatModeOption
import com.example.data.model.VideoAspectMode
import com.example.player.PlaybackUiState
import com.example.ui.components.AudioVisualizerBars
import com.example.ui.components.FormatBadge
import com.example.ui.components.displayLabel
import com.example.ui.components.formatDuration
import com.example.ui.theme.JetBrainsMonoFamily

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    player: Player?,
    videoAspectMode: VideoAspectMode,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleAspectMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSkipToQueueIndex: (Int) -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack
    val context = LocalContext.current
    val activity = context as? Activity
    var isLandscapeFullscreen by remember { mutableStateOf(false) }
    var showVideoControls by remember { mutableStateOf(true) }
    var showQueueSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (isLandscapeFullscreen) {
            isLandscapeFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val isVideo = track?.mediaType == MediaType.VIDEO

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .testTag("now_playing_screen")
    ) {
        if (isVideo && isLandscapeFullscreen) {
            // Dedicated Video Cinema Fullscreen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showVideoControls = !showVideoControls }
            ) {
                VideoSurfaceBox(
                    player = player,
                    videoAspectMode = videoAspectMode,
                    modifier = Modifier.fillMaxSize()
                )
                AnimatedVisibility(
                    visible = showVideoControls,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.42f))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = track?.title.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(
                                    onClick = onCycleAspectMode,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.AspectRatio, contentDescription = stringResource(R.string.cd_aspect_ratio), tint = Color.White)
                                }
                                IconButton(
                                    onClick = {
                                        isLandscapeFullscreen = false
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.FullscreenExit, contentDescription = stringResource(R.string.cd_fullscreen), tint = Color.White)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onSeekBy(-10_000L) }, modifier = Modifier.size(56.dp)) {
                                Icon(Icons.Rounded.Replay10, contentDescription = "Mundur 10 detik", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(64.dp)) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_play_pause),
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            IconButton(onClick = { onSeekBy(10_000L) }, modifier = Modifier.size(56.dp)) {
                                Icon(Icons.Rounded.Forward10, contentDescription = "Maju 10 detik", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            SeekBarSection(
                                positionMs = playbackState.positionMs,
                                durationMs = playbackState.durationMs,
                                onSeekTo = onSeekTo
                            )
                        }
                    }
                }
            }
        } else {
            // Standard Full-Screen Player (Audio & Video)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .widthIn(max = 600.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_now_playing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_close_player),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.now_playing).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (track != null) {
                            Text(
                                text = "${track.format} • ${track.source.displayLabel()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                onClose()
                                onOpenEqualizer()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("now_playing_eq_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = stringResource(R.string.cd_equalizer),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showQueueSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("now_playing_queue_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Antrean"
                            )
                        }
                    }
                }

                // Stage (Video Surface or Audio Vinyl Artwork + Visualizer)
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                    ) {
                        VideoSurfaceBox(
                            player = player,
                            videoAspectMode = videoAspectMode,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (playbackState.isBuffering) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            IconButton(
                                onClick = onCycleAspectMode,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.AspectRatio,
                                    contentDescription = stringResource(R.string.cd_aspect_ratio),
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    isLandscapeFullscreen = true
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("video_fullscreen_button")
                            ) {
                                Icon(
                                    Icons.Rounded.Fullscreen,
                                    contentDescription = stringResource(R.string.cd_fullscreen),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(250.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            tonalElevation = 8.dp
                        ) {
                            if (!track?.artworkUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = track?.artworkUri,
                                    contentDescription = stringResource(R.string.cd_album_art),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    error = painterResource(id = R.drawable.img_default_album_art)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_default_album_art),
                                    contentDescription = stringResource(R.string.cd_album_art),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        AudioVisualizerBars(
                            isPlaying = playbackState.isPlaying,
                            barCount = 24,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(36.dp)
                        )
                    }
                }

                // Track Info + Favorite
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track?.title ?: "Belum ada media diputar",
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track?.artist ?: "Pilih lagu atau video untuk mulai",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (track != null) {
                            FormatBadge(format = track.format, mediaType = track.mediaType)
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("now_playing_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = stringResource(R.string.cd_favorite),
                                    tint = if (track.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!playbackState.playbackError.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = playbackState.playbackError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    SeekBarSection(
                        positionMs = playbackState.positionMs,
                        durationMs = playbackState.durationMs,
                        onSeekTo = onSeekTo
                    )

                    // Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("now_playing_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = stringResource(R.string.cd_shuffle),
                                tint = if (playbackState.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("now_playing_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = stringResource(R.string.cd_previous),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onTogglePlayPause)
                                .testTag("now_playing_play_pause_button"),
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (playbackState.isBuffering) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_play_pause),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("now_playing_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = stringResource(R.string.cd_next),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        IconButton(
                            onClick = onCycleRepeat,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("now_playing_repeat_button")
                        ) {
                            Icon(
                                imageVector = if (playbackState.repeatMode == RepeatModeOption.ONE) {
                                    Icons.Rounded.RepeatOne
                                } else {
                                    Icons.Rounded.Repeat
                                },
                                contentDescription = stringResource(R.string.cd_repeat),
                                tint = if (playbackState.repeatMode != RepeatModeOption.OFF) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Antrean Pemutaran (${playbackState.queue.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(playbackState.queue) { idx, item ->
                        val active = idx == playbackState.currentIndex
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSkipToQueueIndex(idx)
                                    showQueueSheet = false
                                },
                            color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                FormatBadge(format = item.format, mediaType = item.mediaType)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SeekBarSection(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = { frac -> onSeekTo((frac * safeDuration).toLong()) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("now_playing_seek_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(positionMs),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurfaceBox(
    player: Player?,
    videoAspectMode: VideoAspectMode,
    modifier: Modifier = Modifier
) {
    val resizeMode = when (videoAspectMode) {
        VideoAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        VideoAspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        VideoAspectMode.WIDE_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                setKeepContentOnPlayerReset(false)
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                this.resizeMode = resizeMode
                this.player = player
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
        },
        modifier = modifier
    )
}
