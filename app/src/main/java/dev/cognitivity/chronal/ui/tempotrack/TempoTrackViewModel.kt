/*
 * Chronal: Metronome app for Android
 * Copyright (C) 2025-2026  cognitivity
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.cognitivity.chronal.ui.tempotrack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.cognitivity.chronal.ChronalApp
import dev.cognitivity.chronal.metronome.TempoPlan
import dev.cognitivity.chronal.metronome.sound.SoundPack
import dev.cognitivity.chronal.metronome.tracks.ClickTrack
import dev.cognitivity.chronal.settings.Settings
import dev.cognitivity.chronal.tempotrack.TempoTrackData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class TempoTrackScreen {
    data object List : TempoTrackScreen()
    data class Player(val id: String) : TempoTrackScreen()
    data class Editor(val track: TempoTrackData, val isNew: Boolean) : TempoTrackScreen()
}

class TempoTrackViewModel : ViewModel() {
    private val metronome get() = ChronalApp.getInstance().metronome

    var tracks by mutableStateOf(Settings.TEMPO_TRACKS.get())
        private set
    var screen: TempoTrackScreen by mutableStateOf(TempoTrackScreen.List)
        private set

    fun show(newScreen: TempoTrackScreen) { screen = newScreen }

    fun save(track: TempoTrackData) {
        tracks = if (tracks.any { it.id == track.id }) tracks.map { if (it.id == track.id) track else it } else tracks + track
        persist()
    }

    fun delete(id: String) {
        tracks = tracks.filter { it.id != id }
        persist()
    }

    private fun persist() {
        Settings.TEMPO_TRACKS.set(tracks)
        CoroutineScope(Dispatchers.Main).launch { Settings.TEMPO_TRACKS.save() }
    }

    fun play(track: TempoTrackData) {
        val defaultPack = metronome.tracks.filterIsInstance<ClickTrack>().firstOrNull()?.soundPack ?: SoundPack.default()
        metronome.startPlan(TempoPlan.from(track, defaultPack))
    }

    fun stop() = metronome.stop()
}