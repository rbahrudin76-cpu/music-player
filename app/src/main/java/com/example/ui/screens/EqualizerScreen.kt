package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.player.PlaybackUiState
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt

private val reverbOptions = listOf(
    0 to "Off",
    1 to "Ruang Kecil",
    2 to "Studio",
    3 to "Aula Konser",
    4 to "Plate"
)

private val sleepTimerOptions = listOf(0, 15, 30, 60)

@Composable
fun EqualizerScreen(
    eqState: EqualizerState,
    playbackState: PlaybackUiState,
    builtInPresets: List<EqualizerPreset>,
    customPresets: List<EqualizerPreset>,
    onToggleEnabled: (Boolean) -> Unit,
    onBandChanged: (Int, Float) -> Unit,
    onApplyPreset: (EqualizerPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onLoudnessChanged: (Int) -> Unit,
    onReverbChanged: (Int) -> Unit,
    onSpeedPitchChanged: (Float, Float) -> Unit,
    onSleepTimerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    val allPresets = remember(builtInPresets, customPresets) {
        customPresets + builtInPresets
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Master Switch
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (eqState.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.eq_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = if (eqState.isHardwareAttached) {
                                "DSP Aktif • Session #${eqState.audioSessionId} • ${eqState.selectedPresetName}"
                            } else {
                                "Putar audio/video untuk mengaktifkan DSP • ${eqState.selectedPresetName}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Switch(
                        checked = eqState.enabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }
        }

        // Frequency Response Curve Canvas
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kurva Frekuensi (31Hz – 16kHz)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${eqState.minGainDb.roundToInt()}..+${eqState.maxGainDb.roundToInt()} dB",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    FrequencyCurveCanvas(
                        normalizedBands = eqState.normalizedBands,
                        enabled = eqState.enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // 10-Band Vertical Faders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        eqState.normalizedBands.forEachIndexed { index, value ->
                            val freqHz = eqState.bandFrequenciesHz.getOrElse(index) { 1000 }
                            val dbValue = value * eqState.maxGainDb
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (dbValue >= 0) "+${dbValue.roundToInt()}" else "${dbValue.roundToInt()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = if (eqState.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VerticalEqSlider(
                                        value = value,
                                        enabled = eqState.enabled,
                                        onValueChange = { onBandChanged(index, it) },
                                        modifier = Modifier.testTag("eq_band_slider_$index")
                                    )
                                }
                                Text(
                                    text = if (freqHz >= 1000) "${freqHz / 1000}k" else "$freqHz",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Presets Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.eq_presets_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.testTag("save_eq_preset_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.eq_save_preset))
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allPresets, key = { "${it.isCustom}_${it.name}" }) { preset ->
                        val isSelected = eqState.selectedPresetName == preset.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(preset.name) },
                            trailingIcon = if (preset.isCustom) {
                                {
                                    IconButton(
                                        onClick = { onDeleteCustomPreset(preset.name) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Hapus preset ${preset.name}",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier.testTag("eq_preset_${preset.name.lowercase().replace(" ", "_")}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // DSP Studio Effects (Bass Boost, 3D Virtualizer, Loudness Preamp)
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Modul DSP Audio",
                        style = MaterialTheme.typography.titleMedium
                    )

                    DspSliderRow(
                        label = stringResource(R.string.eq_bass_boost),
                        valueText = "${(eqState.bassBoostStrength / 10f).roundToInt()}%",
                        value = eqState.bassBoostStrength / 1000f,
                        enabled = eqState.enabled,
                        onValueChange = { onBassBoostChanged((it * 1000f).roundToInt()) },
                        testTag = "eq_bass_boost_slider"
                    )

                    DspSliderRow(
                        label = stringResource(R.string.eq_virtualizer),
                        valueText = "${(eqState.virtualizerStrength / 10f).roundToInt()}%",
                        value = eqState.virtualizerStrength / 1000f,
                        enabled = eqState.enabled,
                        onValueChange = { onVirtualizerChanged((it * 1000f).roundToInt()) },
                        testTag = "eq_virtualizer_slider"
                    )

                    DspSliderRow(
                        label = stringResource(R.string.eq_loudness),
                        valueText = "+${String.format("%.1f", eqState.loudnessGainMb / 100f)} dB",
                        value = eqState.loudnessGainMb / 1500f,
                        enabled = eqState.enabled,
                        onValueChange = { onLoudnessChanged((it * 1500f).roundToInt()) },
                        testTag = "eq_loudness_slider"
                    )

                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.eq_reverb),
                        style = MaterialTheme.typography.titleSmall
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reverbOptions) { (code, label) ->
                            FilterChip(
                                selected = eqState.reverbPreset == code,
                                onClick = { onReverbChanged(code) },
                                enabled = eqState.enabled,
                                label = { Text(label) },
                                modifier = Modifier.testTag("eq_reverb_$code")
                            )
                        }
                    }
                }
            }
        }

        // Speed, Pitch & Sleep Timer
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dinamika Pemutaran",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = { onSpeedPitchChanged(1.0f, 1.0f) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("reset_speed_pitch_button")
                        ) {
                            Icon(
                                Icons.Rounded.RestartAlt,
                                contentDescription = "Reset kecepatan dan nada"
                            )
                        }
                    }

                    DspSliderRow(
                        label = stringResource(R.string.eq_speed),
                        valueText = String.format("%.2fx", eqState.playbackSpeed),
                        value = ((eqState.playbackSpeed - 0.5f) / 1.5f).coerceIn(0f, 1f),
                        enabled = true,
                        onValueChange = { norm ->
                            val speed = (0.5f + norm * 1.5f).coerceIn(0.5f, 2.0f)
                            onSpeedPitchChanged(speed, eqState.playbackPitch)
                        },
                        testTag = "eq_speed_slider"
                    )

                    DspSliderRow(
                        label = stringResource(R.string.eq_pitch),
                        valueText = String.format("%.2fx", eqState.playbackPitch),
                        value = ((eqState.playbackPitch - 0.75f) / 0.75f).coerceIn(0f, 1f),
                        enabled = true,
                        onValueChange = { norm ->
                            val pitch = (0.75f + norm * 0.75f).coerceIn(0.75f, 1.5f)
                            onSpeedPitchChanged(eqState.playbackSpeed, pitch)
                        },
                        testTag = "eq_pitch_slider"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (playbackState.sleepTimerRemainingSec > 0) {
                                val min = playbackState.sleepTimerRemainingSec / 60
                                val sec = playbackState.sleepTimerRemainingSec % 60
                                "${stringResource(R.string.eq_sleep_timer)}: ${String.format("%02d:%02d", min, sec)}"
                            } else {
                                stringResource(R.string.eq_sleep_timer)
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sleepTimerOptions) { mins ->
                            val isSelected = if (mins == 0) {
                                playbackState.sleepTimerRemainingSec == 0
                            } else {
                                playbackState.sleepTimerRemainingSec in ((mins - 15) * 60 + 1)..(mins * 60)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSleepTimerSelected(mins) },
                                label = { Text(if (mins == 0) "Off" else "$mins mnt") },
                                modifier = Modifier.testTag("sleep_timer_$mins")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                showSaveDialog = false
                onSaveCustomPreset(name)
            }
        )
    }
}

