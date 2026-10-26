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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cognitivity.chronal.ChronalApp
import dev.cognitivity.chronal.R
import dev.cognitivity.chronal.metronome.sound.SoundPack
import dev.cognitivity.chronal.tempotrack.Accent
import dev.cognitivity.chronal.tempotrack.TempoSection
import dev.cognitivity.chronal.tempotrack.TempoTrackData

@Composable
fun TempoTrackPageMain(padding: PaddingValues, viewModel: TempoTrackViewModel) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        when (val screen = viewModel.screen) {
            is TempoTrackScreen.List -> TrackList(viewModel)
            is TempoTrackScreen.Player -> {
                val track = viewModel.tracks.find { it.id == screen.id }
                if (track == null) viewModel.show(TempoTrackScreen.List) else TrackPlayer(viewModel, track)
            }
            is TempoTrackScreen.Editor -> TrackEditor(
                initial = screen.track,
                onSave = {
                    viewModel.save(it)
                    viewModel.show(if (screen.isNew) TempoTrackScreen.List else TempoTrackScreen.Player(it.id))
                },
                onCancel = {
                    viewModel.show(if (screen.isNew) TempoTrackScreen.List else TempoTrackScreen.Player(screen.track.id))
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.TrackList(viewModel: TempoTrackViewModel) {
    var deleting by remember { mutableStateOf<TempoTrackData?>(null) }
    val defaultName = stringResource(R.string.tempo_track_default_name)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.page_tempo_track), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        Button(onClick = {
            viewModel.show(TempoTrackScreen.Editor(TempoTrackData(name = defaultName), isNew = true))
        }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.tempo_track_new))
        }
    }
    Spacer(Modifier.height(16.dp))

    if (viewModel.tracks.isEmpty()) {
        Text(stringResource(R.string.tempo_track_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(viewModel.tracks, key = { it.id }) { track ->
            Card(onClick = { viewModel.show(TempoTrackScreen.Player(track.id)) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.tempo_track_summary, track.sections.size, track.totalBars),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.show(TempoTrackScreen.Editor(track, isNew = false)) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.tempo_track_edit))
                    }
                    IconButton(onClick = { deleting = track }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.tempo_track_delete))
                    }
                }
            }
        }
    }

    deleting?.let { track ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.tempo_track_delete_title, track.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(track.id); deleting = null }) {
                    Text(stringResource(R.string.tempo_track_delete))
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

@Composable
private fun ColumnScope.TrackPlayer(viewModel: TempoTrackViewModel, track: TempoTrackData) {
    val metronome = ChronalApp.getInstance().metronome
    val position by metronome.planPosition.collectAsState()
    val playingNow by metronome.playingFlow.collectAsState()
    val playing = playingNow && position.active
    var showOffsetDialog by remember { mutableStateOf(false) }

    if (showOffsetDialog) {
        var text by remember { mutableStateOf(formatBpm(track.bpmOffset)) }
        AlertDialog(
            onDismissRequest = { showOffsetDialog = false },
            title = { Text(stringResource(R.string.tempo_track_tempo_change_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { new ->
                        text = new.filterIndexed { i, ch -> ch.isDigit() || ch == '.' || (i == 0 && ch == '-') }.take(8)
                    },
                    label = { Text(stringResource(R.string.tempo_track_tempo_change_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = if (text.isBlank() || text == "-") 0f else text.toFloatOrNull()
                    if (value != null) viewModel.adjustTempo(track, value - track.bpmOffset)
                    showOffsetDialog = false
                }) { Text(stringResource(R.string.tempo_track_save)) }
            },
            dismissButton = { TextButton(onClick = { showOffsetDialog = false }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = {
            if (playing) viewModel.stop()
            viewModel.show(TempoTrackScreen.List)
        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.generic_back)) }
        Text(track.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        IconButton(onClick = {
            if (playing) viewModel.stop()
            viewModel.show(TempoTrackScreen.Editor(track, isNew = false))
        }) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.tempo_track_edit)) }
    }

    Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (playing && position.countInBeat > 0) {
            Text(position.countInBeat.toString(), fontSize = 96.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tempo_track_count_in))
        } else if (playing) {
            val section = track.sections.getOrNull(position.sectionIndex)
            Text(position.bpm.toInt().toString(), fontSize = 96.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tempo_track_bar_of, position.bar, position.totalBars), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.tempo_track_section_of, position.sectionIndex + 1, track.sections.size) +
                    (section?.let { "  ·  ${it.timeSignature.first}/${it.timeSignature.second}" } ?: ""),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text((track.sections.first().bpm + track.bpmOffset).coerceAtLeast(1f).toInt().toString(), fontSize = 96.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tempo_track_summary, track.sections.size, track.totalBars), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedButton(onClick = { viewModel.adjustTempo(track, -TempoTrackData.BPM_STEP) }) {
            Text("-${TempoTrackData.BPM_STEP.toInt()}")
        }
        TextButton(onClick = { showOffsetDialog = true }) {
            Text(
                stringResource(R.string.tempo_track_tempo_adjust) + " " +
                    (if (track.bpmOffset > 0) "+" else "") + formatBpm(track.bpmOffset) + " BPM"
            )
        }
        OutlinedButton(onClick = { viewModel.adjustTempo(track, TempoTrackData.BPM_STEP) }) {
            Text("+${TempoTrackData.BPM_STEP.toInt()}")
        }
        if (track.bpmOffset != 0f) {
            TextButton(onClick = { viewModel.adjustTempo(track, -track.bpmOffset) }) {
                Text(stringResource(R.string.tempo_track_tempo_reset))
            }
        }
    }

    Button(
        onClick = { if (playing) viewModel.stop() else viewModel.play(track) },
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Icon(
            painterResource(if (playing) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(stringResource(if (playing) R.string.generic_stop else R.string.generic_start))
    }
}

@Composable
private fun packName(pack: SoundPack): String =
    if (pack.nameRes != 0) stringResource(pack.nameRes) else pack.id.substringAfterLast('/')

@Composable
private fun SoundPicker(label: String, selectedId: String?, onSelect: (String?) -> Unit) {
    val options = listOf<Pair<String?, String>>(null to stringResource(R.string.tempo_track_default_sound)) +
        SoundPack.all().map { it.id to packName(it) }
    Picker(label, options.find { it.first == selectedId }?.second ?: options.first().second, options.map { it.second to it.first }, onSelect)
}

@Composable
private fun <T> Picker(label: String, selectedText: String, options: List<Pair<String, T>>, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $selectedText", maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (text, value) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}

private class EditableSection(val uid: Long, val section: TempoSection)

@Composable
private fun ColumnScope.TrackEditor(initial: TempoTrackData, onSave: (TempoTrackData) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var countIn by remember { mutableStateOf(initial.countInEnabled) }
    var countInPack by remember { mutableStateOf(initial.countInSoundPackId) }
    var nextUid by remember { mutableLongStateOf(initial.sections.size.toLong()) }
    val sections = remember { initial.sections.mapIndexed { i, s -> EditableSection(i.toLong(), s) }.toMutableStateList() }

    OutlinedTextField(
        value = name, onValueChange = { name = it },
        label = { Text(stringResource(R.string.tempo_track_name)) },
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.tempo_track_count_in), modifier = Modifier.weight(1f))
        Switch(checked = countIn, onCheckedChange = { countIn = it })
    }
    if (countIn) {
        SoundPicker(stringResource(R.string.tempo_track_count_in_sound), countInPack) { countInPack = it }
    }

    LazyColumn(
        modifier = Modifier.weight(1f).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sections, key = { it.uid }) { item ->
            val index = sections.indexOf(item)
            SectionCard(
                index = index,
                section = item.section,
                canDelete = sections.size > 1,
                onChange = { sections[sections.indexOf(item)] = EditableSection(item.uid, it) },
                onDelete = { sections.remove(item) }
            )
        }
        item {
            OutlinedButton(onClick = {
                val last = sections.lastOrNull()?.section ?: TempoSection()
                sections.add(EditableSection(nextUid++, last))
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.tempo_track_add_section))
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.generic_cancel)) }
        Button(
            onClick = {
                onSave(initial.copy(
                    name = name.ifBlank { initial.name },
                    sections = sections.map { it.section },
                    countInEnabled = countIn,
                    countInSoundPackId = countInPack
                ))
            },
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.tempo_track_save)) }
    }
}

