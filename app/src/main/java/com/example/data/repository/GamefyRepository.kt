package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.local.AttributeEntity
import com.example.data.local.FolderEntity
import com.example.data.local.GamefyDao
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.ShopItemEntity
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.parseIdCsv
import com.example.data.local.parseStringCsv
import com.example.data.local.parseSubtasksJson
import com.example.data.local.toIdCsv
import com.example.data.local.toStringCsv
import com.example.data.local.toSubtasksJson
import com.example.data.model.AchievementDefinition
import com.example.data.model.AppThemeId
import com.example.data.model.GAMEFY_ACHIEVEMENTS
import com.example.data.model.MILLIS_PER_DAY
import com.example.data.model.PenaltyMode
import com.example.data.model.PlayerStatsSnapshot
import com.example.data.model.PurchaseLimitPeriod
import com.example.data.model.ReminderMode
import com.example.data.model.TaskCategory
import com.example.data.model.calculateLevel
import com.example.data.model.formatDurationShort
import com.example.data.model.localEpochDay
import com.example.data.model.nextCycleTimestamp
import java.io.ByteArrayOutputStream
import java.util.Calendar
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RewardOutcome(
  val toastMessage: String = "",
  val unlockedAchievements: List<AchievementDefinition> = emptyList(),
)

class GamefyRepository(private val dao: GamefyDao) {

  val profile: Flow<PlayerProfileEntity> =
    dao.observeProfile().map { it ?: PlayerProfileEntity() }
  val attributes: Flow<List<AttributeEntity>> = dao.observeAttributes()
  val folders: Flow<List<FolderEntity>> = dao.observeFolders()
  val tags: Flow<List<TagEntity>> = dao.observeTags()
  val tasks: Flow<List<TaskEntity>> = dao.observeTasks()
  val shopItems: Flow<List<ShopItemEntity>> = dao.observeShopItems()

  suspend fun completeTask(task: TaskEntity, fromSubtaskChain: Boolean = false): RewardOutcome {
    val now = System.currentTimeMillis()
    if (task.startTimestamp > now) return RewardOutcome()

    val category = TaskCategory.fromCode(task.categoryCode)
    val newCount = task.completedCount + 1
    val isFinished =
      category == TaskCategory.SIDEQUEST || (task.repeatLimit > 0 && newCount >= task.repeatLimit)

    var itemSuffix = ""
    if (task.rewardItemId > 0L) {
      val shopItem = dao.getShopItemsOnce().find { it.id == task.rewardItemId }
      if (shopItem != null) {
        dao.updateShopItem(shopItem.copy(ownedQuantity = shopItem.ownedQuantity + 1))
        itemSuffix = " 🎁 ${shopItem.name}"
      }
    }

    if (isFinished) {
      dao.deleteTaskById(task.id)
    } else {
      val (nextStart, nextDue) = advanceCycle(task, now)
      val resetSubtasks = parseSubtasksJson(task.subtasksJson).map { it.copy(isDone = false) }
      dao.updateTask(
        task.copy(
          completedCount = newCount,
          startTimestamp = nextStart,
          dueTimestamp = nextDue,
          penaltyDueSnapshot = null,
          penaltyCountApplied = 0,
          lastNotifiedKey = "",
          subtasksJson = toSubtasksJson(resetSubtasks),
        )
      )
    }

    val prefix = (if (isFinished) "✔ Zakończono! " else "") + itemSuffix.trimStart()
    return awardRewards(
      expGain = task.expReward,
      coinGain = task.coinReward,
      gemGain = task.gemReward,
      attributeIds = parseIdCsv(task.attributeIdsCsv),
      incrementTasksDone = true,
      markSubtaskChain = fromSubtaskChain,
      prefix = prefix.trim(),
    )
  }

  suspend fun toggleSubtask(task: TaskEntity, subtaskId: Long): RewardOutcome? {
    val subtasks = parseSubtasksJson(task.subtasksJson)
    if (subtasks.isEmpty()) return null
    val updated = subtasks.map { st ->
      if (st.id == subtaskId) st.copy(isDone = !st.isDone) else st
    }
    val updatedTask = task.copy(subtasksJson = toSubtasksJson(updated))
    return if (updated.all { it.isDone }) {
      completeTask(updatedTask, fromSubtaskChain = true)
    } else {
      dao.updateTask(updatedTask)
      null
    }
  }

