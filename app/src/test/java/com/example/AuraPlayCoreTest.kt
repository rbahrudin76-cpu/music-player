package com.example

import com.example.data.model.MediaType
import com.example.data.model.SupportedMediaFormats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuraPlayCoreTest {

    @Test
    fun `detectMediaType handles audio and video extensions`() {
        assertEquals(MediaType.AUDIO, SupportedMediaFormats.detectMediaType("song.flac", null))
        assertEquals(MediaType.AUDIO, SupportedMediaFormats.detectMediaType("track.opus", null))
        assertEquals(MediaType.VIDEO, SupportedMediaFormats.detectMediaType("movie.mkv", null))
        assertEquals(MediaType.VIDEO, SupportedMediaFormats.detectMediaType("stream.m3u8", null))
        assertNull(SupportedMediaFormats.detectMediaType("notes.pdf", "application/pdf"))
    }

    @Test
    fun `detectFormat extracts container badge`() {
        assertEquals("FLAC", SupportedMediaFormats.detectFormat("master.flac", "audio/flac"))
        assertEquals("MKV", SupportedMediaFormats.detectFormat("cinema.mkv", "video/x-matroska"))
        assertEquals("MP3", SupportedMediaFormats.detectFormat("track", "audio/mpeg"))
    }
}
