package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ShopItemEntity
import com.example.data.model.OnboardingRewardPref
import com.example.data.model.PurchaseLimitPeriod
import com.example.data.model.REWARD_CATALOG_TEMPLATES
import com.example.data.model.RewardCatalogTemplate
import com.example.data.model.calculateLevel
import com.example.ui.GamefyUiState
import com.example.ui.components.ActiveFilterChipItem
import com.example.ui.components.ActiveFilterChipsRow
import com.example.ui.components.LinkifiedText
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelEmptyStateCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.PixelLockedButton
import com.example.ui.components.PixelSectionHeader
import com.example.ui.components.SearchFilterIconButton
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily
import kotlinx.coroutines.launch

private enum class ShopOwnershipFilter(val label: String) {
  ALL("Wszystkie"),
  OWNED("Posiadam"),
  NOT_OWNED("Nie posiadam"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopScreen(
  uiState: GamefyUiState,
  countPurchasesInPeriod: (ShopItemEntity) -> Int,
  onBuyItem: (ShopItemEntity) -> Unit,
  onSaveItem: (ShopItemEntity) -> Unit,
  onDeleteItem: (Long) -> Unit,
  onCompressImageUri: suspend (Uri) -> String?,
  onAddRewardFromCatalog: (RewardCatalogTemplate, Int) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val scope = rememberCoroutineScope()
  var isEditMode by remember { mutableStateOf(false) }
  var showRewardCatalog by remember { mutableStateOf(false) }
  var expandedEditItemId by remember { mutableStateOf<Long?>(null) }
  var pendingImageItemId by remember { mutableStateOf<Long?>(null) }

  // Search & filter state
  var showShopFilters by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var onlyAvailable by remember { mutableStateOf(false) }
  var ownershipFilter by remember { mutableStateOf(ShopOwnershipFilter.ALL) }
  var minCoinsText by remember { mutableStateOf("") }
  var maxCoinsText by remember { mutableStateOf("") }
  var minGemsText by remember { mutableStateOf("") }
  var maxGemsText by remember { mutableStateOf("") }

  val minCoins = minCoinsText.toIntOrNull()
  val maxCoins = maxCoinsText.toIntOrNull()
  val minGems = minGemsText.toIntOrNull()
  val maxGems = maxGemsText.toIntOrNull()

  val clearShopFilters: () -> Unit = {
    searchQuery = ""
    onlyAvailable = false
    ownershipFilter = ShopOwnershipFilter.ALL
    minCoinsText = ""
    maxCoinsText = ""
    minGemsText = ""
    maxGemsText = ""
  }

  val activeShopFilterChips = buildList {
    val trimmed = searchQuery.trim()
    if (trimmed.isNotEmpty()) {
      add(
        ActiveFilterChipItem(
          key = "shop_search",
          label = "Szukaj: „$trimmed”",
          onRemove = { searchQuery = "" },
          testTag = "shop_active_chip_search",
        )
      )
    }
    if (onlyAvailable) {
      add(
        ActiveFilterChipItem(
          key = "shop_available",
          label = "Tylko dostępne",
          onRemove = { onlyAvailable = false },
          testTag = "shop_active_chip_available",
        )
      )
    }
    if (ownershipFilter != ShopOwnershipFilter.ALL) {
      add(
        ActiveFilterChipItem(
          key = "shop_ownership",
          label = ownershipFilter.label,
          onRemove = { ownershipFilter = ShopOwnershipFilter.ALL },
          testTag = "shop_active_chip_ownership",
        )
      )
    }
    if (minCoins != null || maxCoins != null) {
      val rangeLabel = when {
        minCoins != null && maxCoins != null -> "Monety: $minCoins–$maxCoins"
        minCoins != null -> "Monety: od $minCoins"
        else -> "Monety: do $maxCoins"
      }
      add(
        ActiveFilterChipItem(
          key = "shop_coins_range",
          label = rangeLabel,
          onRemove = {
            minCoinsText = ""
            maxCoinsText = ""
          },
          testTag = "shop_active_chip_coins",
        )
      )
    }
    if (minGems != null || maxGems != null) {
      val rangeLabel = when {
        minGems != null && maxGems != null -> "Kryształy: $minGems–$maxGems"
        minGems != null -> "Kryształy: od $minGems"
        else -> "Kryształy: do $maxGems"
      }
      add(
        ActiveFilterChipItem(
          key = "shop_gems_range",
          label = rangeLabel,
          onRemove = {
            minGemsText = ""
            maxGemsText = ""
          },
          testTag = "shop_active_chip_gems",
        )
      )
    }
  }

  val existingRewardTemplateIds = remember(uiState.shopItems) {
    uiState.shopItems.mapNotNull { it.sourceRewardTemplateId }.toSet()
  }

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

  val filteredShopItems = remember(
    uiState.shopItems,
    uiState.profile.coins,
    uiState.profile.gems,
    uiState.attributes,
    searchQuery,
    onlyAvailable,
    ownershipFilter,
    minCoins,
    maxCoins,
    minGems,
    maxGems,
  ) {
    val q = searchQuery.trim()
    uiState.shopItems.filter { item ->
      val queryOk = q.isEmpty() ||
        item.name.contains(q, ignoreCase = true) ||
        item.description.contains(q, ignoreCase = true)

      val ownershipOk = when (ownershipFilter) {
        ShopOwnershipFilter.ALL -> true
        ShopOwnershipFilter.OWNED -> item.ownedQuantity > 0
        ShopOwnershipFilter.NOT_OWNED -> item.ownedQuantity == 0
      }

      val coinsOk = (minCoins == null || item.coinPrice >= minCoins) &&
        (maxCoins == null || item.coinPrice <= maxCoins)
      val gemsOk = (minGems == null || item.gemPrice >= minGems) &&
        (maxGems == null || item.gemPrice <= maxGems)

      val availableOk = if (!onlyAvailable) {
        true
      } else {
        val reqAttr = if (item.requiredAttrId > 0L) {
          uiState.attributes.find { it.id == item.requiredAttrId }
        } else null
        val attrLevel = reqAttr?.let { calculateLevel(it.exp).level } ?: 0
        val attrLocked = reqAttr != null && attrLevel < item.requiredAttrLevel
        val period = PurchaseLimitPeriod.fromCode(item.limitPeriodCode)
        val boughtCount = if (period != PurchaseLimitPeriod.UNLIMITED) countPurchasesInPeriod(item) else 0
        val limitReached = period != PurchaseLimitPeriod.UNLIMITED && boughtCount >= item.limitMaxCount
        val canAfford = uiState.profile.coins >= item.coinPrice && uiState.profile.gems >= item.gemPrice
        !attrLocked && !limitReached && canAfford
      }

      queryOk && ownershipOk && coinsOk && gemsOk && availableOk
    }
  }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier
        .widthIn(max = 640.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (!isEditMode && uiState.shopItems.isNotEmpty()) {
        SearchFilterIconButton(
          activeCount = activeShopFilterChips.size,
          isOpen = showShopFilters,
          onClick = { showShopFilters = !showShopFilters },
          testTag = "shop_filter_toggle",
          contentDescription = "Szukaj i filtruj w sklepie",
        )
      }
      PixelButton(
        text = if (isEditMode) "Gotowe" else "Edytuj nagrody",
        compact = true,
        containerColor = if (isEditMode) palette.accent else palette.surface,
        contentColor = if (isEditMode) Color.White else palette.textPrimary,
        modifier = Modifier
          .weight(1f)
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
          if (isEditMode) showShopFilters = false
        },
      )
      PixelButton(
        text = "Katalog nagród",
        compact = true,
        containerColor = palette.surfaceSubtle,
        contentColor = palette.accent,
        modifier = Modifier
          .weight(1f)
          .testTag("open_reward_catalog_button"),
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.CardGiftcard,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(16.dp),
          )
        },
        onClick = { showRewardCatalog = true },
      )
    }

    if (!isEditMode && uiState.shopItems.isNotEmpty()) {
      ActiveFilterChipsRow(
        chips = activeShopFilterChips,
        onClearAll = clearShopFilters,
        clearButtonTestTag = "shop_clear_filters_button",
      )

      AnimatedVisibility(visible = showShopFilters) {
        PixelCard(
          modifier = Modifier
            .widthIn(max = 640.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
          contentPadding = PaddingValues(12.dp),
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Szukaj nagród po nazwie lub opisie") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Wyczyść wyszukiwanie")
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("shop_search_input"),
          )

          Spacer(modifier = Modifier.height(10.dp))
          PixelSectionHeader(title = "Dostępność i posiadanie")
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            FilterChip(
              selected = onlyAvailable,
              onClick = { onlyAvailable = !onlyAvailable },
              label = { Text("Tylko dostępne") },
              modifier = Modifier.testTag("shop_filter_only_available"),
            )
            ShopOwnershipFilter.entries.forEach { mode ->
              FilterChip(
                selected = ownershipFilter == mode,
                onClick = { ownershipFilter = mode },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("shop_filter_ownership_${mode.name.lowercase()}"),
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          PixelSectionHeader(title = "Zakres kosztów")
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PixelIcon(icon = "coin", size = 20.dp)
            OutlinedTextField(
              value = minCoinsText,
              onValueChange = { minCoinsText = it.filter(Char::isDigit) },
              label = { Text("Monety od") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier
                .weight(1f)
                .testTag("shop_filter_min_coins"),
            )
            OutlinedTextField(
              value = maxCoinsText,
              onValueChange = { maxCoinsText = it.filter(Char::isDigit) },
              label = { Text("Monety do") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier
                .weight(1f)
                .testTag("shop_filter_max_coins"),
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PixelIcon(icon = "gem", size = 20.dp)
            OutlinedTextField(
              value = minGemsText,
              onValueChange = { minGemsText = it.filter(Char::isDigit) },
              label = { Text("Kryształy od") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier
                .weight(1f)
                .testTag("shop_filter_min_gems"),
            )
            OutlinedTextField(
              value = maxGemsText,
              onValueChange = { maxGemsText = it.filter(Char::isDigit) },
              label = { Text("Kryształy do") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier
                .weight(1f)
                .testTag("shop_filter_max_gems"),
            )
          }
        }
      }
    }

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
            text = "Nowa nagroda",
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
                  icon = "gift",
                  name = "Nowa nagroda",
                  coinPrice = 50,
                  gemPrice = 0,
                )
              )
            },
          )
        }
      }
    } else if (uiState.shopItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter,
      ) {
        PixelEmptyStateCard(
          illustrationRes = R.drawable.img_empty_shop,
          illustrationContentDescription = "Ilustracja pustego kramu sklepowego",
          title = "Twój sklep jest jeszcze pusty",
          subtitle = "Dodaj własne nagrody albo wybierz gotowe propozycje z katalogu nagród. Wszystkie ceny i nazwy możesz dowolnie edytować.",
          primaryActionLabel = "Dodaj nagrodę",
          primaryActionTestTag = "empty_shop_add_reward_button",
          onPrimaryAction = {
            onSaveItem(
              ShopItemEntity(
                icon = "gift",
                name = "Nowa nagroda",
                coinPrice = 50,
                gemPrice = 0,
              )
            )
            isEditMode = true
          },
          secondaryActionLabel = "Otwórz katalog",
          secondaryActionTestTag = "empty_shop_open_catalog_button",
          onSecondaryAction = { showRewardCatalog = true },
          modifier = Modifier.widthIn(max = 560.dp),
        )
      }
    } else if (filteredShopItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter,
      ) {
        PixelEmptyStateCard(
          illustrationRes = R.drawable.img_empty_shop,
          illustrationContentDescription = "Ilustracja pustych wyników filtrowania sklepu",
          title = "Brak pasujących nagród",
          subtitle = "Żadna nagroda w sklepie nie spełnia wybranych filtrów.",
          primaryActionLabel = "Wyczyść filtry",
          primaryActionTestTag = "empty_filtered_shop_clear_button",
          onPrimaryAction = clearShopFilters,
          modifier = Modifier.widthIn(max = 560.dp),
        )
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
          items(filteredShopItems, key = { it.id }) { item ->
            val reqAttr = if (item.requiredAttrId > 0L) {
              uiState.attributes.find { it.id == item.requiredAttrId }
            } else null
            val attrLevel = reqAttr?.let { calculateLevel(it.exp).level } ?: 0
            val attrLocked = reqAttr != null && attrLevel < item.requiredAttrLevel
            val period = PurchaseLimitPeriod.fromCode(item.limitPeriodCode)
            val boughtCount = if (period != PurchaseLimitPeriod.UNLIMITED) {
              countPurchasesInPeriod(item)
            } else 0
            val limitReached = period != PurchaseLimitPeriod.UNLIMITED && boughtCount >= item.limitMaxCount
            val missingCoins = uiState.profile.coins < item.coinPrice
            val missingGems = uiState.profile.gems < item.gemPrice
            val isUnavailable = attrLocked || limitReached || missingCoins || missingGems

            val lockOverlayTitle: String? = when {
              attrLocked -> "Wymagany poziom ${item.requiredAttrLevel}"
              limitReached -> "Limit wyczerpany"
              missingCoins && missingGems -> "Za mało środków"
              missingGems -> "Za mało kryształów"
              missingCoins -> "Za mało monet"
              else -> null
            }
            val lockOverlaySubtitle: String? = when {
              attrLocked && reqAttr != null -> reqAttr.name
              limitReached -> "$boughtCount/${item.limitMaxCount} ${period.label}"
              missingCoins && missingGems -> "Brak monet i kryształów"
              missingGems -> "Masz ${uiState.profile.gems}/${item.gemPrice}"
              missingCoins -> "Masz ${uiState.profile.coins}/${item.coinPrice}"
              else -> null
            }

            PixelCard(
              contentPadding = PaddingValues(12.dp),
              containerColor = if (isUnavailable) palette.surfaceSubtle else palette.surface,
              borderColor = if (isUnavailable) palette.divider else palette.outline.copy(alpha = 0.35f),
              modifier = Modifier.testTag("shop_tile_${item.id}"),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  ShopItemThumbnail(
                    imageData = item.imageData,
                    icon = item.icon,
                    sizeDp = 58,
                    grayscale = isUnavailable,
                  )

                  if (item.ownedQuantity > 0) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (isUnavailable) palette.surface.copy(alpha = 0.9f) else palette.accent.copy(alpha = 0.16f),
                      border = BorderStroke(
                        1.dp,
                        if (isUnavailable) palette.divider else palette.accent.copy(alpha = 0.45f),
                      ),
                      modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                      Text(
                        text = "×${item.ownedQuantity}",
                        fontFamily = PixelFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isUnavailable) palette.textMuted else palette.accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isUnavailable) 0.65f else 1f),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUnavailable) palette.textMuted else palette.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )

                  Spacer(modifier = Modifier.height(6.dp))
                  ShopPriceBadges(
                    coinPrice = item.coinPrice,
                    gemPrice = item.gemPrice,
                    missingCoins = missingCoins,
                    missingGems = missingGems,
                    muted = isUnavailable,
                  )

                  if (reqAttr != null && !attrLocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "${reqAttr.name} · poziom ${item.requiredAttrLevel}+",
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.textMuted,
                      textAlign = TextAlign.Center,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }

                  if (period != PurchaseLimitPeriod.UNLIMITED && !limitReached) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                      text = "Limit $boughtCount/${item.limitMaxCount} ${period.label}",
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.textMuted,
                    )
                  }

                  if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinkifiedText(
                      text = item.description,
                      maxLines = 3,
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))
                if (isUnavailable) {
                  val desc = buildString {
                    append("Przedmiot zablokowany")
                    if (!lockOverlayTitle.isNullOrBlank()) {
                      append(": ")
                      append(lockOverlayTitle)
                    }
                    if (!lockOverlaySubtitle.isNullOrBlank()) {
                      append(" (")
                      append(lockOverlaySubtitle)
                      append(")")
                    }
                  }
                  PixelLockedButton(
                    contentDescription = desc,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("buy_item_${item.id}"),
                  )
                  if (lockOverlayTitle != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val reasonAccent = if (attrLocked || limitReached) PixelColors.Red else PixelColors.Warning
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = reasonAccent.copy(alpha = 0.12f),
                      border = BorderStroke(1.dp, reasonAccent.copy(alpha = 0.4f)),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_item_lock_reason_${item.id}"),
                    ) {
                      Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                      ) {
                        Text(
                          text = lockOverlayTitle,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = reasonAccent,
                          textAlign = TextAlign.Center,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis,
                        )
                        if (!lockOverlaySubtitle.isNullOrBlank()) {
                          Text(
                            text = lockOverlaySubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                      }
                    }
                  }
                } else {
                  PixelButton(
                    text = "Kup",
                    compact = false,
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

  if (showRewardCatalog) {
    RewardCatalogDialog(
      existingTemplateIds = existingRewardTemplateIds,
      onAddTemplate = onAddRewardFromCatalog,
      onDismiss = { showRewardCatalog = false },
    )
  }
}

@Composable
private fun RewardCatalogDialog(
  existingTemplateIds: Set<String>,
  onAddTemplate: (RewardCatalogTemplate, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val customPrices = remember {
    mutableStateMapOf<String, String>().apply {
      REWARD_CATALOG_TEMPLATES.forEach { put(it.id, it.defaultCoinPrice.toString()) }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(
          text = "Katalog gotowych nagród",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Wybierz nagrody i dopasuj ich cenę w monetach.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier.height(420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OnboardingRewardPref.entries.forEach { category ->
          val itemsInCat = REWARD_CATALOG_TEMPLATES.filter { it.category == category }
          item(key = "cat_${category.id}") {
            Text(
              text = category.label,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = palette.accent,
              modifier = Modifier.padding(top = 6.dp),
            )
          }
          items(itemsInCat, key = { it.id }) { tpl ->
            val alreadyInShop = tpl.id in existingTemplateIds
            val priceText = customPrices[tpl.id] ?: tpl.defaultCoinPrice.toString()
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = palette.surfaceSubtle,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  PixelIcon(icon = tpl.icon, size = 22.dp)
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = tpl.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = palette.textPrimary,
                    )
                    if (tpl.description.isNotBlank()) {
                      Text(
                        text = tpl.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted,
                      )
                    }
                  }
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  OutlinedTextField(
                    value = priceText,
                    onValueChange = { customPrices[tpl.id] = it.filter(Char::isDigit) },
                    label = { Text("Monety") },
                    singleLine = true,
                    enabled = !alreadyInShop,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(108.dp),
                  )
                  Spacer(modifier = Modifier.weight(1f))
                  if (alreadyInShop) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PixelColors.GreenDark,
                        modifier = Modifier.size(16.dp),
                      )
                      Text(
                        text = "W sklepie",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PixelColors.GreenDark,
                      )
                    }
                  } else {
                    PixelButton(
                      text = "Dodaj",
                      compact = true,
                      modifier = Modifier.testTag("add_catalog_reward_${tpl.id}"),
                      onClick = {
                        val price = (priceText.toIntOrNull() ?: tpl.defaultCoinPrice).coerceAtLeast(0)
                        onAddTemplate(tpl, price)
                      },
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Zamknij")
      }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopPriceBadges(
  coinPrice: Int,
  gemPrice: Int,
  modifier: Modifier = Modifier,
  missingCoins: Boolean = false,
  missingGems: Boolean = false,
  muted: Boolean = false,
) {
  val palette = LocalGamefyPalette.current
  val priceDescription = buildString {
    val parts = mutableListOf<String>()
    if (coinPrice > 0) parts += "$coinPrice monet"
    if (gemPrice > 0) parts += "$gemPrice kryształów"
    if (parts.isEmpty()) append("Za darmo") else append("Cena: ${parts.joinToString(", ")}")
  }

  FlowRow(
    modifier = modifier.semantics(mergeDescendants = true) {
      contentDescription = priceDescription
    },
    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    if (coinPrice > 0) {
      val coinBorder = when {
        missingCoins -> PixelColors.Red.copy(alpha = 0.7f)
        muted -> palette.divider
        else -> PixelColors.Gold.copy(alpha = 0.75f)
      }
      val coinBg = when {
        missingCoins -> PixelColors.Red.copy(alpha = 0.12f)
        muted -> palette.surface
        else -> PixelColors.Gold.copy(alpha = 0.18f)
      }
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = coinBg,
        border = BorderStroke(1.dp, coinBorder),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          PixelIcon(icon = "coin", size = 16.dp, grayscale = muted && !missingCoins)
          Text(
            text = "$coinPrice",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (missingCoins) PixelColors.Red else palette.textPrimary,
          )
        }
      }
    }

    if (gemPrice > 0) {
      val gemBorder = when {
        missingGems -> PixelColors.Red.copy(alpha = 0.7f)
        muted -> palette.divider
        else -> PixelColors.GemBlue.copy(alpha = 0.75f)
      }
      val gemBg = when {
        missingGems -> PixelColors.Red.copy(alpha = 0.12f)
        muted -> palette.surface
        else -> PixelColors.GemBlue.copy(alpha = 0.16f)
      }
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = gemBg,
        border = BorderStroke(1.dp, gemBorder),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          PixelIcon(icon = "gem", size = 16.dp, grayscale = muted && !missingGems)
          Text(
            text = "$gemPrice",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (missingGems) PixelColors.Red else PixelColors.GemBlue,
          )
        }
      }
    }

    if (coinPrice == 0 && gemPrice == 0) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = PixelColors.Green.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, PixelColors.Green.copy(alpha = 0.45f)),
      ) {
        Text(
          text = "Za darmo",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = PixelColors.GreenDark,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }
    }
  }
}

