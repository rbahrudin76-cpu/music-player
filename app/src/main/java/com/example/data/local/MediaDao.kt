package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.data.model.EqualizerPreset
import com.example.data.model.MediaSource
import com.example.data.model.MediaTrack
import com.example.data.model.MediaType
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "media_tracks")
data class MediaTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val artworkUri: String?,
    val mediaType: String,
    val source: String,
    val format: String,
    val mimeType: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val folderName: String,
    val driveFileId: String?,
    val isFavorite: Boolean,
    val addedAt: Long
) {
    fun toDomain(): MediaTrack = MediaTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        uri = uri,
        artworkUri = artworkUri,
        mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.AUDIO),
        source = runCatching { MediaSource.valueOf(source) }.getOrDefault(MediaSource.LOCAL),
        format = format,
        mimeType = mimeType,
        durationMs = durationMs,
        fileSizeBytes = fileSizeBytes,
        folderName = folderName,
        driveFileId = driveFileId,
        isFavorite = isFavorite,
        addedAt = addedAt
    )

    companion object {
        fun fromDomain(track: MediaTrack): MediaTrackEntity = MediaTrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            uri = track.uri,
            artworkUri = track.artworkUri,
            mediaType = track.mediaType.name,
            source = track.source.name,
            format = track.format,
            mimeType = track.mimeType,
            durationMs = track.durationMs,
            fileSizeBytes = track.fileSizeBytes,
            folderName = track.folderName,
            driveFileId = track.driveFileId,
            isFavorite = track.isFavorite,
            addedAt = track.addedAt
        )
    }
}

@Entity(tableName = "linked_folders")
data class LinkedFolderEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val lastScannedAt: Long
)

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val name: String,
    val bandsCsv: String,
    val bassBoost: Int,
    val virtualizer: Int,
    val loudnessMb: Int,
    val reverbPreset: Int
) {
    fun toDomain(): EqualizerPreset = EqualizerPreset(
        name = name,
        normalizedBands = bandsCsv.split(",").mapNotNull { it.toFloatOrNull() }
            .let { if (it.size == 10) it else List(10) { 0f } },
        bassBoost = bassBoost,
        virtualizer = virtualizer,
        loudnessMb = loudnessMb,
        reverbPreset = reverbPreset,
        isCustom = true
    )

    companion object {
        fun fromDomain(preset: EqualizerPreset): CustomEqPresetEntity = CustomEqPresetEntity(
            name = preset.name,
            bandsCsv = preset.normalizedBands.joinToString(","),
            bassBoost = preset.bassBoost,
            virtualizer = preset.virtualizer,
            loudnessMb = preset.loudnessMb,
            reverbPreset = preset.reverbPreset
        )
    }
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long
)

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_tracks WHERE source != 'GOOGLE_DRIVE' ORDER BY addedAt DESC")
    fun observeLocalAndSavedTracks(): Flow<List<MediaTrackEntity>>

    @Query("SELECT * FROM media_tracks WHERE source = 'GOOGLE_DRIVE' ORDER BY title ASC")
    fun observeDriveTracks(): Flow<List<MediaTrackEntity>>

    @Query("SELECT * FROM media_tracks WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<MediaTrackEntity>>

    @Query(
        """
        SELECT * FROM media_tracks
        WHERE title LIKE '%' || :query || '%'
           OR artist LIKE '%' || :query || '%'
           OR album LIKE '%' || :query || '%'
           OR format LIKE '%' || :query || '%'
           OR folderName LIKE '%' || :query || '%'
        ORDER BY isFavorite DESC, addedAt DESC
        """
    )
    suspend fun searchOffline(query: String): List<MediaTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<MediaTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: MediaTrackEntity)

    @Query("UPDATE media_tracks SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("SELECT id FROM media_tracks WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>

    @Query("DELETE FROM media_tracks WHERE source = 'GOOGLE_DRIVE'")
    suspend fun clearDriveTracks()

    @Query("DELETE FROM media_tracks WHERE folderName = :folderName AND source = 'LOCAL'")
    suspend fun clearFolderTracks(folderName: String)

    @Query("DELETE FROM media_tracks WHERE id = :id")
    suspend fun deleteTrack(id: String)

    @Query("SELECT * FROM linked_folders ORDER BY lastScannedAt DESC")
    fun observeLinkedFolders(): Flow<List<LinkedFolderEntity>>

    @Query("SELECT * FROM linked_folders")
    suspend fun getLinkedFoldersOnce(): List<LinkedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLinkedFolder(folder: LinkedFolderEntity)

    @Query("DELETE FROM linked_folders WHERE uri = :uri")
    suspend fun deleteLinkedFolder(uri: String)

    @Query("SELECT * FROM custom_eq_presets ORDER BY name ASC")
    fun observeCustomPresets(): Flow<List<CustomEqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomPreset(preset: CustomEqPresetEntity)

    @Query("DELETE FROM custom_eq_presets WHERE name = :name")
    suspend fun deleteCustomPreset(name: String)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchHistory(query: String)
}
