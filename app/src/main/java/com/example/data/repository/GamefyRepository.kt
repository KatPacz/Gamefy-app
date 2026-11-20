package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.local.ActivityEventEntity
import com.example.data.local.AttributeEntity
import com.example.data.local.FolderEntity
import com.example.data.local.GamefyDao
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.ShopItemEntity
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.SubtaskItem
import com.example.data.local.defaultColorHexForFolder
import com.example.data.local.normalizeColorHex
import com.example.data.local.parseIdCsv
import com.example.data.local.parseStringCsv
import com.example.data.local.parseSubtasksJson
import com.example.data.local.resolvedColorHex
import com.example.data.local.toIdCsv
import com.example.data.local.toStringCsv
import com.example.data.local.toSubtasksJson
import com.example.data.model.AchievementDefinition
import com.example.data.model.ActivityEventType
import com.example.data.model.AppThemeId
import com.example.data.model.GAMEFY_ACHIEVEMENTS
import com.example.data.model.MILLIS_PER_DAY
import com.example.data.model.MILLIS_PER_HOUR
import com.example.data.model.NotificationSettings
import com.example.data.model.NotificationTriggerMode
import com.example.data.model.OnboardingArea
import com.example.data.model.OnboardingDetailPref
import com.example.data.model.OnboardingRewardPref
import com.example.data.model.OnboardingTimePref
import com.example.data.model.PenaltyMode
import com.example.data.model.PlayerStatsSnapshot
import com.example.data.model.PurchaseLimitPeriod
import com.example.data.model.READY_TASK_TEMPLATES
import com.example.data.model.REWARD_CATALOG_TEMPLATES
import com.example.data.model.ReadyTaskTemplate
import com.example.data.model.ReminderMode
import com.example.data.model.RewardCatalogTemplate
import com.example.data.model.TaskCategory
import com.example.data.model.buildSummaryNotificationText
import com.example.data.model.calculateLevel
import com.example.data.model.distributeWeightedExp
import com.example.data.model.formatDurationShort
import com.example.data.model.isTaskPreferredTimeReached
import com.example.data.model.isWithinNotificationSchedule
import com.example.data.model.localEpochDay
import com.example.data.model.nextCycleTimestamp
import com.example.data.model.parseAttributeWeightsJson
import com.example.data.model.resolveGiveUpPenaltyRange
import com.example.data.model.tasksPlannedForDay
import com.example.data.model.toAttributeWeightsJson
import com.example.data.model.toNotificationSettings
import com.example.data.model.withNotificationSettings
import java.io.ByteArrayOutputStream
import java.util.Calendar
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RewardOutcome(
  val toastMessage: String = "",
  val unlockedAchievements: List<AchievementDefinition> = emptyList(),
)

class GamefyRepository(private val dao: GamefyDao) {
  private val completionMutex = Mutex()
  private val templateAddMutex = Mutex()

  val profile: Flow<PlayerProfileEntity> =
    dao.observeProfile().map { it ?: PlayerProfileEntity() }
  val attributes: Flow<List<AttributeEntity>> = dao.observeAttributes()
  val folders: Flow<List<FolderEntity>> = dao.observeFolders()
  val tags: Flow<List<TagEntity>> = dao.observeTags()
  val tasks: Flow<List<TaskEntity>> = dao.observeTasks()
  val shopItems: Flow<List<ShopItemEntity>> = dao.observeShopItems()
  val activityEvents: Flow<List<ActivityEventEntity>> = dao.observeActivityEvents()

