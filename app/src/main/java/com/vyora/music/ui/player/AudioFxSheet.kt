package com.vyora.music.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyora.music.AppModule
import com.vyora.music.data.local.EqPreset
import com.vyora.music.data.local.LoudnessMode
import com.vyora.music.data.local.ReverbMode
import com.vyora.music.data.local.VibePreset
import com.vyora.music.ui.theme.LiquidGlassSwitch
import com.vyora.music.ui.theme.WarmFaint
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import com.vyora.music.ui.theme.WarmSurface
import com.vyora.music.ui.theme.WarmSurfaceElevated
import com.vyora.music.ui.theme.warmWash
import java.util.Locale

private val EQ_BAND_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFxSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fx by AppModule.audioFx.state.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmSurface,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(warmWash(0.24f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "STUDIO AUDIO VYORA",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Equalizer, Slowed & Efek",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                        color = WarmInk,
                    )
                }
                if (fx.isAnyActive) {
                    TextButton(onClick = { AppModule.audioFx.resetAll() }) {
                        Icon(Icons.Rounded.RestartAlt, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FxEyebrow("PRESET SUASANA (SPEED + NADA + REVERB)")
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VibePreset.entries.filter { it != VibePreset.CUSTOM }.forEach { preset ->
                    val selected = fx.vibePreset == preset
                    Surface(
                        onClick = { AppModule.audioFx.applyVibePreset(preset) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) accent.copy(alpha = 0.20f) else WarmSurfaceElevated,
                        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else WarmHairline),
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (preset == VibePreset.SLOWED_VIBE || preset == VibePreset.NIGHTCORE) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        null,
                                        tint = if (selected) accent else WarmMuted,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) accent else WarmInk,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmMuted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Kontrol Kecepatan & Nada Manual
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(WarmSurfaceElevated)
                    .border(1.dp, WarmHairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speed, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Kecepatan Putar",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmInk,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f×", fx.speed),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                }
                Slider(
                    value = fx.speed,
                    onValueChange = { AppModule.audioFx.setSpeed(it) },
                    valueRange = 0.50f..2.00f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = warmWash(0.16f),
                    ),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Nada (Pitch)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmInk,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f×", fx.pitch),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                }
                Slider(
                    value = fx.pitch,
                    onValueChange = { AppModule.audioFx.setPitch(it) },
                    valueRange = 0.50f..2.00f,
                    enabled = !fx.linkPitchToSpeed,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = warmWash(0.16f),
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Kunci nada mengikuti kecepatan (gaya piringan hitam)",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    LiquidGlassSwitch(
                        checked = fx.linkPitchToSpeed,
                        onCheckedChange = { AppModule.audioFx.setLinkPitchToSpeed(it) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            FxEyebrow("EQUALIZER 5-BAND & PROFIL AUTOEQ")
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(WarmSurfaceElevated)
                    .border(1.dp, WarmHairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Equalizer, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Equalizer Aktif",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmInk,
                        )
                        Text(
                            text = fx.eqPreset.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (fx.eqEnabled) accent else WarmMuted,
                        )
                    }
                    LiquidGlassSwitch(
                        checked = fx.eqEnabled,
                        onCheckedChange = { AppModule.audioFx.setEqEnabled(it) },
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EqPreset.entries.filter { it != EqPreset.CUSTOM }.forEach { preset ->
                        val selected = fx.eqEnabled && fx.eqPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) accent.copy(alpha = 0.22f) else warmWash(0.06f))
                                .border(1.dp, if (selected) accent else WarmHairline, RoundedCornerShape(999.dp))
                                .clickable { AppModule.audioFx.applyEqPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) accent else WarmInk,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                EQ_BAND_LABELS.forEachIndexed { idx, freq ->
                    val gain = fx.bandGainsDb.getOrElse(idx) { 0f }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = freq,
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmMuted,
                            modifier = Modifier.width(56.dp),
                        )
                        Slider(
                            value = gain,
                            onValueChange = { AppModule.audioFx.setBandGain(idx, it) },
                            valueRange = -12f..12f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = if (fx.eqEnabled) accent else WarmMuted,
                                activeTrackColor = if (fx.eqEnabled) accent else WarmMuted,
                                inactiveTrackColor = warmWash(0.14f),
                            ),
                        )
                        Text(
                            text = (if (gain > 0f) "+" else "") + String.format(Locale.US, "%.1f dB", gain),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (fx.eqEnabled && gain != 0f) accent else WarmMuted,
                            modifier = Modifier.width(56.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            FxEyebrow("NORMALISASI VOLUME, BASS & SPASIAL")
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(WarmSurfaceElevated)
                    .border(1.dp, WarmHairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolumeUp, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Normalisasi / Penguat Volume",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmInk,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LoudnessMode.entries.forEach { mode ->
                        val selected = fx.loudnessMode == mode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) accent.copy(alpha = 0.20f) else WarmSurface)
                                .border(1.dp, if (selected) accent else WarmHairline, RoundedCornerShape(12.dp))
                                .clickable { AppModule.audioFx.setLoudness(mode) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) accent else WarmInk,
                                maxLines = 1,
                            )
                        }
                    }
                }

                // Bass Boost
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Bass Boost",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmInk,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${fx.bassBoostPercent}%", style = MaterialTheme.typography.labelLarge, color = accent)
                    }
                    Slider(
                        value = fx.bassBoostPercent.toFloat(),
                        onValueChange = { AppModule.audioFx.setBassBoost(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = warmWash(0.14f),
                        ),
                    )
                }

                // 3D Virtualizer
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SurroundSound, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Efek 3D Spasial (Virtualizer)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmInk,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${fx.virtualizerPercent}%", style = MaterialTheme.typography.labelLarge, color = accent)
                    }
                    Slider(
                        value = fx.virtualizerPercent.toFloat(),
                        onValueChange = { AppModule.audioFx.setVirtualizer(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = warmWash(0.14f),
                        ),
                    )
                }

                // Ruang Reverb
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Ruang Gema (Reverb)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmInk,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ReverbMode.entries.forEach { mode ->
                            val selected = fx.reverbMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) accent.copy(alpha = 0.20f) else WarmSurface)
                                    .border(1.dp, if (selected) accent else WarmHairline, RoundedCornerShape(12.dp))
                                    .clickable { AppModule.audioFx.setReverb(mode) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) accent else WarmInk,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FxEyebrow(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = WarmFaint,
    )
}
