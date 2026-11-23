package com.example.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.GamefyApp
import com.example.R
import com.example.data.model.THEME_CODE_SYSTEM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickAddWidgetProvider : AppWidgetProvider() {

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    WidgetRefreshHelper.scheduleMidnightRefresh(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    WidgetRefreshHelper.scheduleMidnightRefresh(context)
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
        updateWidgetsSync(context.applicationContext, appWidgetManager, appWidgetIds)
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
    private const val REQUEST_CODE_QUICK_ADD = 9501

    fun enqueueUpdate(context: Context, appWidgetIds: IntArray) {
      val app = context.applicationContext as? GamefyApp ?: return
      val manager = AppWidgetManager.getInstance(app)
      app.applicationScope.launch(Dispatchers.IO) {
        updateWidgetsSync(app, manager, appWidgetIds)
      }
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

      appWidgetIds.forEach { widgetId ->
        val options = manager.getAppWidgetOptions(widgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 56).coerceAtLeast(48)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56).coerceAtLeast(48)
        val showLabel = minWidth >= 110

        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
        views.setImageViewBitmap(
          R.id.widget_quick_add_bg,
          WidgetBitmapRenderer.renderRoundedPanelBitmap(
            context = context,
            widthDp = minWidth.coerceAtMost(240),
            heightDp = minHeight.coerceAtMost(120),
            fillArgb = colors.accent,
            borderArgb = if (colors.isHighContrast) colors.outline else colors.outline,
            borderWidthDp = if (colors.isHighContrast) 2.5f else 2f,
            cornerRadiusDp = 16f,
          ),
        )
        views.setImageViewBitmap(
          R.id.widget_quick_add_icon,
          WidgetBitmapRenderer.renderPixelSpriteBitmap(
            context = context,
            icon = "plus",
            sizeDp = 26,
            monochromeTintArgb = colors.onAccent,
          ),
        )

        if (showLabel) {
          views.setViewVisibility(R.id.widget_quick_add_label_img, View.VISIBLE)
          views.setImageViewBitmap(
            R.id.widget_quick_add_label_img,
            WidgetBitmapRenderer.renderPixelHeaderBitmap(
              context = context,
              text = "Dodaj",
              textSizeSp = 22f,
              colorArgb = colors.onAccent,
              maxWidthDp = (minWidth - 52).coerceAtLeast(48),
            ),
          )
        } else {
          views.setViewVisibility(R.id.widget_quick_add_label_img, View.GONE)
        }

        val openEditorIntent = WidgetRefreshHelper.buildOpenAppRoutePendingIntent(
          context = context,
          route = "task_edit/0",
          requestCode = REQUEST_CODE_QUICK_ADD,
        )
        views.setOnClickPendingIntent(R.id.widget_quick_add_root, openEditorIntent)
        manager.updateAppWidget(widgetId, views)
      }
    }
  }
}
