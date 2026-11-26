package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.LinkedFolderEntity
import com.example.data.local.MediaDao
import com.example.data.local.MediaTrackEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.DriveAccountInfo
import com.example.data.model.MediaSource
import com.example.data.model.MediaTrack
import com.example.data.model.MediaType
import com.example.data.model.SupportedMediaFormats
import com.example.data.remote.AudiusApiService
import com.example.data.remote.GoogleDriveApiService
import com.example.data.remote.ItunesApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class UnifiedSearchResult(
    val offlineResults: List<MediaTrack> = emptyList(),
    val audiusResults: List<MediaTrack> = emptyList(),
    val itunesResults: List<MediaTrack> = emptyList(),
    val audiusError: String? = null,
    val itunesError: String? = null
)

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val prefs: PreferencesRepository,
    private val audiusApi: AudiusApiService,
    private val itunesApi: ItunesApiService,
    private val driveApi: GoogleDriveApiService
) {
    val localTracksFlow: Flow<List<MediaTrack>> =
        mediaDao.observeLocalAndSavedTracks().map { list -> list.map { it.toDomain() } }

    val driveTracksFlow: Flow<List<MediaTrack>> =
        mediaDao.observeDriveTracks().map { list -> list.map { it.toDomain() } }

    val favoritesFlow: Flow<List<MediaTrack>> =
        mediaDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    val linkedFoldersFlow: Flow<List<LinkedFolderEntity>> = mediaDao.observeLinkedFolders()

    val searchHistoryFlow: Flow<List<String>> =
        mediaDao.observeSearchHistory().map { list -> list.map { it.query } }

    suspend fun toggleFavorite(track: MediaTrack) {
        val nextFav = !track.isFavorite
        val updated = track.copy(isFavorite = nextFav)
        mediaDao.upsertTrack(MediaTrackEntity.fromDomain(updated))
    }

    suspend fun addPickedUris(uris: List<Uri>): List<MediaTrack> = withContext(Dispatchers.IO) {
        val favoriteIds = mediaDao.getFavoriteIds().toSet()
        val imported = mutableListOf<MediaTrack>()
        for (uri in uris) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val track = extractTrackFromUri(uri, folderName = "File Pilihan", favoriteIds = favoriteIds)
            if (track != null) {
                imported += track
            }
        }
        if (imported.isNotEmpty()) {
            mediaDao.upsertTracks(imported.map { MediaTrackEntity.fromDomain(it) })
        }
        imported
    }

    suspend fun addDirectStreamUrl(rawUrl: String, customTitle: String? = null): MediaTrack? =
        withContext(Dispatchers.IO) {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank()) return@withContext null
            val parsed = Uri.parse(trimmed)
            val fileName = parsed.lastPathSegment ?: "Stream Media"
            val detectedType = SupportedMediaFormats.detectMediaType(fileName, null)
                ?: if (trimmed.contains("m3u8", true) || trimmed.contains("mpd", true) || trimmed.contains("mp4", true) || trimmed.contains("mkv", true)) {
                    MediaType.VIDEO
                } else {
                    MediaType.AUDIO
                }
            val format = SupportedMediaFormats.detectFormat(fileName, null)
            val track = MediaTrack(
                id = "url_${trimmed.hashCode()}",
                title = customTitle?.takeIf { it.isNotBlank() }
                    ?: fileName.substringBeforeLast('.').ifBlank { trimmed },
                artist = parsed.host ?: "Direct Stream",
                album = "Network Stream",
                uri = trimmed,
                mediaType = detectedType,
                source = MediaSource.DIRECT_URL,
                format = format,
                mimeType = if (detectedType == MediaType.VIDEO) "video/*" else "audio/*"
            )
            mediaDao.upsertTrack(MediaTrackEntity.fromDomain(track))
            track
        }

    suspend fun linkAndScanFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
        val folderName = rootDoc.name ?: "Folder Perangkat"
        mediaDao.upsertLinkedFolder(
            LinkedFolderEntity(
                uri = treeUri.toString(),
                displayName = folderName,
                lastScannedAt = System.currentTimeMillis()
            )
        )
        scanSingleFolder(rootDoc, folderName)
    }

    suspend fun rescanAllLinkedFolders(): Int = withContext(Dispatchers.IO) {
        val folders = mediaDao.getLinkedFoldersOnce()
        var total = 0
        for (folder in folders) {
            val treeUri = Uri.parse(folder.uri)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null && rootDoc.canRead()) {
                total += scanSingleFolder(rootDoc, folder.displayName)
                mediaDao.upsertLinkedFolder(folder.copy(lastScannedAt = System.currentTimeMillis()))
            }
        }
        total
    }

    suspend fun removeLinkedFolder(folder: LinkedFolderEntity) = withContext(Dispatchers.IO) {
        mediaDao.deleteLinkedFolder(folder.uri)
        mediaDao.clearFolderTracks(folder.displayName)
    }

    private suspend fun scanSingleFolder(rootDoc: DocumentFile, folderName: String): Int {
        val favoriteIds = mediaDao.getFavoriteIds().toSet()
        val found = mutableListOf<MediaTrack>()
        traverseDocumentTree(rootDoc, folderName, favoriteIds, found, depth = 0)
        mediaDao.clearFolderTracks(folderName)
        if (found.isNotEmpty()) {
            mediaDao.upsertTracks(found.map { MediaTrackEntity.fromDomain(it) })
        }
        return found.size
    }

    private fun traverseDocumentTree(
        dir: DocumentFile,
        folderName: String,
        favoriteIds: Set<String>,
        out: MutableList<MediaTrack>,
        depth: Int
    ) {
        if (depth > 6 || out.size >= 800) return
        val children = runCatching { dir.listFiles() }.getOrElse { emptyArray() }
        for (child in children) {
            if (child.isDirectory) {
                traverseDocumentTree(child, folderName, favoriteIds, out, depth + 1)
            } else if (child.isFile) {
                val name = child.name ?: continue
                val mime = child.type
                val mediaType = SupportedMediaFormats.detectMediaType(name, mime) ?: continue
                val format = SupportedMediaFormats.detectFormat(name, mime)
                val uriStr = child.uri.toString()
                val id = "local_${uriStr.hashCode()}"
                val meta = readQuickMetadata(child.uri, name)
                out += MediaTrack(
                    id = id,
                    title = meta.title,
                    artist = meta.artist,
                    album = meta.album,
                    uri = uriStr,
                    mediaType = mediaType,
                    source = MediaSource.LOCAL,
                    format = format,
                    mimeType = mime ?: if (mediaType == MediaType.VIDEO) "video/*" else "audio/*",
                    durationMs = meta.durationMs,
                    fileSizeBytes = child.length(),
                    folderName = folderName,
                    isFavorite = id in favoriteIds
                )
            }
        }
    }

    private fun extractTrackFromUri(
        uri: Uri,
        folderName: String,
        favoriteIds: Set<String>
    ): MediaTrack? {
        var displayName = uri.lastPathSegment ?: "Media"
        var sizeBytes = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }
        val mime = context.contentResolver.getType(uri)
        val mediaType = SupportedMediaFormats.detectMediaType(displayName, mime) ?: MediaType.AUDIO
        val format = SupportedMediaFormats.detectFormat(displayName, mime)
        val id = "local_${uri.toString().hashCode()}"
        val meta = readQuickMetadata(uri, displayName)
        return MediaTrack(
            id = id,
            title = meta.title,
            artist = meta.artist,
            album = meta.album,
            uri = uri.toString(),
            mediaType = mediaType,
            source = MediaSource.LOCAL,
            format = format,
            mimeType = mime ?: if (mediaType == MediaType.VIDEO) "video/*" else "audio/*",
            durationMs = meta.durationMs,
            fileSizeBytes = sizeBytes,
            folderName = folderName,
            isFavorite = id in favoriteIds
        )
    }

    private data class QuickMeta(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long
    )

    private fun readQuickMetadata(uri: Uri, fallbackFileName: String): QuickMeta {
        val cleanName = fallbackFileName.substringBeforeLast('.').ifBlank { fallbackFileName }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: cleanName
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Artis Lokal"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: ""
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            QuickMeta(title, artist, album, duration)
        } catch (_: Exception) {
            QuickMeta(cleanName, "Artis Lokal", "", 0L)
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun searchOfflineAndOnline(query: String): UnifiedSearchResult =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext UnifiedSearchResult()

            mediaDao.upsertSearchHistory(
                SearchHistoryEntity(query = trimmed, timestamp = System.currentTimeMillis())
            )

            val offline = mediaDao.searchOffline(trimmed).map { it.toDomain() }

            var audiusError: String? = null
            val audius = runCatching {
                audiusApi.searchTracks(trimmed).data.mapNotNull { dto ->
                    if (dto.id.isBlank()) return@mapNotNull null
                    MediaTrack(
                        id = "audius_${dto.id}",
                        title = dto.title.ifBlank { "Untitled Track" },
                        artist = dto.user?.name?.takeIf { it.isNotBlank() }
                            ?: dto.user?.handle ?: "Audius Artist",
                        album = dto.genre ?: "Audius Stream",
                        uri = "https://api.audius.co/v1/tracks/${dto.id}/stream?app_name=AuraPlay",
                        artworkUri = dto.artwork?.x480 ?: dto.artwork?.x150,
                        mediaType = MediaType.AUDIO,
                        source = MediaSource.ONLINE_AUDIUS,
                        format = "MP3",
                        mimeType = "audio/mpeg",
                        durationMs = dto.duration * 1000L
                    )
                }
            }.getOrElse { e ->
                audiusError = e.localizedMessage ?: "Gagal memuat Audius"
                emptyList()
            }

            var itunesError: String? = null
            val itunes = runCatching {
                itunesApi.searchMedia(trimmed).results.mapNotNull { dto ->
                    val preview = dto.previewUrl ?: return@mapNotNull null
                    val isVideo = dto.kind == "music-video" || preview.endsWith(".m4v", true) || preview.endsWith(".mp4", true)
                    val art = dto.artworkUrl100?.replace("100x100bb", "600x600bb")
                    MediaTrack(
                        id = "itunes_${dto.trackId ?: preview.hashCode()}",
                        title = dto.trackName ?: "Unknown Title",
                        artist = dto.artistName ?: "Unknown Artist",
                        album = dto.collectionName ?: dto.primaryGenreName ?: "iTunes",
                        uri = preview,
                        artworkUri = art,
                        mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                        source = MediaSource.ONLINE_ITUNES,
                        format = if (isVideo) "M4V" else "M4A",
                        mimeType = if (isVideo) "video/mp4" else "audio/mp4",
                        durationMs = dto.trackTimeMillis ?: 30_000L
                    )
                }
            }.getOrElse { e ->
                itunesError = e.localizedMessage ?: "Gagal memuat iTunes"
                emptyList()
            }

            UnifiedSearchResult(
                offlineResults = offline,
                audiusResults = audius,
                itunesResults = itunes,
                audiusError = audiusError,
                itunesError = itunesError
            )
        }

    suspend fun getTrendingOnlineTracks(): List<MediaTrack> = withContext(Dispatchers.IO) {
        runCatching {
            audiusApi.getTrendingTracks().data.mapNotNull { dto ->
                if (dto.id.isBlank()) return@mapNotNull null
                MediaTrack(
                    id = "audius_${dto.id}",
                    title = dto.title.ifBlank { "Trending Track" },
                    artist = dto.user?.name ?: dto.user?.handle ?: "Audius",
                    album = dto.genre ?: "Trending",
                    uri = "https://api.audius.co/v1/tracks/${dto.id}/stream?app_name=AuraPlay",
                    artworkUri = dto.artwork?.x480 ?: dto.artwork?.x150,
                    mediaType = MediaType.AUDIO,
                    source = MediaSource.ONLINE_AUDIUS,
                    format = "MP3",
                    mimeType = "audio/mpeg",
                    durationMs = dto.duration * 1000L
                )
            }
        }.getOrElse { emptyList() }
    }

    suspend fun deleteSearchHistory(query: String) {
        mediaDao.deleteSearchHistory(query)
    }

    suspend fun syncGoogleDriveMedia(accessToken: String): Result<Pair<DriveAccountInfo?, List<MediaTrack>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bearer = "Bearer $accessToken"
                val about = runCatching { driveApi.getAbout(bearer) }.getOrNull()
                val accountInfo = about?.user?.let {
                    DriveAccountInfo(
                        displayName = it.displayName ?: "Pengguna Google Drive",
                        email = it.emailAddress ?: "",
                        photoUrl = it.photoLink
                    )
                }
                if (accountInfo != null) {
                    prefs.saveDriveAccount(accountInfo.displayName, accountInfo.email)
                }

                val favoriteIds = mediaDao.getFavoriteIds().toSet()
                val allDriveFiles = mutableListOf<MediaTrack>()
                var pageToken: String? = null
                var pageCount = 0
                do {
                    val response = driveApi.listMediaFiles(
                        bearerToken = bearer,
                        pageToken = pageToken
                    )
                    for (file in response.files) {
                        val mediaType = SupportedMediaFormats.detectMediaType(file.name, file.mimeType)
                            ?: continue
                        val format = SupportedMediaFormats.detectFormat(file.name, file.mimeType)
                        val id = "drive_${file.id}"
                        val duration = file.videoMediaMetadata?.durationMillis?.toLongOrNull() ?: 0L
                        allDriveFiles += MediaTrack(
                            id = id,
                            title = file.name.substringBeforeLast('.').ifBlank { file.name },
                            artist = "Google Drive",
                            album = if (mediaType == MediaType.VIDEO) "Drive Video" else "Drive Audio",
                            uri = "https://www.googleapis.com/drive/v3/files/${file.id}?alt=media",
                            artworkUri = file.thumbnailLink,
                            mediaType = mediaType,
                            source = MediaSource.GOOGLE_DRIVE,
                            format = format,
                            mimeType = file.mimeType,
                            durationMs = duration,
                            fileSizeBytes = file.size?.toLongOrNull() ?: 0L,
                            folderName = "Google Drive",
                            driveFileId = file.id,
                            isFavorite = id in favoriteIds
                        )
                    }
                    pageToken = response.nextPageToken
                    pageCount++
                } while (!pageToken.isNullOrBlank() && pageCount < 5)

                mediaDao.clearDriveTracks()
                if (allDriveFiles.isNotEmpty()) {
                    mediaDao.upsertTracks(allDriveFiles.map { MediaTrackEntity.fromDomain(it) })
                }
                accountInfo to allDriveFiles
            }
        }

    suspend fun clearDriveData() = withContext(Dispatchers.IO) {
        prefs.saveDriveToken(null)
        prefs.saveDriveAccount(null, null)
        mediaDao.clearDriveTracks()
    }
}
