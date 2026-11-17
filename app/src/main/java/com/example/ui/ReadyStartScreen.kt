package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.model.ALL_40_ACHIEVEMENTS
import com.example.model.ALL_40_ITEMS
import com.example.model.ALL_40_MODES
import com.example.model.ALL_HERO_SKINS
import com.example.model.Difficulty
import com.example.model.SecretRolePreference

@Composable
fun ReadyStartScreen(
  uiState: GameUiState,
  onStartNormal: () -> Unit,
  onStartEndless: () -> Unit,
  onStartBots: () -> Unit,
  onStartCustomBots: (
    botCount: Int,
    secretMode: Boolean,
    tasksEnabled: Boolean,
    rolesEnabled: Boolean,
    shapeshifterEnabled: Boolean,
    ventsEnabled: Boolean,
    rolePreference: SecretRolePreference
  ) -> Unit = { _, _, _, _, _, _, _ -> onStartBots() },
  onDifficultyChange: (Difficulty) -> Unit,
  onOpenModes: () -> Unit,
  onOpenCreator: () -> Unit,
  onOpenSkins: () -> Unit,
  onOpenShop: () -> Unit,
  onOpenAchievements: () -> Unit,
  onOpenItems: () -> Unit,
  onOpenGallery: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenOnline: () -> Unit,
  onOpenVideosFeed: () -> Unit = {},
  onOpenAi: () -> Unit,
  onCheckUpdate: () -> Unit,
  onSendMeowNotification: () -> Unit,
  onRestoreDefaultFair: () -> Unit,
  onOpenAdminDialog: () -> Unit = {},
  onSearchAccount: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showBotsConfigDialog by remember { mutableStateOf(false) }
  val infiniteTransition = rememberInfiniteTransition(label = "hero_cat_bob")
  val catScale by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  val activeSkin = ALL_HERO_SKINS.find { it.id == uiState.selectedSkinId } ?: ALL_HERO_SKINS.first()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xE60F172A))
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 540.dp)
        .shadow(24.dp, RoundedCornerShape(32.dp))
        .border(2.dp, Color(0xFFFACC15), RoundedCornerShape(32.dp)),
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Hero Graphic with Active Skin and Ghost
        Row(
          modifier = Modifier
            .scale(catScale)
            .padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(66.dp)
              .background(
                brush = Brush.radialGradient(
                  listOf(activeSkin.furColor, activeSkin.stripeColor)
                ),
                shape = CircleShape
              )
              .border(3.dp, activeSkin.capeBorderColor, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(activeSkin.badgeEmoji, fontSize = 36.sp)
          }

          Spacer(modifier = Modifier.width(12.dp))
          Text("⚡", fontSize = 22.sp)
          Spacer(modifier = Modifier.width(12.dp))

          Box(
            modifier = Modifier
              .size(56.dp)
              .background(
                brush = Brush.radialGradient(
                  listOf(Color(0xFFC084FC), Color(0xFF7E22CE))
                ),
                shape = CircleShape
              )
              .border(2.dp, Color(0xFFE9D5FF), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("👻", fontSize = 28.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "GATO CAMPEÃO",
          fontSize = 25.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFDE047),
          letterSpacing = 1.sp
        )

        Text(
          text = "🏆 10 gatos = Campeão • 40 Modos • 40 Itens • 40 Conquistas • Skins & 3D!",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF38BDF8),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        if (uiState.isAdminMode) {
          Button(
            onClick = onOpenAdminDialog,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp)
              .height(48.dp)
              .border(2.dp, Color(0xFFFACC15), RoundedCornerShape(16.dp))
              .testTag("menu_admin_button")
          ) {
            Text(
              text = "👑 PAINEL ADMIN (admin01) ATIVO - TOQUE AQUI",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        }

        // Status & Milestone badges row
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 8.dp)
        ) {
          MenuBadge("⭐ Nível ${uiState.playerLevel}", Color(0xFF38BDF8))
          MenuBadge("🪙 ${uiState.spendablePoints} pts", Color(0xFFFBBF24))
          if (uiState.reviveCount > 0) {
            MenuBadge("💖 ${uiState.reviveCount}", Color(0xFFF472B6))
          }
          if (uiState.bestHighScore > 0) {
            MenuBadge("🏅 Rec ${uiState.bestHighScore}", Color(0xFF34D399))
          }
          if (uiState.championCount > 0) {
            MenuBadge("👑 ${uiState.championCount}x", Color(0xFFFACC15))
          }
          if (uiState.is4dModeEnabled) {
            MenuBadge("🌌 4D", Color(0xFFE879F9))
          } else if (uiState.is3dModeEnabled) {
            MenuBadge("🧊 3D", Color(0xFF22D3EE))
          }
        }

        // Level XP Progress Bar
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "⭐ Progresso de Nível",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF7DD3FC)
            )
            Text(
              text = "${uiState.playerXp}/${uiState.xpToNextLevel} XP",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFE2E8F0)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { (uiState.playerXp.toFloat() / uiState.xpToNextLevel.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF38BDF8),
            trackColor = Color(0xFF0F172A)
          )
        }

        // Pesquisar Contas / Código Secreto (admin01)
        var accountSearchText by remember { mutableStateOf("") }
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF0F172A),
          border = BorderStroke(1.2.dp, Color(0xFF38BDF8)),
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "🔍 Pesquisar Contas / Código Secreto",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
              )
              if (uiState.isAdminMode) {
                Text(
                  text = "👑 Admin Ativo",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFDE047)
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedTextField(
                value = accountSearchText,
                onValueChange = { accountSearchText = it },
                placeholder = { Text("Digite admin01 ou @conta...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedBorderColor = Color(0xFF38BDF8),
                  unfocusedBorderColor = Color(0xFF475569)
                ),
                modifier = Modifier
                  .weight(1f)
                  .defaultMinSize(minHeight = 44.dp)
                  .testTag("menu_account_search_input")
              )
              Button(
                onClick = { onSearchAccount(accountSearchText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("menu_account_search_button")
              ) {
                Text("Buscar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }

        if (uiState.isRewardsBlocked) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF7F1D1D),
            border = BorderStroke(1.dp, Color(0xFFF87171)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "🚫 Modo Deus/Modificado: sem conquistas ou XP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFECACA),
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = onRestoreDefaultFair, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text("Reativar", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE047))
              }
            }
          }
        }

        // Quick Difficulty Selector at Start (includes Fácil + Normal + Difícil + Elite Ghost Hunter)
        Column(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              Difficulty.CALMO to "🐢 Fácil",
              Difficulty.NORMAL to "⚖️ Normal"
            ).forEach { (diff, label) ->
              val selected = uiState.difficulty == diff
              OutlinedButton(
                onClick = { onDifficultyChange(diff) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (selected) Color(0xFF7C3AED) else Color(0xFF0F172A)
                ),
                border = BorderStroke(1.dp, if (selected) Color(0xFFC084FC) else Color(0xFF334155)),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
              ) {
                Text(
                  text = label,
                  fontSize = 11.sp,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                  color = if (selected) Color.White else Color(0xFF94A3B8)
                )
              }
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              Difficulty.DESAFIO to "🔥 Difícil",
              Difficulty.ELITE to "👻 Elite (Fantasma)"
            ).forEach { (diff, label) ->
              val selected = uiState.difficulty == diff
              OutlinedButton(
                onClick = { onDifficultyChange(diff) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (selected) {
                    if (diff == Difficulty.ELITE) Color(0xFF9333EA) else Color(0xFF7C3AED)
                  } else Color(0xFF0F172A)
                ),
                border = BorderStroke(
                  1.dp,
                  if (selected) Color(0xFFFDE047) else Color(0xFF334155)
                ),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("diff_${diff.name.lowercase()}")
              ) {
                Text(
                  text = label,
                  fontSize = 11.sp,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                  color = if (selected) Color.White else Color(0xFF94A3B8)
                )
              }
            }
          }
        }

        // Objectives summary
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF0F172A),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            RuleItem("1️⃣", if (uiState.difficulty == Difficulty.ELITE) {
              "Modo ELITE: Você é o Fantasma! Pegue o gato corredor 3x antes de ele salvar 10!"
            } else {
              "Resgate 10 gatinhos para vencer, subir de nível e ganhar pontos!"
            })
            RuleItem("2️⃣", "Fácil dá conquistas e pontos normal! No Difícil o fantasma prevê seus passos.")
            RuleItem("3️⃣", "Salve tudo na Conta Google, faça amizades, compartilhe e compre skins!")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Start Normal Game Button
        Button(
          onClick = onStartNormal,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (uiState.difficulty == Difficulty.ELITE) Color(0xFF9333EA) else Color(0xFF10B981)
          ),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .testTag("start_game_button")
        ) {
          Text(
            text = if (uiState.difficulty == Difficulty.ELITE) "JOGAR ELITE (CAÇAR COMO FANTASMA) 👻" else "MODO CAMPEÃO (10 GATOS) 🐾",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Endless Mode Button (Unlocked at 1 win!)
        if (uiState.isEndlessUnlocked) {
          Button(
            onClick = onStartEndless,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(18.dp))
              .testTag("menu_endless_button")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                "♾️ MODO INFINITO SEM FIM 🚀",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              if (uiState.bestEndlessCats > 0) {
                Text(
                  "Recorde: ${uiState.bestEndlessCats} gatos infinitos",
                  fontSize = 10.sp,
                  color = Color(0xFFE9D5FF)
                )
              }
            }
          }
        } else {
          OutlinedButton(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            colors = ButtonDefaults.outlinedButtonColors(
              disabledContainerColor = Color(0x660F172A),
              disabledContentColor = Color(0xFF64748B)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("menu_endless_locked")
          ) {
            Text(
              "🔒 Modo Infinito (Vença 1x para liberar)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Modo Bots Button (Configurable 1..20 Bots + Secret Mode when >2 Bots!)
        Button(
          onClick = { showBotsConfigDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .border(1.5.dp, Color(0xFF2DD4BF), RoundedCornerShape(18.dp))
            .testTag("start_bots_mode_button")
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "🤖 MODO BOTS (1 A 20 BOTS • SECRETO) 🏁",
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
            Text(
              text = "Escolha a quantidade de bots • 3+ bots libera Modo Secreto!",
              fontSize = 10.sp,
              color = Color(0xFFCCFBF1)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 40 Modes Selector Button
        Button(
          onClick = onOpenModes,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.5.dp, Color(0xFF7DD3FC), RoundedCornerShape(18.dp))
            .testTag("menu_modes_button")
        ) {
          Text(
            "🎮 40 MODOS DE JOGO (${uiState.unlockedModesCount}/${ALL_40_MODES.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: Estúdio Criativo & Skins
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenCreator,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF34D399)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_creator_button")
          ) {
            Text(
              "🛠️ Criar Mapa\n& Meu Item",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF6EE7B7),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }

          OutlinedButton(
            onClick = onOpenSkins,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE879F9)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_skins_button")
          ) {
            Text(
              "👗 Skins\n(${uiState.unlockedSkinsCount}/${ALL_HERO_SKINS.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFE879F9),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Conquistas & 40 Itens
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenAchievements,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFFACC15)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_achievements_button")
          ) {
            Text(
              "🏅 40 Conquistas\n(${uiState.unlockedAchievements.size}/${ALL_40_ACHIEVEMENTS.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFDE047),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }

          OutlinedButton(
            onClick = onOpenItems,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFF472B6)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_items_button")
          ) {
            Text(
              "🎁 40 Itens\n(${uiState.discoveredItems.size}/${ALL_40_ITEMS.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF472B6),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Galeria & Ajustes
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenGallery,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_gallery_button")
          ) {
            Text(
              "📖 Galeria\n(10 Felinos)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF38BDF8),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }

          OutlinedButton(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF94A3B8)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_settings_button")
          ) {
            Text(
              "⚙️ Ajustes, 3D/4D\n& Velocidade",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFCBD5E1),
              textAlign = TextAlign.Center,
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 4: Loja (Pontos/Trocas) & Multiplayer (200+ players + Conta Google + Amizades)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onOpenShop,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(0.9f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_shop_button")
          ) {
            Text(
              "🛒 Loja\n(${uiState.spendablePoints} pts)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              textAlign = TextAlign.Center,
              lineHeight = 13.sp
            )
          }

          Button(
            onClick = onOpenOnline,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1.3f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_online_button")
          ) {
            Text(
              text = if (uiState.activeRoomCode != null) {
                "👥 Sala ${uiState.activeRoomCode} (${uiState.multiplayerPlayerCount}/${uiState.multiplayerMaxPlayers})"
              } else {
                "🌐 Online 200+ • ☁️ Save\n& 🤝 Amizades"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              textAlign = TextAlign.Center,
              lineHeight = 13.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Community Replays & Videos Feed Button
        Button(
          onClick = onOpenVideosFeed,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .border(1.2.dp, Color(0xFFC084FC), RoundedCornerShape(14.dp))
            .testTag("menu_videos_feed_button")
        ) {
          Text(
            text = "📺 Replays Gravados & Feed da Galera 🎬",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 5: IA Dicas & Notificação Miau
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenAi,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF22D3EE)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_ai_button")
          ) {
            Text(
              "🤖 IA Guia Felino",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF67E8F9),
              textAlign = TextAlign.Center
            )
          }

          OutlinedButton(
            onClick = onSendMeowNotification,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFF472B6)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_meow_notify_button")
          ) {
            Text(
              "🔔 Notif. Miau!",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF9A8D4),
              textAlign = TextAlign.Center
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Auto-Update Quick Button
        OutlinedButton(
          onClick = onCheckUpdate,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(
            1.2.dp,
            if (uiState.isUpdateAvailable) Color(0xFF34D399) else Color(0xFF475569)
          ),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (uiState.isUpdateAvailable) Color(0xFF064E3B) else Color(0x660F172A)
          ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("menu_check_update_button")
        ) {
          val label = when {
            uiState.isUpdateDownloading -> "⬇️ Baixando atualização... ${uiState.updateProgressPercent}%"
            uiState.isUpdateAvailable -> "🚀 Nova versão disponível! Toque para atualizar"
            uiState.appUpdateStatusText.isNotBlank() -> uiState.appUpdateStatusText
            else -> "🔄 Atualização Auto (v${BuildConfig.VERSION_NAME}) • Verificar Agora"
          }
          Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (uiState.isUpdateAvailable) Color(0xFF6EE7B7) else Color(0xFFCBD5E1),
            textAlign = TextAlign.Center
          )
        }
      }
    }

    if (showBotsConfigDialog) {
      MenuBotsSetupDialog(
        onStart = { count, secret, tasks, roles, shapeshifter, vents, rolePref ->
          showBotsConfigDialog = false
          onStartCustomBots(count, secret, tasks, roles, shapeshifter, vents, rolePref)
        },
        onDismiss = { showBotsConfigDialog = false }
      )
    }
  }
}

@Composable
private fun MenuBotsSetupDialog(
  onStart: (
    botCount: Int,
    secretMode: Boolean,
    tasksEnabled: Boolean,
    rolesEnabled: Boolean,
    shapeshifterEnabled: Boolean,
    ventsEnabled: Boolean,
    rolePreference: SecretRolePreference
  ) -> Unit,
  onDismiss: () -> Unit
) {
  var botCount by remember { mutableIntStateOf(20) }
  var secretMode by remember { mutableStateOf(false) }
  var tasksEnabled by remember { mutableStateOf(true) }
  var rolesEnabled by remember { mutableStateOf(true) }
  var shapeshifterEnabled by remember { mutableStateOf(true) }
  var ventsEnabled by remember { mutableStateOf(true) }
  var rolePreference by remember { mutableStateOf(SecretRolePreference.RANDOM) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      border = BorderStroke(2.dp, Color(0xFF2DD4BF)),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 650.dp)
        .testTag("menu_bots_setup_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "🤖 Configurar Modo Bots",
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFF2DD4BF)
        )
        Text(
          text = "Defina a quantidade de bots para jogar. Com mais de 2 bots, ative o Modo Secreto com Funções, Metamorfo e Dutos!",
          fontSize = 11.sp,
          color = Color(0xFFCBD5E1),
          modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        Text(
          text = "⚙️ Quantidade de Bots: $botCount",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFDE047)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { botCount = (botCount - 1).coerceAtLeast(1) },
            enabled = botCount > 1,
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
              .size(48.dp)
              .testTag("menu_bots_decrease")
          ) {
            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF475569)),
            modifier = Modifier.weight(1f).height(48.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "$botCount Bot(s)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }
          }
          OutlinedButton(
            onClick = { botCount = (botCount + 1).coerceAtMost(20) },
            enabled = botCount < 20,
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
              .size(48.dp)
              .testTag("menu_bots_increase")
          ) {
            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(1, 2, 3, 5, 8, 12, 20).forEach { preset ->
            FilterChip(
              selected = botCount == preset,
              onClick = { botCount = preset },
              label = { Text("$preset", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
              modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            )
          }
        }

        if (botCount > 2) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            onClick = { secretMode = !secretMode },
            shape = RoundedCornerShape(12.dp),
            color = if (secretMode) Color(0xFF450A0A) else Color(0xFF1E293B),
            border = BorderStroke(
              width = if (secretMode) 1.5.dp else 1.dp,
              color = if (secretMode) Color(0xFFEF4444) else Color(0xFF475569)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("menu_bots_secret_toggle")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = "🕵️ Modo Secreto (Mais de 2 Bots)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black,
                  color = if (secretMode) Color(0xFFFCA5A5) else Color.White
                )
                Text(
                  text = "1 entre você e os $botCount bots é o Fantasma disfarçado! Denuncie e vote para banir!",
                  fontSize = 10.sp,
                  color = Color(0xFFCBD5E1)
                )
              }
              Switch(
                checked = secretMode,
                onCheckedChange = { secretMode = it },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = Color(0xFFDC2626)
                )
              )
            }
          }

          if (secretMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "🎛️ Seletor do Modo Secreto (Funções & Dutos):",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFE879F9)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Switch: Funções para Jogadores (Engenheiro, Detetive, Médico)
            SecretOptionSwitchCard(
              title = "✨ Funções para Jogadores",
              subtitle = "Ativa Engenheiro (Dutos), Detetive (Radar) e Médico (Escudo) para a tripulação!",
              checked = rolesEnabled,
              accentColor = Color(0xFF38BDF8),
              bgActiveColor = Color(0xFF0C4A6E),
              testTag = "menu_bots_roles_toggle",
              onToggle = { rolesEnabled = it }
            )

            if (rolesEnabled) {
              Spacer(modifier = Modifier.height(6.dp))
              // Switch: Impostor Metamorfo
              SecretOptionSwitchCard(
                title = "🎭 Impostor Metamorfo",
                subtitle = "O Impostor pode se transformar em qualquer jogador vivo para incriminar os outros!",
                checked = shapeshifterEnabled,
                accentColor = Color(0xFFA855F7),
                bgActiveColor = Color(0xFF4C1D95),
                testTag = "menu_bots_shapeshifter_toggle",
                onToggle = { shapeshifterEnabled = it }
              )

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "🎯 Sua Função na Partida:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE047)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                SecretRolePreference.entries.forEach { pref ->
                  if (pref == SecretRolePreference.SHAPESHIFTER && !shapeshifterEnabled) return@forEach
                  FilterChip(
                    selected = rolePreference == pref,
                    onClick = { rolePreference = pref },
                    label = {
                      Text(
                        text = "${pref.emoji} ${pref.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                      )
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            // Switch: Dutos (Vents)
            SecretOptionSwitchCard(
              title = "🕳️ Dutos no Mapa (Vents)",
              subtitle = "5 dutos interligados pelo mapa para o Impostor e o Engenheiro se esconderem e viajarem!",
              checked = ventsEnabled,
              accentColor = Color(0xFF14B8A6),
              bgActiveColor = Color(0xFF134E4A),
              testTag = "menu_bots_vents_toggle",
              onToggle = { ventsEnabled = it }
            )

            Spacer(modifier = Modifier.height(6.dp))
            // Switch: Tarefas da Nave
            SecretOptionSwitchCard(
              title = "📋 Tarefas da Nave (Tasks)",
              subtitle = "Mini-jogos nas salas (Fios, Leite, etc). Se morrer, você fica espectando e ainda ajuda!",
              checked = tasksEnabled,
              accentColor = Color(0xFF10B981),
              bgActiveColor = Color(0xFF064E3B),
              testTag = "menu_bots_tasks_toggle",
              onToggle = { tasksEnabled = it }
            )
          }
        } else {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "💡 Selecione mais de 2 bots (3+) para liberar o Modo Secreto!",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val effectiveSecret = botCount > 2 && secretMode
        val effectiveTasks = effectiveSecret && tasksEnabled
        val effectiveRoles = effectiveSecret && rolesEnabled
        val effectiveShapeshifter = effectiveRoles && shapeshifterEnabled
        val effectiveVents = effectiveSecret && ventsEnabled
        Button(
          onClick = {
            onStart(
              botCount,
              effectiveSecret,
              effectiveTasks,
              effectiveRoles,
              effectiveShapeshifter,
              effectiveVents,
              rolePreference
            )
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (effectiveSecret) Color(0xFFDC2626) else Color(0xFF059669)
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("menu_bots_start_button")
        ) {
          Text(
            text = if (effectiveSecret) "🕵️ Jogar Modo Secreto ($botCount Bots)" else "▶️ Jogar com $botCount Bot(s)!",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
        ) {
          Text("Cancelar", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun SecretOptionSwitchCard(
  title: String,
  subtitle: String,
  checked: Boolean,
  accentColor: Color,
  bgActiveColor: Color,
  testTag: String,
  onToggle: (Boolean) -> Unit
) {
  Surface(
    onClick = { onToggle(!checked) },
    shape = RoundedCornerShape(12.dp),
    color = if (checked) bgActiveColor else Color(0xFF1E293B),
    border = BorderStroke(
      width = if (checked) 1.5.dp else 1.dp,
      color = if (checked) accentColor else Color(0xFF475569)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 48.dp)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = title,
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          color = if (checked) Color.White else Color(0xFFCBD5E1)
        )
        Text(
          text = subtitle,
          fontSize = 10.sp,
          color = Color(0xFFCBD5E1)
        )
      }
      Switch(
        checked = checked,
        onCheckedChange = onToggle,
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = accentColor
        )
      )
    }
  }
}

@Composable
private fun MenuBadge(text: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = color.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, color)
  ) {
    Text(
      text = text,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

@Composable
fun RuleItem(number: String, text: String) {
  Row(
    modifier = Modifier.padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(number, fontSize = 13.sp)
    Spacer(modifier = Modifier.width(8.dp))
    Text(text, fontSize = 11.sp, color = Color(0xFFE2E8F0))
  }
}
