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

package dev.cognitivity.chronal.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import dev.cognitivity.chronal.ChronalApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerState { IDLE, RUNNING, PAUSED }

/** Countdown that stops the metronome when it reaches zero. */
class PracticeTimer(private val context: Context) {
    private val _state = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _selectedMs = MutableStateFlow(60_000L)
    val selectedMs: StateFlow<Long> = _selectedMs.asStateFlow()

    private val _remainingMs = MutableStateFlow(60_000L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private var endElapsed = 0L
    private var ticker: Job? = null

    fun select(durationMs: Long) {
        if (_state.value != TimerState.IDLE) return
        _selectedMs.value = durationMs.coerceAtLeast(1_000L)
        _remainingMs.value = _selectedMs.value
    }

    fun start() {
        if (_state.value != TimerState.IDLE) return
        run(_selectedMs.value)
    }

    fun pause() {
        if (_state.value != TimerState.RUNNING) return
        _remainingMs.value = (endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        ticker?.cancel()
        cancelAlarm()
        _state.value = TimerState.PAUSED
    }

    fun resume() {
        if (_state.value != TimerState.PAUSED) return
        run(_remainingMs.value)
    }

    fun reset() {
        ticker?.cancel()
        cancelAlarm()
        _remainingMs.value = _selectedMs.value
        _state.value = TimerState.IDLE
    }

    fun onFinished() {
        if (_state.value != TimerState.RUNNING) return
        ticker?.cancel()
        cancelAlarm()
        _state.value = TimerState.IDLE
        _remainingMs.value = _selectedMs.value

        val metronome = ChronalApp.getInstance().metronome
        if (metronome.playing) metronome.stop()
    }

    private fun run(durationMs: Long) {
        val metronome = ChronalApp.getInstance().metronome
        if (!metronome.playing) metronome.start()

        endElapsed = SystemClock.elapsedRealtime() + durationMs
        _state.value = TimerState.RUNNING
        scheduleAlarm(System.currentTimeMillis() + durationMs)

        ticker?.cancel()
        ticker = CoroutineScope(Dispatchers.Main).launch {
            while (_state.value == TimerState.RUNNING) {
                val left = endElapsed - SystemClock.elapsedRealtime()
                if (left <= 0) {
                    onFinished()
                    break
                }
                _remainingMs.value = left
                delay(200)
            }
        }
    }

    private fun alarmIntent(): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java).setAction(TimerReceiver.ACTION_FINISHED)
        return PendingIntent.getBroadcast(context, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun scheduleAlarm(atMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent())
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent())
        }
    }

    private fun cancelAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmIntent())
    }
}