@Composable
private fun SectionCard(index: Int, section: TempoSection, canDelete: Boolean, onChange: (TempoSection) -> Unit, onDelete: () -> Unit) {
    var bpmText by remember { mutableStateOf(formatBpm(section.bpm)) }
    var barsText by remember { mutableStateOf(section.bars.toString()) }
    var beatsText by remember { mutableStateOf(section.timeSignature.first.toString()) }
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tempo_track_section, index + 1), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (canDelete) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.tempo_track_delete)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = {
                        bpmText = it.filter { c -> c.isDigit() || c == '.' }.take(7)
                        bpmText.toFloatOrNull()?.takeIf { v -> v >= 1f }?.let { v -> onChange(section.copy(bpm = v.coerceAtMost(1000f))) }
                    },
                    label = { Text(stringResource(R.string.tempo_track_bpm)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = barsText,
                    onValueChange = {
                        barsText = it.filter(Char::isDigit).take(3)
                        barsText.toIntOrNull()?.takeIf { v -> v >= 1 }?.let { v -> onChange(section.copy(bars = v)) }
                    },
                    label = { Text(stringResource(R.string.tempo_track_bars)) },
                    singleLine = true, keyboardOptions = number,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = beatsText,
                    onValueChange = {
                        beatsText = it.filter(Char::isDigit).take(2)
                        beatsText.toIntOrNull()?.takeIf { v -> v in 1..32 }?.let { v ->
                            onChange(section.copy(timeSignature = v to section.timeSignature.second))
                        }
                    },
                    label = { Text(stringResource(R.string.tempo_track_beats)) },
                    singleLine = true, keyboardOptions = number,
                    modifier = Modifier.weight(1f)
                )
                Picker(
                    stringResource(R.string.tempo_track_note), section.timeSignature.second.toString(),
                    listOf(2, 4, 8, 16).map { it.toString() to it },
                    { onChange(section.copy(timeSignature = section.timeSignature.first to it)) },
                    Modifier.weight(1f)
                )
            }
            SoundPicker(stringResource(R.string.tempo_track_sound), section.soundPackId) { onChange(section.copy(soundPackId = it)) }
            val accentNames = mapOf(
                Accent.FIRST to stringResource(R.string.tempo_track_accent_first),
                Accent.ALL to stringResource(R.string.tempo_track_accent_all),
                Accent.NONE to stringResource(R.string.tempo_track_accent_none),
            )
            Picker(
                stringResource(R.string.tempo_track_accent), accentNames.getValue(section.accent),
                accentNames.map { it.value to it.key },
                { onChange(section.copy(accent = it)) }
            )
        }
    }
}

private fun formatBpm(bpm: Float) = if (bpm % 1f == 0f) bpm.toInt().toString() else bpm.toString()