package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaTrack
import com.example.player.PlaybackUiState
import com.example.ui.components.MediaTrackRow
import com.example.viewmodel.SearchUiState

private val quickSearchTopics = listOf(
    "Lo-Fi", "Acoustic", "Electronic", "Jazz", "Rock", "Pop", "Classical", "Ambient"
)

@Composable
fun SearchScreen(
    state: SearchUiState,
    playbackState: PlaybackUiState,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onPlayTracks: (List<MediaTrack>, Int) -> Unit,
    onAddToQueue: (MediaTrack) -> Unit,
    onToggleFavorite: (MediaTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.search_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(R.string.search_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_search_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        onSubmitSearch(state.query)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field")
            )
        }

        // Quick genre chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickSearchTopics) { topic ->
                    AssistChip(
                        onClick = {
                            keyboard?.hide()
                            onSubmitSearch(topic)
                        },
                        label = { Text(topic) },
                        modifier = Modifier.testTag("topic_chip_${topic.lowercase()}")
                    )
                }
            }
        }

        // Search History
        if (state.query.isBlank() && state.history.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Riwayat Pencarian",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.history) { item ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp, end = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { onDeleteHistory(item) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Hapus riwayat $item",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.isSearching) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (state.query.isBlank()) {
            item {
                Text(
                    text = stringResource(R.string.search_empty_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.trendingOnline.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon = Icons.Rounded.CloudQueue,
                        title = "Trending Online (Audius Full Stream)",
                        count = state.trendingOnline.size
                    )
                }
                itemsIndexed(state.trendingOnline, key = { _, t -> "trend_${t.id}" }) { idx, track ->
                    MediaTrackRow(
                        track = track,
                        isActive = playbackState.currentTrack?.id == track.id,
                        isPlaying = playbackState.isPlaying,
                        onPlayClick = { onPlayTracks(state.trendingOnline, idx) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToQueue = { onAddToQueue(track) }
                    )
                }
            }
        } else {
            val res = state.results
            val hasAny = res.offlineResults.isNotEmpty() ||
                res.audiusResults.isNotEmpty() ||
                res.itunesResults.isNotEmpty()

            if (!state.isSearching && !hasAny) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            }

            if (res.offlineResults.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon = Icons.Rounded.Smartphone,
                        title = stringResource(R.string.search_section_offline),
                        count = res.offlineResults.size
                    )
                }
                itemsIndexed(res.offlineResults, key = { _, t -> "off_${t.id}" }) { idx, track ->
                    MediaTrackRow(
                        track = track,
                        isActive = playbackState.currentTrack?.id == track.id,
                        isPlaying = playbackState.isPlaying,
                        onPlayClick = { onPlayTracks(res.offlineResults, idx) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToQueue = { onAddToQueue(track) }
                    )
                }
            }

            if (res.audiusResults.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon = Icons.Rounded.CloudQueue,
                        title = stringResource(R.string.search_section_audius),
                        count = res.audiusResults.size
                    )
                }
                itemsIndexed(res.audiusResults, key = { _, t -> "aud_${t.id}" }) { idx, track ->
                    MediaTrackRow(
                        track = track,
                        isActive = playbackState.currentTrack?.id == track.id,
                        isPlaying = playbackState.isPlaying,
                        onPlayClick = { onPlayTracks(res.audiusResults, idx) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToQueue = { onAddToQueue(track) }
                    )
                }
            }

            if (res.itunesResults.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon = Icons.Rounded.CloudQueue,
                        title = stringResource(R.string.search_section_itunes),
                        count = res.itunesResults.size
                    )
                }
                itemsIndexed(res.itunesResults, key = { _, t -> "itu_${t.id}" }) { idx, track ->
                    MediaTrackRow(
                        track = track,
                        isActive = playbackState.currentTrack?.id == track.id,
                        isPlaying = playbackState.isPlaying,
                        onPlayClick = { onPlayTracks(res.itunesResults, idx) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToQueue = { onAddToQueue(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
