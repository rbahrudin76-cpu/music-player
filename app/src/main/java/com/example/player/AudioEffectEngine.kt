package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import com.example.data.local.PreferencesRepository
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

class AudioEffectEngine(
    private val prefs: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

    val builtInPresets: List<EqualizerPreset> = listOf(
        EqualizerPreset(
            name = "Studio Balanced",
            normalizedBands = listOf(0.05f, 0.05f, 0f, 0f, 0f, 0f, 0.05f, 0.1f, 0.1f, 0.05f),
            bassBoost = 150,
            virtualizer = 150,
            loudnessMb = 100,
            reverbPreset = 0
        ),
        EqualizerPreset(
            name = "Flat",
            normalizedBands = List(10) { 0f },
            bassBoost = 0,
            virtualizer = 0,
            loudnessMb = 0,
            reverbPreset = 0
        ),
        EqualizerPreset(
            name = "Bass Extreme",
            normalizedBands = listOf(0.85f, 0.75f, 0.55f, 0.25f, 0f, -0.05f, 0.05f, 0.15f, 0.2f, 0.15f),
            bassBoost = 850,
            virtualizer = 250,
            loudnessMb = 350,
            reverbPreset = 0
        ),
        EqualizerPreset(
            name = "Electronic / EDM",
            normalizedBands = listOf(0.65f, 0.55f, 0.25f, -0.1f, -0.15f, 0.1f, 0.35f, 0.5f, 0.6f, 0.55f),
            bassBoost = 650,
            virtualizer = 500,
            loudnessMb = 300,
            reverbPreset = 2
        ),
        EqualizerPreset(
            name = "Vocal Clarity",
            normalizedBands = listOf(-0.25f, -0.15f, -0.05f, 0.15f, 0.45f, 0.6f, 0.5f, 0.3f, 0.1f, -0.05f),
            bassBoost = 100,
            virtualizer = 200,
            loudnessMb = 250,
            reverbPreset = 1
        ),
        EqualizerPreset(
            name = "Acoustic",
            normalizedBands = listOf(0.25f, 0.2f, 0.1f, 0.05f, 0.15f, 0.2f, 0.3f, 0.35f, 0.25f, 0.15f),
            bassBoost = 200,
            virtualizer = 300,
            loudnessMb = 150,
            reverbPreset = 1
        ),
        EqualizerPreset(
            name = "Rock",
            normalizedBands = listOf(0.55f, 0.45f, 0.2f, -0.1f, -0.2f, 0.05f, 0.35f, 0.55f, 0.6f, 0.5f),
            bassBoost = 500,
            virtualizer = 350,
            loudnessMb = 250,
            reverbPreset = 2
        ),
        EqualizerPreset(
            name = "Jazz",
            normalizedBands = listOf(0.3f, 0.2f, 0.05f, 0.2f, -0.1f, -0.1f, 0.15f, 0.3f, 0.4f, 0.35f),
            bassBoost = 250,
            virtualizer = 400,
            loudnessMb = 100,
            reverbPreset = 2
        ),
        EqualizerPreset(
            name = "Cinema 3D",
            normalizedBands = listOf(0.5f, 0.4f, 0.2f, 0.05f, 0.1f, 0.2f, 0.35f, 0.45f, 0.4f, 0.3f),
            bassBoost = 600,
            virtualizer = 850,
            loudnessMb = 400,
            reverbPreset = 3
        ),
        EqualizerPreset(
            name = "Late Night",
            normalizedBands = listOf(-0.2f, -0.1f, 0.05f, 0.2f, 0.25f, 0.2f, 0.15f, 0.05f, -0.1f, -0.2f),
            bassBoost = 100,
            virtualizer = 100,
            loudnessMb = 450,
            reverbPreset = 0
        )
    )

    init {
        scope.launch {
            val saved = prefs.loadEqualizerState()
            _state.value = saved
        }
    }

    @Synchronized
    fun attachToAudioSession(sessionId: Int) {
        if (sessionId == 0) return
        if (_state.value.audioSessionId == sessionId && _state.value.isHardwareAttached) {
            applyAllEffectsToHardware()
            return
        }
        releaseHardwareEffects()

        var attached = false
        runCatching {
            equalizer = Equalizer(0, sessionId).apply { enabled = _state.value.enabled }
            attached = true
        }
        runCatching {
            bassBoost = BassBoost(0, sessionId).apply { enabled = _state.value.enabled }
        }
        runCatching {
            virtualizer = Virtualizer(0, sessionId).apply { enabled = _state.value.enabled }
        }
        runCatching {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = _state.value.enabled }
        }
        runCatching {
            presetReverb = PresetReverb(0, sessionId).apply { enabled = _state.value.enabled && _state.value.reverbPreset > 0 }
        }

        val eq = equalizer
        val minDb = (eq?.bandLevelRange?.getOrNull(0)?.toFloat() ?: -1500f) / 100f
        val maxDb = (eq?.bandLevelRange?.getOrNull(1)?.toFloat() ?: 1500f) / 100f

        _state.update {
            it.copy(
                audioSessionId = sessionId,
                isHardwareAttached = attached,
                minGainDb = minDb,
                maxGainDb = maxDb
            )
        }
        applyAllEffectsToHardware()
    }

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        applyAllEffectsToHardware()
        persistState()
    }

    fun setBandLevel(bandIndex: Int, normalizedValue: Float) {
        val clamped = normalizedValue.coerceIn(-1f, 1f)
        _state.update { current ->
            val updated = current.normalizedBands.toMutableList()
            if (bandIndex in updated.indices) {
                updated[bandIndex] = clamped
            }
            current.copy(
                normalizedBands = updated,
                selectedPresetName = "Custom"
            )
        }
        applyEqualizerBandsToHardware()
        persistState()
    }

    fun applyPreset(preset: EqualizerPreset) {
        _state.update {
            it.copy(
                enabled = true,
                selectedPresetName = preset.name,
                normalizedBands = preset.normalizedBands,
                bassBoostStrength = preset.bassBoost,
                virtualizerStrength = preset.virtualizer,
                loudnessGainMb = preset.loudnessMb,
                reverbPreset = preset.reverbPreset
            )
        }
        applyAllEffectsToHardware()
        persistState()
    }

    fun setBassBoost(strength: Int) {
        _state.update { it.copy(bassBoostStrength = strength.coerceIn(0, 1000)) }
        applyBassBoostToHardware()
        persistState()
    }

    fun setVirtualizer(strength: Int) {
        _state.update { it.copy(virtualizerStrength = strength.coerceIn(0, 1000)) }
        applyVirtualizerToHardware()
        persistState()
    }

    fun setLoudnessGain(gainMb: Int) {
        _state.update { it.copy(loudnessGainMb = gainMb.coerceIn(0, 1500)) }
        applyLoudnessToHardware()
        persistState()
    }

    fun setReverbPreset(preset: Int) {
        _state.update { it.copy(reverbPreset = preset.coerceIn(0, 6)) }
        applyReverbToHardware()
        persistState()
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed.coerceIn(0.5f, 2.0f)) }
        persistState()
    }

    fun setPlaybackPitch(pitch: Float) {
        _state.update { it.copy(playbackPitch = pitch.coerceIn(0.75f, 1.5f)) }
        persistState()
    }

    private fun applyAllEffectsToHardware() {
        applyEqualizerBandsToHardware()
        applyBassBoostToHardware()
        applyVirtualizerToHardware()
        applyLoudnessToHardware()
        applyReverbToHardware()
    }

    private fun applyEqualizerBandsToHardware() {
        val eq = equalizer ?: return
        val current = _state.value
        runCatching {
            eq.enabled = current.enabled
            val numHwBands = eq.numberOfBands.toInt()
            val minMb = eq.bandLevelRange[0].toFloat()
            val maxMb = eq.bandLevelRange[1].toFloat()
            val centerMb = (minMb + maxMb) / 2f
            val halfSpanMb = (maxMb - minMb) / 2f

            val uiFreqs = current.bandFrequenciesHz
            val uiLevels = current.normalizedBands

            for (hwIdx in 0 until numHwBands) {
                val hwFreqHz = (eq.getCenterFreq(hwIdx.toShort()) / 1000.0).coerceAtLeast(20.0)
                // Interpolate normalized gain from nearest studio bands in log-frequency space
                var weightedSum = 0.0
                var weightTotal = 0.0
                for (i in uiFreqs.indices) {
                    val dist = abs(ln(hwFreqHz) - ln(uiFreqs[i].toDouble()))
                    val w = 1.0 / (dist + 0.15)
                    weightedSum += uiLevels[i] * w
                    weightTotal += w
                }
                val norm = (weightedSum / weightTotal).toFloat().coerceIn(-1f, 1f)
                val targetMb = (centerMb + norm * halfSpanMb).roundToInt()
                    .coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                eq.setBandLevel(hwIdx.toShort(), targetMb)
            }
        }
    }

    private fun applyBassBoostToHardware() {
        val bb = bassBoost ?: return
        val current = _state.value
        runCatching {
            bb.enabled = current.enabled && current.bassBoostStrength > 0
            if (bb.strengthSupported) {
                bb.setStrength(current.bassBoostStrength.toShort())
            }
        }
    }

    private fun applyVirtualizerToHardware() {
        val virt = virtualizer ?: return
        val current = _state.value
        runCatching {
            virt.enabled = current.enabled && current.virtualizerStrength > 0
            if (virt.strengthSupported) {
                virt.setStrength(current.virtualizerStrength.toShort())
            }
        }
    }

    private fun applyLoudnessToHardware() {
        val le = loudnessEnhancer ?: return
        val current = _state.value
        runCatching {
            le.enabled = current.enabled && current.loudnessGainMb > 0
            le.setTargetGain(current.loudnessGainMb)
        }
    }

    private fun applyReverbToHardware() {
        val rev = presetReverb ?: return
        val current = _state.value
        runCatching {
            val presetCode = when (current.reverbPreset) {
                1 -> PresetReverb.PRESET_SMALLROOM
                2 -> PresetReverb.PRESET_MEDIUMROOM
                3 -> PresetReverb.PRESET_LARGEHALL
                4 -> PresetReverb.PRESET_PLATE
                else -> PresetReverb.PRESET_NONE
            }
            rev.preset = presetCode
            rev.enabled = current.enabled && current.reverbPreset > 0
        }
    }

    private fun persistState() {
        val snapshot = _state.value
        scope.launch {
            prefs.saveEqualizerState(snapshot)
        }
    }

    @Synchronized
    fun releaseHardwareEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null
    }
}
