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

object NotificationHelper {
  const val CHANNEL_REMINDERS = "gamefy_reminders"
  const val CHANNEL_TIMER = "gamefy_timer"
  const val EXTRA_TARGET_ROUTE = "target_route"

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val reminderChannel = NotificationChannel(
        CHANNEL_REMINDERS,
        "Przypomnienia o zadaniach",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Lokalne przypomnienia o zbliżających się lub spóźnionych zadaniach"
      }
      val timerChannel = NotificationChannel(
        CHANNEL_TIMER,
        "Timer i Pomodoro",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Powiadomienia o zakończeniu sesji timera"
      }
      manager.createNotificationChannel(reminderChannel)
      manager.createNotificationChannel(timerChannel)
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
      .setSmallIcon(R.mipmap.ic_launcher_round)
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