  suspend fun skipTask(task: TaskEntity): String {
    val now = System.currentTimeMillis()
    val penaltyMode = PenaltyMode.fromCode(task.penaltyModeCode)
    var message = "Pominięto zadanie."

    if (penaltyMode == PenaltyMode.ON_SKIP_ONLY) {
      val penalty = computePenaltyAmount(task.penaltyMinCoins, task.penaltyMaxCoins)
      if (penalty > 0) {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(profile.copy(coins = (profile.coins - penalty).coerceAtLeast(0)))
        message = "Pominięto. Kara −$penalty 🪙"
      }
    }

    val category = TaskCategory.fromCode(task.categoryCode)
    if (category == TaskCategory.SIDEQUEST) {
      dao.deleteTaskById(task.id)
    } else {
      val (nextStart, nextDue) = advanceCycle(task, now)
      val resetSubtasks = parseSubtasksJson(task.subtasksJson).map { it.copy(isDone = false) }
      dao.updateTask(
        task.copy(
          startTimestamp = nextStart,
          dueTimestamp = nextDue,
          penaltyDueSnapshot = null,
          penaltyCountApplied = 0,
          lastNotifiedKey = "",
          subtasksJson = toSubtasksJson(resetSubtasks),
        )
      )
    }
    return message
  }

  private fun advanceCycle(task: TaskEntity, now: Long): Pair<Long, Long?> {
    val category = TaskCategory.fromCode(task.categoryCode)
    val due = task.dueTimestamp
    return if (due == null || now > due) {
      // Spóźnione lub bez terminu: nowy cykl zaczyna się od teraz
      now to nextCycleTimestamp(now, category, task.repeatInterval)
    } else {
      // Wykonane przed terminem: znika do końca bieżącego okresu, potem liczy kolejny cykl
      due to nextCycleTimestamp(due, category, task.repeatInterval)
    }
  }

  suspend fun completeFocusSession(totalSeconds: Int): RewardOutcome {
    val minutes = max(1, totalSeconds / 60)
    val expGain = minutes
    val coinGain = max(1, totalSeconds / 300)
    val discipline = dao.getAttributesOnce().find { it.name.equals("Dyscyplina", ignoreCase = true) }
    val attrIds = if (discipline != null) listOf(discipline.id) else emptyList()
    return awardRewards(
      expGain = expGain,
      coinGain = coinGain,
      attributeIds = attrIds,
      incrementPomosDone = true,
      prefix = "⏱️ Sesja ukończona!",
    )
  }

  private fun isNightActionNow(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour >= 23 || hour < 4
  }

