package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItunesItemDto(
    val wrapperType: String? = null,
    val kind: String? = null,
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val previewUrl: String? = null,
    val artworkUrl100: String? = null,
    val trackTimeMillis: Long? = null,
    val primaryGenreName: String? = null
)

interface ItunesApiService {
    @GET("search")
    suspend fun searchMedia(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song,musicVideo",
        @Query("limit") limit: Int = 20
    ): ItunesSearchResponse
}
