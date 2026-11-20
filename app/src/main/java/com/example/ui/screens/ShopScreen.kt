package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ShopItemEntity
import com.example.data.model.PurchaseLimitPeriod
import com.example.data.model.calculateLevel
import com.example.ui.GamefyUiState
import com.example.ui.components.LinkifiedText
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(
  uiState: GamefyUiState,
  countPurchasesInPeriod: (ShopItemEntity) -> Int,
  onBuyItem: (ShopItemEntity) -> Unit,
  onSaveItem: (ShopItemEntity) -> Unit,
  onDeleteItem: (Long) -> Unit,
  onCompressImageUri: suspend (Uri) -> String?,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val scope = rememberCoroutineScope()
  var isEditMode by remember { mutableStateOf(false) }
  var expandedEditItemId by remember { mutableStateOf<Long?>(null) }
  var pendingImageItemId by remember { mutableStateOf<Long?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    val targetId = pendingImageItemId ?: return@rememberLauncherForActivityResult
    val targetItem = uiState.shopItems.find { it.id == targetId } ?: return@rememberLauncherForActivityResult
    if (uri != null) {
      scope.launch {
        val dataUrl = onCompressImageUri(uri)
        if (dataUrl != null) {
          onSaveItem(targetItem.copy(imageData = dataUrl))
        }
      }
    }
  }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PixelButton(
      text = if (isEditMode) "Gotowe" else "Edytuj nagrody",
      compact = true,
      containerColor = if (isEditMode) palette.accent else palette.surface,
      contentColor = if (isEditMode) Color.White else palette.textPrimary,
      modifier = Modifier
        .widthIn(max = 640.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("toggle_shop_edit"),
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = null,
          tint = if (isEditMode) Color.White else palette.textPrimary,
          modifier = Modifier.size(16.dp),
        )
      },
      onClick = {
        isEditMode = !isEditMode
        expandedEditItemId = null
      },
    )

    if (isEditMode) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(uiState.shopItems, key = { it.id }) { item ->
          ShopItemEditorRow(
            item = item,
            uiState = uiState,
            isExpanded = expandedEditItemId == item.id,
            onToggleExpand = {
              expandedEditItemId = if (expandedEditItemId == item.id) null else item.id
            },
            onSave = onSaveItem,
            onDelete = { onDeleteItem(item.id) },
            onPickPhoto = {
              pendingImageItemId = item.id
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
          )
        }

        item {
          PixelButton(
            text = "Dodaj nagrodę",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            },
            onClick = {
              onSaveItem(
                ShopItemEntity(
                  icon = "🎁",
                  name = "Nowa nagroda",
                  coinPrice = 50,
                  gemPrice = 0,
                )
              )
            },
          )
        }
      }
    } else {
      BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
      ) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        LazyVerticalGrid(
          columns = GridCells.Fixed(columns),
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(uiState.shopItems, key = { it.id }) { item ->
            val reqAttr = if (item.requiredAttrId > 0L) {
              uiState.attributes.find { it.id == item.requiredAttrId }
            } else null
            val attrLocked = reqAttr != null && calculateLevel(reqAttr.exp).level < item.requiredAttrLevel
            val period = PurchaseLimitPeriod.fromCode(item.limitPeriodCode)
            val boughtCount = if (period != PurchaseLimitPeriod.UNLIMITED) {
              countPurchasesInPeriod(item)
            } else 0
            val limitReached = period != PurchaseLimitPeriod.UNLIMITED && boughtCount >= item.limitMaxCount
            val canAfford = uiState.profile.coins >= item.coinPrice && uiState.profile.gems >= item.gemPrice

            PixelCard(contentPadding = PaddingValues(12.dp)) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                ShopItemThumbnail(imageData = item.imageData, icon = item.icon, sizeDp = 58)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = item.name,
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.textPrimary,
                  textAlign = TextAlign.Center,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  if (item.coinPrice > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      PixelIcon(icon = "coin", size = 15.dp)
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                        text = "${item.coinPrice}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                      )
                    }
                  }
                  if (item.gemPrice > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      PixelIcon(icon = "gem", size = 15.dp)
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                        text = "${item.gemPrice}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                      )
                    }
                  }
                  if (item.coinPrice == 0 && item.gemPrice == 0) {
                    Text(
                      text = "Za darmo",
                      style = MaterialTheme.typography.bodySmall,
                      color = PixelColors.GreenDark,
                    )
                  }
                }

                // Show "masz: X" in Shop instead of "Użyj" button (per user feedback!)
                if (item.ownedQuantity > 0) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.surfaceSubtle,
                  ) {
                    Text(
                      text = "masz: ${item.ownedQuantity}",
                      style = MaterialTheme.typography.labelMedium,
                      color = palette.accent,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                  }
                }

                if (reqAttr != null) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (attrLocked) {
                      Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PixelColors.Red,
                        modifier = Modifier.size(13.dp),
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                      text = "${reqAttr.name} ≥ ${item.requiredAttrLevel}",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (attrLocked) PixelColors.Red else palette.textMuted,
                    )
                  }
                }

                if (period != PurchaseLimitPeriod.UNLIMITED) {
                  Spacer(modifier = Modifier.height(3.dp))
                  Text(
                    text = "Limit $boughtCount/${item.limitMaxCount} ${period.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (limitReached) PixelColors.Red else palette.textMuted,
                  )
                }

                if (item.description.isNotBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  if (item.unlocked) {
                    LinkifiedText(
                      text = item.description,
                      maxLines = 3,
                    )
                  } else {
                    Text(
                      text = "🔒 Opis i link po zakupie",
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textMuted,
                      textAlign = TextAlign.Center,
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))
                PixelButton(
                  text = if (limitReached) "Limit" else "Kup",
                  compact = true,
                  enabled = !limitReached && !attrLocked && canAfford,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("buy_item_${item.id}"),
                  onClick = { onBuyItem(item) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ShopItemThumbnail(
  imageData: String,
  icon: String,
  sizeDp: Int,
) {
  val bitmap = remember(imageData) {
    if (imageData.startsWith("data:image")) {
      runCatching {
        val base64 = imageData.substringAfter("base64,", "")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
      }.getOrNull()
    } else null
  }

  if (bitmap != null) {
    Image(
      bitmap = bitmap,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(sizeDp.dp)
        .clip(RoundedCornerShape(14.dp)),
    )
  } else {
    PixelIcon(icon = icon, size = sizeDp.dp)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopItemEditorRow(
  item: ShopItemEntity,
  uiState: GamefyUiState,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onSave: (ShopItemEntity) -> Unit,
  onDelete: () -> Unit,
  onPickPhoto: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  var icon by remember(item) { mutableStateOf(item.icon) }
  var name by remember(item) { mutableStateOf(item.name) }
  var coins by remember(item) { mutableStateOf(item.coinPrice.toString()) }
  var gems by remember(item) { mutableStateOf(item.gemPrice.toString()) }
  var reqAttrId by remember(item) { mutableStateOf(item.requiredAttrId) }
  var reqLevel by remember(item) { mutableStateOf(item.requiredAttrLevel.toString()) }
  var limitPeriod by remember(item) { mutableStateOf(PurchaseLimitPeriod.fromCode(item.limitPeriodCode)) }
  var limitMax by remember(item) { mutableStateOf(item.limitMaxCount.toString()) }
  var desc by remember(item) { mutableStateOf(item.description) }

  var attrMenuExpanded by remember { mutableStateOf(false) }
  var limitMenuExpanded by remember { mutableStateOf(false) }

  PixelCard(contentPadding = PaddingValues(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ShopItemThumbnail(imageData = item.imageData, icon = item.icon, sizeDp = 36)
      Text(
        text = item.name,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
      PixelButton(
        text = if (isExpanded) "Zwiń" else "Edytuj",
        compact = true,
        containerColor = palette.surfaceSubtle,
        contentColor = palette.textPrimary,
        onClick = onToggleExpand,
      )
      IconButton(onClick = onDelete) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Usuń nagrodę",
          tint = PixelColors.Red,
        )
      }
    }

    if (isExpanded) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = icon,
          onValueChange = { icon = it },
          label = { Text("Ikona") },
          singleLine = true,
          modifier = Modifier.width(80.dp),
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nazwa") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = coins,
          onValueChange = { coins = it.filter(Char::isDigit) },
          label = { Text("Monety") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = gems,
          onValueChange = { gems = it.filter(Char::isDigit) },
          label = { Text("Kryształy") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
          expanded = attrMenuExpanded,
          onExpandedChange = { attrMenuExpanded = !attrMenuExpanded },
          modifier = Modifier.weight(1.3f),
        ) {
          val attrName = uiState.attributes.find { it.id == reqAttrId }?.name ?: "Brak"
          OutlinedTextField(
            value = attrName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Wym. atrybut") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = attrMenuExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = attrMenuExpanded,
            onDismissRequest = { attrMenuExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("Brak") },
              onClick = { reqAttrId = 0L; attrMenuExpanded = false },
            )
            uiState.attributes.forEach { a ->
              DropdownMenuItem(
                text = { Text(a.name) },
                onClick = { reqAttrId = a.id; attrMenuExpanded = false },
              )
            }
          }
        }
        OutlinedTextField(
          value = reqLevel,
          onValueChange = { reqLevel = it.filter(Char::isDigit) },
          label = { Text("Min. poz.") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(0.7f),
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
          expanded = limitMenuExpanded,
          onExpandedChange = { limitMenuExpanded = !limitMenuExpanded },
          modifier = Modifier.weight(1.3f),
        ) {
          OutlinedTextField(
            value = limitPeriod.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Limit zakupów") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = limitMenuExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = limitMenuExpanded,
            onDismissRequest = { limitMenuExpanded = false },
          ) {
            PurchaseLimitPeriod.entries.forEach { p ->
              DropdownMenuItem(
                text = { Text(p.label) },
                onClick = { limitPeriod = p; limitMenuExpanded = false },
              )
            }
          }
        }
        OutlinedTextField(
          value = limitMax,
          onValueChange = { limitMax = it.filter(Char::isDigit) },
          label = { Text("Maks. razy") },
          enabled = limitPeriod != PurchaseLimitPeriod.UNLIMITED,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(0.7f),
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = desc,
        onValueChange = { desc = it },
        label = { Text("Opis i link (odblokowane po zakupie)") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PixelButton(
          text = "Wybierz zdjęcie",
          compact = true,
          containerColor = palette.surfaceSubtle,
          contentColor = palette.textPrimary,
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Image,
              contentDescription = null,
              tint = palette.textPrimary,
              modifier = Modifier.size(16.dp),
            )
          },
          onClick = onPickPhoto,
        )
        if (item.imageData.isNotEmpty()) {
          PixelButton(
            text = "Usuń foto",
            compact = true,
            containerColor = PixelColors.Red,
            onClick = { onSave(item.copy(imageData = "")) },
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      PixelButton(
        text = "Zapisz zmiany",
        compact = true,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
          onSave(
            item.copy(
              icon = icon.ifBlank { "🎁" },
              name = name.trim().ifBlank { "Nagroda" },
              coinPrice = (coins.toIntOrNull() ?: 0).coerceAtLeast(0),
              gemPrice = (gems.toIntOrNull() ?: 0).coerceAtLeast(0),
              requiredAttrId = reqAttrId,
              requiredAttrLevel = (reqLevel.toIntOrNull() ?: 1).coerceAtLeast(1),
              limitPeriodCode = limitPeriod.code,
              limitMaxCount = (limitMax.toIntOrNull() ?: 1).coerceAtLeast(1),
              description = desc.trim(),
            )
          )
        },
      )
    }
  }
}
