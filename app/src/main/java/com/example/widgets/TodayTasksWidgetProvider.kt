package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.GamefyApp
import com.example.R
import com.example.data.model.THEME_CODE_SYSTEM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TodayTasksWidgetProvider : AppWidgetProvider() {

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    WidgetRefreshHelper.scheduleMidnightRefresh(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    WidgetRefreshHelper.scheduleMidnightRefresh(context)
  }

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_COMPLETE_TASK, ACTION_UNDO_TASK -> {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        if (taskId <= 0L) return
        val pendingResult = goAsync()
        val app = context.applicationContext as? GamefyApp
        if (app == null) {
          pendingResult.finish()
          return
        }
        app.applicationScope.launch(Dispatchers.IO) {
          try {
            when (intent.action) {
              ACTION_COMPLETE_TASK -> handleCompleteTask(app, taskId)
              ACTION_UNDO_TASK -> handleUndoTask(app, taskId)
            }
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, TodayTasksWidgetProvider::class.java))
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
    const val ACTION_COMPLETE_TASK = "com.example.widgets.ACTION_COMPLETE_TASK"
    const val ACTION_UNDO_TASK = "com.example.widgets.ACTION_UNDO_TASK"
    const val EXTRA_TASK_ID = "extra_widget_task_id"

    private const val REQUEST_CODE_OPEN_TASKS = 9701
    private const val REQUEST_CODE_OPEN_NEW_TASK = 9702
    private const val REQUEST_CODE_OPEN_TASK_BASE = 9720
    private const val REQUEST_CODE_COMPLETE_BASE = 9740
    private const val REQUEST_CODE_UNDO_BASE = 9760

    private data class RowViewIds(
      val containerId: Int,
      val checkId: Int,
      val bodyId: Int,
      val titleId: Int,
      val subId: Int,
      val undoId: Int,
    )

    private val ROW_VIEW_IDS = listOf(
      RowViewIds(
        R.id.widget_task_row_0,
        R.id.widget_task_check_0,
        R.id.widget_task_body_0,
        R.id.widget_task_title_0,
        R.id.widget_task_sub_0,
        R.id.widget_task_undo_0,
      ),
      RowViewIds(
        R.id.widget_task_row_1,
        R.id.widget_task_check_1,
        R.id.widget_task_body_1,
        R.id.widget_task_title_1,
        R.id.widget_task_sub_1,
        R.id.widget_task_undo_1,
      ),
      RowViewIds(
        R.id.widget_task_row_2,
        R.id.widget_task_check_2,
        R.id.widget_task_body_2,
        R.id.widget_task_title_2,
        R.id.widget_task_sub_2,
        R.id.widget_task_undo_2,
      ),
      RowViewIds(
        R.id.widget_task_row_3,
        R.id.widget_task_check_3,
        R.id.widget_task_body_3,
        R.id.widget_task_title_3,
        R.id.widget_task_sub_3,
        R.id.widget_task_undo_3,
      ),
      RowViewIds(
        R.id.widget_task_row_4,
        R.id.widget_task_check_4,
        R.id.widget_task_body_4,
        R.id.widget_task_title_4,
        R.id.widget_task_sub_4,
        R.id.widget_task_undo_4,
      ),
    )

    fun enqueueUpdate(context: Context, appWidgetIds: IntArray) {
      val app = context.applicationContext as? GamefyApp ?: return
      val manager = AppWidgetManager.getInstance(app)
      app.applicationScope.launch(Dispatchers.IO) {
        updateWidgetsSync(app, manager, appWidgetIds)
      }
    }

    private suspend fun handleCompleteTask(app: GamefyApp, taskId: Long) {
      val task = app.database.gamefyDao().getTaskByIdOnce(taskId) ?: return
      val outcome = app.repository.completeTask(task)
      val snapshot = outcome.undoSnapshot ?: return
      val expiresAt = System.currentTimeMillis() + WidgetUndoStore.UNDO_WINDOW_MILLIS
      WidgetUndoStore.put(
        PendingWidgetUndo(
          taskId = task.id,
          taskName = task.name,
          snapshot = snapshot,
          expiresAtMillis = expiresAt,
        )
      )
      app.applicationScope.launch(Dispatchers.IO) {
        delay(WidgetUndoStore.UNDO_WINDOW_MILLIS + 150L)
        if (WidgetUndoStore.purgeExpired()) {
          WidgetRefreshHelper.refreshTodayTasksWidgets(app)
        }
      }
    }

    private suspend fun handleUndoTask(app: GamefyApp, taskId: Long) {
      val pending = WidgetUndoStore.take(taskId) ?: return
      app.repository.undoTaskAction(pending.snapshot)
    }

    internal suspend fun updateWidgetsSync(
      context: Context,
      manager: AppWidgetManager,
      appWidgetIds: IntArray,
    ) {
      val app = context.applicationContext as? GamefyApp ?: return
      val dao = app.database.gamefyDao()
      val profile = dao.getProfileOnce()
      val themeCode = profile?.themeCode ?: THEME_CODE_SYSTEM
      val colors = WidgetThemeHelper.resolveColors(context, themeCode)
      val tasks = dao.getTasksOnce()
      val events = dao.getActivityEventsOnce()
      val now = System.currentTimeMillis()
      val pendingUndos = WidgetUndoStore.activeEntries(now)

      val openTasksIntent = WidgetRefreshHelper.buildOpenAppRoutePendingIntent(
        context = context,
        route = "tasks",
        requestCode = REQUEST_CODE_OPEN_TASKS,
      )
      val openNewTaskIntent = WidgetRefreshHelper.buildOpenAppRoutePendingIntent(
        context = context,
        route = "task_edit/0",
        requestCode = REQUEST_CODE_OPEN_NEW_TASK,
      )

      val headerIconBmp = WidgetBitmapRenderer.renderPixelSpriteBitmap(context, "checklist", sizeDp = 20)
      val addBgBmp = WidgetBitmapRenderer.renderRoundedPanelBitmap(
        context = context,
        widthDp = 36,
        heightDp = 36,
        fillArgb = colors.accent,
        borderArgb = colors.outline,
        borderWidthDp = if (colors.isHighContrast) 2f else 1.5f,
        cornerRadiusDp = 10f,
      )
      val addIconBmp = WidgetBitmapRenderer.renderPixelSpriteBitmap(
        context = context,
        icon = "plus",
        sizeDp = 18,
        monochromeTintArgb = colors.onAccent,
      )
      val emptyIconBmp = WidgetBitmapRenderer.renderPixelSpriteBitmap(context, "sparkle", sizeDp = 28)
      val emptyAddBgBmp = WidgetBitmapRenderer.renderRoundedPanelBitmap(
        context = context,
        widthDp = 120,
        heightDp = 38,
        fillArgb = colors.accent,
        borderArgb = colors.outline,
        borderWidthDp = if (colors.isHighContrast) 2f else 1.5f,
        cornerRadiusDp = 10f,
      )
      val checkboxEmptyBmp = WidgetBitmapRenderer.renderPixelSpriteBitmap(
        context = context,
        icon = "checkbox_empty",
        sizeDp = 22,
        monochromeTintArgb = if (colors.isHighContrast) colors.textPrimary else colors.accent,
      )
      val checkboxDoneBmp = WidgetBitmapRenderer.renderPixelSpriteBitmap(
        context = context,
        icon = "checkbox_done",
        sizeDp = 22,
        grayscale = false,
      )

      appWidgetIds.forEach { widgetId ->
        val options = manager.getAppWidgetOptions(widgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260).coerceAtLeast(180)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160).coerceAtLeast(100)
        val maxRows = TodayTasksWidgetLogic.computeMaxRowsForHeightDp(minHeight)

        val snapshot = TodayTasksWidgetLogic.buildSnapshot(
          tasks = tasks,
          activityEvents = events,
          pendingUndos = pendingUndos,
          nowMillis = now,
          maxRows = maxRows,
        )

        val views = RemoteViews(context.packageName, R.layout.widget_today_tasks)
        views.setImageViewBitmap(
          R.id.widget_today_bg,
          WidgetBitmapRenderer.renderRoundedPanelBitmap(
            context = context,
            widthDp = minWidth.coerceAtMost(400),
            heightDp = minHeight.coerceAtMost(360),
            fillArgb = colors.surface,
            borderArgb = if (colors.isHighContrast) colors.outline else colors.divider,
            borderWidthDp = if (colors.isHighContrast) 2.5f else 2f,
            cornerRadiusDp = 16f,
          ),
        )

        views.setImageViewBitmap(R.id.widget_today_header_icon, headerIconBmp)
        views.setImageViewBitmap(
          R.id.widget_today_header_title_img,
          WidgetBitmapRenderer.renderPixelHeaderBitmap(
            context = context,
            text = snapshot.headerCounterText,
            textSizeSp = 22f,
            colorArgb = colors.textPrimary,
            maxWidthDp = (minWidth - 88).coerceAtLeast(100),
          ),
        )
        views.setOnClickPendingIntent(R.id.widget_today_header_click, openTasksIntent)

        views.setImageViewBitmap(R.id.widget_today_add_bg, addBgBmp)
        views.setImageViewBitmap(R.id.widget_today_add_icon, addIconBmp)
        views.setOnClickPendingIntent(R.id.widget_today_add_btn, openNewTaskIntent)

        if (snapshot.rows.isEmpty()) {
          views.setViewVisibility(R.id.widget_today_empty_container, View.VISIBLE)
          views.setViewVisibility(R.id.widget_today_list_container, View.GONE)
          views.setImageViewBitmap(R.id.widget_today_empty_icon, emptyIconBmp)
          views.setTextColor(R.id.widget_today_empty_text, colors.textMuted)
          views.setImageViewBitmap(R.id.widget_today_empty_add_bg, emptyAddBgBmp)
          views.setTextColor(R.id.widget_today_empty_add_text, colors.onAccent)
          views.setOnClickPendingIntent(R.id.widget_today_empty_add_btn, openNewTaskIntent)
        } else {
          views.setViewVisibility(R.id.widget_today_empty_container, View.GONE)
          views.setViewVisibility(R.id.widget_today_list_container, View.VISIBLE)

          ROW_VIEW_IDS.forEachIndexed { idx, rowIds ->
            val row = snapshot.rows.getOrNull(idx)
            if (row == null) {
              views.setViewVisibility(rowIds.containerId, View.GONE)
            } else {
              views.setViewVisibility(rowIds.containerId, View.VISIBLE)
              views.setTextViewText(rowIds.titleId, row.title)
              views.setTextViewText(rowIds.subId, row.subtitle)

              if (row.isPendingUndo) {
                views.setImageViewBitmap(rowIds.checkId, checkboxDoneBmp)
                views.setTextColor(rowIds.titleId, colors.textMuted)
                views.setTextColor(rowIds.subId, colors.textMuted)
                views.setInt(
                  rowIds.titleId,
                  "setPaintFlags",
                  Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG,
                )
                views.setViewVisibility(rowIds.undoId, View.VISIBLE)
                views.setTextColor(rowIds.undoId, colors.accent)
                views.setInt(rowIds.undoId, "setBackgroundColor", colors.surfaceSubtle)

                val undoPi = buildTaskActionPendingIntent(
                  context = context,
                  action = ACTION_UNDO_TASK,
                  taskId = row.taskId,
                  requestCode = REQUEST_CODE_UNDO_BASE + idx,
                )
                views.setOnClickPendingIntent(rowIds.undoId, undoPi)
                views.setOnClickPendingIntent(rowIds.checkId, undoPi)
                views.setOnClickPendingIntent(rowIds.bodyId, openTasksIntent)
              } else {
                views.setImageViewBitmap(rowIds.checkId, checkboxEmptyBmp)
                views.setTextColor(rowIds.titleId, colors.textPrimary)
                views.setTextColor(
                  rowIds.subId,
                  if (row.isOverdue) colors.danger else colors.textMuted,
                )
                views.setInt(rowIds.titleId, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
                views.setViewVisibility(rowIds.undoId, View.GONE)

                val completePi = buildTaskActionPendingIntent(
                  context = context,
                  action = ACTION_COMPLETE_TASK,
                  taskId = row.taskId,
                  requestCode = REQUEST_CODE_COMPLETE_BASE + idx,
                )
                val openTaskPi = WidgetRefreshHelper.buildOpenAppRoutePendingIntent(
                  context = context,
                  route = "task_edit/${row.taskId}",
                  requestCode = REQUEST_CODE_OPEN_TASK_BASE + idx,
                )
                views.setOnClickPendingIntent(rowIds.checkId, completePi)
                views.setOnClickPendingIntent(rowIds.bodyId, openTaskPi)
              }
            }
          }
        }

        manager.updateAppWidget(widgetId, views)
      }
    }

    private fun buildTaskActionPendingIntent(
      context: Context,
      action: String,
      taskId: Long,
      requestCode: Int,
    ): PendingIntent {
      val intent = Intent(context, TodayTasksWidgetProvider::class.java).apply {
        this.action = action
        putExtra(EXTRA_TASK_ID, taskId)
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
