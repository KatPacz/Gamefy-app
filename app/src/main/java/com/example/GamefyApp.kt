package com.example

import android.app.Application
import com.example.data.local.GamefyDatabase
import com.example.data.repository.GamefyRepository
import com.example.notifications.NotificationHelper
import com.example.widgets.WidgetRefreshHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GamefyApp : Application() {
  val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  val database: GamefyDatabase by lazy { GamefyDatabase.getInstance(this) }
  val repository: GamefyRepository by lazy {
    GamefyRepository(
      dao = database.gamefyDao(),
      onDataChanged = { WidgetRefreshHelper.refreshAllWidgets(this) },
    )
  }

  override fun onCreate() {
    super.onCreate()
    NotificationHelper.ensureChannels(this)
    WidgetRefreshHelper.scheduleMidnightRefresh(this)
  }
}
