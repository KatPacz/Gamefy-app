package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.MILLIS_PER_DAY
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
  entities = [
    PlayerProfileEntity::class,
    AttributeEntity::class,
    FolderEntity::class,
    TagEntity::class,
    TaskEntity::class,
    ShopItemEntity::class,
  ],
  version = 4,
  exportSchema = false,
)
abstract class GamefyDatabase : RoomDatabase() {
  abstract fun gamefyDao(): GamefyDao

  companion object {
    @Volatile
    private var INSTANCE: GamefyDatabase? = null
    private val seedMutex = Mutex()

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN subtasksJson TEXT NOT NULL DEFAULT '[]'")
      }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_profile ADD COLUMN currentStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN bestStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN lastTaskEpochDay INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN customTasksCreated INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN boughtMostExpensive INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN completedNightAction INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN completedSubtaskChain INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN difficultyCode TEXT NOT NULL DEFAULT 'm'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN gemReward INTEGER NOT NULL DEFAULT 0")
      }
    }

    fun getInstance(context: Context): GamefyDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          GamefyDatabase::class.java,
          "gamefy_local.db",
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
          .build()
          .also { INSTANCE = it }
      }
    }

    suspend fun ensureSeeded(db: GamefyDatabase) {
      seedMutex.withLock {
        val dao = db.gamefyDao()
        if (dao.getProfileOnce() != null) return

        val now = System.currentTimeMillis()
        dao.upsertProfile(PlayerProfileEntity(id = 1L))

        val strId = dao.insertAttribute(AttributeEntity(id = 1L, icon = "💪", name = "Siła"))
        dao.insertAttribute(AttributeEntity(id = 2L, icon = "📚", name = "Wiedza"))
        val healthId = dao.insertAttribute(AttributeEntity(id = 3L, icon = "🌿", name = "Zdrowie"))
        dao.insertAttribute(AttributeEntity(id = 4L, icon = "🎨", name = "Kreatywność"))
        val discId = dao.insertAttribute(AttributeEntity(id = 5L, icon = "🎯", name = "Dyscyplina"))

        val homeFolder = dao.insertFolder(FolderEntity(id = 1L, icon = "🏠", name = "Dom"))
        dao.insertFolder(FolderEntity(id = 2L, icon = "💼", name = "Praca"))
        dao.insertFolder(FolderEntity(id = 3L, icon = "📚", name = "Nauka"))
        val healthFolder = dao.insertFolder(FolderEntity(id = 4L, icon = "💪", name = "Zdrowie"))

        val urgentTag = dao.insertTag(TagEntity(id = 1L, name = "pilne"))
        val importantTag = dao.insertTag(TagEntity(id = 2L, name = "ważne"))
        dao.insertTag(TagEntity(id = 3L, name = "łatwe"))
        dao.insertTag(TagEntity(id = 4L, name = "poranek"))

        dao.insertShopItem(
          ShopItemEntity(
            id = 1L,
            icon = "☕",
            name = "Kawa",
            coinPrice = 30,
            gemPrice = 0,
            limitPeriodCode = "d",
            limitMaxCount = 2,
          )
        )
        dao.insertShopItem(
          ShopItemEntity(
            id = 2L,
            icon = "🎬",
            name = "Godzina serialu",
            coinPrice = 80,
            gemPrice = 0,
          )
        )
        dao.insertShopItem(
          ShopItemEntity(
            id = 3L,
            icon = "🍣",
            name = "Sushi",
            coinPrice = 250,
            gemPrice = 0,
          )
        )
        dao.insertShopItem(
          ShopItemEntity(
            id = 4L,
            icon = "☕",
            name = "Ekspres do kawy",
            coinPrice = 500,
            gemPrice = 3,
            requiredAttrId = discId,
            requiredAttrLevel = 2,
            description = "Wymarzony model: https://example.com/ekspres",
          )
        )
        dao.insertShopItem(
          ShopItemEntity(
            id = 5L,
            icon = "🏖️",
            name = "Dzień wolny",
            coinPrice = 600,
            gemPrice = 5,
          )
        )

        dao.insertTask(
          TaskEntity(
            id = 1L,
            name = "Sprzątanie łazienki",
            description = "Dokładne porządki raz w tygodniu.",
            folderId = homeFolder,
            tagIdsCsv = urgentTag.toString(),
            categoryCode = "w",
            repeatInterval = 1,
            expReward = 30,
            coinReward = 20,
            gemReward = 0,
            difficultyCode = "e",
            attributeIdsCsv = discId.toString(),
            durationMinutes = 15,
            reminderModeCode = "d1",
            penaltyModeCode = "o",
            penaltyMinCoins = 5,
            penaltyMaxCoins = 10,
            startTimestamp = now - 2 * MILLIS_PER_DAY,
            dueTimestamp = now + 5 * MILLIS_PER_DAY,
            subtasksJson = toSubtasksJson(
              listOf(
                SubtaskItem(1L, "Umyć lustro", false),
                SubtaskItem(2L, "Wyczyścić umywalkę", false),
                SubtaskItem(3L, "Umyć podłogę", false),
              )
            ),
          )
        )
        dao.insertTask(
          TaskEntity(
            id = 2L,
            name = "Trening siłowy",
            folderId = healthFolder,
            categoryCode = "d",
            repeatInterval = 3,
            expReward = 55,
            coinReward = 40,
            gemReward = 1,
            difficultyCode = "h",
            attributeIdsCsv = "$strId,$healthId",
            reminderModeCode = "od",
            startTimestamp = now - 3 * MILLIS_PER_DAY,
            dueTimestamp = now - 4_000_000L,
          )
        )
        dao.insertTask(
          TaskEntity(
            id = 3L,
            name = "Umówić wizytę u dentysty",
            description = "Rejestracja online: https://example.com",
            link = "https://example.com",
            folderId = healthFolder,
            tagIdsCsv = importantTag.toString(),
            categoryCode = "o",
            repeatInterval = 1,
            expReward = 15,
            coinReward = 10,
            gemReward = 0,
            difficultyCode = "ve",
            attributeIdsCsv = healthId.toString(),
            startTimestamp = now,
            dueTimestamp = null,
          )
        )
      }
    }
  }
}
