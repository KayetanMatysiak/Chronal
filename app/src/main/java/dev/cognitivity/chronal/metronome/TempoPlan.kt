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

package dev.cognitivity.chronal.metronome

import dev.cognitivity.chronal.metronome.sound.SoundPack
import dev.cognitivity.chronal.rhythm.metronome.elements.RhythmAtom
import dev.cognitivity.chronal.rhythm.metronome.elements.RhythmNote
import dev.cognitivity.chronal.rhythm.metronome.elements.RhythmTuplet
import dev.cognitivity.chronal.settings.types.json.SimpleRhythm
import dev.cognitivity.chronal.tempotrack.TempoTrackData

class PlanBeat(
    /** Length in beats of the section's time signature denominator. */
    val lengthBeats: Double,
    val bpm: Float,
    val pitch: Int,
    val pack: SoundPack,
    val sectionIndex: Int,
    val bar: Int,
    val beatInBar: Int,
    val countInBeat: Int,
) {
    val isCountIn get() = countInBeat > 0
}

class TempoPlan(val trackId: String, val beats: List<PlanBeat>, val totalBars: Int) {
    val packs: List<SoundPack> get() = beats.map { it.pack }.distinct()

    companion object {
        fun from(track: TempoTrackData, defaultPack: SoundPack): TempoPlan {
            fun pack(id: String?) = SoundPack.byId(id) ?: defaultPack
            val beats = ArrayList<PlanBeat>()

            if (track.countInEnabled) {
                val first = track.sections.first()
                val countPack = pack(track.countInSoundPackId)
                repeat(TempoTrackData.COUNT_IN_BEATS) {
                    beats.add(PlanBeat(1.0, first.bpm, if (it == 0) 0 else 1, countPack, 0, 0, it + 1, it + 1))
                }
            }

            var bar = 0
            track.sections.forEachIndexed { sectionIndex, section ->
                val rhythm = SimpleRhythm(section.timeSignature, section.timeSignature.second, section.accent.id).asRhythm()
                val sectionPack = pack(section.soundPackId)
                val barBeats = ArrayList<Pair<Double, Int>>()
                for (element in rhythm.measures[0].elements) {
                    when (element) {
                        is RhythmAtom -> barBeats.add(element.getDuration() to ((element as? RhythmNote)?.pitch ?: 1))
                        is RhythmTuplet -> element.notes.forEach { barBeats.add(it.getDuration() to ((it as? RhythmNote)?.pitch ?: 1)) }
                    }
                }
                repeat(section.bars) {
                    bar++
                    barBeats.forEachIndexed { i, (duration, pitch) ->
                        beats.add(PlanBeat(duration * section.timeSignature.second, section.bpm, pitch, sectionPack, sectionIndex, bar, i + 1, 0))
                    }
                }
            }
            return TempoPlan(track.id, beats, bar)
        }
    }
}

data class PlanPosition(
    val active: Boolean = false,
    val sectionIndex: Int = 0,
    val bar: Int = 0,
    val beatInBar: Int = 0,
    val bpm: Float = 0f,
    val countInBeat: Int = 0,
    val totalBars: Int = 0,
)