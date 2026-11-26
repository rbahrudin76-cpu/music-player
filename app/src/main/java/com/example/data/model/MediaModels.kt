package com.example.data.model

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class MediaSource {
    LOCAL,
    ONLINE_AUDIUS,
    ONLINE_ITUNES,
    GOOGLE_DRIVE,
    DIRECT_URL
}

data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val uri: String,
    val artworkUri: String? = null,
    val mediaType: MediaType,
    val source: MediaSource,
    val format: String,
    val mimeType: String,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val folderName: String = "",
    val driveFileId: String? = null,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

enum class RepeatModeOption {
    OFF,
    ONE,
    ALL
}

enum class VideoAspectMode(val label: String) {
    FIT("Fit"),
    FILL("Penuh"),
    WIDE_16_9("16:9")
}

data class EqualizerPreset(
    val name: String,
    val normalizedBands: List<Float>, // 10 bands, range -1.0f..1.0f
    val bassBoost: Int,               // 0..1000
    val virtualizer: Int,             // 0..1000
    val loudnessMb: Int,              // 0..1500
    val reverbPreset: Int,            // 0..6
    val isCustom: Boolean = false
)

data class EqualizerState(
    val enabled: Boolean = true,
    val selectedPresetName: String = "Studio Balanced",
    val normalizedBands: List<Float> = List(10) { 0f },
    val bandFrequenciesHz: List<Int> = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000),
    val minGainDb: Float = -15f,
    val maxGainDb: Float = 15f,
    val bassBoostStrength: Int = 250,
    val virtualizerStrength: Int = 200,
    val loudnessGainMb: Int = 150,
    val reverbPreset: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val audioSessionId: Int = 0,
    val isHardwareAttached: Boolean = false
)

data class DriveAccountInfo(
    val displayName: String,
    val email: String,
    val photoUrl: String? = null
)

object SupportedMediaFormats {
    val audioExtensions = setOf(
        "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "wma",
        "alac", "aiff", "amr", "3ga", "ac3", "eac3", "dts", "mid", "midi", "mka", "ape", "wv"
    )

    val videoExtensions = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "flv", "3gp", "3g2",
        "ts", "m2ts", "mts", "m4v", "mpg", "mpeg", "wmv", "vob", "ogv", "m3u8", "mpd"
    )

    fun detectFormat(fileName: String, mimeType: String?): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isNotBlank() && ext.length <= 6) {
            return ext.uppercase()
        }
        val mime = mimeType.orEmpty().lowercase()
        return when {
            "flac" in mime -> "FLAC"
            "mpeg" in mime || "mp3" in mime -> "MP3"
            "mp4" in mime -> "MP4"
            "matroska" in mime || "mkv" in mime -> "MKV"
            "webm" in mime -> "WEBM"
            "opus" in mime -> "OPUS"
            "ogg" in mime -> "OGG"
            "aac" in mime || "mp4a" in mime -> "AAC"
            "wav" in mime -> "WAV"
            "x-msvideo" in mime || "avi" in mime -> "AVI"
            "quicktime" in mime -> "MOV"
            "mpegurl" in mime || "m3u8" in mime -> "HLS"
            "dash" in mime -> "DASH"
            mime.startsWith("audio/") -> "AUDIO"
            mime.startsWith("video/") -> "VIDEO"
            else -> "MEDIA"
        }
    }

    fun detectMediaType(fileName: String, mimeType: String?): MediaType? {
        val mime = mimeType.orEmpty().lowercase()
        if (mime.startsWith("video/")) return MediaType.VIDEO
        if (mime.startsWith("audio/")) return MediaType.AUDIO
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext in videoExtensions -> MediaType.VIDEO
            ext in audioExtensions -> MediaType.AUDIO
            else -> null
        }
    }
}
