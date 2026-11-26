package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.AuraPlayApp
import com.example.data.local.CustomEqPresetEntity
import com.example.data.local.LinkedFolderEntity
import com.example.data.model.DriveAccountInfo
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.data.model.MediaTrack
import com.example.data.model.MediaType
import com.example.data.model.VideoAspectMode
import com.example.data.remote.DriveAuthManager
import com.example.data.remote.DriveAuthOutcome
import com.example.data.repository.UnifiedSearchResult
import com.example.player.PlaybackUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter(val label: String) {
    ALL("Semua"),
    AUDIO("Musik"),
    VIDEO("Video"),
    FAVORITES("Favorit")
}

enum class DriveFilter(val label: String) {
    ALL("Semua Drive"),
    AUDIO("Audio Drive"),
    VIDEO("Video Drive")
}

data class LibraryUiState(
    val tracks: List<MediaTrack> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.ALL,
    val linkedFolders: List<LinkedFolderEntity> = emptyList(),
    val isScanning: Boolean = false,
    val statusMessage: String? = null,
    val audioCount: Int = 0,
    val videoCount: Int = 0
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: UnifiedSearchResult = UnifiedSearchResult(),
    val trendingOnline: List<MediaTrack> = emptyList(),
    val history: List<String> = emptyList()
)

