package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class AudiusSearchResponse(
    val data: List<AudiusTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusTrackDto(
    val id: String = "",
    val title: String = "",
    val duration: Long = 0L,
    val genre: String? = null,
    val artwork: AudiusArtworkDto? = null,
    val user: AudiusUserDto? = null
)

@JsonClass(generateAdapter = true)
data class AudiusArtworkDto(
    @Json(name = "150x150") val x150: String? = null,
    @Json(name = "480x480") val x480: String? = null,
    @Json(name = "1000x1000") val x1000: String? = null
)

@JsonClass(generateAdapter = true)
data class AudiusUserDto(
    val name: String? = null,
    val handle: String? = null
)

interface AudiusApiService {
    @GET("v1/tracks/search")
    suspend fun searchTracks(
        @Query("query") query: String,
        @Query("app_name") appName: String = "AuraPlay"
    ): AudiusSearchResponse

    @GET("v1/tracks/trending")
    suspend fun getTrendingTracks(
        @Query("app_name") appName: String = "AuraPlay",
        @Query("limit") limit: Int = 15
    ): AudiusSearchResponse
}
