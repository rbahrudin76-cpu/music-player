package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.MediaTrack
import com.example.data.model.RepeatModeOption
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val queue: List<MediaTrack> = emptyList(),
    val currentIndex: Int = -1,
    val currentTrack: MediaTrack? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val playbackError: String? = null,
    val sleepTimerRemainingSec: Int = 0
)

class PlayerController(
    private val context: Context,
    private val audioEffectEngine: AudioEffectEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _activePlayer = MutableStateFlow<Player?>(null)
    val activePlayer: StateFlow<Player?> = _activePlayer.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var exoPlayer: ExoPlayer? = null

    private var pendingPlayAction: (() -> Unit)? = null
    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionTicker() else stopPositionTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            _playbackState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = player.duration.coerceAtLeast(0L)
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = exoPlayer ?: return
            val idx = player.currentMediaItemIndex
            val queue = _playbackState.value.queue
            val track = queue.getOrNull(idx)
            _playbackState.update {
                it.copy(
                    currentIndex = idx,
                    currentTrack = track,
                    positionMs = 0L,
                    durationMs = player.duration.coerceAtLeast(track?.durationMs ?: 0L),
                    playbackError = null
                )
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
                Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
                else -> RepeatModeOption.OFF
            }
            _playbackState.update { it.copy(repeatMode = mapped) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    playbackError = "Gagal memutar media (${error.errorCodeName}): ${error.localizedMessage ?: "Format atau koneksi bermasalah"}"
                )
            }
        }
    }

    fun ensureConnected() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    mediaController = future.get()
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun bindInProcessPlayer(player: ExoPlayer) {
        exoPlayer?.removeListener(playerListener)
        exoPlayer = player
        _activePlayer.value = player
        player.addListener(playerListener)

        val eqState = audioEffectEngine.state.value
        player.playbackParameters = PlaybackParameters(eqState.playbackSpeed, eqState.playbackPitch)

        pendingPlayAction?.invoke()
        pendingPlayAction = null
    }

    fun unbindInProcessPlayer() {
        stopPositionTicker()
        exoPlayer?.removeListener(playerListener)
        exoPlayer = null
        _activePlayer.value = null
    }

    fun playQueue(tracks: List<MediaTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _playbackState.update {
            it.copy(
                queue = tracks,
                currentIndex = safeIndex,
                currentTrack = tracks[safeIndex],
                playbackError = null,
                positionMs = 0L,
                durationMs = tracks[safeIndex].durationMs
            )
        }

        val action = {
            val player = exoPlayer
            if (player != null) {
                val mediaItems = tracks.map { it.toMediaItem() }
                player.setMediaItems(mediaItems, safeIndex, 0L)
                player.prepare()
                player.play()
                val eq = audioEffectEngine.state.value
                player.playbackParameters = PlaybackParameters(eq.playbackSpeed, eq.playbackPitch)
                if (player.audioSessionId != 0) {
                    audioEffectEngine.attachToAudioSession(player.audioSessionId)
                }
            }
        }

        ensureConnected()
        if (exoPlayer != null) {
            action()
        } else {
            pendingPlayAction = action
        }
    }

    fun addToQueue(track: MediaTrack) {
        val currentQueue = _playbackState.value.queue
        if (currentQueue.isEmpty()) {
            playQueue(listOf(track), 0)
            return
        }
        if (currentQueue.any { it.id == track.id }) return
        val updated = currentQueue + track
        _playbackState.update { it.copy(queue = updated) }
        exoPlayer?.addMediaItem(track.toMediaItem())
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && _playbackState.value.queue.isNotEmpty()) {
                val idx = _playbackState.value.currentIndex.coerceAtLeast(0)
                playQueue(_playbackState.value.queue, idx)
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val safe = positionMs.coerceAtLeast(0L)
        player.seekTo(safe)
        _playbackState.update { it.copy(positionMs = safe) }
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun skipToNext() {
        val player = exoPlayer ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        }
    }

    fun skipToPrevious() {
        val player = exoPlayer ?: return
        if (player.currentPosition > 3000L || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
            player.play()
        }
    }

    fun skipToQueueIndex(index: Int) {
        val player = exoPlayer ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekToDefaultPosition(index)
            player.play()
        }
    }

    fun toggleShuffle() {
        val player = exoPlayer ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val player = exoPlayer ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun applyPlaybackParameters(speed: Float, pitch: Float) {
        audioEffectEngine.setPlaybackSpeed(speed)
        audioEffectEngine.setPlaybackPitch(pitch)
        exoPlayer?.playbackParameters = PlaybackParameters(
            speed.coerceIn(0.5f, 2.0f),
            pitch.coerceIn(0.75f, 1.5f)
        )
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playbackState.update { it.copy(sleepTimerRemainingSec = 0) }
            return
        }
        var remaining = minutes * 60
        _playbackState.update { it.copy(sleepTimerRemainingSec = remaining) }
        sleepTimerJob = scope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1
                _playbackState.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            exoPlayer?.pause()
        }
    }

    fun clearError() {
        _playbackState.update { it.copy(playbackError = null) }
    }

    fun updateFavoriteInQueue(trackId: String, isFavorite: Boolean) {
        _playbackState.update { state ->
            val updatedQueue = state.queue.map {
                if (it.id == trackId) it.copy(isFavorite = isFavorite) else it
            }
            val updatedCurrent = state.currentTrack?.let {
                if (it.id == trackId) it.copy(isFavorite = isFavorite) else it
            }
            state.copy(queue = updatedQueue, currentTrack = updatedCurrent)
        }
    }

    private fun startPositionTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (true) {
                val player = exoPlayer
                if (player != null) {
                    _playbackState.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(it.currentTrack?.durationMs ?: 0L)
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun MediaTrack.toMediaItem(): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .apply {
                if (!artworkUri.isNullOrBlank()) {
                    setArtworkUri(Uri.parse(artworkUri))
                }
            }
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(meta)
            .build()
    }
}
