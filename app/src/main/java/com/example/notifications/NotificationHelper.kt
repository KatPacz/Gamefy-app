package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.MainTimerMode
import com.example.ui.MainTimerState
import com.example.ui.PomodoroPhase
import com.example.widgets.TimerWidgetProvider
import com.example.widgets.WidgetTimerStore

object NotificationHelper {
  const val CHANNEL_REMINDERS = "gamefy_reminders"
  const val CHANNEL_TIMER = "gamefy_timer"
  const val CHANNEL_TIMER_ONGOING = "gamefy_timer_ongoing"
  const val EXTRA_TARGET_ROUTE = "target_route"
  const val NOTIFICATION_ID_TIMER_ONGOING = 8800

  private const val REQUEST_CODE_TIMER_OPEN = 8810
  private const val REQUEST_CODE_TIMER_PAUSE_RESUME = 8811
  private const val REQUEST_CODE_TIMER_CANCEL = 8812

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val reminderChannel = NotificationChannel(
        CHANNEL_REMINDERS,
        "Przypomnienia o zadaniach",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Lokalne przypomnienia o zbliżających się zadaniach lub po terminie"
      }
      val timerChannel = NotificationChannel(
        CHANNEL_TIMER,
        "Timer i Pomodoro",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Powiadomienia o zakończeniu sesji timera"
      }
      val timerOngoingChannel = NotificationChannel(
        CHANNEL_TIMER_ONGOING,
        "Aktywne odliczanie",
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = "Stałe powiadomienie z trwającym odliczaniem i szybką kontrolą"
        setSound(null, null)
        enableVibration(false)
        setShowBadge(false)
      }
      manager.createNotificationChannel(reminderChannel)
      manager.createNotificationChannel(timerChannel)
      manager.createNotificationChannel(timerOngoingChannel)
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun areSystemNotificationsEnabled(context: Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  fun openSystemNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
      }
    } else {
      Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
      }
    }.apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
      context.startActivity(intent)
    }.onFailure {
      val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      runCatching { context.startActivity(fallback) }
    }
  }

  fun showNotification(
    context: Context,
    notificationId: Int,
    channelId: String,
    title: String,
    body: String,
    targetRoute: String? = if (channelId == CHANNEL_REMINDERS) "tasks" else null,
  ) {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      if (!targetRoute.isNullOrBlank()) {
        putExtra(EXTRA_TARGET_ROUTE, targetRoute)
      }
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_stat_gamefy)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(
        if (channelId == CHANNEL_TIMER) NotificationCompat.PRIORITY_HIGH
        else NotificationCompat.PRIORITY_DEFAULT
      )
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
  }

  fun updateOngoingTimerNotification(
    context: Context,
    state: MainTimerState,
    targetWallClockMs: Long,
    nowWallMs: Long = System.currentTimeMillis(),
  ) {
    val isActivelyRunning = state.isRunning && state.remainingSeconds > 0 && targetWallClockMs > nowWallMs
    val isPausedMidSession = !state.isRunning && state.remainingSeconds in 1 until state.totalSeconds
    if (!isActivelyRunning && !isPausedMidSession) {
      cancelOngoingTimerNotification(context)
      return
    }
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return

    val phaseTitle = when (state.mode) {
      MainTimerMode.POMODORO -> when (state.pomodoroPhase) {
        PomodoroPhase.FOCUS -> "Pomodoro · Sesja ${state.completedFocusCount + 1}"
        PomodoroPhase.SHORT_BREAK -> "Pomodoro · Krótka przerwa"
        PomodoroPhase.LONG_BREAK -> "Pomodoro · Długa przerwa"
      }
      MainTimerMode.TIMER -> "Timer"
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_TARGET_ROUTE, "start")
    }
    val openPendingIntent = PendingIntent.getActivity(
      context,
      REQUEST_CODE_TIMER_OPEN,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val toggleIntent = Intent(context, TimerWidgetProvider::class.java).apply {
      action = TimerWidgetProvider.ACTION_TOGGLE_TIMER
    }
    val togglePendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_TIMER_PAUSE_RESUME,
      toggleIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val cancelIntent = Intent(context, TimerWidgetProvider::class.java).apply {
      action = TimerWidgetProvider.ACTION_RESET_TIMER
    }
    val cancelPendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_TIMER_CANCEL,
      cancelIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_TIMER_ONGOING)
      .setSmallIcon(R.drawable.ic_stat_gamefy)
      .setContentIntent(openPendingIntent)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (isActivelyRunning) {
      builder
        .setContentTitle(phaseTitle)
        .setContentText("Odliczanie w toku")
        .setOngoing(true)
        .setShowWhen(true)
        .setWhen(targetWallClockMs)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .addAction(0, "Pauza", togglePendingIntent)
        .addAction(0, "Anuluj", cancelPendingIntent)
    } else {
      val remainingFormatted = WidgetTimerStore.formatClockText(state.remainingSeconds)
      builder
        .setContentTitle("$phaseTitle · Pauza")
        .setContentText("Pozostało: $remainingFormatted")
        .setOngoing(false)
        .setShowWhen(false)
        .setUsesChronometer(false)
        .addAction(0, "Wznów", togglePendingIntent)
        .addAction(0, "Anuluj", cancelPendingIntent)
    }

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER_ONGOING, builder.build())
  }

  fun cancelOngoingTimerNotification(context: Context) {
    runCatching {
      NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_TIMER_ONGOING)
    }
  }

  @Suppress("DEPRECATION")
  fun vibratePattern(context: Context) {
    runCatching {
      val pattern = longArrayOf(0, 220, 100, 220)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
      } else {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
          vibrator.vibrate(pattern, -1)
        }
      }
    }
  }
}
