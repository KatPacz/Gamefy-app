package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShopItemEntity
import com.example.ui.GamefyUiState
import com.example.ui.components.LinkifiedText
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelFontFamily

@Composable
fun ChestScreen(
  uiState: GamefyUiState,
  onUseItem: (ShopItemEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val ownedItems = remember(uiState.shopItems) {
    uiState.shopItems.filter { it.ownedQuantity > 0 }
  }

  if (ownedItems.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(16.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      PixelCard(modifier = Modifier.widthIn(max = 640.dp)) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          PixelIcon(icon = "chest", size = 56.dp)
          Text(
            text = "Skrzynia jest pusta",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
          )
          Text(
            text = "Kup nagrody w sklepie albo zdobądź przedmioty za ukończone zadania.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
    return
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    val columns = if (maxWidth >= 600.dp) 3 else 2
    LazyVerticalGrid(
      columns = GridCells.Fixed(columns),
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 840.dp),
      contentPadding = PaddingValues(16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(ownedItems, key = { it.id }) { item ->
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(4.dp))
              if (item.description.isNotBlank()) {
                LinkifiedText(
                  text = item.description.lineSequence().firstOrNull().orEmpty(),
                  maxLines = 1,
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
                compact = true,
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