  suspend fun completeTask(task: TaskEntity, fromSubtaskChain: Boolean = false): RewardOutcome =
    completionMutex.withLock {
      val now = System.currentTimeMillis()
      val currentTask = dao.getTaskByIdOnce(task.id) ?: return@withLock RewardOutcome()
      if (!currentTask.isActive || currentTask.startTimestamp > now) {
        return@withLock RewardOutcome()
      }

      val onTime = currentTask.dueTimestamp == null || now <= currentTask.dueTimestamp
      val category = TaskCategory.fromCode(currentTask.categoryCode)
      val newCount = currentTask.completedCount + 1
      val isFinished =
        category == TaskCategory.SIDEQUEST || (currentTask.repeatLimit > 0 && newCount >= currentTask.repeatLimit)

      var itemSuffix = ""
      var updatedShopItem: ShopItemEntity? = null
      if (currentTask.rewardItemId > 0L) {
        val shopItem = dao.getShopItemsOnce().find { it.id == currentTask.rewardItemId }
        if (shopItem != null) {
          updatedShopItem = shopItem.copy(ownedQuantity = shopItem.ownedQuantity + 1)
          itemSuffix = " 🎁 ${shopItem.name}"
        }
      }

      val taskIdToDelete = if (isFinished) currentTask.id else null
      val taskToUpdate = if (isFinished) {
        null
      } else {
        val (nextStart, nextDue) = advanceCycle(currentTask, now)
        val resetSubtasks = parseSubtasksJson(currentTask.subtasksJson).map { it.copy(isDone = false) }
        currentTask.copy(
          completedCount = newCount,
          startTimestamp = nextStart,
          dueTimestamp = nextDue,
          penaltyDueSnapshot = null,
          penaltyCountApplied = 0,
          lastNotifiedKey = "",
          subtasksJson = toSubtasksJson(resetSubtasks),
        )
      }

      val taskEvent = ActivityEventEntity(
        timestamp = now,
        eventType = ActivityEventType.TASK_DONE,
        taskId = currentTask.id,
        taskName = currentTask.name,
        categoryCode = currentTask.categoryCode,
        onTime = onTime,
        coinsDelta = currentTask.coinReward,
        gemsDelta = currentTask.gemReward,
        expDelta = currentTask.expReward,
        durationMinutes = currentTask.durationMinutes,
      )

      val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
      val oldLevel = calculateLevel(profile.exp).level
      val allAttrs = dao.getAttributesOnce()
      val weightsByName = parseAttributeWeightsJson(currentTask.attributeWeightsJson)
      val attrsToUpdate = mutableListOf<AttributeEntity>()
      var gemsGained = currentTask.gemReward.coerceAtLeast(0)

      if (currentTask.expReward > 0) {
        if (weightsByName.isNotEmpty()) {
          val attributeIds = parseIdCsv(currentTask.attributeIdsCsv)
          val resolvedWeights = linkedMapOf<AttributeEntity, Int>()
          val unmatchedWeightEntries = mutableListOf<Pair<String, Int>>()
          for ((attrName, weight) in weightsByName) {
            val byName = allAttrs.find { it.name.equals(attrName, ignoreCase = true) }
            if (byName != null) {
              resolvedWeights[byName] = (resolvedWeights[byName] ?: 0) + weight
            } else {
              unmatchedWeightEntries.add(attrName to weight)
            }
          }
          if (unmatchedWeightEntries.isNotEmpty() && attributeIds.isNotEmpty()) {
            val matchedIds = resolvedWeights.keys.map { it.id }.toSet()
            val remainingAttrs = attributeIds
              .filter { it !in matchedIds }
              .mapNotNull { id -> allAttrs.find { it.id == id } }
            unmatchedWeightEntries.zip(remainingAttrs).forEach { (entry, attr) ->
              val weight = entry.second
              resolvedWeights[attr] = (resolvedWeights[attr] ?: 0) + weight
            }
          }
          val weightsByIdKey = resolvedWeights.entries.associate { (attr, w) -> attr.id.toString() to w }
          val weightedExp = distributeWeightedExp(currentTask.expReward, weightsByIdKey)
          for ((attrIdStr, share) in weightedExp) {
            val attrId = attrIdStr.toLongOrNull() ?: continue
            val attr = allAttrs.find { it.id == attrId } ?: continue
            val prevAttrLevel = calculateLevel(attr.exp).level
            val newAttrExp = attr.exp + share
            val nextAttrLevel = calculateLevel(newAttrExp).level
            if (nextAttrLevel > prevAttrLevel) {
              gemsGained += (nextAttrLevel - prevAttrLevel)
            }
            attrsToUpdate.add(attr.copy(exp = newAttrExp))
          }
        } else {
          val attributeIds = parseIdCsv(currentTask.attributeIdsCsv)
          for (attrId in attributeIds) {
            val attr = allAttrs.find { it.id == attrId } ?: continue
            val prevAttrLevel = calculateLevel(attr.exp).level
            val newAttrExp = attr.exp + currentTask.expReward
            val nextAttrLevel = calculateLevel(newAttrExp).level
            if (nextAttrLevel > prevAttrLevel) {
              gemsGained += (nextAttrLevel - prevAttrLevel)
            }
            attrsToUpdate.add(attr.copy(exp = newAttrExp))
          }
        }
      }

      val today = localEpochDay(now)
      val newStreak = when (profile.lastTaskEpochDay) {
        today -> max(1, profile.currentStreak)
        today - 1L -> profile.currentStreak + 1
        else -> 1
      }
      val newBestStreak = max(profile.bestStreak, newStreak)

      val updatedProfile = profile.copy(
        exp = profile.exp + currentTask.expReward,
        coins = profile.coins + currentTask.coinReward,
        gems = profile.gems + gemsGained,
        tasksDone = profile.tasksDone + 1,
        coinsEarned = profile.coinsEarned + currentTask.coinReward,
        currentStreak = newStreak,
        bestStreak = newBestStreak,
        lastTaskEpochDay = today,
        completedNightAction = profile.completedNightAction || isNightActionNow(),
        completedSubtaskChain = profile.completedSubtaskChain || fromSubtaskChain,
      )

      val (finalProfile, newlyUnlocked) = evaluateAndGrantAchievements(updatedProfile)
      val achEvents = newlyUnlocked.mapNotNull { ach ->
        if (ach.rewardCoins > 0 || ach.rewardGems > 0 || ach.rewardExp > 0) {
          ActivityEventEntity(
            timestamp = now,
            eventType = ActivityEventType.ACH_REWARD,
            taskName = ach.title,
            coinsDelta = ach.rewardCoins,
            gemsDelta = ach.rewardGems,
            expDelta = ach.rewardExp,
          )
        } else null
      }

      dao.applyTaskCompletionAtomically(
        taskIdToDelete = taskIdToDelete,
        taskToUpdate = taskToUpdate,
        shopItemToUpdate = updatedShopItem,
        activityEvent = taskEvent,
        attributesToUpdate = attrsToUpdate,
        updatedProfile = finalProfile,
        achievementEvents = achEvents,
      )

      val prefix = ((if (isFinished) "✔ Zakończono! " else "") + itemSuffix.trimStart()).trim()
      val newLevel = calculateLevel(finalProfile.exp).level
      val parts = mutableListOf<String>()
      if (newLevel > oldLevel) parts.add("🎉 Poziom $newLevel!")
      if (prefix.isNotEmpty()) parts.add(prefix)
      parts.add("+${currentTask.expReward} EXP  +${currentTask.coinReward} 🪙")
      if (gemsGained > 0) parts.add("+$gemsGained 💎")
      RewardOutcome(
        toastMessage = parts.joinToString("  "),
        unlockedAchievements = newlyUnlocked,
      )
    }