  private suspend fun awardRewards(
    expGain: Int,
    coinGain: Int,
    gemGain: Int = 0,
    attributeIds: List<Long>,
    incrementTasksDone: Boolean = false,
    incrementPomosDone: Boolean = false,
    markSubtaskChain: Boolean = false,
    prefix: String = "",
  ): RewardOutcome {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    val oldLevel = calculateLevel(profile.exp).level

    var gemsGained = gemGain.coerceAtLeast(0)
    if (expGain > 0 && attributeIds.isNotEmpty()) {
      val allAttrs = dao.getAttributesOnce()
      for (attrId in attributeIds) {
        val attr = allAttrs.find { it.id == attrId } ?: continue
        val prevAttrLevel = calculateLevel(attr.exp).level
        val newAttrExp = attr.exp + expGain
        val nextAttrLevel = calculateLevel(newAttrExp).level
        if (nextAttrLevel > prevAttrLevel) {
          gemsGained += (nextAttrLevel - prevAttrLevel)
        }
        dao.updateAttribute(attr.copy(exp = newAttrExp))
      }
    }

    var newStreak = profile.currentStreak
    var newBestStreak = profile.bestStreak
    var newLastTaskDay = profile.lastTaskEpochDay
    if (incrementTasksDone) {
      val today = localEpochDay()
      newStreak = when (profile.lastTaskEpochDay) {
        today -> max(1, profile.currentStreak)
        today - 1L -> profile.currentStreak + 1
        else -> 1
      }
      newBestStreak = max(profile.bestStreak, newStreak)
      newLastTaskDay = today
    }

    val updatedProfile = profile.copy(
      exp = profile.exp + expGain,
      coins = profile.coins + coinGain,
      gems = profile.gems + gemsGained,
      tasksDone = profile.tasksDone + if (incrementTasksDone) 1 else 0,
      pomosDone = profile.pomosDone + if (incrementPomosDone) 1 else 0,
      coinsEarned = profile.coinsEarned + coinGain,
      currentStreak = newStreak,
      bestStreak = newBestStreak,
      lastTaskEpochDay = newLastTaskDay,
      completedNightAction = profile.completedNightAction || isNightActionNow(),
      completedSubtaskChain = profile.completedSubtaskChain || markSubtaskChain,
    )

    val (finalProfile, newlyUnlocked) = evaluateAndGrantAchievements(updatedProfile)
    dao.upsertProfile(finalProfile)

    val newLevel = calculateLevel(finalProfile.exp).level
    val parts = mutableListOf<String>()
    if (newLevel > oldLevel) parts.add("🎉 Poziom $newLevel!")
    if (prefix.isNotEmpty()) parts.add(prefix)
    parts.add("+$expGain EXP  +$coinGain 🪙")
    if (gemsGained > 0) parts.add("+$gemsGained 💎")
    return RewardOutcome(
      toastMessage = parts.joinToString("  "),
      unlockedAchievements = newlyUnlocked,
    )
  }

  private fun evaluateAndGrantAchievements(
    baseProfile: PlayerProfileEntity,
  ): Pair<PlayerProfileEntity, List<AchievementDefinition>> {
    var current = baseProfile
    val unlockedSet = parseStringCsv(current.unlockedAchievementsCsv).toMutableSet()
    val newlyUnlocked = mutableListOf<AchievementDefinition>()

    repeat(2) {
      val stats = PlayerStatsSnapshot(
        exp = current.exp,
        tasksDone = current.tasksDone,
        pomosDone = current.pomosDone,
        coinsEarned = current.coinsEarned,
        itemsBought = current.itemsBought,
        currentStreak = current.currentStreak,
        bestStreak = current.bestStreak,
        customTasksCreated = current.customTasksCreated,
        boughtMostExpensive = current.boughtMostExpensive,
        completedNightAction = current.completedNightAction,
        completedSubtaskChain = current.completedSubtaskChain,
      )
      var bonusExp = 0
      var bonusCoins = 0
      var bonusGems = 0
      for (ach in GAMEFY_ACHIEVEMENTS) {
        if (ach.id !in unlockedSet && ach.isUnlocked(stats)) {
          unlockedSet.add(ach.id)
          newlyUnlocked.add(ach)
          bonusExp += ach.rewardExp
          bonusCoins += ach.rewardCoins
          bonusGems += ach.rewardGems
        }
      }
      if (bonusExp == 0 && bonusCoins == 0 && bonusGems == 0) {
        return@repeat
      }
      current = current.copy(
        exp = current.exp + bonusExp,
        coins = current.coins + bonusCoins,
        gems = current.gems + bonusGems,
        coinsEarned = current.coinsEarned + bonusCoins,
      )
    }

    return current.copy(unlockedAchievementsCsv = toStringCsv(unlockedSet)) to newlyUnlocked
  }

