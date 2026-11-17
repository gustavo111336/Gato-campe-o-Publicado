package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MorphTargetInfo

@Composable
fun SecretMorphPickerDialog(
  candidates: List<MorphTargetInfo>,
  currentMorphedName: String?,
  onSelectTarget: (String) -> Unit,
  onRevertMorph: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      border = BorderStroke(2.dp, Color(0xFFA855F7)),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 560.dp)
        .testTag("secret_morph_picker_dialog")
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "🎭 Impostor Metamorfo",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFE879F9)
            )
            Text(
              text = "Escolha um jogador vivo para copiar a aparência e incriminar!",
              fontSize = 11.sp,
              color = Color(0xFFCBD5E1),
              modifier = Modifier.padding(top = 2.dp)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize()
          ) {
            Text("✖", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        }

        if (currentMorphedName != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = onRevertMorph,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("morph_revert_button")
          ) {
            Text(
              text = "💨 Reverter Disfarce (@$currentMorphedName)",
              color = Color.White,
              fontWeight = FontWeight.Black,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (candidates.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Nenhum outro jogador vivo disponível para copiar no momento.",
              color = Color(0xFF94A3B8),
              fontSize = 12.sp,
              modifier = Modifier.padding(16.dp)
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(candidates, key = { it.uid }) { target ->
              Surface(
                onClick = { onSelectTarget(target.uid) },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.5.dp, Color(0xFF7E22CE)),
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 52.dp)
                  .testTag("morph_candidate_${target.uid}")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(target.furColor),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(target.accentEmoji, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "@${target.name}",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF9333EA)
                  ) {
                    Text(
                      text = "🎭 COPIAR",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Black,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
        ) {
          Text("Fechar", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
