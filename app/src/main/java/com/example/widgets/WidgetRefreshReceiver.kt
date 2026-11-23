package com.example.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetRefreshReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      WidgetRefreshHelper.ACTION_MIDNIGHT_REFRESH,
      Intent.ACTION_DATE_CHANGED,
      Intent.ACTION_TIME_CHANGED,
      Intent.ACTION_TIMEZONE_CHANGED,
      -> {
        WidgetRefreshHelper.refreshAllWidgets(context)
        WidgetRefreshHelper.scheduleMidnightRefresh(context)
      }
    }
  }
}