  suspend fun evaluatePenalties(): Int {
    val now = System.currentTimeMillis()
    val tasks = dao.getTasksOnce()
    var totalPenalty = 0

    for (task in tasks) {
      val mode = PenaltyMode.fromCode(task.penaltyModeCode)
      val due = task.dueTimestamp ?: continue
      if (mode != PenaltyMode.ON_DUE_ONCE && mode != PenaltyMode.EVERY_MISSED_PERIOD) continue
      if (task.startTimestamp > now) continue

      val category = TaskCategory.fromCode(task.categoryCode)
      var snapshot = task.penaltyDueSnapshot
      var applied = task.penaltyCountApplied
      if (snapshot != due) {
        snapshot = due
        applied = 0
      }

      var checkBoundary = due
      for (i in 0 until applied) {
        checkBoundary =
          if (mode == PenaltyMode.ON_DUE_ONCE || category == TaskCategory.SIDEQUEST) {
            Long.MAX_VALUE
          } else {
            nextCycleTimestamp(checkBoundary, category, task.repeatInterval)
          }
      }

      var changed = snapshot != task.penaltyDueSnapshot || applied != task.penaltyCountApplied
      while (checkBoundary <= now && applied < 99) {
        totalPenalty += computePenaltyAmount(task.penaltyMinCoins, task.penaltyMaxCoins)
        applied++
        changed = true
        if (mode == PenaltyMode.ON_DUE_ONCE || category == TaskCategory.SIDEQUEST) break
        checkBoundary = nextCycleTimestamp(checkBoundary, category, task.repeatInterval)
      }

      if (changed) {
        dao.updateTask(task.copy(penaltyDueSnapshot = snapshot, penaltyCountApplied = applied))
      }
    }

    if (totalPenalty > 0) {
      val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
      dao.upsertProfile(profile.copy(coins = (profile.coins - totalPenalty).coerceAtLeast(0)))
    }
    return totalPenalty
  }

  private fun computePenaltyAmount(minCoins: Int, maxCoins: Int): Int {
    val low = minCoins.coerceAtLeast(0)
    val high = maxCoins.coerceAtLeast(low)
    return if (high > low) Random.nextInt(low, high + 1) else low
  }

  suspend fun saveTask(task: TaskEntity): RewardOutcome {
    return if (task.id == 0L) {
      dao.insertTask(task)
      val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
      val (finalProfile, unlocked) = evaluateAndGrantAchievements(
        profile.copy(customTasksCreated = profile.customTasksCreated + 1)
      )
      dao.upsertProfile(finalProfile)
      RewardOutcome(toastMessage = "Zapisano zadanie", unlockedAchievements = unlocked)
    } else {
      dao.updateTask(task)
      RewardOutcome(toastMessage = "Zapisano zadanie")
    }
  }

  suspend fun deleteTask(taskId: Long) {
    dao.deleteTaskById(taskId)
  }

  suspend fun applyBulkUpdate(
    taskIds: Set<Long>,
    category: TaskCategory?,
    interval: Int,
    folderId: Long?,
    reminder: ReminderMode?,
  ): Int {
    if (taskIds.isEmpty()) return 0
    val now = System.currentTimeMillis()
    val allTasks = dao.getTasksOnce()
    var count = 0
    for (task in allTasks) {
      if (task.id !in taskIds) continue
      var updated = task
      if (category != null) {
        val safeInterval = interval.coerceAtLeast(1)
        val newDue =
          if (category != TaskCategory.SIDEQUEST && updated.dueTimestamp == null) {
            nextCycleTimestamp(now, category, safeInterval)
          } else {
            updated.dueTimestamp
          }
        updated = updated.copy(
          categoryCode = category.code,
          repeatInterval = safeInterval,
          dueTimestamp = newDue,
        )
      }
      if (folderId != null) {
        updated = updated.copy(folderId = folderId)
      }
      if (reminder != null) {
        updated = updated.copy(reminderModeCode = reminder.code)
      }
      dao.updateTask(updated)
      count++
    }
    return count
  }