@Composable
fun ShopItemThumbnail(
  imageData: String,
  icon: String,
  sizeDp: Int,
  grayscale: Boolean = false,
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
  val grayFilter = remember {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
  }

  if (bitmap != null) {
    Image(
      bitmap = bitmap,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      colorFilter = if (grayscale) grayFilter else null,
      modifier = Modifier
        .size(sizeDp.dp)
        .alpha(if (grayscale) 0.55f else 1f)
        .clip(RoundedCornerShape(14.dp)),
    )
  } else {
    PixelIcon(
      icon = icon,
      size = sizeDp.dp,
      grayscale = grayscale,
      modifier = Modifier.alpha(if (grayscale) 0.58f else 1f),
    )
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
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(10.dp))
          .clickable(
            role = Role.Button,
            onClickLabel = if (isExpanded) "Zwiń edycję nagrody ${item.name}" else "Rozwiń edycję nagrody ${item.name}",
            onClick = onToggleExpand,
          )
          .testTag("shop_editor_toggle_${item.id}")
          .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ShopItemThumbnail(imageData = item.imageData, icon = item.icon, sizeDp = 36)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = if (isExpanded) "Dotknij, aby zwinąć" else "Dotknij, aby edytować",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
          )
        }
        Icon(
          imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (isExpanded) "Zwiń edycję" else "Rozwiń edycję",
          tint = palette.accent,
          modifier = Modifier.size(24.dp),
        )
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .size(48.dp)
          .testTag("shop_editor_delete_${item.id}"),
      ) {
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
          modifier = Modifier.weight(1.1f),
        ) {
          val attrName = uiState.attributes.find { it.id == reqAttrId }?.name ?: "Brak"
          OutlinedTextField(
            value = attrName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Wymagany atrybut") },
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
          label = { Text("Wymagany poziom") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(0.9f),
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
          label = { Text("Ile razy") },
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
            text = "Usuń zdjęcie",
            compact = true,
            containerColor = PixelColors.Red,
            onClick = { onSave(item.copy(imageData = "")) },
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      PixelButton(
        text = "Zapisz",
        compact = true,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
          onSave(
            item.copy(
              icon = icon.ifBlank { "gift" },
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
