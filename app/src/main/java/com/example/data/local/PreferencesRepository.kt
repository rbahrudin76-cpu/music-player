package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.EqualizerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auraplay_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val DRIVE_TOKEN = stringPreferencesKey("drive_access_token")
        val DRIVE_ACCOUNT_NAME = stringPreferencesKey("drive_account_name")
        val DRIVE_ACCOUNT_EMAIL = stringPreferencesKey("drive_account_email")
        val DRIVE_AUTO_PLAY = booleanPreferencesKey("drive_auto_play")

        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS = stringPreferencesKey("eq_bands_csv")
        val EQ_BASS = intPreferencesKey("eq_bass_boost")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val EQ_LOUDNESS = intPreferencesKey("eq_loudness")
        val EQ_REVERB = intPreferencesKey("eq_reverb")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
    }

    val driveTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.DRIVE_TOKEN] }

    val driveAccountNameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.DRIVE_ACCOUNT_NAME] }

    val driveAccountEmailFlow: Flow<String?> = context.dataStore.data.map { it[Keys.DRIVE_ACCOUNT_EMAIL] }

    val driveAutoPlayFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.DRIVE_AUTO_PLAY] ?: true }

    suspend fun getDriveToken(): String? = context.dataStore.data.first()[Keys.DRIVE_TOKEN]

    suspend fun saveDriveToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token.isNullOrBlank()) {
                prefs.remove(Keys.DRIVE_TOKEN)
            } else {
                prefs[Keys.DRIVE_TOKEN] = token
            }
        }
    }

    suspend fun saveDriveAccount(name: String?, email: String?) {
        context.dataStore.edit { prefs ->
            if (name != null) prefs[Keys.DRIVE_ACCOUNT_NAME] = name else prefs.remove(Keys.DRIVE_ACCOUNT_NAME)
            if (email != null) prefs[Keys.DRIVE_ACCOUNT_EMAIL] = email else prefs.remove(Keys.DRIVE_ACCOUNT_EMAIL)
        }
    }

    suspend fun setDriveAutoPlay(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DRIVE_AUTO_PLAY] = enabled }
    }

    suspend fun saveEqualizerState(state: EqualizerState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EQ_ENABLED] = state.enabled
            prefs[Keys.EQ_PRESET] = state.selectedPresetName
            prefs[Keys.EQ_BANDS] = state.normalizedBands.joinToString(",")
            prefs[Keys.EQ_BASS] = state.bassBoostStrength
            prefs[Keys.EQ_VIRTUALIZER] = state.virtualizerStrength
            prefs[Keys.EQ_LOUDNESS] = state.loudnessGainMb
            prefs[Keys.EQ_REVERB] = state.reverbPreset
            prefs[Keys.PLAYBACK_SPEED] = state.playbackSpeed
            prefs[Keys.PLAYBACK_PITCH] = state.playbackPitch
        }
    }

    suspend fun loadEqualizerState(): EqualizerState {
        val prefs = context.dataStore.data.first()
        val bands = prefs[Keys.EQ_BANDS]
            ?.split(",")
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 10 }
            ?: List(10) { 0f }
        return EqualizerState(
            enabled = prefs[Keys.EQ_ENABLED] ?: true,
            selectedPresetName = prefs[Keys.EQ_PRESET] ?: "Studio Balanced",
            normalizedBands = bands,
            bassBoostStrength = prefs[Keys.EQ_BASS] ?: 250,
            virtualizerStrength = prefs[Keys.EQ_VIRTUALIZER] ?: 200,
            loudnessGainMb = prefs[Keys.EQ_LOUDNESS] ?: 150,
            reverbPreset = prefs[Keys.EQ_REVERB] ?: 0,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            playbackPitch = prefs[Keys.PLAYBACK_PITCH] ?: 1.0f
        )
    }
}