  suspend fun buyShopItem(item: ShopItemEntity): RewardOutcome {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    if (item.requiredAttrId > 0L) {
      val attr = dao.getAttributesOnce().find { it.id == item.requiredAttrId }
      if (attr != null && calculateLevel(attr.exp).level < item.requiredAttrLevel) {
        return RewardOutcome("Wymagany poziom ${item.requiredAttrLevel}: ${attr.name}")
      }
    }
    if (profile.coins < item.coinPrice) return RewardOutcome("Za mało monet")
    if (profile.gems < item.gemPrice) return RewardOutcome("Za mało kryształów")

    val period = PurchaseLimitPeriod.fromCode(item.limitPeriodCode)
    if (period != PurchaseLimitPeriod.UNLIMITED && countPurchasesInPeriod(item) >= item.limitMaxCount) {
      return RewardOutcome("Limit zakupów wyczerpany")
    }

    val allShopItems = dao.getShopItemsOnce()
    val itemValueScore = item.coinPrice + item.gemPrice * 100
    val maxShopValueScore = allShopItems.maxOfOrNull { it.coinPrice + it.gemPrice * 100 } ?: 0
    val isMostExpensive = maxShopValueScore > 0 && itemValueScore >= maxShopValueScore

    val now = System.currentTimeMillis()
    val history = parseIdCsv(item.purchaseHistoryCsv) + now
    dao.updateShopItem(
      item.copy(
        ownedQuantity = item.ownedQuantity + 1,
        unlocked = true,
        purchaseHistoryCsv = toIdCsv(history),
      )
    )

    val updatedProfile = profile.copy(
      coins = profile.coins - item.coinPrice,
      gems = profile.gems - item.gemPrice,
      itemsBought = profile.itemsBought + 1,
      boughtMostExpensive = profile.boughtMostExpensive || isMostExpensive,
    )
    val (finalProfile, newlyUnlocked) = evaluateAndGrantAchievements(updatedProfile)
    dao.upsertProfile(finalProfile)
    return RewardOutcome(
      toastMessage = "Kupiono: ${item.name}!",
      unlockedAchievements = newlyUnlocked,
    )
  }

  suspend fun useInventoryItem(item: ShopItemEntity): String {
    if (item.ownedQuantity <= 0) return ""
    dao.updateShopItem(item.copy(ownedQuantity = item.ownedQuantity - 1))
    return "Użyto: ${item.name}!"
  }

  fun countPurchasesInPeriod(item: ShopItemEntity, now: Long = System.currentTimeMillis()): Int {
    val period = PurchaseLimitPeriod.fromCode(item.limitPeriodCode)
    if (period == PurchaseLimitPeriod.UNLIMITED) return 0
    val timestamps = parseIdCsv(item.purchaseHistoryCsv)
    if (period == PurchaseLimitPeriod.TOTAL) return timestamps.size

    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val itemCal = Calendar.getInstance()
    return timestamps.count { ts ->
      itemCal.timeInMillis = ts
      when (period) {
        PurchaseLimitPeriod.DAILY ->
          itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        PurchaseLimitPeriod.WEEKLY -> now - ts < 7 * MILLIS_PER_DAY
        PurchaseLimitPeriod.MONTHLY ->
          itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            itemCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
        PurchaseLimitPeriod.TOTAL, PurchaseLimitPeriod.UNLIMITED -> true
      }
    }
  }

  suspend fun saveShopItem(item: ShopItemEntity): Long =
    if (item.id == 0L) dao.insertShopItem(item) else {
      dao.updateShopItem(item)
      item.id
    }

  suspend fun deleteShopItem(id: Long) = dao.deleteShopItemById(id)

  suspend fun addAttribute(icon: String, name: String) {
    dao.insertAttribute(AttributeEntity(icon = icon.ifBlank { "⭐" }, name = name.trim()))
  }

  suspend fun updateAttribute(attr: AttributeEntity) = dao.updateAttribute(attr)
  suspend fun deleteAttribute(id: Long) = dao.deleteAttributeById(id)

  suspend fun addFolder(icon: String, name: String) {
    dao.insertFolder(FolderEntity(icon = icon.ifBlank { "📁" }, name = name.trim()))
  }

  suspend fun updateFolder(folder: FolderEntity) = dao.updateFolder(folder)
  suspend fun deleteFolder(id: Long) = dao.deleteFolderById(id)

  suspend fun getOrCreateTag(rawName: String): TagEntity {
    val clean = rawName.trim().removePrefix("#")
    val existing = dao.getTagsOnce().find { it.name.equals(clean, ignoreCase = true) }
    if (existing != null) return existing
    val newId = dao.insertTag(TagEntity(name = clean))
    return TagEntity(id = newId, name = clean)
  }

