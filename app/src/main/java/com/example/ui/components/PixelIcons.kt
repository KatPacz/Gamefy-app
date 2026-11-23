package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val PIXEL_PALETTE_ARGB: Map<Char, Int> = mapOf(
  'k' to 0xFF2B2140.toInt(),
  'w' to 0xFFFFFFFF.toInt(),
  'y' to 0xFFFFC933.toInt(),
  'o' to 0xFFF59A23.toInt(),
  'r' to 0xFFE5484D.toInt(),
  'p' to 0xFFFF8FA3.toInt(),
  'b' to 0xFF3B82F6.toInt(),
  'c' to 0xFF7DD3FC.toInt(),
  'g' to 0xFF34C759.toInt(),
  'd' to 0xFF1F8A4C.toInt(),
  'n' to 0xFFA8672B.toInt(),
  's' to 0xFF6B3E1B.toInt(),
  't' to 0xFFF2C28B.toInt(),
  'v' to 0xFF8B5CF6.toInt(),
  'e' to 0xFF9AA3B2.toInt(),
  'l' to 0xFFD8DDE6.toInt(),
)

private val PIXEL_PALETTE = PIXEL_PALETTE_ARGB.mapValues { (_, argb) -> Color(argb) }

internal val PIXEL_SPRITES = mapOf(
  "star" to ".....kk.....|....kyyk....|....kyyk....|kkkkkyykkkkk|kyyyyyyyyyyk|.kyyyyyyyyk.|..kyyyyyyk..|..kyyyyyyk..|.kyyykkyyyk.|.kyk....kyk.|.kk......kk.",
  "coin" to "...kkkkkk...|..kyyyyyyk..|.kyyyooyyyk.|kyyyoyyoyyyk|kyyoyyyyoyyk|kyyoyyyyoyyk|kyyoyyyyoyyk|kyyoyyyyoyyk|kyyyoyyoyyyk|.kyyyooyyyk.|..kyyyyyyk..|...kkkkkk...",
  "heart" to "..kkk..kkk..|.krrrkkrrrk.|krwrrrrrrrrk|krwrrrrrrrrk|krrrrrrrrrrk|.krrrrrrrrk.|..krrrrrrk..|...krrrrk...|....krrk....|.....kk.....",
  "gem" to "..kkkkkkkk..|.kcwbbbbbbk.|kcwbbbbbbbbk|kkkkkkkkkkkk|.kcbbbbbbbk.|..kcbbbbbk..|...kcbbbk...|....kbbk....|.....kk.....",
  "trophy" to "..kkkkkkkk..|.kyywyyyyyk.|kkyywyyyyykk|kykyyyyyykyk|kykyyyyyykyk|.kkkyyyyokk.|...kkyyokk..|....kyyok...|...kkkkkkk..|..kyyyyyyok.|..kkkkkkkkkk",
  "hero" to "...kkkkkk...|..kbbbbbbk..|.kbbccbbbbk.|.kbbbbbbbbk.|.kkkkkkkkkk.|.kbbwwwwbbk.|.kbbwkkwbbk.|.kbbbbbbbbk.|..kbbkkbbk..|..kbbkkbbk..|...kkkkkk...",
  "warning" to ".....kk.....|....kkyk....|...kyyyyk...|...kykkyk...|..kyykkyyk..|..kyykkyyk..|.kyyykkyyyk.|.kyyyyyyyyk.|kyyyykkyyyyk|kyyyyyyyyyyk|kooooooooooo|kkkkkkkkkkkk",
  "timer" to "...kkkkkk...|...kbbbbk...|..kkkkkkkk..|.kbbccbbbbk.|kbbccwkbbbbk|kbbccwkbbbbk|kbbbbwkkkbbk|kbbbbbbbbbbk|kbbbbbbbbbbk|.kbbbbbbbbk.|..kbbbbbbk..|...kkkkkk...",
  "check" to ".kkkkkkkkkk.|kggggggggggk|kggggggggwgk|kgggggggwwgk|kgwggggwwggk|kgwwggwwgggk|kggwwwwggggk|kgggwwgggggk|kggggggggggk|.kkkkkkkkkk.",
  "bag" to "....kkkk....|...k....k...|...k....k...|.kkkkkkkkkk.|.kppppppppk.|.kppwwppppk.|.kppppppppk.|.kppppppppk.|.kppppppppk.|.kppppppppk.|.kkkkkkkkkk.",
  "chest" to "..kkkkkkkk..|.knnnnnnnnk.|knnnnnnnnnnk|kkkkkkkkkkkk|knnnnyynnnnk|knnnnyynnnnk|kssssssssssk|kssssssssssk|kkkkkkkkkkkk",
  "dumb" to "kk........kk|krkk....kkrk|krrkkkkkkrrk|krrkeeeekrrk|krrkkkkkkrrk|krkk....kkrk|kk........kk",
  "book" to ".kkkkkkkkkk.|kbbbbbbbbbbk|kbbwwwwwwwbk|kbbwkkkkkwbk|kbbwwwwwwwbk|kbbwkkkkkwbk|kbbwwwwwwwbk|kbbbbbbbbbbk|.kkkkkkkkkk.",
  "leaf" to ".......kkkk.|.....kkggggk|...kkgggggdk|..kgggggggdk|..kgwgggggdk|.kggwggggdk.|.kgggggddk..|kkgggddkk...|kn..........|k...........",
  "palette" to "...kkkkkk...|.kkttttttkk.|kttrrttbbttk|kttrrttbbttk|kttttttttttk|ktggttttkkkk|ktggttttk...|.kttttttk...|..kkkkkk....",
  "target" to "...kkkkkk...|..krrrrrrk..|.krwwwwwwrk.|krwwrrrrwwrk|krwrrwwrrwrk|krwrwyywrwrk|krwrwyywrwrk|krwrrwwrrwrk|krwwrrrrwwrk|.krwwwwwwrk.|..krrrrrrk..|...kkkkkk...",
  "house" to ".....kk.....|....krrk....|...krrrrk...|..krrrrrrk..|.krrrrrrrrk.|kkkkkkkkkkkk|.kttttttttk.|.kttttkkttk.|.kttttkkttk.|.kttttkkttk.|.kkkkkkkkkk.",
  "brief" to "...kkkkkk...|...k....k...|kkkkkkkkkkkk|knnnnnnnnnnk|knnnnnnnnnnk|kkkkkyykkkkk|knnnnyynnnnk|knnnnnnnnnnk|kkkkkkkkkkkk",
  "cup" to "...l..l.....|....l..l....|.kkkkkkkkk..|kssssssssk..|kwwwwwwwwkkk|kwwwwwwwwkwk|kwwwwwwwwkwk|.kwwwwwwkkk.|..kwwwwwk...|...kkkkkk...",
  "film" to "kkkkkkkkkkkk|kvvvvvvvvvvk|kvvwvvvvvvvk|kvvwwvvvvvvk|kvvwwwvvvvvk|kvvwwvvvvvvk|kvvwvvvvvvvk|kvvvvvvvvvvk|kkkkkkkkkkkk",
  "sushi" to "...kkkkkk...|..koooooook..|.koooooooook.|kooowwwwoook|kooowwwwoook|.koooooooook.|..koooooook..|...kkkkkk...",
  "sun" to ".....oo.....|.o...oo...o.|..o.kkkk.o..|...kyyyyk...|ookyyyyyykoo|ookyywyyykoo|ookyyyyyykoo|...kyyyyk...|..o.kkkk.o..|.o...oo...o.|.....oo.....",
  "gift" to "..kkk..kkk..|.kpppkkpppk.|..kkkkkkkk..|kvvvvvyyvvvk|.kppppyyppk.|.kppppyyppk.|.kppppyyppk.|.kppppyyppk.|.kppppyyppk.|.kkkkkkkkkk.",
  "checklist" to ".kkkkkkkkkk.|kwwwwwwwwwwk|kwggwkkkkwwk|kwggwwwwwwwk|kwwwwwwwwwwk|kwbbwkkkkwwk|kwbbwwwwwwwk|kwwwwwwwwwwk|kwyywkkkkwwk|kwyywwwwwwwk|kwwwwwwwwwwk|.kkkkkkkkkk.",
  "sword" to ".........kkk|........kllk|.......kllk.|......kllk..|.....kllk...|..kk.kllk...|..kykkllk...|...kyyyk....|..knkyyk....|.knnk.kk....|.kkk........|............",
  "flame" to ".....kk.....|....krok....|...krork....|..krooyrk...|..kroyyrk...|.krooyyyrk..|.kroywwyrk..|.krooyyyrk..|..krooork...|...krrrk....|....kkk.....|............",
  "bell" to ".....kk.....|....kyyk....|...kyyyyk...|..kyyyyyyk..|..kyyyyyyk..|..kyyyyyyk..|.kyyyyyyyyk.|kkkkkkkkkkkk|....k..k....|....kyyk....|.....kk.....|............",
  "save" to ".kkkkkkkkkk.|kbbbbwwwwbbk|kbbbbwbbwbbk|kbbbbwwwwbbk|kbbbbbbbbbbk|kbbkkkkkkbbk|kbbkwwwwkbbk|kbbkwwwwkbbk|kbbkwwwwkbbk|.kkkkkkkkkk.",
  "folder" to ".kkkkk......|kyyyyyk.....|kyyyyykkkkkk|kyyyyyyyyyyk|kyyyyyyyyyyk|kyyyyyyyyyyk|kyyyyyyyyyyk|kyyyyyyyyyyk|kooooooooooo|.kkkkkkkkkk.",
  "bolt" to "......kkkk..|.....kyyyk..|....kyyyk...|...kyyyk....|..kyyyyyyyk.|..kyyyyyyk..|.....kyyk...|....kyyk....|...kyyk.....|..kyyk......|..kkk.......|............",
  "lock" to "...kkkkkk...|..kllleekk..|..kl....ek..|..ke....ek..|.kkkkkkkkkk.|.kyyywyyyyk.|.kyyykkyyok.|.kyyykkyyok.|.kooooooook.|.kkkkkkkkkk.",
  "sparkle" to ".....kk.....|.....kyk....|....kyyyk...|...kyywyk...|kkkkyywykkkk|kyyyyyyyyyyk|kkkkyywykkkk|...kyywyk...|....kyyyk...|.....kyk....|.....kk.....|............",
  "freeze" to ".....kk.....|....kcck....|..k.kcck.k..|.kckkwwkkck.|..kccwwcck..|kccwwwwwwcck|kccwwwwwwcck|..kccwwcck..|.kckkwwkkck.|..k.kcck.k..|....kcck....|.....kk.....",
  "plus" to "....kkkk....|....kwwk....|....kwwk....|....kwwk....|kkkkkwwkkkkk|kwwwwwwwwwwk|kwwwwwwwwwwk|kkkkkwwkkkkk|....kwwk....|....kwwk....|....kwwk....|....kkkk....",
  "play" to "..kk........|..kkk.......|..kwwk......|..kwwwk.....|..kwwwwk....|..kwwwwwk...|..kwwwwwk...|..kwwwwk....|..kwwwk.....|..kwwk......|..kkk.......|..kk........",
  "pause" to ".kkkk..kkkk.|.kwwk..kwwk.|.kwwk..kwwk.|.kwwk..kwwk.|.kwwk..kwwk.|.kwwk..kwwk.|.kwwk..kwwk.|.kwwk..kwwk.|.kwwk..kwwk.|.kwwk..kwwk.|.kwwk..kwwk.|.kkkk..kkkk.",
  "reset" to "...kkkkkk...|..kwwwwwkk..|.kwwk..kwwk.|kwwk....kwwk|kwwk........|kwwk...kkkk.|kwwk....kwwk|.kwwk..kwwk.|..kwwwwwkk..|...kkkkkk...|............|............",
  "checkbox_empty" to ".kkkkkkkkkk.|k..........k|k..........k|k..........k|k..........k|k..........k|k..........k|k..........k|k..........k|k..........k|k..........k|.kkkkkkkkkk.",
  "checkbox_done" to ".kkkkkkkkkk.|kggggggggggk|kggggggggggk|kgggggggwwgk|kggggggwwggk|kggwggwwgggk|kggwwwwggggk|kgggwwgggggk|kggggggggggk|kggggggggggk|kggggggggggk|.kkkkkkkkkk.",
)

