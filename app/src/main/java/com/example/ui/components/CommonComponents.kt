package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MediaSource
import com.example.data.model.MediaTrack
import com.example.data.model.MediaType
import com.example.player.PlaybackUiState
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.PI
import kotlin.math.sin

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val totalSec = ms / 1000L
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.1f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        else -> String.format("%.0f KB", kb)
    }
}

fun MediaSource.displayLabel(): String = when (this) {
    MediaSource.LOCAL -> "OFFLINE"
    MediaSource.ONLINE_AUDIUS -> "AUDIUS"
    MediaSource.ONLINE_ITUNES -> "ITUNES"
    MediaSource.GOOGLE_DRIVE -> "G-DRIVE"
    MediaSource.DIRECT_URL -> "STREAM"
}

@Composable
fun FormatBadge(
    format: String,
    mediaType: MediaType,
    modifier: Modifier = Modifier
) {
    val bgColor = if (mediaType == MediaType.VIDEO) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val fgColor = if (mediaType == MediaType.VIDEO) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = format.take(6),
            color = fgColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AudioVisualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val gap = 4.dp.toPx()
        val totalGap = gap * (barCount - 1)
        val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(2.dp.toPx())
        for (i in 0 until barCount) {
            val normalized = if (isPlaying) {
                val wave = 0.5f + 0.45f * sin(phase + i * 0.65f) * sin(phase * 0.5f + i * 0.3f)
                wave.coerceIn(0.14f, 1.0f)
            } else {
                0.18f + (i % 3) * 0.06f
            }
            val barHeight = size.height * normalized
            val x = i * (barWidth + gap)
            val y = size.height - barHeight
            val brush = Brush.verticalGradient(
                colors = listOf(if (i % 4 == 0) secondary else primary, primary.copy(alpha = 0.35f))
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun TrackArtworkThumbnail(
    track: MediaTrack,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!track.artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = stringResource(R.string.cd_album_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = painterResource(id = R.drawable.img_default_album_art),
                placeholder = painterResource(id = R.drawable.img_default_album_art)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_default_album_art),
                contentDescription = stringResource(R.string.cd_album_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.AudioFile,
                contentDescription = null,
                tint = if (track.mediaType == MediaType.VIDEO) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun MediaTrackRow(
    track: MediaTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlayClick)
            .testTag("track_row_${track.id}"),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtworkThumbnail(
                track = track,
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(track.artist)
                        if (track.album.isNotBlank()) {
                            append(" • ")
                            append(track.album)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormatBadge(format = track.format, mediaType = track.mediaType)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = track.source.displayLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (track.durationMs > 0L) {
                        Text(
                            text = formatDuration(track.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (track.fileSizeBytes > 0L) {
                        Text(
                            text = formatFileSize(track.fileSizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isActive && isPlaying) {
                AudioVisualizerBars(
                    isPlaying = true,
                    barCount = 5,
                    modifier = Modifier
                        .width(24.dp)
                        .height(20.dp)
                        .padding(end = 4.dp)
                )
            }

            if (onAddToQueue != null) {
                IconButton(
                    onClick = onAddToQueue,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("queue_btn_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.cd_add_queue),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("favorite_btn_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (track.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    onClickExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClickExpand)
            .testTag("mini_player_bar"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtworkThumbnail(
                    track = track,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FormatBadge(format = track.format, mediaType = track.mediaType)
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.cd_next),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