  suspend fun updateTag(tag: TagEntity) = dao.updateTag(tag)
  suspend fun deleteTag(id: Long) = dao.deleteTagById(id)

  suspend fun setTheme(theme: AppThemeId) {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    dao.upsertProfile(profile.copy(themeCode = theme.code))
  }

  suspend fun setNotificationsEnabled(enabled: Boolean) {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    dao.upsertProfile(profile.copy(notificationsEnabled = enabled))
  }

  suspend fun checkDueReminders(): List<Pair<Long, String>> {
    val profile = dao.getProfileOnce() ?: return emptyList()
    if (!profile.notificationsEnabled) return emptyList()

    val now = System.currentTimeMillis()
    val dayBucket = now / MILLIS_PER_DAY
    val tasks = dao.getTasksOnce()
    val alerts = mutableListOf<Pair<Long, String>>()

    for (task in tasks) {
      if (task.startTimestamp > now) continue
      val due = task.dueTimestamp ?: continue
      val mode = ReminderMode.fromCode(task.reminderModeCode)
      if (mode == ReminderMode.NONE) continue

      val remaining = due - now
      val key = when (mode) {
        ReminderMode.NONE -> null
        ReminderMode.AT_DUE -> if (remaining <= 0L) "a_$due" else null
        ReminderMode.DAY_BEFORE -> if (remaining in 1L until MILLIS_PER_DAY) "b_$due" else null
        ReminderMode.THREE_DAYS_BEFORE ->
          if (remaining in 1L until 3 * MILLIS_PER_DAY) "c_$dayBucket" else null
        ReminderMode.DAILY_OVERDUE -> if (remaining < 0L) "o_$dayBucket" else null
        ReminderMode.THREE_DAYS_AND_OVERDUE -> when {
          remaining in 1L until 3 * MILLIS_PER_DAY -> "c_$dayBucket"
          remaining < 0L -> "o_$dayBucket"
          else -> null
        }
      }

      if (key != null && task.lastNotifiedKey != key) {
        dao.updateTask(task.copy(lastNotifiedKey = key))
        val text = if (remaining < 0L) {
          "Spóźnione (${formatDurationShort(remaining)}): ${task.name}"
        } else {
          "Termin za ${formatDurationShort(remaining)}: ${task.name}"
        }
        alerts.add(task.id to text)
      }
    }
    return alerts
  }

