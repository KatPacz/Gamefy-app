package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
  @PrimaryKey val id: Long = 1L,
  val exp: Int = 0,
  val coins: Int = 0,
  val gems: Int = 0,
  val tasksDone: Int = 0,
  val pomosDone: Int = 0,
  val coinsEarned: Int = 0,
  val itemsBought: Int = 0,
  val notificationsEnabled: Boolean = false,
  val themeCode: String = "violet",
  val unlockedAchievementsCsv: String = "",
  val currentStreak: Int = 0,
  val bestStreak: Int = 0,
  val lastTaskEpochDay: Long = -1L,
  val customTasksCreated: Int = 0,
  val boughtMostExpensive: Boolean = false,
  val completedNightAction: Boolean = false,
  val completedSubtaskChain: Boolean = false,
  val notifDaysCsv: String = "1,2,3,4,5,6,7",
  val notifSlotsCsv: String = "any",
  val notifCustomStartHour: Int = 9,
  val notifCustomStartMinute: Int = 0,
  val notifCustomEndHour: Int = 21,
  val notifCustomEndMinute: Int = 0,
  val notifTriggerModeCode: String = "reminder",
  val notifMaxPerDay: Int = 0,
  val notifSummaryStyleCode: String = "digest",
  val notifUseTaskSettings: Boolean = true,
  val notifSentCountToday: Int = 0,
  val notifSentEpochDay: Long = -1L,
)

@Entity(tableName = "attributes")
data class AttributeEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val icon: String,
  val name: String,
  val exp: Int = 0,
)

@Entity(tableName = "folders")
data class FolderEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val icon: String,
  val name: String,
)

@Entity(tableName = "tags")
data class TagEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
)

data class SubtaskItem(
  val id: Long,
  val title: String,
  val isDone: Boolean = false,
)

@Entity(tableName = "tasks")
data class TaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val description: String = "",
  val link: String = "",
  val folderId: Long = 0L,
  val tagIdsCsv: String = "",
  val categoryCode: String = "w",
  val repeatInterval: Int = 1,
  val repeatLimit: Int = 0,
  val completedCount: Int = 0,
  val expReward: Int = 20,
  val coinReward: Int = 10,
  val attributeIdsCsv: String = "",
  val rewardItemId: Long = 0L,
  val durationMinutes: Int = 0,
  val reminderModeCode: String = "d1",
  val penaltyModeCode: String = "0",
  val penaltyMinCoins: Int = 5,
  val penaltyMaxCoins: Int = 10,
  val startTimestamp: Long = System.currentTimeMillis(),
  val dueTimestamp: Long? = null,
  val penaltyDueSnapshot: Long? = null,
  val penaltyCountApplied: Int = 0,
  val lastNotifiedKey: String = "",
  val subtasksJson: String = "[]",
  val difficultyCode: String = "m",
  val gemReward: Int = 0,
  val preferredReminderHour: Int = -1,
  val preferredReminderMinute: Int = 0,
)

@Entity(tableName = "shop_items")
data class ShopItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val icon: String,
  val name: String,
  val coinPrice: Int = 50,
  val gemPrice: Int = 0,
  val requiredAttrId: Long = 0L,
  val requiredAttrLevel: Int = 1,
  val ownedQuantity: Int = 0,
  val unlocked: Boolean = false,
  val imageData: String = "",
  val description: String = "",
  val limitPeriodCode: String = "0",
  val limitMaxCount: Int = 1,
  val purchaseHistoryCsv: String = "",
)

fun parseIdCsv(csv: String): List<Long> =
  csv.split(',').mapNotNull { it.trim().toLongOrNull() }.filter { it > 0L }

fun toIdCsv(ids: Iterable<Long>): String =
  ids.distinct().filter { it > 0L }.joinToString(",")

fun parseStringCsv(csv: String): Set<String> =
  csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

fun toStringCsv(values: Iterable<String>): String =
  values.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

fun parseSubtasksJson(json: String): List<SubtaskItem> {
  if (json.isBlank() || json == "[]") return emptyList()
  return runCatching {
    val arr = JSONArray(json)
    (0 until arr.length()).mapNotNull { idx ->
      val o = arr.getJSONObject(idx)
      val title = o.optString("title", "").trim()
      if (title.isEmpty()) null
      else SubtaskItem(
        id = o.optLong("id", idx + 1L),
        title = title,
        isDone = o.optBoolean("done", false),
      )
    }
  }.getOrDefault(emptyList())
}

fun toSubtasksJson(subtasks: List<SubtaskItem>): String {
  val arr = JSONArray()
  subtasks.forEach { st ->
    arr.put(
      JSONObject().apply {
        put("id", st.id)
        put("title", st.title)
        put("done", st.isDone)
      }
    )
  }
  return arr.toString()
}
