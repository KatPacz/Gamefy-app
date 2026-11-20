package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object ReminderScheduler {
  private const val REQUEST_CODE_PERIODIC = 9001
  private const val CHECK_INTERVAL_MILLIS = 30 * 60 * 1000L

  fun scheduleNextCheck(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildPendingIntent(context)
    val triggerAt = System.currentTimeMillis() + CHECK_INTERVAL_MILLIS
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
  }

  fun cancel(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(buildPendingIntent(context))
  }

  private fun buildPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_PERIODIC,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
