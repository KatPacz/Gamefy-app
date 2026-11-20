package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.AttributeEntity
import com.example.data.local.FolderEntity
import com.example.data.local.TagEntity
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelIcon
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import kotlinx.coroutines.launch

private val ATTRIBUTE_ICON_PRESETS = listOf("💪", "📚", "🌿", "🎨", "🎯", "💼", "⭐", "⚡")
private val FOLDER_ICON_PRESETS = listOf("📁", "🏠", "💼", "💪", "📚", "🌿", "🎯", "🎨")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttributeEditorDialog(
  existingAttr: AttributeEntity?,
  onSaveNew: (String, String) -> Unit,
  onUpdate: (AttributeEntity) -> Unit,
  onDelete: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  var icon by remember(existingAttr) { mutableStateOf(existingAttr?.icon ?: "⭐") }
  var name by remember(existingAttr) { mutableStateOf(existingAttr?.name ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (existingAttr == null) "Nowy atrybut" else "Edytuj atrybut")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Wybierz ikonę lub wpisz własną:",
          style = MaterialTheme.typography.labelMedium,
          color = palette.textMuted,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ATTRIBUTE_ICON_PRESETS.forEach { preset ->
            val selected = icon == preset
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.surfaceSubtle,
              border = BorderStroke(1.5.dp, if (selected) palette.accent else palette.divider),
              modifier = Modifier.clickable { icon = preset },
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                PixelIcon(icon = preset, size = 24.dp)
              }
            }
          }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = icon,
            onValueChange = { icon = it },
            label = { Text("Ikona") },
            singleLine = true,
            modifier = Modifier.width(84.dp),
          )
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nazwa atrybutu") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        if (existingAttr != null) {
          TextButton(
            onClick = {
              onDelete(existingAttr.id)
              onDismiss()
            },
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = null,
              tint = PixelColors.Red,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Usuń ten atrybut", color = PixelColors.Red)
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Anuluj") }
    },
    confirmButton = {
      PixelButton(
        text = if (existingAttr == null) "Dodaj" else "Zapisz",
        compact = true,
        enabled = name.isNotBlank(),
        onClick = {
          val cleanName = name.trim()
          val cleanIcon = icon.ifBlank { "⭐" }
          if (cleanName.isNotEmpty()) {
            if (existingAttr == null) {
              onSaveNew(cleanIcon, cleanName)
            } else {
              onUpdate(existingAttr.copy(icon = cleanIcon, name = cleanName))
            }
            onDismiss()
          }
        },
      )
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderManagerDialog(
  folders: List<FolderEntity>,
  onAddFolder: (String, String) -> Unit,
  onUpdateFolder: (FolderEntity) -> Unit,
  onDeleteFolder: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  var newIcon by remember { mutableStateOf("📁") }
  var newName by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Zarządzanie folderami (${folders.size})") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FOLDER_ICON_PRESETS.forEach { preset ->
            val selected = newIcon == preset
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.surfaceSubtle,
              border = BorderStroke(1.5.dp, if (selected) palette.accent else palette.divider),
              modifier = Modifier.clickable { newIcon = preset },
            ) {
              Row(modifier = Modifier.padding(6.dp)) {
                PixelIcon(icon = preset, size = 20.dp)
              }
            }
          }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = newIcon,
            onValueChange = { newIcon = it },
            label = { Text("Ikona") },
            singleLine = true,
            modifier = Modifier.width(68.dp),
          )
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Nowy folder") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              val clean = newName.trim()
              if (clean.isNotEmpty()) {
                onAddFolder(newIcon.ifBlank { "📁" }, clean)
                newName = ""
              }
            },
          ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj folder")
          }
        }

        HorizontalDivider(color = palette.divider)

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(folders, key = { it.id }) { folder ->
            var editedIcon by remember(folder) { mutableStateOf(folder.icon) }
            var editedName by remember(folder) { mutableStateOf(folder.name) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedTextField(
                value = editedIcon,
                onValueChange = {
                  editedIcon = it
                  onUpdateFolder(folder.copy(icon = it.ifBlank { "📁" }))
                },
                singleLine = true,
                modifier = Modifier.width(60.dp),
              )
              OutlinedTextField(
                value = editedName,
                onValueChange = {
                  editedName = it
                  if (it.isNotBlank()) onUpdateFolder(folder.copy(name = it))
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              IconButton(onClick = { onDeleteFolder(folder.id) }) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Usuń folder",
                  tint = PixelColors.Red,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Zamknij") }
    },
  )
}

@Composable
fun TagManagerDialog(
  tags: List<TagEntity>,
  onCreateTag: suspend (String) -> TagEntity,
  onUpdateTag: (TagEntity) -> Unit,
  onDeleteTag: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var query by remember { mutableStateOf("") }
  var newTagName by remember { mutableStateOf("") }

  val filteredTags = remember(tags, query) {
    val q = query.trim().removePrefix("#")
    if (q.isEmpty()) tags else tags.filter { it.name.contains(q, ignoreCase = true) }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edycja tagów (${tags.size})") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("Szukaj tagu...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = newTagName,
            onValueChange = { newTagName = it },
            label = { Text("Nowy tag") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              val clean = newTagName.trim().removePrefix("#")
              if (clean.isNotEmpty()) {
                scope.launch {
                  onCreateTag(clean)
                  newTagName = ""
                }
              }
            },
          ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj tag")
          }
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(filteredTags, key = { it.id }) { tag ->
            var editedName by remember(tag) { mutableStateOf(tag.name) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedTextField(
                value = editedName,
                onValueChange = {
                  editedName = it
                  val clean = it.trim().removePrefix("#")
                  if (clean.isNotEmpty()) {
                    onUpdateTag(tag.copy(name = clean))
                  }
                },
                prefix = { Text("#") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              IconButton(onClick = { onDeleteTag(tag.id) }) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Usuń tag",
                  tint = PixelColors.Red,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Zamknij") }
    },
  )
}
