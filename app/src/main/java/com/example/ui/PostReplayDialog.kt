package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import com.example.data.GameplayVideoPost
import com.example.model.ALL_HERO_SKINS
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

@Composable
fun PostReplayDialog(
  uiState: GameUiState,
  onPost: (title: String) -> Unit,
  onOpenCommunityFeed: () -> Unit = {},
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  var isSigningIn by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }
  var showLocalPreview by remember { mutableStateOf(false) }

  val skinDef = remember(uiState.selectedSkinId) {
    ALL_HERO_SKINS.find { it.id == uiState.selectedSkinId } ?: ALL_HERO_SKINS.first()
  }

  val modeName = uiState.lastRecordedReplayModeName.ifBlank { uiState.currentMode.name }
  val score = if (uiState.lastRecordedReplayScore > 0) uiState.lastRecordedReplayScore else uiState.currentScore
  val durSec = if (uiState.lastRecordedReplayDurationSec > 0f) uiState.lastRecordedReplayDurationSec else uiState.gameTimeSeconds
  val framesCount = uiState.recordedFramesCount

  var customTitle by remember {
    mutableStateOf("🎬 Gameplay Épica: $score pts em $modeName!")
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
      shape = RoundedCornerShape(24.dp),
      border = BorderStroke(2.dp, Color(0xFF8B5CF6)),
      modifier = modifier
        .fillMaxWidth()
        .padding(8.dp)
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .testTag("post_replay_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFF312E81))
            .border(2.dp, Color(0xFFC084FC), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text("🎬", fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Gravação Concluída!",
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFDE047)
        )

        Text(
          text = "Assista à prévia da sua partida gravada e publique no feed para outros jogadores verem!",
          fontSize = 12.sp,
          color = Color(0xFFCBD5E1),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Match Summary Pill
        Surface(
          color = Color(0xFF0F172A),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, Color(0xFF475569)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Text(skinDef.badgeEmoji, fontSize = 24.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = modeName,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "$framesCount quadros gravados",
                  fontSize = 10.sp,
                  color = Color(0xFF34D399)
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "🏆 $score pts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF34D399)
              )
              Text(
                text = "⏱️ ${String.format("%.1f", durSec)}s",
                fontSize = 11.sp,
                color = Color(0xFF38BDF8)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Local preview button so player can immediately verify the real recording!
        OutlinedButton(
          onClick = { showLocalPreview = true },
          enabled = uiState.lastRecordedReplayJson.isNotBlank(),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("preview_recorded_replay_button")
        ) {
          Text(
            text = "👁️ Assistir Minha Gravação Agora",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = customTitle,
          onValueChange = { customTitle = it.take(100) },
          label = { Text("Título do Vídeo") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF8B5CF6),
            unfocusedBorderColor = Color(0xFF475569)
          ),
          modifier = Modifier.fillMaxWidth().testTag("replay_title_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (currentUser == null) {
          Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "🔑 Entre com Google para postar no feed e ver replays de outros jogadores",
                fontSize = 11.sp,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = {
                  isSigningIn = true
                  authError = null
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = { isSigningIn = false },
                    onAuthError = { msg ->
                      isSigningIn = false
                      authError = msg
                    },
                    scope = scope,
                    onAuthCancelled = { isSigningIn = false }
                  )
                },
                enabled = !isSigningIn,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("post_replay_google_signin_button")
              ) {
                if (isSigningIn) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                  Text("🔑 Entrar com Conta Google", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
              }
              if (!authError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("⚠️ $authError", fontSize = 11.sp, color = Color(0xFFF87171))
              }
            }
          }
        } else {
          Button(
            onClick = { onPost(customTitle) },
            enabled = !uiState.isPostingReplay,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("publish_replay_button")
          ) {
            if (uiState.isPostingReplay) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Publicando...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
              Text("🎬 Publicar e Ver Feed 🚀", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onOpenCommunityFeed,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, Color(0xFF64748B)),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("open_feed_from_post_dialog_button")
        ) {
          Text("📺 Ver Replays de Outros Jogadores", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(
          onClick = onDismiss,
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("cancel_post_replay_button")
        ) {
          Text("Fechar", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
      }
    }
  }

  if (showLocalPreview) {
    val previewPost = GameplayVideoPost(
      postId = "local_preview",
      userId = currentUser?.uid ?: "local",
      username = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Você",
      userSkinId = uiState.selectedSkinId,
      title = customTitle.ifBlank { "Prévia da Minha Gameplay" },
      score = score.toLong(),
      modeName = modeName,
      durationSec = durSec,
      replayFramesJson = uiState.lastRecordedReplayJson
    )
    ReplayViewerDialog(
      post = previewPost,
      isLocalPreview = true,
      onDismiss = { showLocalPreview = false }
    )
  }
}
