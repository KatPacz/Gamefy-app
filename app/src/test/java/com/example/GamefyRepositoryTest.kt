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
import com.example.data.model.MILLIS_PER_DAY
import com.example.data.model.localEpochDay
import com.example.data.repository.GamefyRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
}
