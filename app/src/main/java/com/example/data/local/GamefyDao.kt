package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GamefyDao {

  @Query("SELECT * FROM player_profile WHERE id = 1")
  fun observeProfile(): Flow<PlayerProfileEntity?>

  @Query("SELECT * FROM player_profile WHERE id = 1")
  suspend fun getProfileOnce(): PlayerProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProfile(profile: PlayerProfileEntity)

  @Query("SELECT * FROM attributes ORDER BY id ASC")
  fun observeAttributes(): Flow<List<AttributeEntity>>

  @Query("SELECT * FROM attributes ORDER BY id ASC")
  suspend fun getAttributesOnce(): List<AttributeEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttribute(attribute: AttributeEntity): Long

  @Update
  suspend fun updateAttribute(attribute: AttributeEntity)

  @Query("DELETE FROM attributes WHERE id = :id")
  suspend fun deleteAttributeById(id: Long)

  @Query("SELECT * FROM folders ORDER BY id ASC")
  fun observeFolders(): Flow<List<FolderEntity>>

  @Query("SELECT * FROM folders ORDER BY id ASC")
  suspend fun getFoldersOnce(): List<FolderEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFolder(folder: FolderEntity): Long

  @Update
  suspend fun updateFolder(folder: FolderEntity)

  @Query("DELETE FROM folders WHERE id = :id")
  suspend fun deleteFolderById(id: Long)

  @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
  fun observeTags(): Flow<List<TagEntity>>

  @Query("SELECT * FROM tags ORDER BY id ASC")
  suspend fun getTagsOnce(): List<TagEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTag(tag: TagEntity): Long

  @Update
  suspend fun updateTag(tag: TagEntity)

  @Query("DELETE FROM tags WHERE id = :id")
  suspend fun deleteTagById(id: Long)

  @Query("SELECT * FROM tasks ORDER BY id ASC")
  fun observeTasks(): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks ORDER BY id ASC")
  suspend fun getTasksOnce(): List<TaskEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: TaskEntity): Long

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Query("DELETE FROM tasks WHERE id = :id")
  suspend fun deleteTaskById(id: Long)

  @Query("SELECT * FROM shop_items ORDER BY id ASC")
  fun observeShopItems(): Flow<List<ShopItemEntity>>

  @Query("SELECT * FROM shop_items ORDER BY id ASC")
  suspend fun getShopItemsOnce(): List<ShopItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertShopItem(item: ShopItemEntity): Long

  @Update
  suspend fun updateShopItem(item: ShopItemEntity)

  @Query("DELETE FROM shop_items WHERE id = :id")
  suspend fun deleteShopItemById(id: Long)

  @Query("DELETE FROM attributes")
  suspend fun clearAttributes()

  @Query("DELETE FROM folders")
  suspend fun clearFolders()

  @Query("DELETE FROM tags")
  suspend fun clearTags()

  @Query("DELETE FROM tasks")
  suspend fun clearTasks()

  @Query("DELETE FROM shop_items")
  suspend fun clearShopItems()

  @Transaction
  suspend fun replaceAllData(
    profile: PlayerProfileEntity,
    attributes: List<AttributeEntity>,
    folders: List<FolderEntity>,
    tags: List<TagEntity>,
    tasks: List<TaskEntity>,
    shopItems: List<ShopItemEntity>,
  ) {
    clearTasks()
    clearShopItems()
    clearAttributes()
    clearFolders()
    clearTags()
    upsertProfile(profile.copy(id = 1L))
    attributes.forEach { insertAttribute(it) }
    folders.forEach { insertFolder(it) }
    tags.forEach { insertTag(it) }
    tasks.forEach { insertTask(it) }
    shopItems.forEach { insertShopItem(it) }
  }
}
