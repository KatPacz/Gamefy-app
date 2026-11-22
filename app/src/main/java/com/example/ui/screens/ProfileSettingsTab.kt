package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FolderEntity
import com.example.data.local.TagEntity
import com.example.data.model.AppFontFamilyOption
import com.example.data.model.AppFontSize
import com.example.data.model.AppThemeId
import com.example.data.model.AppWordSpacing
import com.example.data.model.NotificationSettings
import com.example.data.model.isBackupOverdueWarning
import com.example.data.model.toNotificationSettings
import com.example.ui.GamefyUiState
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelSectionHeader
import com.example.ui.components.resolveFolderColor
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.bodyFontFamilyFor
import com.example.ui.theme.displayFontFamilyFor
import com.example.ui.theme.paletteForTheme

enum class ProfileSettingsSection(
  val label: String,
  val tag: String,
) {
  NOTIFICATIONS("Powiadomienia", "settings_shortcut_notifications"),
  ORGANIZATION("Organizacja", "settings_shortcut_organization"),
  BACKUP("Kopia", "settings_shortcut_backup"),
  APPEARANCE("Wygląd", "settings_shortcut_appearance"),
}

fun settingsSectionIndex(
  section: ProfileSettingsSection,
  hasBackupWarningBanner: Boolean,
): Int {
  val offset = if (hasBackupWarningBanner) 1 else 0
  return when (section) {
    ProfileSettingsSection.NOTIFICATIONS -> offset + 0
    ProfileSettingsSection.ORGANIZATION -> offset + 2
    ProfileSettingsSection.BACKUP -> offset + 5
    ProfileSettingsSection.APPEARANCE -> offset + 7
  }
}

