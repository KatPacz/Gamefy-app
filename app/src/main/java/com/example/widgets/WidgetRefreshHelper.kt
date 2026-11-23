package com.example.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.notifications.NotificationHelper
import java.util.Calendar

object WidgetRefreshHelper {
  private const val REQUEST_CODE_MIDNIGHT = 9401
  const val ACTION_MIDNIGHT_REFRESH = "com.example.widgets.ACTION_MIDNIGHT_REFRESH"

  fun hasAnyWidgets(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    return manager.getAppWidgetIds(ComponentName(context, TodayTasksWidgetProvider::class.java)).isNotEmpty() ||
      manager.getAppWidgetIds(ComponentName(context, TimerWidgetProvider::class.java)).isNotEmpty() ||
      manager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java)).isNotEmpty()
  }

  fun refreshAllWidgets(context: Context) {
    val appCtx = context.applicationContext
    refreshTodayTasksWidgets(appCtx)
    refreshTimerWidgets(appCtx)
    refreshQuickAddWidgets(appCtx)
  }

  fun refreshTodayTasksWidgets(context: Context) {
    val appCtx = context.applicationContext
    val manager = AppWidgetManager.getInstance(appCtx)
    val ids = manager.getAppWidgetIds(ComponentName(appCtx, TodayTasksWidgetProvider::class.java))
    if (ids.isEmpty()) return
    TodayTasksWidgetProvider.enqueueUpdate(appCtx, ids)
  }

  fun refreshTimerWidgets(context: Context) {
    val appCtx = context.applicationContext
    val manager = AppWidgetManager.getInstance(appCtx)
    val ids = manager.getAppWidgetIds(ComponentName(appCtx, TimerWidgetProvider::class.java))
    if (ids.isEmpty()) return
    TimerWidgetProvider.enqueueUpdate(appCtx, ids)
  }

  fun refreshQuickAddWidgets(context: Context) {
    val appCtx = context.applicationContext
    val manager = AppWidgetManager.getInstance(appCtx)
    val ids = manager.getAppWidgetIds(ComponentName(appCtx, QuickAddWidgetProvider::class.java))
    if (ids.isEmpty()) return
    QuickAddWidgetProvider.enqueueUpdate(appCtx, ids)
  }

  fun scheduleMidnightRefresh(context: Context) {
    val appCtx = context.applicationContext
    val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = buildMidnightPendingIntent(appCtx)
    if (!hasAnyWidgets(appCtx)) {
      alarmManager.cancel(pendingIntent)
      return
    }
    val nextMidnight = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 5)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMidnight, pendingIntent)
  }

  private fun buildMidnightPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, WidgetRefreshReceiver::class.java).apply {
      action = ACTION_MIDNIGHT_REFRESH
    }
    return PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_MIDNIGHT,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  fun buildOpenAppRoutePendingIntent(
    context: Context,
    route: String,
    requestCode: Int,
  ): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(NotificationHelper.EXTRA_TARGET_ROUTE, route)
    }
    return PendingIntent.getActivity(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
