package com.example.widgets

import android.content.Context
import android.os.SystemClock
import com.example.ui.MainTimerMode
import com.example.ui.MainTimerState
import com.example.ui.PomodoroPhase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PersistedTimerSnapshot(
  val state: MainTimerState = MainTimerState(),
  val targetElapsedRealtimeMs: Long = 0L,
  val targetWallClockMs: Long = 0L,
)

object WidgetTimerStore {
  private const val PREFS_NAME = "gamefy_widget_timer_prefs"
  private const val KEY_MODE = "mode"
  private const val KEY_PHASE = "phase"
  private const val KEY_FOCUS_COUNT = "completed_focus_count"
  private const val KEY_CUSTOM_H = "custom_h"
  private const val KEY_CUSTOM_M = "custom_m"
  private const val KEY_CUSTOM_S = "custom_s"
  private const val KEY_TOTAL_SEC = "total_sec"
  private const val KEY_REMAINING_SEC = "remaining_sec"
  private const val KEY_IS_RUNNING = "is_running"
  private const val KEY_TARGET_ELAPSED = "target_elapsed"
  private const val KEY_TARGET_WALL = "target_wall"

  private val _externalChanges = MutableSharedFlow<Long>(extraBufferCapacity = 8)
  val externalChanges: SharedFlow<Long> = _externalChanges.asSharedFlow()

  fun notifyExternalChange() {
    _externalChanges.tryEmit(System.nanoTime())
  }

  fun save(
    context: Context,
    state: MainTimerState,
    targetElapsedRealtimeMs: Long,
    nowElapsedMs: Long = SystemClock.elapsedRealtime(),
    nowWallMs: Long = System.currentTimeMillis(),
  ) {
    val computedTargetElapsed = if (state.isRunning) {
      if (targetElapsedRealtimeMs > 0L) targetElapsedRealtimeMs else nowElapsedMs + state.remainingSeconds * 1000L
    } else {
      0L
    }
    val computedTargetWall = if (state.isRunning) {
      nowWallMs + (computedTargetElapsed - nowElapsedMs).coerceAtLeast(0L)
    } else {
      0L
    }
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_MODE, state.mode.name)
      .putString(KEY_PHASE, state.pomodoroPhase.name)
      .putInt(KEY_FOCUS_COUNT, state.completedFocusCount)
      .putInt(KEY_CUSTOM_H, state.customHours)
      .putInt(KEY_CUSTOM_M, state.customMinutes)
      .putInt(KEY_CUSTOM_S, state.customSeconds)
      .putInt(KEY_TOTAL_SEC, state.totalSeconds)
      .putInt(KEY_REMAINING_SEC, state.remainingSeconds)
      .putBoolean(KEY_IS_RUNNING, state.isRunning)
      .putLong(KEY_TARGET_ELAPSED, computedTargetElapsed)
      .putLong(KEY_TARGET_WALL, computedTargetWall)
      .apply()
  }

  fun load(
    context: Context,
    nowElapsedMs: Long = SystemClock.elapsedRealtime(),
    nowWallMs: Long = System.currentTimeMillis(),
  ): PersistedTimerSnapshot {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (!prefs.contains(KEY_MODE)) {
      return PersistedTimerSnapshot()
    }
    val mode = runCatching {
      MainTimerMode.valueOf(prefs.getString(KEY_MODE, MainTimerMode.POMODORO.name) ?: MainTimerMode.POMODORO.name)
    }.getOrDefault(MainTimerMode.POMODORO)
    val phase = runCatching {
      PomodoroPhase.valueOf(prefs.getString(KEY_PHASE, PomodoroPhase.FOCUS.name) ?: PomodoroPhase.FOCUS.name)
    }.getOrDefault(PomodoroPhase.FOCUS)
    val completedFocus = prefs.getInt(KEY_FOCUS_COUNT, 0).coerceAtLeast(0)
    val customH = prefs.getInt(KEY_CUSTOM_H, 0).coerceIn(0, 23)
    val customM = prefs.getInt(KEY_CUSTOM_M, 25).coerceIn(0, 59)
    val customS = prefs.getInt(KEY_CUSTOM_S, 0).coerceIn(0, 59)
    val totalSec = prefs.getInt(KEY_TOTAL_SEC, 25 * 60).coerceAtLeast(1)
    val storedRem = prefs.getInt(KEY_REMAINING_SEC, totalSec).coerceIn(0, totalSec)
    val wasRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
    val storedTargetElapsed = prefs.getLong(KEY_TARGET_ELAPSED, 0L)
    val storedTargetWall = prefs.getLong(KEY_TARGET_WALL, 0L)

    if (!wasRunning) {
      return PersistedTimerSnapshot(
        state = MainTimerState(
          mode = mode,
          pomodoroPhase = phase,
          completedFocusCount = completedFocus,
          customHours = customH,
          customMinutes = customM,
          customSeconds = customS,
          totalSeconds = totalSec,
          remainingSeconds = storedRem,
          isRunning = false,
        ),
        targetElapsedRealtimeMs = 0L,
        targetWallClockMs = 0L,
      )
    }

    // Handle elapsedRealtime cleanly, including reboot recovery via wall clock
    val remainingMs = if (storedTargetElapsed > 0L && storedTargetElapsed - nowElapsedMs <= totalSec * 1000L + 60_000L) {
      storedTargetElapsed - nowElapsedMs
    } else if (storedTargetWall > 0L) {
      storedTargetWall - nowWallMs
    } else {
      storedRem * 1000L
    }

    val liveRemainingSec = ((remainingMs + 999L) / 1000L).toInt().coerceIn(0, totalSec)
    val normalizedTargetElapsed = nowElapsedMs + remainingMs.coerceAtLeast(0L)
    return PersistedTimerSnapshot(
      state = MainTimerState(
        mode = mode,
        pomodoroPhase = phase,
        completedFocusCount = completedFocus,
        customHours = customH,
        customMinutes = customM,
        customSeconds = customS,
        totalSeconds = totalSec,
        remainingSeconds = liveRemainingSec,
        isRunning = liveRemainingSec > 0,
      ),
      targetElapsedRealtimeMs = normalizedTargetElapsed,
      targetWallClockMs = nowWallMs + remainingMs.coerceAtLeast(0L),
    )
  }

  fun formatClockText(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
      String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format("%02d:%02d", minutes, seconds)
    }
  }

  fun phaseDisplayLabel(state: MainTimerState): String = when (state.mode) {
    MainTimerMode.POMODORO -> when (state.pomodoroPhase) {
      PomodoroPhase.FOCUS -> "Skupienie"
      PomodoroPhase.SHORT_BREAK -> "Krótka przerwa"
      PomodoroPhase.LONG_BREAK -> "Długa przerwa"
    }
    MainTimerMode.TIMER -> "Timer"
  }
}
