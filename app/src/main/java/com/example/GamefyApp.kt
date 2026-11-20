package com.example

import android.app.Application
import com.example.data.local.GamefyDatabase
import com.example.data.repository.GamefyRepository
import com.example.notifications.NotificationHelper

class GamefyApp : Application() {
  val database: GamefyDatabase by lazy { GamefyDatabase.getInstance(this) }
  val repository: GamefyRepository by lazy { GamefyRepository(database.gamefyDao()) }

  override fun onCreate() {
    super.onCreate()
    NotificationHelper.ensureChannels(this)
  }
}