@Composable
fun ProfileSettingsShortcutsBar(
  onJumpToSection: (ProfileSettingsSection) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 2.dp)
      .testTag("settings_shortcuts_bar"),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ProfileSettingsSection.entries.forEach { section ->
      Surface(
        shape = RoundedCornerShape(50),
        color = palette.surface,
        border = BorderStroke(1.5.dp, palette.divider),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(50))
          .clickable(role = Role.Button) { onJumpToSection(section) }
          .testTag(section.tag),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Text(
            text = section.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.profileSettingsItems(
  uiState: GamefyUiState,
  notificationPermissionDenied: Boolean,
  onToggleNotifications: (Boolean) -> Unit,
  onUpdateNotificationSettings: (NotificationSettings) -> Unit,
  onOpenFolderManager: () -> Unit,
  onOpenTagManager: () -> Unit,
  onOpenReadyTasksDb: () -> Unit,
  onSaveBackup: () -> Unit,
  onLoadBackup: () -> Unit,
  onSelectTheme: (AppThemeId) -> Unit,
  onSelectFontSize: (AppFontSize) -> Unit,
  onSelectFontFamily: (AppFontFamilyOption) -> Unit,
  onSelectWordSpacing: (AppWordSpacing) -> Unit,
) {
  val showWarningBanner = isBackupOverdueWarning(uiState.profile.lastBackupExportTimestamp)

  // 1. Amber backup banner (only when never backed up or > 14 days)
  if (showWarningBanner) {
    item(key = "settings_backup_warning_banner") {
      SettingsBackupWarningBanner(
        lastBackupExportTimestamp = uiState.profile.lastBackupExportTimestamp,
        onSaveBackup = onSaveBackup,
      )
    }
  }

  // 2. Powiadomienia
  item(key = "settings_notifications_header") {
    PixelSectionHeader(
      title = "Powiadomienia",
      modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
  }
  item(key = "settings_notifications_card") {
    PixelCard {
      val notifSettings = remember(uiState.profile) {
        uiState.profile.toNotificationSettings()
      }
      NotificationPreferencesBlock(
        notificationsEnabled = uiState.profile.notificationsEnabled,
        permissionDeniedByUser = notificationPermissionDenied,
        settings = notifSettings,
        tasks = uiState.tasks,
        onToggleNotifications = onToggleNotifications,
        onUpdateSettings = onUpdateNotificationSettings,
      )
    }
  }

  // 3. Organizacja zadań (Folders & Tags card + separate Gotowe zadania card)
  item(key = "settings_org_header") {
    PixelSectionHeader(
      title = "Organizacja zadań",
      modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
  }
  item(key = "settings_org_folders_tags_card") {
    SettingsFoldersAndTagsCard(
      folders = uiState.folders,
      tags = uiState.tags,
      onEditFolders = onOpenFolderManager,
      onEditTags = onOpenTagManager,
    )
  }
  item(key = "settings_ready_tasks_card") {
    SettingsReadyTasksCard(
      onOpenReadyTasksDb = onOpenReadyTasksDb,
    )
  }

  // 4. Kopia zapasowa
  item(key = "settings_backup_header") {
    PixelSectionHeader(
      title = "Kopia zapasowa",
      modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
  }
  item(key = "settings_backup_card") {
    SettingsBackupSectionCard(
      lastBackupExportTimestamp = uiState.profile.lastBackupExportTimestamp,
      onSaveBackup = onSaveBackup,
      onLoadBackup = onLoadBackup,
    )
  }

  // 5. Wygląd i czytelność (longest section, placed at the end)
  item(key = "settings_appearance_header") {
    PixelSectionHeader(
      title = "Wygląd i czytelność",
      modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
  }
  item(key = "settings_appearance_card") {
    SettingsAppearanceAndReadabilityCard(
      selectedTheme = uiState.themeId,
      selectedFontSize = uiState.fontSize,
      selectedFontFamily = uiState.fontFamilyOption,
      selectedWordSpacing = uiState.wordSpacing,
      onSelectTheme = onSelectTheme,
      onSelectFontSize = onSelectFontSize,
      onSelectFontFamily = onSelectFontFamily,
      onSelectWordSpacing = onSelectWordSpacing,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsFoldersAndTagsCard(
  folders: List<FolderEntity>,
  tags: List<TagEntity>,
  onEditFolders: () -> Unit,
  onEditTags: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val maxVisibleChips = 6

  PixelCard {
    // Foldery header row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Foldery (${folders.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
      )
      SettingsEditActionButton(
        contentDesc = "Edytuj foldery",
        testTag = "open_folder_editor_button",
        onClick = onEditFolders,
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (folders.isEmpty()) {
      Text(
        text = "Brak własnych folderów",
        style = MaterialTheme.typography.bodySmall,
        fontSize = 13.sp,
        color = palette.textMuted,
      )
    } else {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        folders.take(maxVisibleChips).forEach { folder ->
          val folderColor = resolveFolderColor(folder)
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = folderColor.copy(alpha = 0.14f),
            border = BorderStroke(1.25.dp, folderColor),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(folderColor),
              )
              Text(
                text = folder.name,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        val extraFolders = folders.size - maxVisibleChips
        if (extraFolders > 0) {
          OverflowCountChip(count = extraFolders)
        }
      }
    }

    HorizontalDivider(
      modifier = Modifier.padding(vertical = 12.dp),
      color = palette.divider,
    )

    // Tagi header row (identical layout & button style to Folders)
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Tagi (${tags.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
      )
      SettingsEditActionButton(
        contentDesc = "Edytuj tagi",
        testTag = "open_tag_editor_button",
        onClick = onEditTags,
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (tags.isEmpty()) {
      Text(
        text = "Brak tagów — dodaj własne etykiety do szybkiego filtrowania",
        style = MaterialTheme.typography.bodySmall,
        fontSize = 13.sp,
        color = palette.textMuted,
      )
    } else {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        tags.take(maxVisibleChips).forEach { tag ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.surfaceSubtle,
            border = BorderStroke(1.25.dp, palette.divider),
          ) {
            Text(
              text = "#${tag.name}",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }
        val extraTags = tags.size - maxVisibleChips
        if (extraTags > 0) {
          OverflowCountChip(count = extraTags)
        }
      }
    }
  }
}

@Composable
private fun OverflowCountChip(count: Int) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.25.dp, palette.divider),
  ) {
    Text(
      text = "+$count",
      style = MaterialTheme.typography.labelSmall,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = palette.textMuted,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    )
  }
}

@Composable
private fun SettingsEditActionButton(
  contentDesc: String,
  testTag: String,
  onClick: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color.Transparent,
    border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.65f)),
    modifier = Modifier
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(
        role = Role.Button,
        onClickLabel = contentDesc,
        onClick = onClick,
      )
      .semantics { contentDescription = contentDesc }
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = null,
        tint = palette.accent,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = "Edytuj",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = palette.accent,
      )
    }
  }
}

@Composable
private fun SettingsReadyTasksCard(
  onOpenReadyTasksDb: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  PixelCard {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Gotowe zadania",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
      )
      Text(
        text = "Przeglądaj przykładowe zadania i dodawaj je do swojej biblioteki.",
        style = MaterialTheme.typography.bodySmall,
        fontSize = 13.sp,
        color = palette.textMuted,
      )
      PixelButton(
        text = "Baza gotowych zadań",
        compact = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("open_ready_tasks_db_button"),
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.LibraryBooks,
            contentDescription = null,
            tint = palette.onAccent,
            modifier = Modifier.size(18.dp),
          )
        },
        onClick = onOpenReadyTasksDb,
      )
    }
  }
}

