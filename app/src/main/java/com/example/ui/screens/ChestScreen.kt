package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ShopItemEntity
import com.example.data.local.parseIdCsv
import com.example.ui.GamefyUiState
import com.example.ui.components.ActiveFilterChipItem
import com.example.ui.components.ActiveFilterChipsRow
import com.example.ui.components.LinkifiedText
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelEmptyStateCard
import com.example.ui.components.PixelSectionHeader
import com.example.ui.components.SearchFilterIconButton
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelFontFamily

private enum class ChestSortOption(val label: String) {
  LAST_ADDED("Ostatnio dodane"),
  PRICE_COINS("Cena (monety)"),
  PRICE_GEMS("Cena (kryształy)"),
  PRICE_TOTAL("Wartość łączna"),
  QUANTITY("Ilość"),
  NAME("Nazwa"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChestScreen(
  uiState: GamefyUiState,
  onUseItem: (ShopItemEntity) -> Unit,
  onOpenShop: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val ownedItems = remember(uiState.shopItems) {
    uiState.shopItems.filter { it.ownedQuantity > 0 }
  }

  var showSearchPanel by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var sortOption by remember { mutableStateOf(ChestSortOption.LAST_ADDED) }
  var sortDescending by remember { mutableStateOf(true) }

  val clearChestFilters: () -> Unit = {
    searchQuery = ""
    sortOption = ChestSortOption.LAST_ADDED
    sortDescending = true
  }

  val isNonDefaultSort = sortOption != ChestSortOption.LAST_ADDED || !sortDescending
  val activeChips = buildList {
    val trimmed = searchQuery.trim()
    if (trimmed.isNotEmpty()) {
      add(
        ActiveFilterChipItem(
          key = "chest_search",
          label = "Szukaj: „$trimmed”",
          onRemove = { searchQuery = "" },
          testTag = "chest_active_chip_search",
        )
      )
    }
    if (isNonDefaultSort) {
      val dirLabel = if (sortDescending) "malejąco" else "rosnąco"
      add(
        ActiveFilterChipItem(
          key = "chest_sort",
          label = "Sortuj: ${sortOption.label} ($dirLabel)",
          onRemove = {
            sortOption = ChestSortOption.LAST_ADDED
            sortDescending = true
          },
          testTag = "chest_active_chip_sort",
        )
      )
    }
  }

  val displayedItems = remember(ownedItems, searchQuery, sortOption, sortDescending) {
    val q = searchQuery.trim()
    val filtered = if (q.isEmpty()) {
      ownedItems
    } else {
      ownedItems.filter { item ->
        item.name.contains(q, ignoreCase = true) ||
          item.description.contains(q, ignoreCase = true)
      }
    }

    val comparator: Comparator<ShopItemEntity> = when (sortOption) {
      ChestSortOption.LAST_ADDED -> compareBy<ShopItemEntity>(
        { parseIdCsv(it.purchaseHistoryCsv).maxOrNull() ?: 0L },
        { it.id },
      )
      ChestSortOption.PRICE_COINS -> compareBy<ShopItemEntity>({ it.coinPrice }, { it.gemPrice }, { it.name.lowercase() })
      ChestSortOption.PRICE_GEMS -> compareBy<ShopItemEntity>({ it.gemPrice }, { it.coinPrice }, { it.name.lowercase() })
      ChestSortOption.PRICE_TOTAL -> compareBy<ShopItemEntity>({ it.coinPrice + it.gemPrice * 100 }, { it.name.lowercase() })
      ChestSortOption.QUANTITY -> compareBy<ShopItemEntity>({ it.ownedQuantity }, { it.name.lowercase() })
      ChestSortOption.NAME -> compareBy<ShopItemEntity> { it.name.lowercase() }
    }

    if (sortDescending) filtered.sortedWith(comparator.reversed()) else filtered.sortedWith(comparator)
  }

  if (ownedItems.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(16.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      PixelEmptyStateCard(
        illustrationRes = R.drawable.img_empty_chest,
        illustrationContentDescription = "Ilustracja pustej skrzyni skarbów",
        title = "Skrzynia jest pusta",
        subtitle = "Kup nagrody w sklepie albo zdobądź przedmioty za ukończone zadania.",
        primaryActionLabel = "Przejdź do sklepu",
        onPrimaryAction = onOpenShop,
        primaryActionTestTag = "empty_chest_go_to_shop_button",
        modifier = Modifier.widthIn(max = 560.dp),
      )
    }
    return
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
      SearchFilterIconButton(
        activeCount = activeChips.size,
        isOpen = showSearchPanel,
        onClick = { showSearchPanel = !showSearchPanel },
        testTag = "chest_filter_toggle",
        contentDescription = "Szukaj i sortuj w skrzyni",
      )

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        border = BorderStroke(1.5.dp, palette.divider),
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(10.dp))
          .clickable(
            role = Role.Button,
            onClickLabel = "Zmień sortowanie w skrzyni",
          ) { showSearchPanel = !showSearchPanel }
          .testTag("chest_sort_summary_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Sortowanie",
              style = MaterialTheme.typography.labelSmall,
              color = palette.textMuted,
            )
            Text(
              text = sortOption.label,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = "${displayedItems.size}/${ownedItems.size}",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = palette.accent,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        border = BorderStroke(2.dp, palette.outline),
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(10.dp))
          .clickable(
            role = Role.Button,
            onClickLabel = if (sortDescending) "Zmień na rosnąco" else "Zmień na malejąco",
          ) { sortDescending = !sortDescending }
          .testTag("chest_sort_direction_button"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (sortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            contentDescription = if (sortDescending) "Sortowanie malejące" else "Sortowanie rosnące",
            tint = palette.accent,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    ActiveFilterChipsRow(
      chips = activeChips,
      onClearAll = clearChestFilters,
      clearButtonTestTag = "chest_clear_filters_button",
    )

    AnimatedVisibility(visible = showSearchPanel) {
      PixelCard(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .padding(horizontal = 16.dp, vertical = 6.dp),
        contentPadding = PaddingValues(12.dp),
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = { Text("Szukaj w skrzyni po nazwie lub opisie") },
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
            .testTag("chest_search_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))
        PixelSectionHeader(title = "Sortuj według")
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          ChestSortOption.entries.forEach { option ->
            FilterChip(
              selected = sortOption == option,
              onClick = {
                if (sortOption == option) {
                  sortDescending = !sortDescending
                } else {
                  sortOption = option
                  sortDescending = option != ChestSortOption.NAME
                }
              },
              label = { Text(option.label) },
              modifier = Modifier.testTag("chest_sort_option_${option.name.lowercase()}"),
            )
          }
        }
      }
    }

    if (displayedItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        contentAlignment = Alignment.TopCenter,
      ) {
        PixelEmptyStateCard(
          illustrationRes = R.drawable.img_empty_chest,
          illustrationContentDescription = "Ilustracja pustych wyników wyszukiwania w skrzyni",
          title = "Brak pasujących przedmiotów",
          subtitle = "W skrzyni nie znaleziono przedmiotów dla podanych kryteriów.",
          primaryActionLabel = "Wyczyść filtry",
          primaryActionTestTag = "chest_empty_clear_filters_button",
          onPrimaryAction = clearChestFilters,
          modifier = Modifier.widthIn(max = 560.dp),
        )
      }
    } else {
      BoxWithConstraints(
        modifier = Modifier.weight(1f).fillMaxWidth(),
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
          items(displayedItems, key = { it.id }) { item ->
            PixelCard(contentPadding = PaddingValues(12.dp)) {
              Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = palette.accent,
                  border = BorderStroke(1.5.dp, palette.outline),
                  modifier = Modifier.align(Alignment.TopEnd),
                ) {
                  Text(
                    text = "×${item.ownedQuantity}",
                    fontFamily = PixelFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  )
                }

                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  ShopItemThumbnail(imageData = item.imageData, icon = item.icon, sizeDp = 56)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  ShopPriceBadges(
                    coinPrice = item.coinPrice,
                    gemPrice = item.gemPrice,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  if (item.description.isNotBlank()) {
                    LinkifiedText(
                      text = item.description,
                      maxLines = 3,
                    )
                  } else {
                    Text(
                      text = "Gotowe do użycia",
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textMuted,
                      maxLines = 1,
                    )
                  }
                  Spacer(modifier = Modifier.height(10.dp))
                  PixelButton(
                    text = "Użyj",
                    compact = false,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("use_chest_item_${item.id}"),
                    onClick = { onUseItem(item) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
