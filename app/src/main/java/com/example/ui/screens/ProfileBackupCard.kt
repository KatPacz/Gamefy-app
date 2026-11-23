package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.backupDaysSince
import com.example.data.model.formatDetailedBackupStatus
import com.example.data.model.formatLastBackupStatus
import com.example.data.model.isBackupOverdueWarning
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.TutorialTargetIds
import com.example.ui.components.tutorialTarget
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors

private val WarningButtonInk = Color(0xFF1C1305)

@Composable
fun ProfileBackupStatusCard(
  lastBackupExportTimestamp: Long,
  onCreateBackup: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val now = System.currentTimeMillis()
  val isWarning = isBackupOverdueWarning(lastBackupExportTimestamp, now)
  val statusText = if (lastBackupExportTimestamp <= 0L) {
    "Brak kopii zapasowej"
  } else {
    formatLastBackupStatus(lastBackupExportTimestamp, now)
  }

  val cardBg = if (isWarning) {
    if (palette.isHighContrast) {
      palette.surface
    } else if (palette.isDark) {
      PixelColors.AmberDarkBg
    } else {
      PixelColors.AmberLightBg
    }
  } else {
    palette.surface
  }
  val cardBorder = if (isWarning) PixelColors.Amber else palette.divider
  val badgeBg = if (isWarning) {
    PixelColors.Amber.copy(alpha = 0.22f)
  } else {
    PixelColors.Green.copy(alpha = 0.16f)
  }
  val primaryTextColor = if (isWarning && !palette.isDark && !palette.isHighContrast) {
    PixelColors.AmberInkLight
  } else {
    palette.textPrimary
  }

  PixelCard(
    modifier = modifier.testTag(
      if (isWarning) "profile_backup_card_warning" else "profile_backup_card_ok"
    ),
    containerColor = cardBg,
    borderColor = cardBorder,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(badgeBg),
      ) {
        PixelIcon(
          icon = if (isWarning) "warning" else "save",
          size = 24.dp,
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = "Kopia zapasowa",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = primaryTextColor,
        )
        Text(
          text = statusText,
          style = MaterialTheme.typography.bodySmall,
          fontSize = 13.sp,
          fontWeight = if (isWarning) FontWeight.SemiBold else FontWeight.Normal,
          color = if (isWarning) primaryTextColor else palette.textMuted,
          modifier = Modifier.testTag("profile_backup_status_text"),
        )
      }

      PixelButton(
        text = "Zapisz kopię",
        compact = true,
        containerColor = if (isWarning) PixelColors.Gold else palette.accent,
        contentColor = if (isWarning) WarningButtonInk else palette.onAccent,
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            tint = if (isWarning) WarningButtonInk else palette.onAccent,
            modifier = Modifier.size(16.dp),
          )
        },
        modifier = Modifier.testTag("profile_quick_backup_button"),
        onClick = onCreateBackup,
      )
    }
  }
}

@Composable
fun SettingsBackupWarningBanner(
  lastBackupExportTimestamp: Long,
  onSaveBackup: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val now = System.currentTimeMillis()
  val days = backupDaysSince(lastBackupExportTimestamp, now)

  val message = if (days == null) {
    "Nie masz jeszcze kopii zapasowej. Dane są tylko na tym telefonie."
  } else {
    "Ostatnia kopia powstała $days dni temu. Dane są tylko na tym telefonie."
  }

  val bannerBg = if (palette.isHighContrast) {
    palette.surface
  } else if (palette.isDark) {
    PixelColors.AmberDarkBg
  } else {
    PixelColors.AmberLightBg
  }
  val bannerInk = if (palette.isHighContrast || palette.isDark) {
    palette.textPrimary
  } else {
    PixelColors.AmberInkLight
  }

  PixelCard(
    modifier = modifier.testTag("settings_backup_warning_banner"),
    containerColor = bannerBg,
    borderColor = PixelColors.Amber,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(PixelColors.Amber.copy(alpha = 0.22f)),
      ) {
        PixelIcon(icon = "warning", size = 24.dp)
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = bannerInk,
        )
        PixelButton(
          text = "Zapisz kopię",
          compact = true,
          containerColor = PixelColors.Gold,
          contentColor = WarningButtonInk,
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = null,
              tint = WarningButtonInk,
              modifier = Modifier.size(16.dp),
            )
          },
          modifier = Modifier.testTag("settings_banner_save_backup_button"),
          onClick = onSaveBackup,
        )
      }
    }
  }
}

@Composable
fun SettingsBackupSectionCard(
  lastBackupExportTimestamp: Long,
  onSaveBackup: () -> Unit,
  onLoadBackup: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val now = System.currentTimeMillis()
  val isWarning = isBackupOverdueWarning(lastBackupExportTimestamp, now)
  val detailedStatus = formatDetailedBackupStatus(lastBackupExportTimestamp, now)

  val statusBg = if (isWarning) {
    if (palette.isHighContrast) {
      palette.surfaceSubtle
    } else if (palette.isDark) {
      PixelColors.AmberDarkBg
    } else {
      PixelColors.AmberLightBg
    }
  } else {
    if (palette.isHighContrast) {
      palette.surfaceSubtle
    } else {
      PixelColors.Green.copy(alpha = 0.14f)
    }
  }
  val statusBorder = if (isWarning) PixelColors.Amber else PixelColors.GreenDark
  val statusInk = if (isWarning && !palette.isDark && !palette.isHighContrast) {
    PixelColors.AmberInkLight
  } else {
    palette.textPrimary
  }

  PixelCard(
    modifier = modifier
      .tutorialTarget(TutorialTargetIds.BACKUP_CARD)
      .testTag("settings_backup_section_card"),
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = statusBg,
      border = BorderStroke(1.5.dp, statusBorder),
      modifier = Modifier
        .fillMaxWidth()
        .testTag(if (isWarning) "settings_backup_status_warning" else "settings_backup_status_ok"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PixelIcon(
          icon = if (isWarning) "warning" else "save",
          size = 22.dp,
        )
        Text(
          text = detailedStatus,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = statusInk,
          modifier = Modifier.weight(1f),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "Dane są tylko na tym telefonie. Kopię zapiszesz na urządzeniu lub w folderze Dysku Google.",
      style = MaterialTheme.typography.bodySmall,
      fontSize = 13.sp,
      color = palette.textMuted,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      PixelButton(
        text = "Zapisz kopię",
        compact = true,
        containerColor = palette.accent,
        contentColor = palette.onAccent,
        modifier = Modifier
          .weight(1f)
          .testTag("settings_save_backup_button"),
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            tint = palette.onAccent,
            modifier = Modifier.size(18.dp),
          )
        },
        onClick = onSaveBackup,
      )
      PixelButton(
        text = "Wczytaj kopię",
        compact = true,
        containerColor = palette.surface,
        contentColor = palette.textPrimary,
        modifier = Modifier
          .weight(1f)
          .testTag("settings_load_backup_button"),
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = palette.textPrimary,
            modifier = Modifier.size(18.dp),
          )
        },
        onClick = onLoadBackup,
      )
    }
  }
}
