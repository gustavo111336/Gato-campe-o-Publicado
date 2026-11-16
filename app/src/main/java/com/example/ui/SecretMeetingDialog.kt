package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MultiplayerRoom
import com.example.data.RoomPlayer

@Composable
fun SecretMeetingDialog(
  room: MultiplayerRoom,
  roomPlayers: List<RoomPlayer>,
  currentUserId: String,
  isBusy: Boolean,
  onVoteTarget: (String) -> Unit,
  onResolveMeeting: () -> Unit,
  onContinueAfterResult: () -> Unit,
  onLeaveToLobby: () -> Unit
) {
  val eliminatedSet = remember(room.eliminatedUserIdsCsv) {
    room.eliminatedUserIdsCsv
      .split(",")
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .toSet()
  }
  val isLocalAlive = !eliminatedSet.contains(currentUserId)
  val alivePlayers = remember(roomPlayers, eliminatedSet) {
    roomPlayers.filter { !eliminatedSet.contains(it.userId) }
  }
  val eliminatedPlayers = remember(roomPlayers, eliminatedSet) {
    roomPlayers.filter { eliminatedSet.contains(it.userId) }
  }
  val myPlayer = remember(roomPlayers, currentUserId) {
    roomPlayers.find { it.userId == currentUserId }
  }
  val myVoteThisRound = if (myPlayer != null && myPlayer.banishVoteRound == room.meetingRound) {
    myPlayer.banishVoteForUid
  } else {
    ""
  }

  val voteCounts = remember(alivePlayers, room.meetingRound) {
    alivePlayers
      .filter { it.banishVoteRound == room.meetingRound && it.banishVoteForUid.isNotBlank() }
      .groupingBy { it.banishVoteForUid }
      .eachCount()
  }
  val skipVotes = voteCounts["SKIP"] ?: 0
  val totalVotesCast = voteCounts.values.sum()

  Dialog(
    onDismissRequest = {},
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = false,
      dismissOnClickOutside = false
    )
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      border = BorderStroke(
        width = 2.dp,
        color = if (room.meetingStatus == "result") {
          if (room.lastBanishedWasImpostor) Color(0xFF10B981) else Color(0xFFEF4444)
        } else {
          Color(0xFFEF4444)
        }
      ),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 560.dp)
        .heightIn(max = 680.dp)
        .testTag("secret_meeting_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(18.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (room.meetingStatus == "active") {
          Text(
            text = "🚨 REUNIÃO DE EMERGÊNCIA",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFCA5A5),
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Denúncia feita por @${room.meetingCallerUsername.ifBlank { "Gato" }} • Rodada ${room.meetingRound}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Quem é o Fantasma disfarçado? Acuse um jogador para bani-lo! (${totalVotesCast}/${alivePlayers.size} votos)",
            fontSize = 12.sp,
            color = Color(0xFFE2E8F0),
            textAlign = TextAlign.Center
          )

          if (!isLocalAlive) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF334155),
              border = BorderStroke(1.dp, Color(0xFF94A3B8)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "💀 Você foi eliminado e está assistindo à reunião como espectador.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            alivePlayers.forEach { player ->
              val votes = voteCounts[player.userId] ?: 0
              val votedForThis = myVoteThisRound == player.userId
              val isMe = player.userId == currentUserId

              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (votedForThis) Color(0xFF450A0A) else Color(0xFF1E293B),
                border = BorderStroke(
                  width = if (votedForThis) 2.dp else 1.dp,
                  color = if (votedForThis) Color(0xFFEF4444) else Color(0xFF334155)
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "🐱 @${player.username}${if (isMe) " (Você)" else ""}",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "🗳️ $votes voto(s) para banir • 🐾 ${player.score} gatos",
                      fontSize = 11.sp,
                      color = if (votes > 0) Color(0xFFFCA5A5) else Color(0xFF94A3B8)
                    )
                  }

                  if (isLocalAlive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                      onClick = { onVoteTarget(player.userId) },
                      enabled = !isBusy,
                      colors = ButtonDefaults.buttonColors(
                        containerColor = if (votedForThis) Color(0xFFDC2626) else Color(0xFF7F1D1D)
                      ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("accuse_player_${player.userId}")
                    ) {
                      Text(
                        text = if (votedForThis) "✓ Acusado" else "🗳️ Acusar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                      )
                    }
                  }
                }
              }
            }
          }

          if (eliminatedPlayers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "💀 Eliminados: ${eliminatedPlayers.joinToString(", ") { "@${it.username}" }}",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8),
              textAlign = TextAlign.Center
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (isLocalAlive) {
            OutlinedButton(
              onClick = { onVoteTarget("SKIP") },
              enabled = !isBusy,
              border = BorderStroke(
                width = if (myVoteThisRound == "SKIP") 2.dp else 1.dp,
                color = if (myVoteThisRound == "SKIP") Color(0xFFFDE047) else Color(0xFF64748B)
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("skip_banish_vote_button")
            ) {
              Text(
                text = "⏭️ Pular Voto ($skipVotes)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (myVoteThisRound == "SKIP") Color(0xFFFDE047) else Color(0xFFCBD5E1)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
          }

          Button(
            onClick = onResolveMeeting,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 52.dp)
              .testTag("resolve_meeting_button")
          ) {
            Text(
              text = "⚖️ Encerrar Votação e Banir",
              fontSize = 14.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF0F172A)
            )
          }
        } else {
          // room.meetingStatus == "result"
          val headlineEmoji = when {
            room.lastBanishedUserId.isBlank() -> "⚖️"
            room.lastBanishedWasImpostor -> "🎉"
            else -> "😱"
          }
          val headlineTitle = when {
            room.lastBanishedUserId.isBlank() -> "NINGUÉM FOI BANIDO"
            room.lastBanishedWasImpostor -> "IMPOSTOR DESCOBERTO!"
            else -> "INOCENTE BANIDO!"
          }
          Text(
            text = "$headlineEmoji $headlineTitle",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = when {
              room.lastBanishedUserId.isBlank() -> Color(0xFFFDE047)
              room.lastBanishedWasImpostor -> Color(0xFF4ADE80)
              else -> Color(0xFFF87171)
            },
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(14.dp))

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.5.dp, Color(0xFF475569)),
            modifier = Modifier.fillMaxWidth()
          ) {
            val rawResult = room.meetingResultText.ifBlank {
              "Votação encerrada."
            }
            val styledText = buildAnnotatedString {
              val start = rawResult.indexOf("**")
              val end = if (start >= 0) rawResult.indexOf("**", start + 2) else -1
              if (start >= 0 && end > start + 2) {
                append(rawResult.substring(0, start))
                withStyle(SpanStyle(fontWeight = FontWeight.Black, color = Color(0xFFFDE047))) {
                  append("**${rawResult.substring(start + 2, end)}**")
                }
                append(rawResult.substring(end + 2))
              } else {
                append(rawResult)
              }
            }
            Text(
              text = styledText,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              textAlign = TextAlign.Center,
              lineHeight = 26.sp,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .testTag("meeting_result_text")
            )
          }

          if (room.status == "finished") {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "🏆 Partida encerrada! Vencedor: ${room.winnerUsername.ifBlank { "Gatos Campeões" }}",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF38BDF8),
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = onLeaveToLobby,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag("finish_after_meeting_button")
            ) {
              Text(
                text = "🏆 Voltar ao Menu / Sala",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }
          } else {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = onContinueAfterResult,
              enabled = !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag("continue_after_meeting_button")
            ) {
              Text(
                text = "▶️ Continuar Partida",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }
          }
        }
      }
    }
  }
}
