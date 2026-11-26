package com.example

import android.app.Application
import com.example.data.local.AuraPlayDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.remote.AudiusApiService
import com.example.data.remote.GoogleDriveApiService
import com.example.data.remote.ItunesApiService
import com.example.data.repository.MediaRepository
import com.example.player.AudioEffectEngine
import com.example.player.PlayerController
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AuraPlayApp : Application() {

    lateinit var database: AuraPlayDatabase
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var driveAwareOkHttpClient: OkHttpClient
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var audioEffectEngine: AudioEffectEngine
        private set

    lateinit var playerController: PlayerController
        private set

    @Volatile
    var cachedDriveToken: String? = null

    override fun onCreate() {
        super.onCreate()
        database = AuraPlayDatabase.getInstance(this)
        preferencesRepository = PreferencesRepository(this)
        cachedDriveToken = runBlocking { preferencesRepository.getDriveToken() }

        val driveAuthInterceptor = Interceptor { chain ->
            val original = chain.request()
            val host = original.url.host
            val token = cachedDriveToken
            val req = if (host.endsWith("googleapis.com") && !token.isNullOrBlank() &&
                original.header("Authorization").isNullOrBlank()
            ) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(req)
        }

        driveAwareOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(driveAuthInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val audiusApi = Retrofit.Builder()
            .baseUrl("https://api.audius.co/")
            .client(driveAwareOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AudiusApiService::class.java)

        val itunesApi = Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(driveAwareOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ItunesApiService::class.java)

        val driveApi = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(driveAwareOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleDriveApiService::class.java)

        mediaRepository = MediaRepository(
            context = this,
            mediaDao = database.mediaDao(),
            prefs = preferencesRepository,
            audiusApi = audiusApi,
            itunesApi = itunesApi,
            driveApi = driveApi
        )

        audioEffectEngine = AudioEffectEngine(preferencesRepository)
        playerController = PlayerController(this, audioEffectEngine)
    }
}
