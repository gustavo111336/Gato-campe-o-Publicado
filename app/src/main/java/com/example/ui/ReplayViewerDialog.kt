package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GameplayVideoPost
import com.example.model.ALL_HERO_SKINS
import com.example.model.ReplayFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private data class ReplayObstacle(val xRatio: Float, val yRatio: Float, val r: Float, val color: Color)

private val REPLAY_ARENA_OBSTACLES = listOf(
  ReplayObstacle(0.50f, 0.50f, 75f, Color(0xFF0EA5E9)),
  ReplayObstacle(0.25f, 0.30f, 55f, Color(0xFF10B981)),
  ReplayObstacle(0.75f, 0.30f, 55f, Color(0xFF10B981)),
  ReplayObstacle(0.25f, 0.70f, 50f, Color(0xFFF472B6)),
  ReplayObstacle(0.75f, 0.70f, 50f, Color(0xFFF472B6)),
  ReplayObstacle(0.50f, 0.20f, 45f, Color(0xFFA855F7)),
  ReplayObstacle(0.50f, 0.80f, 45f, Color(0xFFA855F7))
)

@Composable
fun ReplayViewerDialog(
  post: GameplayVideoPost,
  onDismiss: () -> Unit,
  onLike: () -> Unit = {},
  isLiked: Boolean = false,
  likesCount: Long = post.likes,
  isLocalPreview: Boolean = false,
  modifier: Modifier = Modifier
) {
  val skinDef = remember(post.userSkinId) {
    ALL_HERO_SKINS.find { it.id == post.userSkinId } ?: ALL_HERO_SKINS.first()
  }

  val rawFrames = remember(post.replayFramesJson) {
    if (post.replayFramesJson.isNotBlank()) {
      ReplayFrame.listFromJson(post.replayFramesJson)
    } else {
      emptyList()
    }
  }
  val hasRealRecording = rawFrames.isNotEmpty()

  val frames = remember(rawFrames, post.score, post.durationSec) {
    if (rawFrames.isNotEmpty()) {
      rawFrames
    } else {
      val total = 50
      val dur = if (post.durationSec > 0.1f) post.durationSec else 18f
      (0 until total).map { i ->
        val t = (i.toFloat() / (total - 1)) * dur
        val angle = (i.toFloat() / total) * (2 * Math.PI.toFloat() * 2f)
        val hx = 1600f + 920f * cos(angle)
        val hy = 2400f + 1350f * sin(angle)
        val gx = 1600f + 720f * cos(angle - 0.45f)
        val gy = 2400f + 1150f * sin(angle - 0.45f)
        val cx = 1600f + 950f * cos(angle + 0.35f)
        val cy = 2400f + 1400f * sin(angle + 0.35f)
        val cats = ((i.toFloat() / total) * max(1L, post.score / 60)).toInt()
        val sc = ((i.toFloat() / total) * post.score).toInt()
        ReplayFrame(
          timeSec = t,
          heroX = hx,
          heroY = hy,
          ghostX = gx,
          ghostY = gy,
          catX = cx,
          catY = cy,
          catsCollected = cats,
          score = sc,
          isDashing = (i % 5 == 0)
        )
      }
    }
  }

  val inferredWorldSize = remember(frames) {
    val maxX = frames.maxOfOrNull { max(it.heroX, max(it.ghostX, it.catX)) } ?: 3200f
    val maxY = frames.maxOfOrNull { max(it.heroY, max(it.ghostY, it.catY)) } ?: 4800f
    if (maxX > 1650f || maxY > 2450f) 3200f to 4800f else 1600f to 2400f
  }

  var currentFrameIndex by remember { mutableIntStateOf(0) }
  var isPlaying by remember { mutableStateOf(true) }
  var playbackSpeed by remember { mutableFloatStateOf(1f) }

  // Real-time delta playback timer loop matching captured timestamps
  LaunchedEffect(isPlaying, playbackSpeed, frames) {
    if (!isPlaying || frames.size <= 1) return@LaunchedEffect
    while (isActive && isPlaying) {
      val nextIdx = if (currentFrameIndex + 1 >= frames.size) 0 else currentFrameIndex + 1
      val rawDeltaSec = if (nextIdx > currentFrameIndex) {
        (frames[nextIdx].timeSec - frames[currentFrameIndex].timeSec).coerceIn(0.05f, 0.35f)
      } else {
        0.15f
      }
      val delayMs = ((rawDeltaSec * 1000f) / playbackSpeed).toLong().coerceIn(30L, 400L)
      delay(delayMs)
      currentFrameIndex = nextIdx
    }
  }

  val currentFrame = remember(currentFrameIndex, frames) {
    if (frames.isNotEmpty() && currentFrameIndex in frames.indices) {
      frames[currentFrameIndex]
    } else {
      ReplayFrame()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
      shape = RoundedCornerShape(24.dp),
      border = BorderStroke(2.dp, Color(0xFF8B5CF6)),
      modifier = modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.92f)
        .padding(8.dp)
        .shadow(28.dp, RoundedCornerShape(24.dp))
        .testTag("replay_viewer_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // TOP HEADER: Title, Author and Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1B4B))
                .border(1.5.dp, Color(0xFF8B5CF6), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(skinDef.badgeEmoji, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = post.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Por @${post.username} • ${post.modeName} • 🏆 ${post.score} pts",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp).testTag("close_replay_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fechar Replay",
              tint = Color(0xFFCBD5E1)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // THEATER CANVAS: Plays back the match
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Scale dynamically from either 1600x2400 (legacy) or 3200x4800 (expanded map)
            val (worldW, worldH) = inferredWorldSize
            val scaleX = canvasW / worldW
            val scaleY = canvasH / worldH
            val unitScale = (canvasW / 400f).coerceIn(0.7f, 1.8f)

            // Arena Background Gradient
            drawRect(
              brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541))
              ),
              size = size
            )

            // Arena subtle decorative grid dots
            val dotSpacingX = canvasW / 8f
            val dotSpacingY = canvasH / 12f
            for (gx in 1..7) {
              for (gy in 1..11) {
                drawCircle(
                  color = Color(0x2264748B),
                  radius = 2.5f,
                  center = Offset(gx * dotSpacingX, gy * dotSpacingY)
                )
              }
            }

            // Arena Obstacles (subtle park landmarks)
            for (obs in REPLAY_ARENA_OBSTACLES) {
              val ox = obs.xRatio * canvasW
              val oy = obs.yRatio * canvasH
              val orad = (obs.r / 1600f) * canvasW
              drawCircle(
                color = obs.color.copy(alpha = 0.16f),
                radius = orad,
                center = Offset(ox, oy)
              )
              drawCircle(
                color = obs.color.copy(alpha = 0.35f),
                radius = orad,
                center = Offset(ox, oy),
                style = Stroke(width = 2f)
              )
            }

            // 1. Draw Collectible Cat
            val catX = currentFrame.catX * scaleX
            val catY = currentFrame.catY * scaleY
            val catRadius = 14f * unitScale

            drawCircle(
              color = Color(0x55F59E0B),
              radius = catRadius * 1.8f,
              center = Offset(catX, catY)
            )
            drawCircle(
              color = Color(0xFFFBBF24),
              radius = catRadius,
              center = Offset(catX, catY)
            )
            drawEar(Offset(catX - catRadius * 0.5f, catY - catRadius * 0.6f), catRadius * 0.7f, Color(0xFFF59E0B))
            drawEar(Offset(catX + catRadius * 0.5f, catY - catRadius * 0.6f), catRadius * 0.7f, Color(0xFFF59E0B))

            // 2. Draw Ghosts (up to 3)
            val ghostList = buildList {
              add(currentFrame.ghostX to currentFrame.ghostY)
              if (currentFrame.ghost2X >= 0f && currentFrame.ghost2Y >= 0f) {
                add(currentFrame.ghost2X to currentFrame.ghost2Y)
              }
              if (currentFrame.ghost3X >= 0f && currentFrame.ghost3Y >= 0f) {
                add(currentFrame.ghost3X to currentFrame.ghost3Y)
              }
            }
            val ghostRadius = 18f * unitScale
            val ghostBodyColor = if (currentFrame.isGhostFleeing) Color(0xFF38BDF8) else Color(0xFFC084FC)
            val ghostAuraColor = if (currentFrame.isGhostFleeing) Color(0x4438BDF8) else Color(0x44C084FC)

            for ((rawGx, rawGy) in ghostList) {
              val ghostX = rawGx * scaleX
              val ghostY = rawGy * scaleY
              drawCircle(
                color = ghostAuraColor,
                radius = ghostRadius * 2f,
                center = Offset(ghostX, ghostY)
              )
              drawCircle(
                color = ghostBodyColor,
                radius = ghostRadius,
                center = Offset(ghostX, ghostY)
              )
              drawCircle(
                color = Color.White,
                radius = ghostRadius * 0.28f,
                center = Offset(ghostX - ghostRadius * 0.35f, ghostY - ghostRadius * 0.15f)
              )
              drawCircle(
                color = Color(0xFF581C87),
                radius = ghostRadius * 0.15f,
                center = Offset(ghostX - ghostRadius * 0.35f, ghostY - ghostRadius * 0.15f)
              )
              drawCircle(
                color = Color.White,
                radius = ghostRadius * 0.28f,
                center = Offset(ghostX + ghostRadius * 0.35f, ghostY - ghostRadius * 0.15f)
              )
              drawCircle(
                color = Color(0xFF581C87),
                radius = ghostRadius * 0.15f,
                center = Offset(ghostX + ghostRadius * 0.35f, ghostY - ghostRadius * 0.15f)
              )
            }

            // 3. Draw Hero Cat Player (or Player Ghost in Elite mode)
            val heroX = currentFrame.heroX * scaleX
            val heroY = currentFrame.heroY * scaleY
            val heroRadius = 20f * unitScale

            if (currentFrame.isSonicActive) {
              drawCircle(
                color = Color(0x88E879F9),
                radius = heroRadius * 4.0f,
                center = Offset(heroX, heroY),
                style = Stroke(width = 4f)
              )
              drawCircle(
                color = Color(0x33A855F7),
                radius = heroRadius * 3.2f,
                center = Offset(heroX, heroY)
              )
            }

            if (currentFrame.isDashing) {
              drawCircle(
                color = skinDef.trailColor.copy(alpha = 0.6f),
                radius = heroRadius * 2.2f,
                center = Offset(heroX, heroY)
              )
              drawCircle(
                color = skinDef.trailColor.copy(alpha = 0.3f),
                radius = heroRadius * 3.0f,
                center = Offset(heroX, heroY)
              )
            } else if (skinDef.hasAura) {
              drawCircle(
                color = skinDef.trailColor.copy(alpha = 0.35f),
                radius = heroRadius * 1.6f,
                center = Offset(heroX, heroY)
              )
            }

            if (currentFrame.isPlayerGhost) {
              drawCircle(
                color = Color(0xFF9333EA),
                radius = heroRadius * 1.1f,
                center = Offset(heroX, heroY)
              )
              drawCircle(
                color = Color(0xFFFDE047),
                radius = heroRadius * 0.22f,
                center = Offset(heroX - heroRadius * 0.32f, heroY - heroRadius * 0.1f)
              )
              drawCircle(
                color = Color(0xFFFDE047),
                radius = heroRadius * 0.22f,
                center = Offset(heroX + heroRadius * 0.32f, heroY - heroRadius * 0.1f)
              )
            } else {
              drawCircle(
                color = skinDef.furColor,
                radius = heroRadius,
                center = Offset(heroX, heroY)
              )
              drawCape(
                center = Offset(heroX, heroY + heroRadius * 0.5f),
                radius = heroRadius,
                color = skinDef.capeColor
              )
              drawEar(Offset(heroX - heroRadius * 0.6f, heroY - heroRadius * 0.65f), heroRadius * 0.8f, skinDef.earColor)
              drawEar(Offset(heroX + heroRadius * 0.6f, heroY - heroRadius * 0.65f), heroRadius * 0.8f, skinDef.earColor)
              drawEar(Offset(heroX - heroRadius * 0.6f, heroY - heroRadius * 0.65f), heroRadius * 0.45f, skinDef.innerEarColor)
              drawEar(Offset(heroX + heroRadius * 0.6f, heroY - heroRadius * 0.65f), heroRadius * 0.45f, skinDef.innerEarColor)

              drawCircle(
                color = skinDef.eyeColor,
                radius = heroRadius * 0.22f,
                center = Offset(heroX - heroRadius * 0.32f, heroY - heroRadius * 0.1f)
              )
              drawCircle(
                color = skinDef.eyeColor,
                radius = heroRadius * 0.22f,
                center = Offset(heroX + heroRadius * 0.32f, heroY - heroRadius * 0.1f)
              )
              drawCircle(
                color = Color.White,
                radius = heroRadius * 0.08f,
                center = Offset(heroX - heroRadius * 0.30f, heroY - heroRadius * 0.14f)
              )
              drawCircle(
                color = Color.White,
                radius = heroRadius * 0.08f,
                center = Offset(heroX + heroRadius * 0.34f, heroY - heroRadius * 0.14f)
              )
            }
          }

          // In-Player Live Ticker / HUD
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.TopCenter)
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = Color(0xDD0F172A),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(
                1.dp,
                if (hasRealRecording) Color(0xFFEF4444) else Color(0xFFF59E0B)
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hasRealRecording) Color(0xFFEF4444) else Color(0xFFF59E0B))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (hasRealRecording) "GRAVAÇÃO REAL • ${playbackSpeed}x" else "SIMULAÇÃO • ${playbackSpeed}x",
                  color = if (hasRealRecording) Color(0xFFFCA5A5) else Color(0xFFFDE047),
                  fontWeight = FontWeight.Black,
                  fontSize = 10.sp
                )
              }
            }

            Surface(
              color = Color(0xDD0F172A),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
              Text(
                text = "❤️ ${currentFrame.lives} • 🐱 ${currentFrame.catsCollected} • 🏆 ${currentFrame.score}",
                color = Color(0xFFFDE047),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          // Live Timer Indicator at bottom left of video
          Surface(
            color = Color(0xBB000000),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(8.dp)
          ) {
            Text(
              text = "⏱️ ${String.format("%.1f", currentFrame.timeSec)}s",
              color = Color(0xFFE2E8F0),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TIMELINE SCRUBBER SLIDER
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${currentFrameIndex + 1}/${frames.size.coerceAtLeast(1)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(end = 6.dp)
          )
          Slider(
            value = currentFrameIndex.toFloat(),
            onValueChange = {
              currentFrameIndex = it.toInt().coerceIn(0, (frames.size - 1).coerceAtLeast(0))
              isPlaying = false
            },
            valueRange = 0f..(frames.size - 1).coerceAtLeast(1).toFloat(),
            colors = SliderDefaults.colors(
              thumbColor = Color(0xFF8B5CF6),
              activeTrackColor = Color(0xFF8B5CF6),
              inactiveTrackColor = Color(0xFF334155)
            ),
            modifier = Modifier.weight(1f).testTag("replay_timeline_slider")
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // PLAYBACK CONTROLS BAR: Play/Pause, Rewind, Speeds, Like
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = {
                currentFrameIndex = 0
                isPlaying = true
              },
              modifier = Modifier.size(48.dp).testTag("replay_rewind_button")
            ) {
              Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Reiniciar Replay",
                tint = Color(0xFF38BDF8)
              )
            }

            FilledIconButton(
              onClick = { isPlaying = !isPlaying },
              colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF8B5CF6)),
              modifier = Modifier.size(48.dp).testTag("replay_play_pause_button")
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                tint = Color.White
              )
            }

            listOf(1f, 1.5f, 2f).forEach { spd ->
              val isSelected = (playbackSpeed == spd)
              Surface(
                color = if (isSelected) Color(0xFF6D28D9) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                  1.dp,
                  if (isSelected) Color(0xFFA78BFA) else Color(0xFF475569)
                ),
                modifier = Modifier
                  .defaultMinSize(minWidth = 44.dp, minHeight = 40.dp)
                  .clickable { playbackSpeed = spd }
                  .testTag("replay_speed_${spd}x")
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                  Text(
                    text = "${if (spd == 1f) "1" else if (spd == 1.5f) "1.5" else "2"}x",
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          if (isLocalPreview) {
            Surface(
              color = Color(0xFF1E293B),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, Color(0xFF38BDF8))
            ) {
              Text(
                text = "👁️ Prévia",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
              )
            }
          } else {
            OutlinedButton(
              onClick = onLike,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(
                1.dp,
                if (isLiked) Color(0xFFEC4899) else Color(0xFF475569)
              ),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isLiked) Color(0xFF831843) else Color.Transparent
              ),
              modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("replay_like_button")
            ) {
              Text(
                text = "${if (isLiked) "❤️" else "🤍"} $likesCount",
                fontSize = 11.sp,
                color = if (isLiked) Color(0xFFF9A8D4) else Color.White
              )
            }
          }
        }
      }
    }
  }
}

private fun DrawScope.drawEar(center: Offset, size: Float, color: Color) {
  val path = Path().apply {
    moveTo(center.x - size * 0.5f, center.y + size * 0.5f)
    lineTo(center.x, center.y - size * 0.5f)
    lineTo(center.x + size * 0.5f, center.y + size * 0.5f)
    close()
  }
  drawPath(path, color = color, style = Fill)
}

private fun DrawScope.drawCape(center: Offset, radius: Float, color: Color) {
  drawCircle(
    color = color.copy(alpha = 0.5f),
    radius = radius * 0.7f,
    center = center
  )
}