  suspend fun toggleSubtask(task: TaskEntity, subtaskId: Long): RewardOutcome? {
    if (!task.isActive) return null
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
    if (!task.isActive) return ""
    val now = System.currentTimeMillis()
    val penaltyMode = PenaltyMode.fromCode(task.penaltyModeCode)
    var message = "Pominięto zadanie."

    var appliedSkipPenalty = 0
    if (penaltyMode == PenaltyMode.ON_SKIP_ONLY) {
      val penalty = computePenaltyAmount(task.penaltyMinCoins, task.penaltyMaxCoins)
      if (penalty > 0) {
        appliedSkipPenalty = penalty
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(profile.copy(coins = (profile.coins - penalty).coerceAtLeast(0)))
        message = "Pominięto. Kara −$penalty 🪙"
      }
    }

    dao.insertActivityEvent(
      ActivityEventEntity(
        timestamp = now,
        eventType = ActivityEventType.TASK_SKIP,
        taskId = task.id,
        taskName = task.name,
        categoryCode = task.categoryCode,
        onTime = false,
        coinsDelta = -appliedSkipPenalty,
      )
    )

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

  suspend fun giveUpTask(task: TaskEntity): String {
    if (!task.isActive) return ""
    val now = System.currentTimeMillis()
    val (minP, maxP) = resolveGiveUpPenaltyRange(
      coinReward = task.coinReward,
      difficultyCode = task.difficultyCode,
      penaltyModeCode = task.penaltyModeCode,
      penaltyMinCoins = task.penaltyMinCoins,
      penaltyMaxCoins = task.penaltyMaxCoins,
    )
    val penalty = computePenaltyAmount(minP, maxP)
    if (penalty > 0) {
      val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
      dao.upsertProfile(profile.copy(coins = (profile.coins - penalty).coerceAtLeast(0)))
    }

    dao.insertActivityEvent(
      ActivityEventEntity(
        timestamp = now,
        eventType = ActivityEventType.TASK_SKIP,
        taskId = task.id,
        taskName = task.name,
        categoryCode = task.categoryCode,
        onTime = false,
        coinsDelta = -penalty,
      )
    )

    val category = TaskCategory.fromCode(task.categoryCode)
    return if (category == TaskCategory.SIDEQUEST) {
      dao.deleteTaskById(task.id)
      if (penalty > 0) "Poddano zadanie. Kara −$penalty 🪙" else "Poddano zadanie."
    } else {
      val nextDue = nextCycleTimestamp(now, category, task.repeatInterval)
      val resetSubtasks = parseSubtasksJson(task.subtasksJson).map { it.copy(isDone = false) }
      dao.updateTask(
        task.copy(
          startTimestamp = now,
          dueTimestamp = nextDue,
          penaltyDueSnapshot = null,
          penaltyCountApplied = 0,
          lastNotifiedKey = "",
          subtasksJson = toSubtasksJson(resetSubtasks),
        )
      )
      if (penalty > 0) {
        "Poddano zadanie. Kara −$penalty 🪙 · Nowy czas do terminu"
      } else {
        "Poddano zadanie · Nowy czas do terminu"
      }
    }
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
    val now = System.currentTimeMillis()
    val minutes = max(1, totalSeconds / 60)
    val expGain = minutes
    val coinGain = max(1, totalSeconds / 300)
    dao.insertActivityEvent(
      ActivityEventEntity(
        timestamp = now,
        eventType = ActivityEventType.FOCUS_DONE,
        coinsDelta = coinGain,
        expDelta = expGain,
        durationMinutes = minutes,
      )
    )
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
    logAchievementRewards(newlyUnlocked)

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
      if (!task.isActive) continue
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

      var taskPenalty = 0
      var changed = snapshot != task.penaltyDueSnapshot || applied != task.penaltyCountApplied
      while (checkBoundary <= now && applied < 99) {
        val delta = computePenaltyAmount(task.penaltyMinCoins, task.penaltyMaxCoins)
        taskPenalty += delta
        totalPenalty += delta
        applied++
        changed = true
        if (mode == PenaltyMode.ON_DUE_ONCE || category == TaskCategory.SIDEQUEST) break
        checkBoundary = nextCycleTimestamp(checkBoundary, category, task.repeatInterval)
      }

      if (taskPenalty > 0) {
        dao.insertActivityEvent(
          ActivityEventEntity(
            timestamp = now,
            eventType = ActivityEventType.PENALTY,
            taskId = task.id,
            taskName = task.name,
            categoryCode = task.categoryCode,
            onTime = false,
            coinsDelta = -taskPenalty,
          )
        )
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

  private suspend fun logAchievementRewards(
    unlocked: List<AchievementDefinition>,
    timestamp: Long = System.currentTimeMillis(),
  ) {
    unlocked.forEach { ach ->
      if (ach.rewardCoins > 0 || ach.rewardGems > 0 || ach.rewardExp > 0) {
        dao.insertActivityEvent(
          ActivityEventEntity(
            timestamp = timestamp,
            eventType = ActivityEventType.ACH_REWARD,
            taskName = ach.title,
            coinsDelta = ach.rewardCoins,
            gemsDelta = ach.rewardGems,
            expDelta = ach.rewardExp,
          )
        )
      }
    }
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
      logAchievementRewards(unlocked)
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

    dao.insertActivityEvent(
      ActivityEventEntity(
        timestamp = now,
        eventType = ActivityEventType.SHOP_BUY,
        coinsDelta = -item.coinPrice,
        gemsDelta = -item.gemPrice,
        itemId = item.id,
        itemName = item.name,
        itemIcon = item.icon,
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
    logAchievementRewards(newlyUnlocked, now)
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

  suspend fun addFolder(icon: String, name: String, colorHex: String = "") {
    val cleanName = name.trim()
    val resolvedHex = normalizeColorHex(colorHex).ifEmpty { defaultColorHexForFolder(cleanName) }
    dao.insertFolder(
      FolderEntity(
        icon = icon.ifBlank { "📁" },
        name = cleanName,
        colorHex = resolvedHex,
      )
    )
  }

  suspend fun updateFolder(folder: FolderEntity) {
    val resolvedHex = normalizeColorHex(folder.colorHex).ifEmpty {
      defaultColorHexForFolder(folder.name, folder.id)
    }
    dao.updateFolder(folder.copy(colorHex = resolvedHex))
  }
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

  suspend fun updateNotificationSettings(settings: NotificationSettings) {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    dao.upsertProfile(profile.withNotificationSettings(settings))
  }

  // --- Onboarding & Ready Tasks/Rewards Catalog ---

  suspend fun completeOnboardingStartFromScratch() {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    dao.upsertProfile(profile.copy(onboardingCompleted = true))
  }

  suspend fun completeOnboardingFromSurvey(
    areas: List<OnboardingArea>?,
    timePref: OnboardingTimePref?,
    detailPref: OnboardingDetailPref?,
    rewardPrefs: List<OnboardingRewardPref>?,
    selectedTemplateIds: List<String>,
    selectedRewardTemplateIds: List<String>,
  ): Pair<Int, Int> {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    dao.upsertProfile(
      profile.copy(
        onboardingCompleted = true,
        onboardingInterestsCsv = areas?.takeIf { it.isNotEmpty() }?.joinToString(",") { it.id },
        onboardingTimePref = timePref?.id,
        onboardingDetailPref = detailPref?.id,
        onboardingRewardPrefsCsv = rewardPrefs?.takeIf { it.isNotEmpty() }?.joinToString(",") { it.id },
      )
    )

    var addedTasks = 0
    for (tplId in selectedTemplateIds.distinct()) {
      val tpl = READY_TASK_TEMPLATES.find { it.id == tplId } ?: continue
      if (addTaskFromTemplate(tpl, asInactive = true)) {
        addedTasks++
      }
    }

    var addedRewards = 0
    for (rewId in selectedRewardTemplateIds.distinct()) {
      val tpl = REWARD_CATALOG_TEMPLATES.find { it.id == rewId } ?: continue
      if (addRewardFromCatalog(tpl)) {
        addedRewards++
      }
    }

    return addedTasks to addedRewards
  }

  suspend fun setOnboardingDetailPref(detailPref: OnboardingDetailPref?) {
    val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
    dao.upsertProfile(profile.copy(onboardingDetailPref = detailPref?.id))
  }

  suspend fun addTaskFromTemplate(
    template: ReadyTaskTemplate,
    asInactive: Boolean = true,
  ): Boolean = templateAddMutex.withLock {
    if (dao.getTaskBySourceTemplateId(template.id) != null) return@withLock false

    val folders = dao.getFoldersOnce()
    val defaultFolderColor = defaultColorHexForFolder(template.area.folderName)
    val folder = folders.find { it.name.equals(template.area.folderName, ignoreCase = true) }
      ?: FolderEntity(
        id = dao.insertFolder(
          FolderEntity(
            icon = template.area.icon,
            name = template.area.folderName,
            colorHex = defaultFolderColor,
          )
        ),
        icon = template.area.icon,
        name = template.area.folderName,
        colorHex = defaultFolderColor,
      )

    val tagIds = template.tags.map { rawTag -> getOrCreateTag(rawTag).id }
    val allAttrs = dao.getAttributesOnce()
    val linkedAttrIds = template.attributeWeights.keys.mapNotNull { attrName ->
      allAttrs.find { it.name.equals(attrName, ignoreCase = true) }?.id
    }
    val subtasks = template.subtasks.mapIndexed { idx, title ->
      SubtaskItem(id = idx + 1L, title = title, isDone = false)
    }

    val now = System.currentTimeMillis()
    val due = if (asInactive || template.suggestedCategory == TaskCategory.SIDEQUEST) {
      null
    } else {
      nextCycleTimestamp(now, template.suggestedCategory, template.suggestedInterval)
    }

    dao.insertTask(
      TaskEntity(
        name = template.name,
        description = template.description,
        folderId = folder.id,
        tagIdsCsv = toIdCsv(tagIds),
        categoryCode = template.suggestedCategory.code,
        repeatInterval = template.suggestedInterval.coerceAtLeast(1),
        expReward = template.exp,
        coinReward = template.coins,
        gemReward = 0,
        difficultyCode = template.taskDifficulty.code,
        attributeIdsCsv = toIdCsv(linkedAttrIds),
        durationMinutes = template.estimatedMinutes,
        reminderModeCode = ReminderMode.NONE.code,
        penaltyModeCode = PenaltyMode.NONE.code,
        startTimestamp = now,
        dueTimestamp = due,
        subtasksJson = toSubtasksJson(subtasks),
        isActive = !asInactive,
        sourceTemplateId = template.id,
        attributeWeightsJson = toAttributeWeightsJson(template.attributeWeights),
      )
    )
    true
  }

  suspend fun activateLibraryTask(taskId: Long): Boolean {
    val task = dao.getTaskByIdOnce(taskId) ?: return false
    if (task.isActive) return false
    val now = System.currentTimeMillis()
    val category = TaskCategory.fromCode(task.categoryCode)
    val nextDue = if (category == TaskCategory.SIDEQUEST) {
      task.dueTimestamp
    } else {
      task.dueTimestamp ?: nextCycleTimestamp(now, category, task.repeatInterval)
    }
    dao.updateTask(
      task.copy(
        isActive = true,
        startTimestamp = now,
        dueTimestamp = nextDue,
      )
    )
    return true
  }

  suspend fun addRewardFromCatalog(
    template: RewardCatalogTemplate,
    customCoinPrice: Int = template.defaultCoinPrice,
  ): Boolean = templateAddMutex.withLock {
    if (dao.getShopItemBySourceTemplateId(template.id) != null) return@withLock false
    dao.insertShopItem(
      ShopItemEntity(
        icon = template.icon,
        name = template.name,
        coinPrice = customCoinPrice.coerceAtLeast(0),
        gemPrice = 0,
        ownedQuantity = 0,
        unlocked = false,
        description = template.description,
        sourceRewardTemplateId = template.id,
      )
    )
    true
  }

  suspend fun checkDueReminders(nowMillis: Long = System.currentTimeMillis()): List<Pair<Long, String>> {
    val profile = dao.getProfileOnce() ?: return emptyList()
    if (!profile.notificationsEnabled) return emptyList()

    val settings = profile.toNotificationSettings()
    val todayEpoch = localEpochDay(nowMillis)
    val sentToday = if (profile.notifSentEpochDay == todayEpoch) profile.notifSentCountToday else 0
    val maxPerDay = settings.maxPerDay
    if (maxPerDay > 0 && sentToday >= maxPerDay) return emptyList()
    val remainingQuota = if (maxPerDay > 0) maxPerDay - sentToday else Int.MAX_VALUE

    val dayBucket = todayEpoch
    val tasks = dao.getTasksOnce().filter { it.isActive }
    val candidateAlerts = mutableListOf<Triple<TaskEntity, String, String>>()

    if (!settings.useTaskSettings) {
      // Forced global mode: enforce global schedule and ignore per-task reminder & preferred hour
      if (!isWithinNotificationSchedule(settings, nowMillis)) return emptyList()

      val plannedTodayIds = tasksPlannedForDay(tasks, nowMillis).map { it.id }.toSet()
      for (task in tasks) {
        if (task.startTimestamp > nowMillis) continue
        val due = task.dueTimestamp
        val remaining = if (due != null) due - nowMillis else Long.MAX_VALUE
        val isEndingSoon = due != null && remaining in -MILLIS_PER_DAY..(2 * MILLIS_PER_HOUR)
        val isPlannedToday = task.id in plannedTodayIds

        val endingKey = if (isEndingSoon) "g_end_${dayBucket}_$due" else null
        val globalReminderKey = if (isPlannedToday) "g_rem_$dayBucket" else null
        val chosenKey = when (settings.triggerMode) {
          NotificationTriggerMode.WITH_REMINDER -> globalReminderKey
          NotificationTriggerMode.ONLY_FINISHING -> endingKey
          NotificationTriggerMode.BOTH -> endingKey ?: globalReminderKey
        }

        if (chosenKey != null && task.lastNotifiedKey != chosenKey) {
          val text = when {
            due != null && remaining < 0L -> "Spóźnione (${formatDurationShort(remaining)}): ${task.name}"
            due != null -> "Termin za ${formatDurationShort(remaining)}: ${task.name}"
            else -> "Zadanie na dziś: ${task.name}"
          }
          candidateAlerts.add(Triple(task, chosenKey, text))
        }
      }
    } else {
      // Per-task mode (default): each task uses its own reminderModeCode & preferredReminderHour/Minute
      for (task in tasks) {
        if (task.startTimestamp > nowMillis) continue
        val mode = ReminderMode.fromCode(task.reminderModeCode)
        if (mode == ReminderMode.NONE) continue
        if (!isTaskPreferredTimeReached(task, nowMillis)) continue

        val due = task.dueTimestamp
        val prefSuffix = "${task.preferredReminderHour}_${task.preferredReminderMinute}"

        if (due == null) {
          if (task.preferredReminderHour >= 0) {
            val chosenKey = "t_pref_${dayBucket}_$prefSuffix"
            if (task.lastNotifiedKey != chosenKey) {
              candidateAlerts.add(Triple(task, chosenKey, "Przypomnienie: ${task.name}"))
            }
          }
          continue
        }

        val remaining = due - nowMillis
        val dueEpoch = localEpochDay(due)
        val hasPref = task.preferredReminderHour >= 0

        val chosenKey = when (mode) {
          ReminderMode.NONE -> null
          ReminderMode.AT_DUE ->
            if (remaining <= 0L || (hasPref && dueEpoch == dayBucket)) "a_${due}_$prefSuffix" else null
          ReminderMode.DAY_BEFORE ->
            if (remaining in 1L until MILLIS_PER_DAY || (hasPref && dueEpoch == dayBucket + 1L)) {
              "b_${due}_$prefSuffix"
            } else null
          ReminderMode.THREE_DAYS_BEFORE ->
            if (remaining in 1L until 3 * MILLIS_PER_DAY || (hasPref && dueEpoch in (dayBucket + 1L)..(dayBucket + 3L))) {
              "c_${dayBucket}_$prefSuffix"
            } else null
          ReminderMode.DAILY_OVERDUE ->
            if (remaining < 0L) "o_${dayBucket}_$prefSuffix" else null
          ReminderMode.THREE_DAYS_AND_OVERDUE -> when {
            remaining in 1L until 3 * MILLIS_PER_DAY || (hasPref && dueEpoch in (dayBucket + 1L)..(dayBucket + 3L)) ->
              "c_${dayBucket}_$prefSuffix"
            remaining < 0L -> "o_${dayBucket}_$prefSuffix"
            else -> null
          }
        }

        if (chosenKey != null && task.lastNotifiedKey != chosenKey) {
          val text = if (remaining < 0L) {
            "Spóźnione (${formatDurationShort(remaining)}): ${task.name}"
          } else {
            "Termin za ${formatDurationShort(remaining)}: ${task.name}"
          }
          candidateAlerts.add(Triple(task, chosenKey, text))
        }
      }
    }

    val finalAlerts = mutableListOf<Pair<Long, String>>()
    if (maxPerDay > 0 && (candidateAlerts.size > remainingQuota || (maxPerDay == 1 && candidateAlerts.isNotEmpty()))) {
      val plannedToday = tasksPlannedForDay(tasks, nowMillis)
      val summaryTarget = if (plannedToday.isNotEmpty()) plannedToday else candidateAlerts.map { it.first }
      val summaryMsg = buildSummaryNotificationText(summaryTarget, settings.summaryStyle)
      val leadTaskId = candidateAlerts.first().first.id
      candidateAlerts.forEach { (task, key, _) ->
        dao.updateTask(task.copy(lastNotifiedKey = key))
      }
      finalAlerts.add(leadTaskId to summaryMsg)
    } else if (!settings.useTaskSettings && maxPerDay == 1 && candidateAlerts.isEmpty() && sentToday == 0) {
      if (settings.triggerMode != NotificationTriggerMode.ONLY_FINISHING) {
        val plannedToday = tasksPlannedForDay(tasks, nowMillis)
        if (plannedToday.isNotEmpty()) {
          val summaryMsg = buildSummaryNotificationText(plannedToday, settings.summaryStyle)
          finalAlerts.add(plannedToday.first().id to summaryMsg)
        }
      }
    } else {
      val allowed = candidateAlerts.take(remainingQuota)
      allowed.forEach { (task, key, msg) ->
        dao.updateTask(task.copy(lastNotifiedKey = key))
        finalAlerts.add(task.id to msg)
      }
    }

    if (finalAlerts.isNotEmpty()) {
      dao.upsertProfile(
        profile.copy(
          notifSentCountToday = sentToday + finalAlerts.size,
          notifSentEpochDay = todayEpoch,
        )
      )
    }
    return finalAlerts
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
    val events = dao.getActivityEventsOnce()

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
          put("notifDaysCsv", profile.notifDaysCsv)
          put("notifSlotsCsv", profile.notifSlotsCsv)
          put("notifCustomStartHour", profile.notifCustomStartHour)
          put("notifCustomStartMinute", profile.notifCustomStartMinute)
          put("notifCustomEndHour", profile.notifCustomEndHour)
          put("notifCustomEndMinute", profile.notifCustomEndMinute)
          put("notifTriggerModeCode", profile.notifTriggerModeCode)
          put("notifMaxPerDay", profile.notifMaxPerDay)
          put("notifSummaryStyleCode", profile.notifSummaryStyleCode)
          put("notifUseTaskSettings", profile.notifUseTaskSettings)
          put("notifSentCountToday", profile.notifSentCountToday)
          put("notifSentEpochDay", profile.notifSentEpochDay)
          put("onboardingCompleted", profile.onboardingCompleted)
          put("onboardingInterestsCsv", profile.onboardingInterestsCsv ?: JSONObject.NULL)
          put("onboardingTimePref", profile.onboardingTimePref ?: JSONObject.NULL)
          put("onboardingDetailPref", profile.onboardingDetailPref ?: JSONObject.NULL)
          put("onboardingRewardPrefsCsv", profile.onboardingRewardPrefsCsv ?: JSONObject.NULL)
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
              put("colorHex", f.resolvedColorHex())
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
              put("preferredReminderHour", t.preferredReminderHour)
              put("preferredReminderMinute", t.preferredReminderMinute)
              put("isActive", t.isActive)
              put("sourceTemplateId", t.sourceTemplateId ?: JSONObject.NULL)
              put("attributeWeightsJson", t.attributeWeightsJson)
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
              put("sourceRewardTemplateId", s.sourceRewardTemplateId ?: JSONObject.NULL)
            })
          }
        },
      )
      put(
        "activityEvents",
        JSONArray().apply {
          events.forEach { e ->
            put(JSONObject().apply {
              put("id", e.id)
              put("timestamp", e.timestamp)
              put("eventType", e.eventType)
              put("taskId", e.taskId)
              put("taskName", e.taskName)
              put("categoryCode", e.categoryCode)
              put("onTime", e.onTime)
              put("coinsDelta", e.coinsDelta)
              put("gemsDelta", e.gemsDelta)
              put("expDelta", e.expDelta)
              put("durationMinutes", e.durationMinutes)
              put("itemId", e.itemId)
              put("itemName", e.itemName)
              put("itemIcon", e.itemIcon)
            })
          }
        },
      )
    }.toString(2)
  }

