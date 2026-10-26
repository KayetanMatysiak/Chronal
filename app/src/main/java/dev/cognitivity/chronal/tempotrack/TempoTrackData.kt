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

package dev.cognitivity.chronal.tempotrack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.UUID

enum class Accent(val id: Int) {
    FIRST(2), ALL(0), NONE(1);

    companion object {
        fun fromId(id: Int) = entries.find { it.id == id } ?: FIRST
    }
}

data class TempoSection(
    val bpm: Float = 120f,
    val bars: Int = 4,
    val timeSignature: Pair<Int, Int> = 4 to 4,
    val soundPackId: String? = null,
    val accent: Accent = Accent.FIRST,
) {
    fun toJson() = JsonObject().apply {
        addProperty("bpm", bpm)
        addProperty("bars", bars)
        addProperty("numerator", timeSignature.first)
        addProperty("denominator", timeSignature.second)
        soundPackId?.let { addProperty("soundPackId", it) }
        addProperty("accent", accent.id)
    }

    companion object {
        fun fromJson(json: JsonObject) = TempoSection(
            bpm = json.get("bpm")?.asFloat ?: 120f,
            bars = json.get("bars")?.asInt ?: 4,
            timeSignature = (json.get("numerator")?.asInt ?: 4) to (json.get("denominator")?.asInt ?: 4),
            soundPackId = json.get("soundPackId")?.takeIf { !it.isJsonNull }?.asString,
            accent = Accent.fromId(json.get("accent")?.asInt ?: 2),
        )
    }
}

data class TempoTrackData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sections: List<TempoSection> = listOf(TempoSection()),
    val countInEnabled: Boolean = true,
    val countInSoundPackId: String? = null,
) {
    val totalBars get() = sections.sumOf { it.bars }

    fun toJson() = JsonObject().apply {
        addProperty("id", id)
        addProperty("name", name)
        add("sections", JsonArray().apply { sections.forEach { add(it.toJson()) } })
        addProperty("countInEnabled", countInEnabled)
        countInSoundPackId?.let { addProperty("countInSoundPackId", it) }
    }

    companion object {
        const val COUNT_IN_BEATS = 4

        fun fromJson(json: JsonObject) = TempoTrackData(
            id = json.get("id")?.asString ?: UUID.randomUUID().toString(),
            name = json.get("name")?.asString ?: "Tempo track",
            sections = json.get("sections")?.asJsonArray?.map { TempoSection.fromJson(it.asJsonObject) }
                ?.takeIf { it.isNotEmpty() } ?: listOf(TempoSection()),
            countInEnabled = json.get("countInEnabled")?.asBoolean ?: true,
            countInSoundPackId = json.get("countInSoundPackId")?.takeIf { !it.isJsonNull }?.asString,
        )
    }
}