package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PauseOverlay(
  hasRecordedReplay: Boolean = false,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onReturnToMenu: () -> Unit,
  onOpenSettings: () -> Unit,
  onPostReplay: () -> Unit = {},
  onOpenVideosFeed: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xCC0F172A))
      .padding(24.dp)
      .testTag("pause_overlay"),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp)
        .shadow(20.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFFFACC15), RoundedCornerShape(24.dp)),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text("⏸️", fontSize = 48.sp)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "JOGO PAUSADO",
          fontSize = 22.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFDE047)
        )

        Text(
          text = "O gatinho e o fantasma estão descansando um momento.",
          fontSize = 12.sp,
          color = Color(0xFFE2E8F0),
          modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = onResume,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button")
        ) {
          Text("Continuar Jogando ▶️", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (hasRecordedReplay) {
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = onPostReplay,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("post_replay_from_pause_button")
          ) {
            Text("🎬 Ver / Postar Última Gravação 🚀", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onOpenVideosFeed,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, Color(0xFFC084FC)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("videos_feed_from_pause_button")
        ) {
          Text("📺 Replays da Comunidade 🎬", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9D5FF))
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onRestart,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, Color(0xFFEF4444)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restart_from_pause_button")
        ) {
          Text("Reiniciar Partida 🔄", fontSize = 14.sp, color = Color(0xFFF87171))
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onReturnToMenu,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, Color(0xFF94A3B8)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("menu_from_pause_button")
        ) {
          Text("Voltar ao Menu Principal 🏠", fontSize = 14.sp, color = Color(0xFFE2E8F0))
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
          onClick = onOpenSettings,
          modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        ) {
          Text("Ajustes & Gameplay ⚙️", color = Color(0xFF38BDF8), fontSize = 13.sp)
        }
      }
    }
  }
}
