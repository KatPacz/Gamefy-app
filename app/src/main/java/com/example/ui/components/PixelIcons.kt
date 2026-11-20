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

private val PIXEL_PALETTE = mapOf(
  'k' to Color(0xFF2B2140),
  'w' to Color(0xFFFFFFFF),
  'y' to Color(0xFFFFC933),
  'o' to Color(0xFFF59A23),
  'r' to Color(0xFFE5484D),
  'p' to Color(0xFFFF8FA3),
  'b' to Color(0xFF3B82F6),
  'c' to Color(0xFF7DD3FC),
  'g' to Color(0xFF34C759),
  'd' to Color(0xFF1F8A4C),
  'n' to Color(0xFFA8672B),
  's' to Color(0xFF6B3E1B),
  't' to Color(0xFFF2C28B),
  'v' to Color(0xFF8B5CF6),
  'e' to Color(0xFF9AA3B2),
  'l' to Color(0xFFD8DDE6),
)

private val PIXEL_SPRITES = mapOf(
  "star" to ".....kk.....|....kyyk....|....kyyk....|kkkkkyykkkkk|kyyyyyyyyyyk|.kyyyyyyyyk.|..kyyyyyyk..|..kyyyyyyk..|.kyyykkyyyk.|.kyk....kyk.|.kk......kk.",
  "coin" to "...kkkkkk...|..kyyyyyyk..|.kyyyooyyyk.|kyyyoyyoyyyk|kyyoyyyyoyyk|kyyoyyyyoyyk|kyyoyyyyoyyk|kyyoyyyyoyyk|kyyyoyyoyyyk|.kyyyooyyyk.|..kyyyyyyk..|...kkkkkk...",
  "heart" to "..kkk..kkk..|.krrrkkrrrk.|krwrrrrrrrrk|krwrrrrrrrrk|krrrrrrrrrrk|.krrrrrrrrk.|..krrrrrrk..|...krrrrk...|....krrk....|.....kk.....",
  "gem" to "..kkkkkkkk..|.kcwbbbbbbk.|kcwbbbbbbbbk|kkkkkkkkkkkk|.kcbbbbbbbk.|..kcbbbbbk..|...kcbbbk...|....kbbk....|.....kk.....",
  "trophy" to "..kkkkkkkk..|.kyywyyyyyk.|kkyywyyyyykk|kykyyyyyykyk|kykyyyyyykyk|.kkkyyyyokk.|...kkyyokk..|....kyyok...|...kkkkkkk..|..kyyyyyyok.|..kkkkkkkkkk",
  "timer" to "....kbbk....|...kkkkkk...|..kwwwwwwk..|.kwwwkwwwwk.|kwwwwkwwwwwk|kwwwwkwwwwwk|kwwwwkkkwwwk|kwwwwwwwwwwk|kwwwwwwwwwwk|.kwwwwwwwwk.|..kwwwwwwk..|...kkkkkk...",
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
          val color = PIXEL_PALETTE[ch]
          if (color != null) {
            drawRect(
              color = color,
              topLeft = Offset(x * cellW, (y + offsetY) * cellH),
              size = Size(cellW + 0.5f, cellH + 0.5f),
            )
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
