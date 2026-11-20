package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AttributeEntity
import com.example.data.local.GamefyDatabase
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.ShopItemEntity
import com.example.data.local.SubtaskItem
import com.example.data.local.TaskEntity
import com.example.data.local.parseStringCsv
import com.example.data.local.parseSubtasksJson
import com.example.data.local.toSubtasksJson
import com.example.data.model.ActivityEventType
import com.example.data.model.MILLIS_PER_DAY
import com.example.data.model.NotificationSettings
import com.example.data.model.NotificationSummaryStyle
import com.example.data.model.NotificationTimeSlot
import com.example.data.model.NotificationTriggerMode
import com.example.data.model.OnboardingArea
import com.example.data.model.OnboardingDetailPref
import com.example.data.model.OnboardingRewardPref
import com.example.data.model.OnboardingTimePref
import com.example.data.model.READY_TASK_TEMPLATES
import com.example.data.model.REWARD_CATALOG_TEMPLATES
import com.example.data.model.ReminderMode
import com.example.data.model.localEpochDay
import com.example.data.model.toNotificationSettings
import com.example.data.repository.GamefyRepository
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GamefyRepositoryTest {

  private lateinit var db: GamefyDatabase
  private lateinit var repo: GamefyRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, GamefyDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repo = GamefyRepository(db.gamefyDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `early task completion hides task until due date and schedules next cycle`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L))
    val now = System.currentTimeMillis()
    val oldDue = now + 4 * MILLIS_PER_DAY
    val taskId = dao.insertTask(
      TaskEntity(
        name = "Tygodniowe",
        categoryCode = "w",
        repeatInterval = 1,
        expReward = 30,
        coinReward = 20,
        startTimestamp = now - 3 * MILLIS_PER_DAY,
        dueTimestamp = oldDue,
      )
    )

    val task = dao.getTasksOnce().first { it.id == taskId }
    repo.completeTask(task)

    val updated = dao.getTasksOnce().first { it.id == taskId }
    assertEquals(oldDue, updated.startTimestamp)
    assertTrue((updated.dueTimestamp ?: 0L) > oldDue)
    assertEquals(1, updated.completedCount)
  }

  @Test
  fun `overdue task keeps single entry and restarts cycle from now on completion`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L))
    val now = System.currentTimeMillis()
    val overdueDue = now - 3 * MILLIS_PER_DAY
    val taskId = dao.insertTask(
      TaskEntity(
        name = "Spóźnione",
        categoryCode = "w",
        repeatInterval = 1,
        expReward = 25,
        coinReward = 15,
        startTimestamp = now - 10 * MILLIS_PER_DAY,
        dueTimestamp = overdueDue,
      )
    )

    val task = dao.getTasksOnce().first { it.id == taskId }
    repo.completeTask(task)

    val tasks = dao.getTasksOnce()
    assertEquals(1, tasks.size)
    val updated = tasks.first()
    assertTrue(updated.startTimestamp >= now - 1000L)
    assertTrue((updated.dueTimestamp ?: 0L) >= now + 6 * MILLIS_PER_DAY)
  }

  @Test
  fun `attribute level up awards gem and unlocks level-gated shop item with daily limit`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, coins = 100))
    val attrId = dao.insertAttribute(AttributeEntity(icon = "💪", name = "Siła", exp = 90))
    val itemId = dao.insertShopItem(
      ShopItemEntity(
        icon = "☕",
        name = "Kawa",
        coinPrice = 30,
        requiredAttrId = attrId,
        requiredAttrLevel = 2,
        limitPeriodCode = "d",
        limitMaxCount = 1,
      )
    )
    val taskId = dao.insertTask(
      TaskEntity(
        name = "Trening",
        categoryCode = "o",
        expReward = 20,
        coinReward = 10,
        attributeIdsCsv = attrId.toString(),
      )
    )

    // First try buying before attribute level 2 -> blocked
    val blockedMsg = repo.buyShopItem(dao.getShopItemsOnce().first { it.id == itemId })
    assertTrue(blockedMsg.toastMessage.contains("Wymagany poziom 2"))

    // Complete task -> attribute reaches 110 EXP (Level 2) and grants 1 gem
    repo.completeTask(dao.getTasksOnce().first { it.id == taskId })
    val profileAfterTask = dao.getProfileOnce()!!
    assertEquals(1, profileAfterTask.gems)
    assertEquals(0, dao.getTasksOnce().size) // sidequest removed

    // Now buy item -> succeeds, second buy blocked by daily limit 1/1
    val okMsg = repo.buyShopItem(dao.getShopItemsOnce().first { it.id == itemId })
    assertTrue(okMsg.toastMessage.startsWith("Kupiono"))
    val limitMsg = repo.buyShopItem(dao.getShopItemsOnce().first { it.id == itemId })
    assertEquals("Limit zakupów wyczerpany", limitMsg.toastMessage)
  }

  @Test
  fun `json export and import preserves full local state`() = runBlocking {
    GamefyDatabase.ensureSeeded(db)
    db.gamefyDao().insertTask(
      TaskEntity(name = "Sprzątanie łazienki", categoryCode = "w", expReward = 30, coinReward = 20)
    )
    val json = repo.exportToJson()
    assertTrue(json.contains("Sprzątanie łazienki"))

    db.gamefyDao().clearTasks()
    assertEquals(0, db.gamefyDao().getTasksOnce().size)

    val imported = repo.importFromJson(json)
    assertTrue(imported)
    val restored = db.gamefyDao().getTasksOnce()
    assertNotNull(restored.find { it.name == "Sprzątanie łazienki" })
  }

  @Test
  fun `completing all subtasks automatically completes main task and resets subtasks for next cycle`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L))
    val now = System.currentTimeMillis()
    val taskId = dao.insertTask(
      TaskEntity(
        name = "Porządki",
        categoryCode = "w",
        repeatInterval = 1,
        expReward = 30,
        coinReward = 15,
        startTimestamp = now - MILLIS_PER_DAY,
        dueTimestamp = now + 5 * MILLIS_PER_DAY,
        subtasksJson = toSubtasksJson(
          listOf(
            SubtaskItem(1L, "Część 1", false),
            SubtaskItem(2L, "Część 2", false),
          )
        ),
      )
    )

    val r1 = repo.toggleSubtask(dao.getTasksOnce().first { it.id == taskId }, 1L)
    assertEquals(null, r1)
    assertEquals(0, dao.getProfileOnce()!!.tasksDone)

    val r2 = repo.toggleSubtask(dao.getTasksOnce().first { it.id == taskId }, 2L)
    assertNotNull(r2)
    assertEquals(1, dao.getProfileOnce()!!.tasksDone)
    assertTrue(r2!!.unlockedAchievements.any { it.id == "first_task" })
    assertTrue(r2.unlockedAchievements.any { it.id == "secret_subtasks" })

    val nextCycleTask = dao.getTasksOnce().first { it.id == taskId }
    assertEquals(1, nextCycleTask.completedCount)
    assertTrue(parseSubtasksJson(nextCycleTask.subtasksJson).none { it.isDone })
  }

  @Test
  fun `achievements award exp coins gems and secret triggers unlock cleanly`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, coins = 500, gems = 5))

    // 1. Secret: creating own custom task unlocks secret_create_task and grants bonus rewards
    val saveOutcome = repo.saveTask(
      TaskEntity(
        name = "Własne zadanie",
        categoryCode = "o",
        expReward = 20,
        coinReward = 10,
      )
    )
    assertTrue(saveOutcome.unlockedAchievements.any { it.id == "secret_create_task" })
    val profileAfterCreate = dao.getProfileOnce()!!
    assertEquals(50, profileAfterCreate.exp) // +50 EXP from secret_create_task
    assertEquals(530, profileAfterCreate.coins) // +30 coins from secret_create_task

    // 2. Secret: buying most expensive item unlocks first_buy + secret_most_expensive
    dao.insertShopItem(ShopItemEntity(icon = "☕", name = "Tanie", coinPrice = 20))
    val expensiveId = dao.insertShopItem(ShopItemEntity(icon = "💎", name = "Najdroższe", coinPrice = 200, gemPrice = 2))
    val expensiveItem = dao.getShopItemsOnce().first { it.id == expensiveId }

    val buyOutcome = repo.buyShopItem(expensiveItem)
    assertTrue(buyOutcome.unlockedAchievements.any { it.id == "first_buy" })
    assertTrue(buyOutcome.unlockedAchievements.any { it.id == "secret_most_expensive" })

    val unlocked = parseStringCsv(dao.getProfileOnce()!!.unlockedAchievementsCsv)
    assertTrue("secret_create_task" in unlocked)
    assertTrue("secret_most_expensive" in unlocked)
    assertTrue("first_buy" in unlocked)
  }

  @Test
  fun `100 day streak unlocks streak_100 achievement and grants grand reward`() = runTest {
    val dao = db.gamefyDao()
    val yesterday = localEpochDay() - 1L
    dao.upsertProfile(
      PlayerProfileEntity(
        id = 1L,
        currentStreak = 99,
        bestStreak = 99,
        lastTaskEpochDay = yesterday,
      )
    )
    val taskId = dao.insertTask(
      TaskEntity(
        name = "Zadanie dnia 100",
        categoryCode = "o",
        expReward = 25,
        coinReward = 10,
      )
    )

    val outcome = repo.completeTask(dao.getTasksOnce().first { it.id == taskId })
    assertTrue(outcome.unlockedAchievements.any { it.id == "streak_100" })
    val p = dao.getProfileOnce()!!
    assertEquals(100, p.currentStreak)
    assertEquals(100, p.bestStreak)
    assertTrue(p.exp >= 25 + 1500)
    assertTrue(p.coins >= 10 + 500)
    assertTrue(p.gems >= 10)
  }

  @Test
  fun `completing task with direct gemReward grants gems and coins`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, coins = 0, gems = 0))
    val taskId = dao.insertTask(
      TaskEntity(
        name = "Wyzwanie z brylantem",
        categoryCode = "o",
        difficultyCode = "x",
        expReward = 100,
        coinReward = 50,
        gemReward = 2,
      )
    )
    repo.completeTask(dao.getTasksOnce().first { it.id == taskId })
    val profile = dao.getProfileOnce()!!
    assertEquals(65, profile.coins) // 50 task + 15 first_task achievement
    assertEquals(2, profile.gems)
  }

  @Test
  fun `notification schedule blocks alerts outside configured days and time slots`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, notificationsEnabled = true))
    repo.updateNotificationSettings(
      NotificationSettings(
        useTaskSettings = false,
        daysOfWeek = setOf(1, 2, 3, 4, 5), // Mon-Fri only
        allowAnytime = false,
        timeSlots = setOf(NotificationTimeSlot.MORNING), // 08:00-11:00
      )
    )

    fun calAt(dayOfWeek: Int, hour: Int, minute: Int): Long =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.WEEK_OF_YEAR, 11)
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis

    val mondayMorning = calAt(Calendar.MONDAY, 9, 30)
    val mondayAfternoon = calAt(Calendar.MONDAY, 14, 30)
    val saturdayMorning = calAt(Calendar.SATURDAY, 9, 30)

    dao.insertTask(
      TaskEntity(
        name = "Poranne zadanie",
        categoryCode = "d",
        reminderModeCode = ReminderMode.DAY_BEFORE.code,
        startTimestamp = mondayMorning - 3600_000L,
        dueTimestamp = mondayMorning + 3600_000L,
      )
    )

    assertTrue(repo.checkDueReminders(mondayAfternoon).isEmpty())
    assertTrue(repo.checkDueReminders(saturdayMorning).isEmpty())
    val fired = repo.checkDueReminders(mondayMorning)
    assertEquals(1, fired.size)
  }

  @Test
  fun `only finishing trigger ignores distant reminders and fires near deadline`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, notificationsEnabled = true))
    repo.updateNotificationSettings(
      NotificationSettings(
        useTaskSettings = false,
        allowAnytime = true,
        triggerMode = NotificationTriggerMode.ONLY_FINISHING,
      )
    )
    val now = System.currentTimeMillis()
    dao.insertTask(
      TaskEntity(
        name = "Jeszcze dużo czasu",
        categoryCode = "o",
        reminderModeCode = ReminderMode.DAY_BEFORE.code,
        startTimestamp = now - 3600_000L,
        dueTimestamp = now + 20 * 3600_000L,
      )
    )
    dao.insertTask(
      TaskEntity(
        name = "Zaraz koniec",
        categoryCode = "o",
        reminderModeCode = ReminderMode.NONE.code,
        startTimestamp = now - 3600_000L,
        dueTimestamp = now + 3600_000L,
      )
    )

    val alerts = repo.checkDueReminders(now)
    assertEquals(1, alerts.size)
    assertTrue(alerts.first().second.contains("Zaraz koniec"))
  }

  @Test
  fun `daily notification cap consolidates multiple tasks into digest and blocks excess`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, notificationsEnabled = true))
    repo.updateNotificationSettings(
      NotificationSettings(
        allowAnytime = true,
        triggerMode = NotificationTriggerMode.WITH_REMINDER,
        maxPerDay = 1,
        summaryStyle = NotificationSummaryStyle.DETAILED_DIGEST,
      )
    )
    val now = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 10)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    dao.insertTask(
      TaskEntity(
        name = "Zadanie 1",
        categoryCode = "d",
        durationMinutes = 30,
        reminderModeCode = ReminderMode.DAY_BEFORE.code,
        startTimestamp = now - 3600_000L,
        dueTimestamp = now + 2 * 3600_000L,
      )
    )
    dao.insertTask(
      TaskEntity(
        name = "Zadanie 2",
        categoryCode = "d",
        durationMinutes = 45,
        reminderModeCode = ReminderMode.DAY_BEFORE.code,
        startTimestamp = now - 3600_000L,
        dueTimestamp = now + 3 * 3600_000L,
      )
    )

    val firstAlerts = repo.checkDueReminders(now)
    assertEquals(1, firstAlerts.size)
    assertTrue(firstAlerts.first().second.contains("2 zadania"))
    assertTrue(firstAlerts.first().second.contains("1 h 15 min"))

    // Second check on the same day is blocked by daily cap = 1
    val secondAlerts = repo.checkDueReminders(now + 60_000L)
    assertTrue(secondAlerts.isEmpty())

    // Backup round-trip preserves notification settings
    val json = repo.exportToJson()
    repo.updateNotificationSettings(NotificationSettings(maxPerDay = 0))
    assertTrue(repo.importFromJson(json))
    val restored = dao.getProfileOnce()!!.toNotificationSettings()
    assertEquals(1, restored.maxPerDay)
    assertEquals(NotificationSummaryStyle.DETAILED_DIGEST, restored.summaryStyle)
  }

  @Test
  fun `default per-task mode respects task preferred reminder hour and ignores NONE reminder`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, notificationsEnabled = true))
    assertTrue(dao.getProfileOnce()!!.notifUseTaskSettings)

    val morningTen = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.MARCH)
      set(Calendar.DAY_OF_MONTH, 10)
      set(Calendar.HOUR_OF_DAY, 10)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val afternoonTwoThirty = morningTen + 4 * 3600_000L + 30 * 60_000L // 14:30

    dao.insertTask(
      TaskEntity(
        name = "Wyłączone w zadaniu",
        categoryCode = "d",
        reminderModeCode = ReminderMode.NONE.code,
        preferredReminderHour = 9,
        preferredReminderMinute = 0,
        startTimestamp = morningTen - 3600_000L,
        dueTimestamp = morningTen + 10 * 3600_000L,
      )
    )
    dao.insertTask(
      TaskEntity(
        name = "O 14:30",
        categoryCode = "d",
        reminderModeCode = ReminderMode.DAY_BEFORE.code,
        preferredReminderHour = 14,
        preferredReminderMinute = 30,
        startTimestamp = morningTen - 3600_000L,
        dueTimestamp = morningTen + 12 * 3600_000L,
      )
    )

    // At 10:00 neither fires (first is NONE, second waits until 14:30)
    assertTrue(repo.checkDueReminders(morningTen).isEmpty())

    // At 14:30 the second task fires
    val fired = repo.checkDueReminders(afternoonTwoThirty)
    assertEquals(1, fired.size)
    assertTrue(fired.first().second.contains("O 14:30"))
  }

  @Test
  fun `unchecking useTaskSettings enforces global schedule and ignores task reminders`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, notificationsEnabled = true))
    repo.updateNotificationSettings(
      NotificationSettings(
        useTaskSettings = false, // user unchecked "Zależne od zadania"
        daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
        allowAnytime = false,
        timeSlots = setOf(NotificationTimeSlot.EVENING), // 19:00-22:00 only
        triggerMode = NotificationTriggerMode.WITH_REMINDER,
      )
    )
    assertFalse(dao.getProfileOnce()!!.notifUseTaskSettings)

    val morningEightThirty = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.MARCH)
      set(Calendar.DAY_OF_MONTH, 10)
      set(Calendar.HOUR_OF_DAY, 8)
      set(Calendar.MINUTE, 30)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val eveningEight = morningEightThirty + 11 * 3600_000L + 30 * 60_000L // 20:00

    // Task explicitly set to NONE and 08:00 — global forced mode ignores both!
    dao.insertTask(
      TaskEntity(
        name = "Ignoruj ustawienia zadania",
        categoryCode = "d",
        reminderModeCode = ReminderMode.NONE.code,
        preferredReminderHour = 8,
        preferredReminderMinute = 0,
        startTimestamp = morningEightThirty - 3600_000L,
        dueTimestamp = eveningEight + 2 * 3600_000L,
      )
    )

    // 08:30 blocked because global schedule requires EVENING (19:00-22:00)
    assertTrue(repo.checkDueReminders(morningEightThirty).isEmpty())

    // 20:00 fires even though task had ReminderMode.NONE!
    val eveningAlerts = repo.checkDueReminders(eveningEight)
    assertEquals(1, eveningAlerts.size)
    assertTrue(eveningAlerts.first().second.contains("Ignoruj ustawienia zadania"))

    // Export/import round-trip preserves preferredReminderHour/Minute and notifUseTaskSettings=false
    val json = repo.exportToJson()
    repo.updateNotificationSettings(NotificationSettings(useTaskSettings = true))
    assertTrue(repo.importFromJson(json))
    assertFalse(dao.getProfileOnce()!!.notifUseTaskSettings)
    val restoredTask = dao.getTasksOnce().first { it.name == "Ignoruj ustawienia zadania" }
    assertEquals(8, restoredTask.preferredReminderHour)
    assertEquals(0, restoredTask.preferredReminderMinute)
  }

  @Test
  fun `repository logs task shop focus activity events and preserves them in json backup`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, coins = 200))
    val now = System.currentTimeMillis()
    val onTimeTaskId = dao.insertTask(
      TaskEntity(
        name = "Na czas",
        categoryCode = "d",
        coinReward = 25,
        expReward = 20,
        startTimestamp = now - 3600_000L,
        dueTimestamp = now + 3600_000L,
      )
    )
    val lateTaskId = dao.insertTask(
      TaskEntity(
        name = "Spóźnione",
        categoryCode = "w",
        coinReward = 15,
        expReward = 15,
        startTimestamp = now - 5 * MILLIS_PER_DAY,
        dueTimestamp = now - 3600_000L,
      )
    )
    val itemId = dao.insertShopItem(ShopItemEntity(icon = "☕", name = "Kawa", coinPrice = 30))

    repo.completeTask(dao.getTasksOnce().first { it.id == onTimeTaskId })
    repo.completeTask(dao.getTasksOnce().first { it.id == lateTaskId })
    repo.completeFocusSession(25 * 60)
    repo.buyShopItem(dao.getShopItemsOnce().first { it.id == itemId })

    val events = dao.getActivityEventsOnce()
    val doneEvents = events.filter { it.eventType == ActivityEventType.TASK_DONE }
    assertEquals(2, doneEvents.size)
    assertTrue(doneEvents.any { it.taskName == "Na czas" && it.onTime })
    assertTrue(doneEvents.any { it.taskName == "Spóźnione" && !it.onTime })
    assertTrue(events.any { it.eventType == ActivityEventType.FOCUS_DONE && it.durationMinutes == 25 })
    assertTrue(events.any { it.eventType == ActivityEventType.SHOP_BUY && it.itemName == "Kawa" && it.coinsDelta == -30 })

    val json = repo.exportToJson()
    dao.clearActivityEvents()
    assertTrue(dao.getActivityEventsOnce().isEmpty())
    assertTrue(repo.importFromJson(json))
    assertEquals(events.size, dao.getActivityEventsOnce().size)
  }

  @Test
  fun `fresh install starts empty and start from scratch marks onboarding complete without adding tasks or rewards`() = runTest {
    GamefyDatabase.ensureSeeded(db)
    val dao = db.gamefyDao()

    assertFalse(dao.getProfileOnce()!!.onboardingCompleted)
    assertTrue(dao.getTasksOnce().isEmpty())
    assertTrue(dao.getShopItemsOnce().isEmpty())
    assertEquals(5, dao.getAttributesOnce().size)

    repo.completeOnboardingStartFromScratch()

    val profile = dao.getProfileOnce()!!
    assertTrue(profile.onboardingCompleted)
    assertEquals(null, profile.onboardingInterestsCsv)
    assertEquals(null, profile.onboardingTimePref)
    assertEquals(null, profile.onboardingDetailPref)
    assertEquals(null, profile.onboardingRewardPrefsCsv)
    assertTrue(dao.getTasksOnce().isEmpty())
    assertTrue(dao.getShopItemsOnce().isEmpty())
  }

  @Test
  fun `backup restore marks onboarding complete only on valid json and keeps welcome on failure`() = runBlocking {
    GamefyDatabase.ensureSeeded(db)
    val dao = db.gamefyDao()
    assertFalse(dao.getProfileOnce()!!.onboardingCompleted)

    // Invalid JSON fails without marking onboarding completed
    assertFalse(repo.importFromJson("{ invalid json", markOnboardingCompleted = true))
    assertFalse(dao.getProfileOnce()!!.onboardingCompleted)

    // Build a valid backup JSON with a task
    dao.insertTask(TaskEntity(name = "Przywrócone zadanie", categoryCode = "d", coinReward = 15, expReward = 15))
    val backupJson = repo.exportToJson()
    dao.clearTasks()
    dao.upsertProfile(dao.getProfileOnce()!!.copy(onboardingCompleted = false))

    assertTrue(repo.importFromJson(backupJson, markOnboardingCompleted = true))
    assertTrue(dao.getProfileOnce()!!.onboardingCompleted)
    assertNotNull(dao.getTasksOnce().find { it.name == "Przywrócone zadanie" })
  }

  @Test
  fun `onboarding survey saves only answered preferences adds inactive tasks without rewards and prevents duplicates`() = runTest {
    GamefyDatabase.ensureSeeded(db)
    val dao = db.gamefyDao()

    val (addedTasks, addedRewards) = repo.completeOnboardingFromSurvey(
      areas = listOf(OnboardingArea.NAUKA),
      timePref = null, // skipped
      detailPref = OnboardingDetailPref.SIMPLE,
      rewardPrefs = listOf(OnboardingRewardPref.BOOKS),
      selectedTemplateIds = listOf("nauka_05", "nauka_05"), // duplicate id ignored
      selectedRewardTemplateIds = listOf("rew_books_01"),
    )

    assertEquals(1, addedTasks)
    assertEquals(1, addedRewards)

    val profile = dao.getProfileOnce()!!
    assertTrue(profile.onboardingCompleted)
    assertEquals("nauka", profile.onboardingInterestsCsv)
    assertEquals(null, profile.onboardingTimePref)
    assertEquals("simple", profile.onboardingDetailPref)
    // Adding from template must award 0 EXP / 0 coins
    assertEquals(0, profile.exp)
    assertEquals(0, profile.coins)
    assertEquals(0, profile.tasksDone)

    val task = dao.getTasksOnce().single()
    assertFalse(task.isActive)
    assertEquals("nauka_05", task.sourceTemplateId)
    assertEquals(null, task.dueTimestamp)
    assertEquals(ReminderMode.NONE.code, task.reminderModeCode)

    // Completing an inactive task must be ignored
    repo.completeTask(task)
    assertEquals(0, dao.getProfileOnce()!!.exp)

    // Duplicate add from Ready Tasks DB is blocked
    val tpl = READY_TASK_TEMPLATES.first { it.id == "nauka_05" }
    assertFalse(repo.addTaskFromTemplate(tpl))

    // Editing user copy does not mutate built-in template
    repo.saveTask(task.copy(name = "Moja własna sesja nauki"))
    assertEquals("Sesja głębokiej nauki — 25 minut", READY_TASK_TEMPLATES.first { it.id == "nauka_05" }.name)

    // Shop reward added without auto-purchasing
    val shopItem = dao.getShopItemsOnce().single()
    assertEquals(0, shopItem.ownedQuantity)
    assertFalse(shopItem.unlocked)
    assertFalse(repo.addRewardFromCatalog(REWARD_CATALOG_TEMPLATES.first { it.id == "rew_books_01" }))
  }

  @Test
  fun `activating template task splits weighted EXP accurately across attributes and subtasks award zero partial rewards`() = runTest {
    GamefyDatabase.ensureSeeded(db)
    val dao = db.gamefyDao()

    val tpl = READY_TASK_TEMPLATES.first { it.id == "nauka_05" } // 10 EXP, 10 coins, Wiedza 60%, Dyscyplina 40%, 4 subtasks
    assertTrue(repo.addTaskFromTemplate(tpl, asInactive = true))
    val taskId = dao.getTasksOnce().single().id

    assertTrue(repo.activateLibraryTask(taskId))
    val activeTask = dao.getTaskByIdOnce(taskId)!!
    assertTrue(activeTask.isActive)

    // Check first 3 of 4 subtasks -> 0 EXP, 0 coins awarded
    val subtasks = parseSubtasksJson(activeTask.subtasksJson)
    assertEquals(4, subtasks.size)
    for (i in 0 until 3) {
      val current = dao.getTaskByIdOnce(taskId)!!
      val res = repo.toggleSubtask(current, subtasks[i].id)
      assertEquals(null, res)
      assertEquals(0, dao.getProfileOnce()!!.exp)
      assertEquals(0, dao.getProfileOnce()!!.coins)
    }

    // Check 4th (final) subtask -> atomically completes main task & splits 10 EXP into Wiedza +6, Dyscyplina +4
    val finalOutcome = repo.toggleSubtask(dao.getTaskByIdOnce(taskId)!!, subtasks[3].id)
    assertNotNull(finalOutcome)

    val attrs = dao.getAttributesOnce()
    val wiedza = attrs.first { it.name == "Wiedza" }
    val dyscyplina = attrs.first { it.name == "Dyscyplina" }
    val sila = attrs.first { it.name == "Siła" }
    assertEquals(6, wiedza.exp)
    assertEquals(4, dyscyplina.exp)
    assertEquals(0, sila.exp)
  }

  @Test
  fun `legacy v1 backup without onboarding or v5-v8 fields restores cleanly and marks onboarding complete`() = runBlocking {
    GamefyDatabase.ensureSeeded(db)
    val dao = db.gamefyDao()
    assertFalse(dao.getProfileOnce()!!.onboardingCompleted)

    val legacyV1Json = """
      {
        "version": 1,
        "profile": {
          "exp": 320,
          "coins": 145,
          "gems": 3,
          "tasksDone": 12,
          "pomosDone": 4,
          "coinsEarned": 240,
          "itemsBought": 2,
          "notificationsEnabled": true,
          "themeCode": "forest",
          "unlockedAchievementsCsv": "first_task"
        },
        "attributes": [
          { "id": 1, "icon": "💪", "name": "Siła", "exp": 120 }
        ],
        "folders": [
          { "id": 1, "icon": "🏠", "name": "Dom" }
        ],
        "tags": [
          { "id": 1, "name": "pilne" }
        ],
        "tasks": [
          {
            "id": 10,
            "name": "Stare zadanie z v1",
            "description": "Opis",
            "link": "",
            "folderId": 1,
            "tagIdsCsv": "1",
            "categoryCode": "d",
            "repeatInterval": 1,
            "repeatLimit": 0,
            "completedCount": 3,
            "expReward": 25,
            "coinReward": 15,
            "attributeIdsCsv": "1",
            "rewardItemId": 0,
            "durationMinutes": 15,
            "reminderModeCode": "d1",
            "penaltyModeCode": "0",
            "penaltyMinCoins": 5,
            "penaltyMaxCoins": 10,
            "startTimestamp": 1700000000000,
            "dueTimestamp": null
          }
        ],
        "shopItems": [
          {
            "id": 5,
            "icon": "☕",
            "name": "Stara kawa",
            "coinPrice": 40,
            "gemPrice": 0,
            "requiredAttrId": 0,
            "requiredAttrLevel": 1,
            "ownedQuantity": 1,
            "unlocked": true,
            "imageData": "",
            "description": "",
            "limitPeriodCode": "0",
            "limitMaxCount": 1,
            "purchaseHistoryCsv": ""
          }
        ]
      }
    """.trimIndent()

    val ok = repo.importFromJson(legacyV1Json, markOnboardingCompleted = false)
    assertTrue(ok)

    val profile = dao.getProfileOnce()!!
    assertTrue(profile.onboardingCompleted)
    assertEquals(320, profile.exp)
    assertEquals(145, profile.coins)
    assertEquals(3, profile.gems)
    assertEquals("forest", profile.themeCode)
    assertTrue(profile.notifUseTaskSettings) // default true preserved

    val task = dao.getTasksOnce().single()
    assertEquals("Stare zadanie z v1", task.name)
    assertTrue(task.isActive) // legacy tasks default to active
    assertEquals(null, task.sourceTemplateId)
    assertEquals("", task.attributeWeightsJson)

    val item = dao.getShopItemsOnce().single()
    assertEquals("Stara kawa", item.name)
    assertEquals(null, item.sourceRewardTemplateId)
  }

  @Test
  fun `renamed attribute still receives weighted EXP share and inactive task cannot be skipped`() = runTest {
    GamefyDatabase.ensureSeeded(db)
    val dao = db.gamefyDao()

    val tpl = READY_TASK_TEMPLATES.first { it.id == "nauka_05" } // 10 EXP: Wiedza 60%, Dyscyplina 40%
    assertTrue(repo.addTaskFromTemplate(tpl, asInactive = true))
    val inactiveTask = dao.getTasksOnce().single()

    // Inactive task skip must be a no-op
    val skipResult = repo.skipTask(inactiveTask)
    assertEquals("", skipResult)
    assertTrue(dao.getActivityEventsOnce().isEmpty())

    // Rename "Wiedza" -> "Rozwój" before completing template task
    val wiedza = dao.getAttributesOnce().first { it.name == "Wiedza" }
    repo.updateAttribute(wiedza.copy(name = "Rozwój"))

    assertTrue(repo.activateLibraryTask(inactiveTask.id))
    repo.completeTask(dao.getTaskByIdOnce(inactiveTask.id)!!)

    val attrsAfter = dao.getAttributesOnce()
    assertEquals(6, attrsAfter.first { it.id == wiedza.id }.exp)
    assertEquals(4, attrsAfter.first { it.name == "Dyscyplina" }.exp)
  }

  @Test
  fun `skipping all 4 survey questions saves only nulls and classic custom task EXP and coins remain intact`() = runTest {
    GamefyDatabase.ensureSeeded(db)
    val dao = db.gamefyDao()

    val (addedTasks, addedRewards) = repo.completeOnboardingFromSurvey(
      areas = null,
      timePref = null,
      detailPref = null,
      rewardPrefs = null,
      selectedTemplateIds = emptyList(),
      selectedRewardTemplateIds = emptyList(),
    )
    assertEquals(0, addedTasks)
    assertEquals(0, addedRewards)

    val p = dao.getProfileOnce()!!
    assertTrue(p.onboardingCompleted)
    assertEquals(null, p.onboardingInterestsCsv)
    assertEquals(null, p.onboardingTimePref)
    assertEquals(null, p.onboardingDetailPref)
    assertEquals(null, p.onboardingRewardPrefsCsv)
    assertTrue(dao.getTasksOnce().isEmpty())
    assertTrue(dao.getShopItemsOnce().isEmpty())

    // Classic custom task with 2 linked attributes (no weightsJson) grants full expReward to each attribute
    val attr1 = dao.getAttributesOnce()[0]
    val attr2 = dao.getAttributesOnce()[1]
    val customId = dao.insertTask(
      TaskEntity(
        name = "Własne klasyczne",
        categoryCode = "o",
        expReward = 35,
        coinReward = 25,
        gemReward = 1,
        attributeIdsCsv = "${attr1.id},${attr2.id}",
      )
    )
    repo.completeTask(dao.getTaskByIdOnce(customId)!!)

    val profileAfter = dao.getProfileOnce()!!
    // 35 task EXP + 30 first_task achievement = 65 EXP; 25 task coins + 15 first_task = 40 coins; 1 gem
    assertEquals(65, profileAfter.exp)
    assertEquals(40, profileAfter.coins)
    assertEquals(1, profileAfter.gems)
    val attrsAfter = dao.getAttributesOnce()
    assertEquals(35, attrsAfter.first { it.id == attr1.id }.exp)
    assertEquals(35, attrsAfter.first { it.id == attr2.id }.exp)
  }

  @Test
  fun `giving up one-time task applies coin penalty and removes task from to-do list`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, coins = 100))
    val taskId = dao.insertTask(
      TaskEntity(
        name = "Jednorazowe trudne",
        categoryCode = "o",
        coinReward = 20,
        penaltyModeCode = com.example.data.model.PenaltyMode.ON_SKIP_ONLY.code,
        penaltyMinCoins = 12,
        penaltyMaxCoins = 12,
      )
    )

    val msg = repo.giveUpTask(dao.getTaskByIdOnce(taskId)!!)
    assertTrue(msg.contains("−12"))
    assertEquals(88, dao.getProfileOnce()!!.coins)
    assertEquals(null, dao.getTaskByIdOnce(taskId))
    assertTrue(dao.getActivityEventsOnce().any { it.eventType == ActivityEventType.TASK_SKIP && it.coinsDelta == -12 })
  }

  @Test
  fun `giving up overdue recurring task applies penalty removes overdue state and restarts deadline countdown`() = runTest {
    val dao = db.gamefyDao()
    dao.upsertProfile(PlayerProfileEntity(id = 1L, coins = 80))
    val now = System.currentTimeMillis()
    val taskId = dao.insertTask(
      TaskEntity(
        name = "Codzienne zaległe",
        categoryCode = "d",
        repeatInterval = 1,
        coinReward = 15,
        difficultyCode = "m",
        penaltyModeCode = "0", // even without custom penalty, giving up applies auto surrender penalty
        startTimestamp = now - 5 * MILLIS_PER_DAY,
        dueTimestamp = now - 2 * MILLIS_PER_DAY,
        penaltyDueSnapshot = now - 2 * MILLIS_PER_DAY,
        penaltyCountApplied = 2,
        subtasksJson = toSubtasksJson(listOf(SubtaskItem(1L, "Krok 1", true))),
      )
    )

    val msg = repo.giveUpTask(dao.getTaskByIdOnce(taskId)!!)
    assertTrue(msg.contains("Kara"))
    val coinsAfter = dao.getProfileOnce()!!.coins
    assertTrue("Coins should decrease after giving up", coinsAfter < 80)

    val updated = dao.getTaskByIdOnce(taskId)!!
    assertTrue(updated.isActive)
    assertTrue("Task should stay active immediately (not hidden)", updated.startTimestamp >= now - 1000L && updated.startTimestamp <= System.currentTimeMillis() + 1000L)
    assertTrue("Task should no longer be overdue and count down to new deadline", (updated.dueTimestamp ?: 0L) > now + 20 * 3600_000L)
    assertEquals(null, updated.penaltyDueSnapshot)
    assertEquals(0, updated.penaltyCountApplied)
    assertTrue(parseSubtasksJson(updated.subtasksJson).none { it.isDone })
  }

  @Test
  fun `categories have default colors allow custom color editing and survive backup import export`() = runBlocking {
    GamefyDatabase.ensureSeeded(db)
    val dao = db.gamefyDao()

    val seeded = dao.getFoldersOnce()
    assertEquals(4, seeded.size)
    assertTrue("All seeded categories should have distinct hex colors", seeded.map { it.colorHex }.toSet().size == 4)
    assertEquals("#F59E0B", seeded.first { it.name == "Dom" }.colorHex)

    // Add custom category with custom color from color wheel
    repo.addFolder(icon = "🎸", name = "Muzyka", colorHex = "#e11d48")
    val music = dao.getFoldersOnce().first { it.name == "Muzyka" }
    assertEquals("#E11D48", music.colorHex)

    // Edit category color
    repo.updateFolder(music.copy(colorHex = "#06b6d4"))
    assertEquals("#06B6D4", dao.getFoldersOnce().first { it.id == music.id }.colorHex)

    // Export and import preserves custom color
    val backupJson = repo.exportToJson()
    repo.updateFolder(music.copy(colorHex = "#111111"))
    assertTrue(repo.importFromJson(backupJson))
    assertEquals("#06B6D4", dao.getFoldersOnce().first { it.name == "Muzyka" }.colorHex)

    // Legacy backup JSON without colorHex field assigns valid defaults gracefully
    val legacyRoot = org.json.JSONObject(backupJson)
    val foldersArr = legacyRoot.getJSONArray("folders")
    for (i in 0 until foldersArr.length()) {
      foldersArr.getJSONObject(i).remove("colorHex")
    }
    assertTrue(repo.importFromJson(legacyRoot.toString()))
    val restoredAfterLegacy = dao.getFoldersOnce()
    assertEquals("#F59E0B", restoredAfterLegacy.first { it.name == "Dom" }.colorHex)
    assertTrue(restoredAfterLegacy.all { it.colorHex.startsWith("#") && it.colorHex.length == 7 })
  }
}