@Composable
private fun SettingsAppearanceAndReadabilityCard(
  selectedTheme: AppThemeId,
  selectedFontSize: AppFontSize,
  selectedFontFamily: AppFontFamilyOption,
  selectedWordSpacing: AppWordSpacing,
  onSelectTheme: (AppThemeId) -> Unit,
  onSelectFontSize: (AppFontSize) -> Unit,
  onSelectFontFamily: (AppFontFamilyOption) -> Unit,
  onSelectWordSpacing: (AppWordSpacing) -> Unit,
) {
  val palette = LocalGamefyPalette.current

  PixelCard {
    // 1. Motyw kolorystyczny: 3-column miniature preview grid
    Text(
      text = "Motyw kolorystyczny",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = palette.textPrimary,
    )
    Spacer(modifier = Modifier.height(10.dp))

    val themeGroups = remember {
      listOf(
        "Jasne" to AppThemeId.entries.filter { !it.isDark && !it.isHighContrast },
        "Ciemne" to AppThemeId.entries.filter { it.isDark && !it.isHighContrast },
        "Wysoki kontrast" to AppThemeId.entries.filter { it.isHighContrast },
      )
    }

    themeGroups.forEachIndexed { groupIndex, (groupTitle, themes) ->
      if (groupIndex > 0) {
        Spacer(modifier = Modifier.height(12.dp))
      }
      Text(
        text = groupTitle,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 13.sp,
        color = palette.textMuted,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(8.dp))

      themes.chunked(3).forEach { rowThemes ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          rowThemes.forEach { theme ->
            ThemeMiniPreviewTile(
              theme = theme,
              selected = selectedTheme == theme,
              onClick = { onSelectTheme(theme) },
              modifier = Modifier.weight(1f),
            )
          }
          repeat(3 - rowThemes.size) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }

    HorizontalDivider(
      modifier = Modifier.padding(vertical = 16.dp),
      color = palette.divider,
    )

    // 2. Czcionka i czytelność (Preview right at the top!)
    Text(
      text = "Czcionka i czytelność",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = palette.textPrimary,
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Live readability preview right under the section title
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = palette.surfaceSubtle,
      border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.45f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("readability_preview_box"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "Podgląd czytelności",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Text(
          text = "Zażółć gęślą jaźń — Twoje zadania i opisy będą wyglądać właśnie tak.",
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textPrimary,
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2a. Rozmiar czcionki: 4-segment control with growing "A" + label below
    Text(
      text = "Rozmiar czcionki",
      style = MaterialTheme.typography.labelSmall,
      fontSize = 13.sp,
      color = palette.textMuted,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(6.dp))

    val sizeIconSp = remember {
      mapOf(
        AppFontSize.SMALL to 12.sp,
        AppFontSize.DEFAULT to 16.sp,
        AppFontSize.LARGE to 20.sp,
        AppFontSize.SUPER_LARGE to 24.sp,
      )
    }

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = palette.surfaceSubtle,
      border = BorderStroke(1.5.dp, palette.divider),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("font_size_segmented_control"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AppFontSize.entries.forEach { sizeOpt ->
          val isSelected = selectedFontSize == sizeOpt
          Surface(
            shape = RoundedCornerShape(9.dp),
            color = if (isSelected) palette.accent else Color.Transparent,
            border = if (isSelected && palette.isHighContrast) {
              BorderStroke(1.5.dp, palette.outline)
            } else {
              null
            },
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .clip(RoundedCornerShape(9.dp))
              .clickable(
                role = Role.RadioButton,
                onClickLabel = "Rozmiar czcionki: ${sizeOpt.label}",
              ) { onSelectFontSize(sizeOpt) }
              .semantics {
                selected = isSelected
                contentDescription = "Rozmiar czcionki: ${sizeOpt.label}"
              }
              .testTag("font_size_${sizeOpt.code}"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "A",
                fontSize = sizeIconSp[sizeOpt] ?: 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) palette.onAccent else palette.textPrimary,
              )
            }
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Wybrany rozmiar: ${selectedFontSize.label}",
      style = MaterialTheme.typography.bodySmall,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      color = palette.textMuted,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("font_size_selected_label"),
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 2b. Typ czcionki: vertical list of 4 full-width rows with own-font sample & RadioButton
    Text(
      text = "Typ czcionki",
      style = MaterialTheme.typography.labelSmall,
      fontSize = 13.sp,
      color = palette.textMuted,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(6.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      AppFontFamilyOption.entries.forEach { familyOpt ->
        val isSelected = selectedFontFamily == familyOpt
        val titleFamily = remember(familyOpt) { displayFontFamilyFor(familyOpt) }
        val sampleFamily = remember(familyOpt) { bodyFontFamilyFor(familyOpt) }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) palette.accent.copy(alpha = 0.12f) else palette.surfaceSubtle,
          border = BorderStroke(
            width = if (isSelected || palette.isHighContrast) 2.dp else 1.dp,
            color = if (isSelected) palette.accent else palette.divider,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
              role = Role.RadioButton,
              onClickLabel = "Wybierz czcionkę ${familyOpt.label}",
            ) { onSelectFontFamily(familyOpt) }
            .semantics { selected = isSelected }
            .testTag("font_family_${familyOpt.code}"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = familyOpt.label,
                  fontFamily = titleFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "Aa Zażółć",
                  fontFamily = sampleFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = 13.sp,
                  color = palette.accent,
                  maxLines = 1,
                )
              }
              Text(
                text = familyOpt.subtitle,
                fontFamily = sampleFamily,
                fontSize = 12.5.sp,
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            RadioButton(
              selected = isSelected,
              onClick = { onSelectFontFamily(familyOpt) },
              colors = RadioButtonDefaults.colors(
                selectedColor = palette.accent,
                unselectedColor = palette.textMuted,
              ),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = palette.divider)
    Spacer(modifier = Modifier.height(10.dp))

    // 2c. Odstępy między słowami: single full-row switch
    val widerEnabled = selectedWordSpacing == AppWordSpacing.WIDER
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(
          role = Role.Switch,
          onClickLabel = "Większe odstępy między słowami",
        ) {
          onSelectWordSpacing(if (widerEnabled) AppWordSpacing.STANDARD else AppWordSpacing.WIDER)
        }
        .padding(vertical = 4.dp)
        .testTag("word_spacing_switch_row"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = "Większe odstępy między słowami",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Text(
          text = "Zwiększa rozstaw liter i wysokość wierszy w opisach.",
          style = MaterialTheme.typography.bodySmall,
          fontSize = 12.5.sp,
          color = palette.textMuted,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      HighContrastPixelSwitch(
        checked = widerEnabled,
        onCheckedChange = { checked ->
          onSelectWordSpacing(if (checked) AppWordSpacing.WIDER else AppWordSpacing.STANDARD)
        },
        modifier = Modifier.testTag("word_spacing_switch"),
      )
    }
  }
}

@Composable
private fun ThemeMiniPreviewTile(
  theme: AppThemeId,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activePalette = LocalGamefyPalette.current
  val previewPalette = remember(theme) { paletteForTheme(theme) }
  val tileBorderColor = when {
    selected -> if (activePalette.isHighContrast) activePalette.outline else activePalette.accent
    activePalette.isHighContrast -> activePalette.outline
    else -> activePalette.divider
  }

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(
        role = Role.RadioButton,
        onClickLabel = "Motyw ${theme.label}",
        onClick = onClick,
      )
      .semantics { this.selected = selected }
      .padding(vertical = 2.dp)
      .testTag("theme_${theme.code}"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    // 96x64-proportioned miniature UI preview: background + card + accent bar + check badge
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(previewPalette.background)
        .border(
          width = if (selected) 2.5.dp else 1.5.dp,
          color = tileBorderColor,
          shape = RoundedCornerShape(12.dp),
        )
        .padding(7.dp),
    ) {
      // Mini card inside background
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.Center)
          .clip(RoundedCornerShape(7.dp))
          .background(previewPalette.surface)
          .border(1.dp, previewPalette.divider, RoundedCornerShape(7.dp))
          .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // Mini heading line
        Box(
          modifier = Modifier
            .fillMaxWidth(0.65f)
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(previewPalette.textPrimary.copy(alpha = 0.85f)),
        )
        // Mini secondary line
        Box(
          modifier = Modifier
            .fillMaxWidth(0.88f)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(previewPalette.textMuted.copy(alpha = 0.55f)),
        )
        // Mini accent pill button
        Box(
          modifier = Modifier
            .fillMaxWidth(0.55f)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(previewPalette.accent),
        )
      }

      if (selected) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .size(20.dp)
            .clip(CircleShape)
            .background(activePalette.accent)
            .border(1.5.dp, activePalette.surface, CircleShape),
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Wybrany motyw",
            tint = activePalette.onAccent,
            modifier = Modifier.size(13.dp),
          )
        }
      }
    }

    Text(
      text = theme.label,
      style = MaterialTheme.typography.labelSmall,
      fontSize = 12.5.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
      color = if (selected) activePalette.accent else activePalette.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
  }
}
