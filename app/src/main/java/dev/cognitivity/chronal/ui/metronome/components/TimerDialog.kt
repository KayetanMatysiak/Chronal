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

package dev.cognitivity.chronal.ui.metronome.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cognitivity.chronal.ChronalApp
import dev.cognitivity.chronal.R
import dev.cognitivity.chronal.timer.TimerState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerDialog(onDismissRequest: () -> Unit) {
    val timer = ChronalApp.getInstance().timer
    val state by timer.state.collectAsState()
    val selectedMs by timer.selectedMs.collectAsState()
    val remainingMs by timer.remainingMs.collectAsState()
    var customMinutes by remember { mutableStateOf("") }
    var customSeconds by remember { mutableStateOf("") }
    var showCustom by remember { mutableStateOf(false) }

    val presets = listOf(1, 2, 3, 4, 5)
    val isCustom = presets.none { selectedMs == it * 60_000L }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.page_timer)) },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.generic_close)) }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val totalSeconds = (remainingMs + 999) / 1000
                Text(
                    text = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { minutes ->
                        FilterChip(
                            selected = selectedMs == minutes * 60_000L,
                            enabled = state == TimerState.IDLE,
                            onClick = {
                                showCustom = false
                                timer.select(minutes * 60_000L)
                            },
                            label = { Text(stringResource(R.string.timer_minutes_short, minutes)) }
                        )
                    }
                    FilterChip(
                        selected = isCustom || showCustom,
                        enabled = state == TimerState.IDLE,
                        onClick = { showCustom = true },
                        label = { Text(stringResource(R.string.timer_custom)) }
                    )
                }

                if (showCustom && state == TimerState.IDLE) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = customMinutes,
                            onValueChange = {
                                customMinutes = it.filter(Char::isDigit).take(3)
                                applyCustom(customMinutes, customSeconds)
                            },
                            label = { Text(stringResource(R.string.timer_minutes)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = customSeconds,
                            onValueChange = {
                                customSeconds = it.filter(Char::isDigit).take(2)
                                applyCustom(customMinutes, customSeconds)
                            },
                            label = { Text(stringResource(R.string.timer_seconds)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (state) {
                        TimerState.IDLE -> Button(onClick = { timer.start() }) {
                            Text(stringResource(R.string.generic_start))
                        }
                        TimerState.RUNNING -> {
                            Button(onClick = { timer.pause() }) { Text(stringResource(R.string.generic_pause)) }
                            OutlinedButton(onClick = { timer.reset() }) { Text(stringResource(R.string.timer_reset)) }
                        }
                        TimerState.PAUSED -> {
                            Button(onClick = { timer.resume() }) { Text(stringResource(R.string.generic_resume)) }
                            OutlinedButton(onClick = { timer.reset() }) { Text(stringResource(R.string.timer_reset)) }
                        }
                    }
                }
            }
        }
    )
}

private fun applyCustom(minutes: String, seconds: String) {
    val total = (minutes.toLongOrNull() ?: 0L) * 60 + (seconds.toLongOrNull() ?: 0L)
    if (total > 0) ChronalApp.getInstance().timer.select(total * 1000)
}