@Composable
private fun FrequencyCurveCanvas(
    normalizedBands: List<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Horizontal gridlines (+15, 0, -15 dB)
        drawLine(gridColor, Offset(0f, h * 0.15f), Offset(w, h * 0.15f), strokeWidth = 1.dp.toPx())
        drawLine(gridColor.copy(alpha = 0.9f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1.5.dp.toPx())
        drawLine(gridColor, Offset(0f, h * 0.85f), Offset(w, h * 0.85f), strokeWidth = 1.dp.toPx())

        if (normalizedBands.isEmpty()) return@Canvas

        val stepX = w / (normalizedBands.size - 1).coerceAtLeast(1)
        val points = normalizedBands.mapIndexed { idx, norm ->
            val x = idx * stepX
            val y = midY - (norm.coerceIn(-1f, 1f) * (h * 0.38f))
            Offset(x, y)
        }

        val curvePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = curvePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEachIndexed { idx, pt ->
            drawCircle(
                color = if (idx < 3) secondaryColor else lineColor,
                radius = 4.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
private fun VerticalEqSlider(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = -1f..1f,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .rotate(-90f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(
                        x = -(placeable.width - placeable.height) / 2,
                        y = (placeable.width - placeable.height) / 2
                    )
                }
            }
            .width(140.dp)
    )
}

@Composable
private fun DspSliderRow(
    label: String,
    valueText: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simpan Preset Equalizer") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Preset") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_preset_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotBlank(),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
