package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class DriveAboutResponse(
    val user: DriveUserDto? = null
)

@JsonClass(generateAdapter = true)
data class DriveUserDto(
    val displayName: String? = null,
    val emailAddress: String? = null,
    val photoLink: String? = null
)

@JsonClass(generateAdapter = true)
data class DriveFilesResponse(
    val nextPageToken: String? = null,
    val files: List<DriveFileDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DriveFileDto(
    val id: String = "",
    val name: String = "",
    val mimeType: String = "",
    val size: String? = null,
    val thumbnailLink: String? = null,
    val videoMediaMetadata: DriveVideoMetadataDto? = null
)

@JsonClass(generateAdapter = true)
data class DriveVideoMetadataDto(
    val width: Int? = null,
    val height: Int? = null,
    val durationMillis: String? = null
)

interface GoogleDriveApiService {
    @GET("drive/v3/about")
    suspend fun getAbout(
        @Header("Authorization") bearerToken: String,
        @Query("fields") fields: String = "user(displayName,emailAddress,photoLink)"
    ): DriveAboutResponse

    @GET("drive/v3/files")
    suspend fun listMediaFiles(
        @Header("Authorization") bearerToken: String,
        @Query("q") query: String = DRIVE_MEDIA_QUERY,
        @Query("fields") fields: String = "nextPageToken,files(id,name,mimeType,size,thumbnailLink,videoMediaMetadata)",
        @Query("pageSize") pageSize: Int = 100,
        @Query("pageToken") pageToken: String? = null,
        @Query("orderBy") orderBy: String = "modifiedTime desc"
    ): DriveFilesResponse

    companion object {
        const val DRIVE_MEDIA_QUERY =
            "(mimeType contains 'audio/' or mimeType contains 'video/' " +
                "or name contains '.mp3' or name contains '.flac' or name contains '.wav' " +
                "or name contains '.aac' or name contains '.m4a' or name contains '.ogg' " +
                "or name contains '.opus' or name contains '.mp4' or name contains '.mkv' " +
                "or name contains '.webm' or name contains '.avi' or name contains '.mov') " +
                "and trashed = false"
    }
}