data class DriveUiState(
    val isConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val accountInfo: DriveAccountInfo? = null,
    val tracks: List<MediaTrack> = emptyList(),
    val filter: DriveFilter = DriveFilter.ALL,
    val autoPlayEnabled: Boolean = true,
    val errorMessage: String? = null,
    val pendingAuthResolution: IntentSenderRequest? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AuraPlayApp
    private val repo = app.mediaRepository
    private val prefs = app.preferencesRepository
    private val mediaDao = app.database.mediaDao()
    val playerController = app.playerController
    val audioEffectEngine = app.audioEffectEngine

    val playbackState: StateFlow<PlaybackUiState> = playerController.playbackState
    val activePlayer: StateFlow<Player?> = playerController.activePlayer
    val equalizerState: StateFlow<EqualizerState> = audioEffectEngine.state

    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    private val _isScanning = MutableStateFlow(false)
    private val _libraryMessage = MutableStateFlow<String?>(null)

    val libraryUiState: StateFlow<LibraryUiState> = combine(
        repo.localTracksFlow,
        repo.favoritesFlow,
        repo.linkedFoldersFlow,
        _libraryFilter,
        _isScanning,
        _libraryMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allLocal = values[0] as List<MediaTrack>
        @Suppress("UNCHECKED_CAST")
        val favorites = values[1] as List<MediaTrack>
        @Suppress("UNCHECKED_CAST")
        val folders = values[2] as List<LinkedFolderEntity>
        val filter = values[3] as LibraryFilter
        val scanning = values[4] as Boolean
        val message = values[5] as String?

        val filtered = when (filter) {
            LibraryFilter.ALL -> allLocal
            LibraryFilter.AUDIO -> allLocal.filter { it.mediaType == MediaType.AUDIO }
            LibraryFilter.VIDEO -> allLocal.filter { it.mediaType == MediaType.VIDEO }
            LibraryFilter.FAVORITES -> favorites
        }

        LibraryUiState(
            tracks = filtered,
            filter = filter,
            linkedFolders = folders,
            isScanning = scanning,
            statusMessage = message,
            audioCount = allLocal.count { it.mediaType == MediaType.AUDIO },
            videoCount = allLocal.count { it.mediaType == MediaType.VIDEO }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)
    private val _searchResults = MutableStateFlow(UnifiedSearchResult())
    private val _trendingOnline = MutableStateFlow<List<MediaTrack>>(emptyList())
    private var searchDebounceJob: Job? = null

    val searchUiState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        _isSearching,
        _searchResults,
        _trendingOnline,
        repo.searchHistoryFlow
    ) { query, searching, results, trending, history ->
        SearchUiState(
            query = query,
            isSearching = searching,
            results = results,
            trendingOnline = trending,
            history = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    private val _driveFilter = MutableStateFlow(DriveFilter.ALL)
    private val _isDriveSyncing = MutableStateFlow(false)
    private val _driveError = MutableStateFlow<String?>(null)
    private val _pendingDriveResolution = MutableStateFlow<IntentSenderRequest?>(null)

    val driveUiState: StateFlow<DriveUiState> = combine(
        prefs.driveTokenFlow,
        prefs.driveAccountNameFlow,
        prefs.driveAccountEmailFlow,
        prefs.driveAutoPlayFlow,
        repo.driveTracksFlow,
        _driveFilter,
        _isDriveSyncing,
        _driveError,
        _pendingDriveResolution
    ) { values ->
        val token = values[0] as String?
        val name = values[1] as String?
        val email = values[2] as String?
        val autoPlay = values[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val allDrive = values[4] as List<MediaTrack>
        val filter = values[5] as DriveFilter
        val syncing = values[6] as Boolean
        val err = values[7] as String?
        val resolution = values[8] as IntentSenderRequest?

        val filtered = when (filter) {
            DriveFilter.ALL -> allDrive
            DriveFilter.AUDIO -> allDrive.filter { it.mediaType == MediaType.AUDIO }
            DriveFilter.VIDEO -> allDrive.filter { it.mediaType == MediaType.VIDEO }
        }
        val account = if (!name.isNullOrBlank() || !email.isNullOrBlank()) {
            DriveAccountInfo(
                displayName = name ?: "Google Drive",
                email = email ?: ""
            )
        } else null

        DriveUiState(
            isConnected = !token.isNullOrBlank(),
            isSyncing = syncing,
            accountInfo = account,
            tracks = filtered,
            filter = filter,
            autoPlayEnabled = autoPlay,
            errorMessage = err,
            pendingAuthResolution = resolution
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriveUiState())

    val customEqPresets: StateFlow<List<EqualizerPreset>> = mediaDao.observeCustomPresets()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isFullPlayerOpen = MutableStateFlow(false)
    val isFullPlayerOpen: StateFlow<Boolean> = _isFullPlayerOpen.asStateFlow()

    private val _videoAspectMode = MutableStateFlow(VideoAspectMode.FIT)
    val videoAspectMode: StateFlow<VideoAspectMode> = _videoAspectMode.asStateFlow()

    init {
        playerController.ensureConnected()
        autoScanPersistedFoldersOnLaunch()
        loadTrendingOnline()
        autoSyncDriveOnLaunch()
    }

    private fun autoScanPersistedFoldersOnLaunch() {
        viewModelScope.launch {
            _isScanning.value = true
            runCatching { repo.rescanAllLinkedFolders() }
            _isScanning.value = false
        }
    }

    private fun loadTrendingOnline() {
        viewModelScope.launch {
            _trendingOnline.value = repo.getTrendingOnlineTracks()
        }
    }

    private fun autoSyncDriveOnLaunch() {
        viewModelScope.launch {
            val token = prefs.getDriveToken()
            if (!token.isNullOrBlank()) {
                app.cachedDriveToken = token
                syncGoogleDrive(autoStartIfEnabled = false)
            }
        }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    fun linkAndScanFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            _libraryMessage.value = null
            val count = repo.linkAndScanFolder(treeUri)
            _isScanning.value = false
            _libraryMessage.value = if (count > 0) {
                "$count file media ditemukan & diindeks."
            } else {
                "Tidak ada file audio/video ditemukan di folder tersebut."
            }
        }
    }

    fun rescanFolders() {
        viewModelScope.launch {
            _isScanning.value = true
            val count = repo.rescanAllLinkedFolders()
            _isScanning.value = false
            _libraryMessage.value = "Pemindaian selesai: $count file media aktif."
        }
    }

    fun removeLinkedFolder(folder: LinkedFolderEntity) {
        viewModelScope.launch {
            repo.removeLinkedFolder(folder)
        }
    }

    fun importPickedFiles(uris: List<Uri>, autoPlayFirst: Boolean = true) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val tracks = repo.addPickedUris(uris)
            if (autoPlayFirst && tracks.isNotEmpty()) {
                playTracks(tracks, 0, openFullIfVideo = true)
            }
        }
    }

    fun addAndPlayDirectUrl(url: String, title: String?) {
        viewModelScope.launch {
            val track = repo.addDirectStreamUrl(url, title)
            if (track != null) {
                playTracks(listOf(track), 0, openFullIfVideo = true)
            }
        }
    }

    fun handleIncomingViewUri(uri: Uri) {
        importPickedFiles(listOf(uri), autoPlayFirst = true)
    }

    fun clearLibraryMessage() {
        _libraryMessage.value = null
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchDebounceJob?.cancel()
        if (newQuery.isBlank()) {
            _isSearching.value = false
            _searchResults.value = UnifiedSearchResult()
            return
        }
        searchDebounceJob = viewModelScope.launch {
            delay(350L)
            executeSearchNow(newQuery)
        }
    }

    fun executeSearchNow(query: String = _searchQuery.value) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _searchQuery.value = trimmed
        searchDebounceJob?.cancel()
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = repo.searchOfflineAndOnline(trimmed)
            _isSearching.value = false
        }
    }

    fun deleteSearchHistory(query: String) {
        viewModelScope.launch {
            repo.deleteSearchHistory(query)
        }
    }

    fun toggleFavorite(track: MediaTrack) {
        viewModelScope.launch {
            repo.toggleFavorite(track)
            playerController.updateFavoriteInQueue(track.id, !track.isFavorite)
        }
    }

    fun playTracks(tracks: List<MediaTrack>, startIndex: Int = 0, openFullIfVideo: Boolean = true) {
        if (tracks.isEmpty()) return
        val target = tracks.getOrNull(startIndex) ?: tracks.first()
        playerController.playQueue(tracks, startIndex)
        if (openFullIfVideo && target.mediaType == MediaType.VIDEO) {
            _isFullPlayerOpen.value = true
        }
    }

    fun addToQueue(track: MediaTrack) {
        playerController.addToQueue(track)
    }

    fun openFullPlayer() {
        _isFullPlayerOpen.value = true
    }

    fun closeFullPlayer() {
        _isFullPlayerOpen.value = false
    }

    fun cycleVideoAspectMode() {
        val modes = VideoAspectMode.entries
        val next = modes[(modes.indexOf(_videoAspectMode.value) + 1) % modes.size]
        _videoAspectMode.value = next
    }

    // Google Drive Actions
    fun connectGoogleDrive(activity: Activity) {
        viewModelScope.launch {
            _driveError.value = null
            _isDriveSyncing.value = true
            when (val outcome = DriveAuthManager.requestDriveAuthorization(activity)) {
                is DriveAuthOutcome.Authorized -> {
                    onDriveTokenObtained(outcome.accessToken, autoPlayAfterSync = true)
                }
                is DriveAuthOutcome.NeedsResolution -> {
                    _isDriveSyncing.value = false
                    _pendingDriveResolution.value = outcome.intentSenderRequest
                }
                is DriveAuthOutcome.Error -> {
                    _isDriveSyncing.value = false
                    _driveError.value = outcome.message
                }
            }
        }
    }

    fun consumePendingDriveResolution() {
        _pendingDriveResolution.value = null
    }

    fun onDriveResolutionResult(data: Intent?) {
        val res = DriveAuthManager.extractTokenFromIntent(app, data)
        res.onSuccess { token ->
            viewModelScope.launch {
                onDriveTokenObtained(token, autoPlayAfterSync = true)
            }
        }.onFailure { err ->
            _driveError.value = err.localizedMessage ?: "Otorisasi Google Drive dibatalkan."
        }
    }

    fun connectDriveWithManualToken(token: String) {
        val clean = token.trim().removePrefix("Bearer ").trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            onDriveTokenObtained(clean, autoPlayAfterSync = true)
        }
    }

    private suspend fun onDriveTokenObtained(token: String, autoPlayAfterSync: Boolean) {
        app.cachedDriveToken = token
        prefs.saveDriveToken(token)
        syncGoogleDrive(autoStartIfEnabled = autoPlayAfterSync)
    }

    fun syncGoogleDrive(autoStartIfEnabled: Boolean = false) {
        viewModelScope.launch {
            val token = prefs.getDriveToken()
            if (token.isNullOrBlank()) {
                _driveError.value = "Hubungkan Google Drive terlebih dahulu."
                return@launch
            }
            app.cachedDriveToken = token
            _isDriveSyncing.value = true
            _driveError.value = null
            val result = repo.syncGoogleDriveMedia(token)
            _isDriveSyncing.value = false
            result.onSuccess { (_, tracks) ->
                if (autoStartIfEnabled && driveUiState.value.autoPlayEnabled && tracks.isNotEmpty()) {
                    playTracks(tracks, 0, openFullIfVideo = false)
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Gagal memuat file Google Drive."
                _driveError.value = if (msg.contains("401")) {
                    "Sesi token Google Drive kedaluwarsa. Hubungkan ulang akun Drive Anda."
                } else {
                    msg
                }
            }
        }
    }

    fun setDriveFilter(filter: DriveFilter) {
        _driveFilter.value = filter
    }

    fun setDriveAutoPlay(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setDriveAutoPlay(enabled)
        }
    }

    fun autoPlayAllDriveMedia() {
        val tracks = driveUiState.value.tracks
        if (tracks.isNotEmpty()) {
            playTracks(tracks, 0, openFullIfVideo = true)
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            app.cachedDriveToken = null
            repo.clearDriveData()
            _driveError.value = null
        }
    }

    // Equalizer Custom Presets
    fun saveCurrentAsCustomPreset(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val eq = equalizerState.value
        val preset = EqualizerPreset(
            name = cleanName,
            normalizedBands = eq.normalizedBands,
            bassBoost = eq.bassBoostStrength,
            virtualizer = eq.virtualizerStrength,
            loudnessMb = eq.loudnessGainMb,
            reverbPreset = eq.reverbPreset,
            isCustom = true
        )
        viewModelScope.launch {
            mediaDao.upsertCustomPreset(CustomEqPresetEntity.fromDomain(preset))
            audioEffectEngine.applyPreset(preset)
        }
    }

    fun deleteCustomPreset(name: String) {
        viewModelScope.launch {
            mediaDao.deleteCustomPreset(name)
        }
    }
}