  suspend fun importFromJson(
    rawJson: String,
    markOnboardingCompleted: Boolean = false,
  ): Boolean = withContext(Dispatchers.IO) {
    runCatching {
      val root = JSONObject(rawJson)
      val pObj = root.getJSONObject("profile")
      val importedOnboardingDone = if (pObj.has("onboardingCompleted")) {
        pObj.optBoolean("onboardingCompleted", true)
      } else {
        true
      }
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
        notifDaysCsv = pObj.optString("notifDaysCsv", "1,2,3,4,5,6,7"),
        notifSlotsCsv = pObj.optString("notifSlotsCsv", "any"),
        notifCustomStartHour = pObj.optInt("notifCustomStartHour", 9),
        notifCustomStartMinute = pObj.optInt("notifCustomStartMinute", 0),
        notifCustomEndHour = pObj.optInt("notifCustomEndHour", 21),
        notifCustomEndMinute = pObj.optInt("notifCustomEndMinute", 0),
        notifTriggerModeCode = pObj.optString("notifTriggerModeCode", "reminder"),
        notifMaxPerDay = pObj.optInt("notifMaxPerDay", 0),
        notifSummaryStyleCode = pObj.optString("notifSummaryStyleCode", "digest"),
        notifUseTaskSettings = pObj.optBoolean("notifUseTaskSettings", true),
        notifSentCountToday = pObj.optInt("notifSentCountToday", 0),
        notifSentEpochDay = pObj.optLong("notifSentEpochDay", -1L),
        onboardingCompleted = markOnboardingCompleted || importedOnboardingDone,
        onboardingInterestsCsv = if (pObj.isNull("onboardingInterestsCsv")) null else pObj.optString("onboardingInterestsCsv").ifBlank { null },
        onboardingTimePref = if (pObj.isNull("onboardingTimePref")) null else pObj.optString("onboardingTimePref").ifBlank { null },
        onboardingDetailPref = if (pObj.isNull("onboardingDetailPref")) null else pObj.optString("onboardingDetailPref").ifBlank { null },
        onboardingRewardPrefsCsv = if (pObj.isNull("onboardingRewardPrefsCsv")) null else pObj.optString("onboardingRewardPrefsCsv").ifBlank { null },
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
        val id = o.getLong("id")
        val name = o.getString("name")
        val colorHex = normalizeColorHex(o.optString("colorHex", ""))
          .ifEmpty { defaultColorHexForFolder(name, id) }
        FolderEntity(
          id = id,
          icon = o.optString("icon", "📁"),
          name = name,
          colorHex = colorHex,
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
          preferredReminderHour = o.optInt("preferredReminderHour", -1),
          preferredReminderMinute = o.optInt("preferredReminderMinute", 0),
          isActive = o.optBoolean("isActive", true),
          sourceTemplateId = if (o.isNull("sourceTemplateId")) null else o.optString("sourceTemplateId").ifBlank { null },
          attributeWeightsJson = o.optString("attributeWeightsJson", ""),
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
          sourceRewardTemplateId = if (o.isNull("sourceRewardTemplateId")) null else o.optString("sourceRewardTemplateId").ifBlank { null },
        )
      }

      val eventsArr = root.optJSONArray("activityEvents")
      val events = if (eventsArr != null) {
        (0 until eventsArr.length()).map { idx ->
          val o = eventsArr.getJSONObject(idx)
          ActivityEventEntity(
            id = o.optLong("id", 0L),
            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
            eventType = o.optString("eventType", ""),
            taskId = o.optLong("taskId", 0L),
            taskName = o.optString("taskName", ""),
            categoryCode = o.optString("categoryCode", ""),
            onTime = o.optBoolean("onTime", true),
            coinsDelta = o.optInt("coinsDelta", 0),
            gemsDelta = o.optInt("gemsDelta", 0),
            expDelta = o.optInt("expDelta", 0),
            durationMinutes = o.optInt("durationMinutes", 0),
            itemId = o.optLong("itemId", 0L),
            itemName = o.optString("itemName", ""),
            itemIcon = o.optString("itemIcon", ""),
          )
        }
      } else {
        emptyList()
      }

      dao.replaceAllData(profile, attrs, folders, tags, tasks, shop, events)
      true
    }.getOrDefault(false)
  }
}
