package com.example.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.AuraPlayApp
import com.example.MainActivity

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as AuraPlayApp

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(app.driveAwareOkHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(this, okHttpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                app.audioEffectEngine.attachToAudioSession(audioSessionId)
            }
        })

        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET && player.audioSessionId != 0) {
            app.audioEffectEngine.attachToAudioSession(player.audioSessionId)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        exoPlayer = player
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()

        app.playerController.bindInProcessPlayer(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val app = application as AuraPlayApp
        app.playerController.unbindInProcessPlayer()
        app.audioEffectEngine.releaseHardwareEffects()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
