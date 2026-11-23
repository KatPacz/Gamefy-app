package com.example.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.GamefyApp
import com.example.R
import com.example.data.model.THEME_CODE_SYSTEM
import com.example.notifications.NotificationHelper
import com.example.notifications.TimerSoundHelper
import com.example.ui.MainTimerMode
import com.example.ui.PomodoroPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerWidgetProvider : AppWidgetProvider() {

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    WidgetRefreshHelper.scheduleMidnightRefresh(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    cancelFinishAlarm(context)
    WidgetRefreshHelper.scheduleMidnightRefresh(context)
  }

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_TOGGLE_TIMER, ACTION_RESET_TIMER, ACTION_TIMER_FINISHED -> {
        val pendingResult = goAsync()
        val app = context.applicationContext as? GamefyApp
        if (app == null) {
          pendingResult.finish()
          return
        }
        app.applicationScope.launch(Dispatchers.IO) {
          try {
            when (intent.action) {
              ACTION_TOGGLE_TIMER -> handleToggle(app)
              ACTION_RESET_TIMER -> handleReset(app)
              ACTION_TIMER_FINISHED -> completeIfExpired(app)
            }
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, TimerWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
              updateWidgetsSync(app, manager, ids)
            }
          } finally {
            pendingResult.finish()
          }
        }
      }
      else -> super.onReceive(context, intent)
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val pendingResult = goAsync()
    val app = context.applicationContext as? GamefyApp
    if (app == null) {
      pendingResult.finish()
      return
    }
    app.applicationScope.launch(Dispatchers.IO) {
      try {
        completeIfExpired(app)
        updateWidgetsSync(app, appWidgetManager, appWidgetIds)
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle,
  ) {
    onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
  }

  companion object {
    const val ACTION_TOGGLE_TIMER = "com.example.widgets.ACTION_TOGGLE_TIMER"
    const val ACTION_RESET_TIMER = "com.example.widgets.ACTION_RESET_TIMER"
    const val ACTION_TIMER_FINISHED = "com.example.widgets.ACTION_TIMER_FINISHED"

    private const val REQUEST_CODE_OPEN_START = 9601
    private const val REQUEST_CODE_TOGGLE = 9602
    private const val REQUEST_CODE_RESET = 9603
    private const val REQUEST_CODE_FINISH_ALARM = 9604

    fun enqueueUpdate(context: Context, appWidgetIds: IntArray) {
      val app = context.applicationContext as? GamefyApp ?: return
      val manager = AppWidgetManager.getInstance(app)
      app.applicationScope.launch(Dispatchers.IO) {
        updateWidgetsSync(app, manager, appWidgetIds)
      }
    }

    fun scheduleFinishAlarm(context: Context, targetWallClockMs: Long) {
      val appCtx = context.applicationContext
      val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val pi = buildBroadcastPendingIntent(appCtx, ACTION_TIMER_FINISHED, REQUEST_CODE_FINISH_ALARM)
      if (targetWallClockMs <= System.currentTimeMillis()) {
        alarmManager.cancel(pi)
        return
      }
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetWallClockMs, pi)
    }

    fun cancelFinishAlarm(context: Context) {
      val appCtx = context.applicationContext
      val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val pi = buildBroadcastPendingIntent(appCtx, ACTION_TIMER_FINISHED, REQUEST_CODE_FINISH_ALARM)
      alarmManager.cancel(pi)
    }

    private suspend fun handleToggle(app: GamefyApp) {
      val nowElapsed = SystemClock.elapsedRealtime()
      val nowWall = System.currentTimeMillis()
      val snap = WidgetTimerStore.load(app, nowElapsed, nowWall)
      val cur = snap.state

      if (cur.isRunning && cur.remainingSeconds > 0) {
        val paused = cur.copy(isRunning = false)
        WidgetTimerStore.save(app, paused, targetElapsedRealtimeMs = 0L, nowElapsedMs = nowElapsed, nowWallMs = nowWall)
        cancelFinishAlarm(app)
        NotificationHelper.updateOngoingTimerNotification(app, paused, targetWallClockMs = 0L, nowWallMs = nowWall)
        WidgetTimerStore.notifyExternalChange()
      } else {
        val rem = if (cur.remainingSeconds <= 0) cur.totalSeconds.coerceAtLeast(1) else cur.remainingSeconds
        val targetElapsed = nowElapsed + rem * 1000L
        val targetWall = nowWall + rem * 1000L
        val running = cur.copy(remainingSeconds = rem, isRunning = true)
        WidgetTimerStore.save(app, running, targetElapsedRealtimeMs = targetElapsed, nowElapsedMs = nowElapsed, nowWallMs = nowWall)
        scheduleFinishAlarm(app, targetWall)
        NotificationHelper.updateOngoingTimerNotification(app, running, targetWallClockMs = targetWall, nowWallMs = nowWall)
        WidgetTimerStore.notifyExternalChange()
      }
    }

    private fun handleReset(app: GamefyApp) {
      val nowElapsed = SystemClock.elapsedRealtime()
      val nowWall = System.currentTimeMillis()
      val cur = WidgetTimerStore.load(app, nowElapsed, nowWall).state
      val reset = cur.copy(remainingSeconds = cur.totalSeconds, isRunning = false)
      WidgetTimerStore.save(app, reset, targetElapsedRealtimeMs = 0L, nowElapsedMs = nowElapsed, nowWallMs = nowWall)
      cancelFinishAlarm(app)
      NotificationHelper.cancelOngoingTimerNotification(app)
      WidgetTimerStore.notifyExternalChange()
    }

    private suspend fun completeIfExpired(app: GamefyApp) {
      val prefs = app.getSharedPreferences("gamefy_widget_timer_prefs", Context.MODE_PRIVATE)
      val wasRunningRaw = prefs.getBoolean("is_running", false)
      if (!wasRunningRaw) return

      val nowElapsed = SystemClock.elapsedRealtime()
      val nowWall = System.currentTimeMillis()
      val snap = WidgetTimerStore.load(app, nowElapsed, nowWall)
      if (snap.state.isRunning && snap.state.remainingSeconds > 0) return

      val cur = snap.state
      cancelFinishAlarm(app)
      NotificationHelper.cancelOngoingTimerNotification(app)
      NotificationHelper.vibratePattern(app)
      TimerSoundHelper.playGentleGong(app)

      if (cur.mode == MainTimerMode.POMODORO) {
        if (cur.pomodoroPhase == PomodoroPhase.FOCUS) {
          val newFocusCount = cur.completedFocusCount + 1
          val nextPhase = if (newFocusCount % 4 == 0) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
          val outcome = app.repository.completeFocusSession(
            totalSeconds = cur.totalSeconds,
            completedFullPomodoroCycle = nextPhase == PomodoroPhase.LONG_BREAK,
          )
          val nextSec = if (nextPhase == PomodoroPhase.LONG_BREAK) 15 * 60 else 5 * 60
          val nextState = cur.copy(
            completedFocusCount = newFocusCount,
            pomodoroPhase = nextPhase,
            totalSeconds = nextSec,
            remainingSeconds = nextSec,
            isRunning = false,
          )
          WidgetTimerStore.save(app, nextState, 0L, nowElapsed, nowWall)
          NotificationHelper.showNotification(
            context = app,
            notificationId = 8801,
            channelId = NotificationHelper.CHANNEL_TIMER,
            title = "Pomodoro ukończone!",
            body = "${outcome.toastMessage} — czas na: ${nextPhase.label}",
          )
        } else {
          val nextSec = 25 * 60
          val nextState = cur.copy(
            pomodoroPhase = PomodoroPhase.FOCUS,
            totalSeconds = nextSec,
            remainingSeconds = nextSec,
            isRunning = false,
          )
          WidgetTimerStore.save(app, nextState, 0L, nowElapsed, nowWall)
          NotificationHelper.showNotification(
            context = app,
            notificationId = 8802,
            channelId = NotificationHelper.CHANNEL_TIMER,
            title = "Koniec przerwy!",
            body = "Czas wrócić do skupienia.",
          )
        }
      } else {
        val outcome = app.repository.completeFocusSession(cur.totalSeconds)
        val nextState = cur.copy(remainingSeconds = 0, isRunning = false)
        WidgetTimerStore.save(app, nextState, 0L, nowElapsed, nowWall)
        NotificationHelper.showNotification(
          context = app,
          notificationId = 8803,
          channelId = NotificationHelper.CHANNEL_TIMER,
          title = "Timer zakończony!",
          body = outcome.toastMessage,
          targetRoute = "start",
        )
      }
      WidgetTimerStore.notifyExternalChange()
    }

    internal suspend fun updateWidgetsSync(
      context: Context,
      manager: AppWidgetManager,
      appWidgetIds: IntArray,
    ) {
      val app = context.applicationContext as? GamefyApp
      val themeCode = runCatching {
        app?.database?.gamefyDao()?.getProfileOnce()?.themeCode
      }.getOrNull() ?: THEME_CODE_SYSTEM
      val colors = WidgetThemeHelper.resolveColors(context, themeCode)
      val snap = WidgetTimerStore.load(context)
      val state = snap.state

      val phaseLabel = WidgetTimerStore.phaseDisplayLabel(state)
      val actionLabel = if (state.isRunning) "Pauza" else "Start"
      val actionIconKey = if (state.isRunning) "pause" else "play"
      val staticTimeText = WidgetTimerStore.formatClockText(state.remainingSeconds)

      val openStartIntent = WidgetRefreshHelper.buildOpenAppRoutePendingIntent(
        context = context,
        route = "start",
        requestCode = REQUEST_CODE_OPEN_START,
      )
      val toggleIntent = buildBroadcastPendingIntent(context, ACTION_TOGGLE_TIMER, REQUEST_CODE_TOGGLE)
      val resetIntent = buildBroadcastPendingIntent(context, ACTION_RESET_TIMER, REQUEST_CODE_RESET)

      appWidgetIds.forEach { widgetId ->
        val options = manager.getAppWidgetOptions(widgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140).coerceAtLeast(100)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120).coerceAtLeast(48)
        val isHorizontal = minHeight < 105 && minWidth >= 180

        val views = RemoteViews(context.packageName, R.layout.widget_timer)
        views.setImageViewBitmap(
          R.id.widget_timer_bg,
          WidgetBitmapRenderer.renderRoundedPanelBitmap(
            context = context,
            widthDp = minWidth.coerceAtMost(360),
            heightDp = minHeight.coerceAtMost(240),
            fillArgb = colors.surface,
            borderArgb = if (colors.isHighContrast) colors.outline else colors.divider,
            borderWidthDp = if (colors.isHighContrast) 2.5f else 2f,
            cornerRadiusDp = 16f,
          ),
        )

        val toggleBgBitmap = WidgetBitmapRenderer.renderRoundedPanelBitmap(
          context = context,
          widthDp = 110,
          heightDp = 44,
          fillArgb = colors.accent,
          borderArgb = colors.outline,
          borderWidthDp = if (colors.isHighContrast) 2f else 1.5f,
          cornerRadiusDp = 11f,
        )
        val resetBgBitmap = WidgetBitmapRenderer.renderRoundedPanelBitmap(
          context = context,
          widthDp = 44,
          heightDp = 44,
          fillArgb = colors.surfaceSubtle,
          borderArgb = if (colors.isHighContrast) colors.outline else colors.divider,
          borderWidthDp = if (colors.isHighContrast) 2f else 1.5f,
          cornerRadiusDp = 11f,
        )
        val timerIconBmp = WidgetBitmapRenderer.renderPixelSpriteBitmap(context, "timer", sizeDp = 22)
        val toggleIconBmp = WidgetBitmapRenderer.renderPixelSpriteBitmap(
          context = context,
          icon = actionIconKey,
          sizeDp = 16,
          monochromeTintArgb = colors.onAccent,
        )
        val resetIconBmp = WidgetBitmapRenderer.renderPixelSpriteBitmap(
          context = context,
          icon = "reset",
          sizeDp = 18,
          monochromeTintArgb = colors.textPrimary,
        )
        val phaseHeaderBmp = WidgetBitmapRenderer.renderPixelHeaderBitmap(
          context = context,
          text = phaseLabel,
          textSizeSp = if (isHorizontal) 18f else 20f,
          colorArgb = colors.accent,
          maxWidthDp = (minWidth - 56).coerceAtLeast(80),
        )

        if (isHorizontal) {
          views.setViewVisibility(R.id.widget_timer_vertical_container, View.GONE)
          views.setViewVisibility(R.id.widget_timer_horizontal_container, View.VISIBLE)

          views.setImageViewBitmap(R.id.widget_timer_icon_h, timerIconBmp)
          views.setImageViewBitmap(R.id.widget_timer_phase_img_h, phaseHeaderBmp)
          views.setImageViewBitmap(R.id.widget_timer_toggle_bg_h, toggleBgBitmap)
          views.setImageViewBitmap(R.id.widget_timer_toggle_icon_h, toggleIconBmp)
          views.setTextViewText(R.id.widget_timer_toggle_text_h, actionLabel)
          views.setTextColor(R.id.widget_timer_toggle_text_h, colors.onAccent)

          views.setImageViewBitmap(R.id.widget_timer_reset_bg_h, resetBgBitmap)
          views.setImageViewBitmap(R.id.widget_timer_reset_icon_h, resetIconBmp)

          bindClockDisplay(
            views = views,
            chronoId = R.id.widget_timer_chronometer_h,
            staticId = R.id.widget_timer_static_h,
            isRunning = state.isRunning,
            targetElapsedMs = snap.targetElapsedRealtimeMs,
            staticText = staticTimeText,
            textColor = colors.textPrimary,
          )

          views.setOnClickPendingIntent(R.id.widget_timer_open_area_h, openStartIntent)
          views.setOnClickPendingIntent(R.id.widget_timer_toggle_btn_h, toggleIntent)
          views.setOnClickPendingIntent(R.id.widget_timer_reset_btn_h, resetIntent)
        } else {
          views.setViewVisibility(R.id.widget_timer_vertical_container, View.VISIBLE)
          views.setViewVisibility(R.id.widget_timer_horizontal_container, View.GONE)

          views.setImageViewBitmap(R.id.widget_timer_icon_v, timerIconBmp)
          views.setImageViewBitmap(R.id.widget_timer_phase_img_v, phaseHeaderBmp)
          views.setImageViewBitmap(R.id.widget_timer_toggle_bg_v, toggleBgBitmap)
          views.setImageViewBitmap(R.id.widget_timer_toggle_icon_v, toggleIconBmp)
          views.setTextViewText(R.id.widget_timer_toggle_text_v, actionLabel)
          views.setTextColor(R.id.widget_timer_toggle_text_v, colors.onAccent)

          views.setImageViewBitmap(R.id.widget_timer_reset_bg_v, resetBgBitmap)
          views.setImageViewBitmap(R.id.widget_timer_reset_icon_v, resetIconBmp)

          bindClockDisplay(
            views = views,
            chronoId = R.id.widget_timer_chronometer_v,
            staticId = R.id.widget_timer_static_v,
            isRunning = state.isRunning,
            targetElapsedMs = snap.targetElapsedRealtimeMs,
            staticText = staticTimeText,
            textColor = colors.textPrimary,
          )

          views.setOnClickPendingIntent(R.id.widget_timer_open_area_v, openStartIntent)
          views.setOnClickPendingIntent(R.id.widget_timer_toggle_btn_v, toggleIntent)
          views.setOnClickPendingIntent(R.id.widget_timer_reset_btn_v, resetIntent)
        }

        manager.updateAppWidget(widgetId, views)
      }
    }

    private fun bindClockDisplay(
      views: RemoteViews,
      chronoId: Int,
      staticId: Int,
      isRunning: Boolean,
      targetElapsedMs: Long,
      staticText: String,
      textColor: Int,
    ) {
      if (isRunning && targetElapsedMs > 0L) {
        views.setViewVisibility(chronoId, View.VISIBLE)
        views.setViewVisibility(staticId, View.GONE)
        views.setTextColor(chronoId, textColor)
        views.setChronometerCountDown(chronoId, true)
        views.setChronometer(chronoId, targetElapsedMs, "%s", true)
      } else {
        views.setChronometer(chronoId, SystemClock.elapsedRealtime(), "%s", false)
        views.setViewVisibility(chronoId, View.GONE)
        views.setViewVisibility(staticId, View.VISIBLE)
        views.setTextViewText(staticId, staticText)
        views.setTextColor(staticId, textColor)
      }
    }

    private fun buildBroadcastPendingIntent(
      context: Context,
      action: String,
      requestCode: Int,
    ): PendingIntent {
      val intent = Intent(context, TimerWidgetProvider::class.java).apply {
        this.action = action
      }
      return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }
}