  suspend fun compressImageUriToDataUrl(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
      runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream)
        } ?: return@runCatching null
        val maxSide = 220f
        val scale = (maxSide / max(raw.width, raw.height)).coerceAtMost(1f)
        val width = max(1, (raw.width * scale).toInt())
        val height = max(1, (raw.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(raw, width, height, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 78, baos)
        if (scaled !== raw) raw.recycle()
        raw.recycle()
        "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
      }.getOrNull()
    }

  suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    val attrs = dao.getAttributesOnce()
    val folders = dao.getFoldersOnce()
    val tags = dao.getTagsOnce()
    val tasks = dao.getTasksOnce()
    val shop = dao.getShopItemsOnce()

    JSONObject().apply {
      put("version", 1)
      put(
        "profile",
        JSONObject().apply {
          put("exp", profile.exp)
          put("coins", profile.coins)
          put("gems", profile.gems)
          put("tasksDone", profile.tasksDone)
          put("pomosDone", profile.pomosDone)
          put("coinsEarned", profile.coinsEarned)
          put("itemsBought", profile.itemsBought)
          put("notificationsEnabled", profile.notificationsEnabled)
          put("themeCode", profile.themeCode)
          put("unlockedAchievementsCsv", profile.unlockedAchievementsCsv)
          put("currentStreak", profile.currentStreak)
          put("bestStreak", profile.bestStreak)
          put("lastTaskEpochDay", profile.lastTaskEpochDay)
          put("customTasksCreated", profile.customTasksCreated)
          put("boughtMostExpensive", profile.boughtMostExpensive)
          put("completedNightAction", profile.completedNightAction)
          put("completedSubtaskChain", profile.completedSubtaskChain)
        },
      )
      put(
        "attributes",
        JSONArray().apply {
          attrs.forEach { a ->
            put(JSONObject().apply {
              put("id", a.id)
              put("icon", a.icon)
              put("name", a.name)
              put("exp", a.exp)
            })
          }
        },
      )
      put(
        "folders",
        JSONArray().apply {
          folders.forEach { f ->
            put(JSONObject().apply {
              put("id", f.id)
              put("icon", f.icon)
              put("name", f.name)
            })
          }
        },
      )
      put(
        "tags",
        JSONArray().apply {
          tags.forEach { g ->
            put(JSONObject().apply {
              put("id", g.id)
              put("name", g.name)
            })
          }
        },
      )
      put(
        "tasks",
        JSONArray().apply {
          tasks.forEach { t ->
            put(JSONObject().apply {
              put("id", t.id)
              put("name", t.name)
              put("description", t.description)
              put("link", t.link)
              put("folderId", t.folderId)
              put("tagIdsCsv", t.tagIdsCsv)
              put("categoryCode", t.categoryCode)
              put("repeatInterval", t.repeatInterval)
              put("repeatLimit", t.repeatLimit)
              put("completedCount", t.completedCount)
              put("expReward", t.expReward)
              put("coinReward", t.coinReward)
              put("attributeIdsCsv", t.attributeIdsCsv)
              put("rewardItemId", t.rewardItemId)
              put("durationMinutes", t.durationMinutes)
              put("reminderModeCode", t.reminderModeCode)
              put("penaltyModeCode", t.penaltyModeCode)
              put("penaltyMinCoins", t.penaltyMinCoins)
              put("penaltyMaxCoins", t.penaltyMaxCoins)
              put("startTimestamp", t.startTimestamp)
              put("dueTimestamp", t.dueTimestamp ?: JSONObject.NULL)
              put("subtasksJson", t.subtasksJson)
              put("difficultyCode", t.difficultyCode)
              put("gemReward", t.gemReward)
            })
          }
        },
      )
      put(
        "shopItems",
        JSONArray().apply {
          shop.forEach { s ->
            put(JSONObject().apply {
              put("id", s.id)
              put("icon", s.icon)
              put("name", s.name)
              put("coinPrice", s.coinPrice)
              put("gemPrice", s.gemPrice)
              put("requiredAttrId", s.requiredAttrId)
              put("requiredAttrLevel", s.requiredAttrLevel)
              put("ownedQuantity", s.ownedQuantity)
              put("unlocked", s.unlocked)
              put("imageData", s.imageData)
              put("description", s.description)
              put("limitPeriodCode", s.limitPeriodCode)
              put("limitMaxCount", s.limitMaxCount)
              put("purchaseHistoryCsv", s.purchaseHistoryCsv)
            })
          }
        },
      )
    }.toString(2)
  }

  suspend fun importFromJson(rawJson: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
      val root = JSONObject(rawJson)
      val pObj = root.getJSONObject("profile")
      val profile = PlayerProfileEntity(
        id = 1L,
        exp = pObj.optInt("exp", 0),
        coins = pObj.optInt("coins", 0),
        gems = pObj.optInt("gems", 0),
        tasksDone = pObj.optInt("tasksDone", 0),
        pomosDone = pObj.optInt("pomosDone", 0),
        coinsEarned = pObj.optInt("coinsEarned", 0),
        itemsBought = pObj.optInt("itemsBought", 0),
        notificationsEnabled = pObj.optBoolean("notificationsEnabled", false),
        themeCode = pObj.optString("themeCode", "violet"),
        unlockedAchievementsCsv = pObj.optString("unlockedAchievementsCsv", ""),
        currentStreak = pObj.optInt("currentStreak", 0),
        bestStreak = pObj.optInt("bestStreak", 0),
        lastTaskEpochDay = pObj.optLong("lastTaskEpochDay", -1L),
        customTasksCreated = pObj.optInt("customTasksCreated", 0),
        boughtMostExpensive = pObj.optBoolean("boughtMostExpensive", false),
        completedNightAction = pObj.optBoolean("completedNightAction", false),
        completedSubtaskChain = pObj.optBoolean("completedSubtaskChain", false),
      )

      val attrsArr = root.getJSONArray("attributes")
      val attrs = (0 until attrsArr.length()).map { idx ->
        val o = attrsArr.getJSONObject(idx)
        AttributeEntity(
          id = o.getLong("id"),
          icon = o.optString("icon", "⭐"),
          name = o.getString("name"),
          exp = o.optInt("exp", 0),
        )
      }

      val foldersArr = root.getJSONArray("folders")
      val folders = (0 until foldersArr.length()).map { idx ->
        val o = foldersArr.getJSONObject(idx)
        FolderEntity(
          id = o.getLong("id"),
          icon = o.optString("icon", "📁"),
          name = o.getString("name"),
        )
      }

      val tagsArr = root.getJSONArray("tags")
      val tags = (0 until tagsArr.length()).map { idx ->
        val o = tagsArr.getJSONObject(idx)
        TagEntity(
          id = o.getLong("id"),
          name = o.getString("name"),
        )
      }

      val tasksArr = root.getJSONArray("tasks")
      val tasks = (0 until tasksArr.length()).map { idx ->
        val o = tasksArr.getJSONObject(idx)
        val due = if (o.isNull("dueTimestamp")) null else o.optLong("dueTimestamp")
        TaskEntity(
          id = o.getLong("id"),
          name = o.getString("name"),
          description = o.optString("description", ""),
          link = o.optString("link", ""),
          folderId = o.optLong("folderId", 0L),
          tagIdsCsv = o.optString("tagIdsCsv", ""),
          categoryCode = o.optString("categoryCode", "w"),
          repeatInterval = o.optInt("repeatInterval", 1),
          repeatLimit = o.optInt("repeatLimit", 0),
          completedCount = o.optInt("completedCount", 0),
          expReward = o.optInt("expReward", 20),
          coinReward = o.optInt("coinReward", 10),
          attributeIdsCsv = o.optString("attributeIdsCsv", ""),
          rewardItemId = o.optLong("rewardItemId", 0L),
          durationMinutes = o.optInt("durationMinutes", 0),
          reminderModeCode = o.optString("reminderModeCode", "d1"),
          penaltyModeCode = o.optString("penaltyModeCode", "0"),
          penaltyMinCoins = o.optInt("penaltyMinCoins", 5),
          penaltyMaxCoins = o.optInt("penaltyMaxCoins", 10),
          startTimestamp = o.optLong("startTimestamp", System.currentTimeMillis()),
          dueTimestamp = due,
          subtasksJson = o.optString("subtasksJson", "[]"),
          difficultyCode = o.optString("difficultyCode", "m"),
          gemReward = o.optInt("gemReward", 0),
        )
      }

      val shopArr = root.getJSONArray("shopItems")
      val shop = (0 until shopArr.length()).map { idx ->
        val o = shopArr.getJSONObject(idx)
        ShopItemEntity(
          id = o.getLong("id"),
          icon = o.optString("icon", "🎁"),
          name = o.getString("name"),
          coinPrice = o.optInt("coinPrice", 50),
          gemPrice = o.optInt("gemPrice", 0),
          requiredAttrId = o.optLong("requiredAttrId", 0L),
          requiredAttrLevel = o.optInt("requiredAttrLevel", 1),
          ownedQuantity = o.optInt("ownedQuantity", 0),
          unlocked = o.optBoolean("unlocked", false),
          imageData = o.optString("imageData", ""),
          description = o.optString("description", ""),
          limitPeriodCode = o.optString("limitPeriodCode", "0"),
          limitMaxCount = o.optInt("limitMaxCount", 1),
          purchaseHistoryCsv = o.optString("purchaseHistoryCsv", ""),
        )
      }

      dao.replaceAllData(profile, attrs, folders, tags, tasks, shop)
      true
    }.getOrDefault(false)
  }
}
