package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.GamefyDatabase
import com.example.data.repository.GamefyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = GamefyDatabase.getInstance(context)
        val repo = GamefyRepository(db.gamefyDao())
        val profile = db.gamefyDao().getProfileOnce()
        if (profile?.notificationsEnabled == true) {
          val alerts = repo.checkDueReminders()
          alerts.forEach { (taskId, message) ->
            NotificationHelper.showNotification(
              context = context,
              notificationId = (taskId % Int.MAX_VALUE).toInt(),
              channelId = NotificationHelper.CHANNEL_REMINDERS,
              title = "Gamefy – Zadanie",
              body = message,
            )
          }
          ReminderScheduler.scheduleNextCheck(context)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