private val EMOJI_TO_SPRITE = mapOf(
  "💪" to "dumb",
  "📚" to "book",
  "🌿" to "leaf",
  "🎨" to "palette",
  "🎯" to "target",
  "🏠" to "house",
  "💼" to "brief",
  "☕" to "cup",
  "🎬" to "film",
  "🍣" to "sushi",
  "🏖" to "sun",
  "🎁" to "gift",
  "⭐" to "star",
  "🌱" to "leaf",
  "🔟" to "check",
  "💯" to "trophy",
  "🍅" to "timer",
  "🛍" to "bag",
  "💰" to "coin",
  "📁" to "folder",
  "🔥" to "flame",
  "⚔" to "sword",
  "🔔" to "bell",
  "💾" to "save",
  "⚡" to "bolt",
  "🔒" to "lock",
  "⏱" to "timer",
  "📋" to "checklist",
  "✨" to "sparkle",
  "✅" to "check",
  "🏆" to "trophy",
  "🪙" to "coin",
  "💎" to "gem",
  "⚠" to "warning",
  "🛡" to "hero",
)

fun resolveSpriteKey(raw: String): String? {
  val clean = raw.replace("\uFE0F", "").trim()
  if (PIXEL_SPRITES.containsKey(clean)) return clean
  return EMOJI_TO_SPRITE[clean]
}

