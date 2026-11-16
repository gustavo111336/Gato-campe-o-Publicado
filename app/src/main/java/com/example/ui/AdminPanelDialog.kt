package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AdminPanelDialog(
  uiState: GameUiState,
  onUnlockEverything: () -> Unit,
  onSetMoney: (Int) -> Unit,
  onSetLevel: (Int) -> Unit,
  onSetWins: (Int) -> Unit,
  onSetRevives: (Int) -> Unit = {},
  onDismiss: () -> Unit
) {
  var moneyInput by remember { mutableStateOf(uiState.spendablePoints.toString()) }
  var levelInput by remember { mutableStateOf(uiState.playerLevel.toString()) }
  var winsInput by remember { mutableStateOf(uiState.championCount.toString()) }
  var revivesInput by remember { mutableStateOf(uiState.reviveCount.toString()) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.88f)
        .padding(4.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFFFACC15), RoundedCornerShape(24.dp))
        .testTag("admin_panel_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "👑 PAINEL ADMINISTRADOR (admin01)",
              fontSize = 17.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFDE047)
            )
            Text(
              text = "Controle total liberado: dinheiro, nível, vitórias, skins, modos e conquistas!",
              fontSize = 11.sp,
              color = Color(0xFFCBD5E1)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_admin_dialog")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fechar Painel Admin",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Master Unlock Button
        Button(
          onClick = {
            onUnlockEverything()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("admin_unlock_everything_button")
        ) {
          Text(
            text = "🚀 LIBERAR TUDO & COMPLETAR CONQUISTAS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Money / Spendable Points Section
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0F172A),
          border = BorderStroke(1.dp, Color(0xFFFBBF24)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "🪙 Definir Dinheiro / Pontos na Loja",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFDE047)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = moneyInput,
              onValueChange = { moneyInput = it.filter { c -> c.isDigit() }.take(8) },
              label = { Text("Quantidade de pontos", fontSize = 11.sp) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFBBF24),
                unfocusedBorderColor = Color(0xFF475569)
              ),
              modifier = Modifier.fillMaxWidth().testTag("admin_money_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  val amt = moneyInput.toIntOrNull() ?: 10000
                  onSetMoney(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_apply_money_button")
              ) {
                Text("Aplicar Dinheiro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
              Button(
                onClick = {
                  moneyInput = "999999"
                  onSetMoney(999999)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp)
              ) {
                Text("💰 +999.999 pts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Level Section
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0F172A),
          border = BorderStroke(1.dp, Color(0xFF38BDF8)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "⭐ Definir Nível do Jogador",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = levelInput,
              onValueChange = { levelInput = it.filter { c -> c.isDigit() }.take(4) },
              label = { Text("Nível (ex: 50, 100, 999)", fontSize = 11.sp) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
              ),
              modifier = Modifier.fillMaxWidth().testTag("admin_level_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  val lvl = levelInput.toIntOrNull() ?: 10
                  onSetLevel(lvl)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_apply_level_button")
              ) {
                Text("Aplicar Nível", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
              Button(
                onClick = {
                  levelInput = "999"
                  onSetLevel(999)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp)
              ) {
                Text("⭐ Nível 999", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Wins Section
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0F172A),
          border = BorderStroke(1.dp, Color(0xFF34D399)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "🏆 Definir Vitórias / Modo Campeão",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF34D399)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = winsInput,
              onValueChange = { winsInput = it.filter { c -> c.isDigit() }.take(5) },
              label = { Text("Número de vitórias", fontSize = 11.sp) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF34D399),
                unfocusedBorderColor = Color(0xFF475569)
              ),
              modifier = Modifier.fillMaxWidth().testTag("admin_wins_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = {
                val wins = winsInput.toIntOrNull() ?: 10
                onSetWins(wins)
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_apply_wins_button")
            ) {
              Text("Aplicar Vitórias", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Revives / Vidas Section
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0F172A),
          border = BorderStroke(1.dp, Color(0xFFF472B6)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "💖 Definir Revives / Vidas Infinitas",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF472B6)
            )
            Text(
              text = "Coloque a quantidade de revives que quiser para reviver sempre que for pego!",
              fontSize = 10.sp,
              color = Color(0xFFCBD5E1),
              modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            OutlinedTextField(
              value = revivesInput,
              onValueChange = { revivesInput = it.filter { c -> c.isDigit() }.take(6) },
              label = { Text("Quantidade de revives", fontSize = 11.sp) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFF472B6),
                unfocusedBorderColor = Color(0xFF475569)
              ),
              modifier = Modifier.fillMaxWidth().testTag("admin_revives_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  val count = revivesInput.toIntOrNull() ?: 99
                  onSetRevives(count)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_apply_revives_button")
              ) {
                Text("Aplicar Revives", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
              Button(
                onClick = {
                  revivesInput = "9999"
                  onSetRevives(9999)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE185D)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp)
              ) {
                Text("💖 +9.999 Revives", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Text("Fechar Painel Admin", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}
