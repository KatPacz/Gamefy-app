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
    ActivityEventEntity::class,
  ],
  version = 12,
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

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifDaysCsv TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifSlotsCsv TEXT NOT NULL DEFAULT 'any'")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifCustomStartHour INTEGER NOT NULL DEFAULT 9")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifCustomStartMinute INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifCustomEndHour INTEGER NOT NULL DEFAULT 21")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifCustomEndMinute INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifTriggerModeCode TEXT NOT NULL DEFAULT 'reminder'")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifMaxPerDay INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifSummaryStyleCode TEXT NOT NULL DEFAULT 'digest'")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifSentCountToday INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifSentEpochDay INTEGER NOT NULL DEFAULT -1")
      }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_profile ADD COLUMN notifUseTaskSettings INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE tasks ADD COLUMN preferredReminderHour INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE tasks ADD COLUMN preferredReminderMinute INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `activity_events` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `eventType` TEXT NOT NULL,
            `taskId` INTEGER NOT NULL,
            `taskName` TEXT NOT NULL,
            `categoryCode` TEXT NOT NULL,
            `onTime` INTEGER NOT NULL,
            `coinsDelta` INTEGER NOT NULL,
            `gemsDelta` INTEGER NOT NULL,
            `expDelta` INTEGER NOT NULL,
            `durationMinutes` INTEGER NOT NULL,
            `itemId` INTEGER NOT NULL,
            `itemName` TEXT NOT NULL,
            `itemIcon` TEXT NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_profile ADD COLUMN onboardingCompleted INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN onboardingInterestsCsv TEXT")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN onboardingTimePref TEXT")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN onboardingDetailPref TEXT")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN onboardingRewardPrefsCsv TEXT")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE tasks ADD COLUMN sourceTemplateId TEXT")
        db.execSQL("ALTER TABLE tasks ADD COLUMN attributeWeightsJson TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE shop_items ADD COLUMN sourceRewardTemplateId TEXT")
      }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE folders ADD COLUMN colorHex TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE folders SET colorHex = '#F59E0B' WHERE LOWER(TRIM(name)) = 'dom' AND colorHex = ''")
        db.execSQL("UPDATE folders SET colorHex = '#3B82F6' WHERE LOWER(TRIM(name)) = 'praca' AND colorHex = ''")
        db.execSQL("UPDATE folders SET colorHex = '#8B5CF6' WHERE LOWER(TRIM(name)) = 'nauka' AND colorHex = ''")
        db.execSQL("UPDATE folders SET colorHex = '#10B981' WHERE LOWER(TRIM(name)) = 'zdrowie' AND colorHex = ''")
        db.execSQL("UPDATE folders SET colorHex = '#14B8A6' WHERE LOWER(TRIM(name)) = 'samopoczucie' AND colorHex = ''")
        db.execSQL("UPDATE folders SET colorHex = '#EC4899' WHERE LOWER(TRIM(name)) = 'hobby' AND colorHex = ''")
        db.execSQL("UPDATE folders SET colorHex = '#F97316' WHERE LOWER(TRIM(name)) IN ('zwierzęta', 'zwierzeta') AND colorHex = ''")
        db.execSQL("UPDATE folders SET colorHex = '#6366F1' WHERE LOWER(TRIM(name)) = 'side questy' AND colorHex = ''")
      }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_profile ADD COLUMN rsqBoardDayKey INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN rsqBoardIdsCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN rsqAcceptedIdsCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN rsqAcceptedCountToday INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_profile ADD COLUMN lastBackupReminderTimestamp INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE player_profile ADD COLUMN fontSizeCode TEXT NOT NULL DEFAULT 'default'")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN fontFamilyCode TEXT NOT NULL DEFAULT 'default'")
        db.execSQL("ALTER TABLE player_profile ADD COLUMN wordSpacingCode TEXT NOT NULL DEFAULT 'standard'")
      }
    }

    fun getInstance(context: Context): GamefyDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          GamefyDatabase::class.java,
          "gamefy_local.db",
        )
          .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
          )
          .build()
          .also { INSTANCE = it }
      }
    }

    suspend fun ensureSeeded(db: GamefyDatabase) {
      seedMutex.withLock {
        val dao = db.gamefyDao()
        if (dao.getProfileOnce() != null) return

        dao.upsertProfile(PlayerProfileEntity(id = 1L, onboardingCompleted = false))

        dao.insertAttribute(AttributeEntity(id = 1L, icon = "💪", name = "Siła"))
        dao.insertAttribute(AttributeEntity(id = 2L, icon = "📚", name = "Wiedza"))
        dao.insertAttribute(AttributeEntity(id = 3L, icon = "🌿", name = "Zdrowie"))
        dao.insertAttribute(AttributeEntity(id = 4L, icon = "🎨", name = "Kreatywność"))
        dao.insertAttribute(AttributeEntity(id = 5L, icon = "🎯", name = "Dyscyplina"))

        dao.insertFolder(FolderEntity(id = 1L, icon = "🏠", name = "Dom", colorHex = defaultColorHexForFolder("Dom", 1L)))
        dao.insertFolder(FolderEntity(id = 2L, icon = "💼", name = "Praca", colorHex = defaultColorHexForFolder("Praca", 2L)))
        dao.insertFolder(FolderEntity(id = 3L, icon = "📚", name = "Nauka", colorHex = defaultColorHexForFolder("Nauka", 3L)))
        dao.insertFolder(FolderEntity(id = 4L, icon = "💪", name = "Zdrowie", colorHex = defaultColorHexForFolder("Zdrowie", 4L)))

        dao.insertTag(TagEntity(id = 1L, name = "pilne"))
        dao.insertTag(TagEntity(id = 2L, name = "ważne"))
        dao.insertTag(TagEntity(id = 3L, name = "łatwe"))
        dao.insertTag(TagEntity(id = 4L, name = "poranek"))
      }
    }
  }
}