@Composable
fun PixelIcon(
  icon: String,
  size: Dp = 24.dp,
  modifier: Modifier = Modifier,
  grayscale: Boolean = false,
  monochromeTint: Color? = null,
) {
  val spriteKey = resolveSpriteKey(icon)
  val sprite = spriteKey?.let { PIXEL_SPRITES[it] }
  if (sprite != null) {
    val rows = sprite.split('|')
    val offsetY = (12 - rows.size) / 2
    Canvas(modifier = modifier.size(size)) {
      val cellW = this.size.width / 12f
      val cellH = this.size.height / 12f
      rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, ch ->
          val baseColor = PIXEL_PALETTE[ch]
          if (baseColor != null) {
            val color = when {
              monochromeTint != null -> when (ch) {
                'k' -> monochromeTint
                'w' -> Color.Transparent
                else -> monochromeTint.copy(alpha = 0.24f)
              }
              grayscale -> {
                val luma = (0.299f * baseColor.red + 0.587f * baseColor.green + 0.114f * baseColor.blue)
                  .coerceIn(0f, 1f)
                Color(red = luma, green = luma, blue = (luma * 1.03f).coerceAtMost(1f), alpha = baseColor.alpha)
              }
              else -> baseColor
            }
            if (color.alpha > 0f) {
              drawRect(
                color = color,
                topLeft = Offset(x * cellW, (y + offsetY) * cellH),
                size = Size(cellW + 0.5f, cellH + 0.5f),
              )
            }
          }
        }
      }
    }
  } else {
    Box(
      modifier = modifier.size(size),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = icon.ifBlank { "⭐" },
        fontSize = (size.value * 0.72f).sp,
      )
    }
  }
}
