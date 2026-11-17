package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.SecretTaskStationDef
import com.example.model.SecretTaskType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SecretTaskDialog(
  station: SecretTaskStationDef,
  onComplete: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var isDone by remember { mutableStateOf(station.isCompletedByPlayer) }
  val scope = rememberCoroutineScope()

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      border = BorderStroke(2.dp, if (isDone) Color(0xFF10B981) else Color(0xFF38BDF8)),
      modifier = Modifier
        .fillMaxWidth()
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .testTag("secret_task_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(station.emoji, fontSize = 26.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = station.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Text(
                text = "Local: ${station.roomName}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Fechar Tarefa", tint = Color(0xFF94A3B8))
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isDone) {
          // Task already completed state
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF064E3B),
            border = BorderStroke(1.5.dp, Color(0xFF34D399)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("✅", fontSize = 42.sp)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "TAREFA CONCLUÍDA!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFA7F3D0)
              )
              Text(
                text = "Você ajudou a tripulação a encher a barra de progresso!",
                fontSize = 11.sp,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Text("Fechar", fontWeight = FontWeight.Black, color = Color.White)
          }
        } else {
          // Interactive mini-game based on station.type
          when (station.type) {
            SecretTaskType.WIRES -> {
              WiresMiniGame(
                onFinished = {
                  isDone = true
                  scope.launch {
                    delay(400)
                    onComplete(station.id)
                  }
                }
              )
            }
            SecretTaskType.MILK -> {
              MilkFillMiniGame(
                onFinished = {
                  isDone = true
                  scope.launch {
                    delay(400)
                    onComplete(station.id)
                  }
                }
              )
            }
            SecretTaskType.DOWNLOAD -> {
              DownloadMiniGame(
                onFinished = {
                  isDone = true
                  scope.launch {
                    delay(400)
                    onComplete(station.id)
                  }
                }
              )
            }
            SecretTaskType.FILTER -> {
              FilterCleanMiniGame(
                onFinished = {
                  isDone = true
                  scope.launch {
                    delay(400)
                    onComplete(station.id)
                  }
                }
              )
            }
            SecretTaskType.ANTENNA -> {
              AntennaAlignMiniGame(
                onFinished = {
                  isDone = true
                  scope.launch {
                    delay(400)
                    onComplete(station.id)
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WiresMiniGame(onFinished: () -> Unit) {
  val wireColors = remember {
    listOf(
      "Vermelho" to Color(0xFFEF4444),
      "Azul" to Color(0xFF3B82F6),
      "Amarelo" to Color(0xFFFBBF24),
      "Verde" to Color(0xFF10B981)
    )
  }
  var connectedCount by remember { mutableIntStateOf(0) }

  Text(
    text = "Conecte os 4 cabos de energia:",
    fontSize = 12.sp,
    color = Color(0xFFCBD5E1),
    fontWeight = FontWeight.Bold
  )
  Spacer(modifier = Modifier.height(10.dp))

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    wireColors.forEachIndexed { index, (label, color) ->
      val isConnected = index < connectedCount
      Surface(
        onClick = {
          if (index == connectedCount) {
            connectedCount++
            if (connectedCount == wireColors.size) {
              onFinished()
            }
          }
        },
        shape = RoundedCornerShape(12.dp),
        color = if (isConnected) color.copy(alpha = 0.25f) else Color(0xFF1E293B),
        border = BorderStroke(1.5.dp, if (isConnected) color else Color(0xFF475569)),
        modifier = Modifier.fillMaxWidth().height(48.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(16.dp)
                .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Cabo $label",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          Text(
            text = if (isConnected) "⚡ CONECTADO" else "👉 Toque p/ Ligar",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isConnected) color else Color(0xFF94A3B8)
          )
        }
      }
    }
  }
}

@Composable
private fun MilkFillMiniGame(onFinished: () -> Unit) {
  var fillsCount by remember { mutableIntStateOf(0) }
  val target = 5
  val progress = (fillsCount.toFloat() / target.toFloat()).coerceIn(0f, 1f)

  Text(
    text = "Toque no botão para encher a tigela de leite:",
    fontSize = 12.sp,
    color = Color(0xFFCBD5E1),
    fontWeight = FontWeight.Bold
  )
  Spacer(modifier = Modifier.height(10.dp))

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = if (progress >= 1f) "🥛 TIGELA CHEIA!" else "🥛 Nível de Leite: ${(progress * 100).toInt()}%",
      fontSize = 14.sp,
      fontWeight = FontWeight.Black,
      color = Color(0xFFFDE047)
    )
    Spacer(modifier = Modifier.height(6.dp))
    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth()
        .height(14.dp)
        .clip(RoundedCornerShape(7.dp)),
      color = Color(0xFF38BDF8),
      trackColor = Color(0xFF1E293B)
    )

    Spacer(modifier = Modifier.height(14.dp))

    Button(
      onClick = {
        if (fillsCount < target) {
          fillsCount++
          if (fillsCount >= target) {
            onFinished()
          }
        }
      },
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
    ) {
      Text(
        text = if (fillsCount < target) "🥛 Despejar Leite ($fillsCount/$target)" else "✅ Concluído!",
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        color = Color.White
      )
    }
  }
}

@Composable
private fun DownloadMiniGame(onFinished: () -> Unit) {
  var isDownloading by remember { mutableStateOf(false) }
  var downloadProgress by remember { mutableFloatStateOf(0f) }
  val animatedProgress by animateFloatAsState(
    targetValue = downloadProgress,
    animationSpec = tween(durationMillis = 2000),
    label = "download_progress"
  )

  LaunchedEffect(isDownloading) {
    if (isDownloading) {
      downloadProgress = 1.0f
      delay(2200)
      onFinished()
    }
  }

  Text(
    text = "Baixe os arquivos de segurança da nave/mansão:",
    fontSize = 12.sp,
    color = Color(0xFFCBD5E1),
    fontWeight = FontWeight.Bold
  )
  Spacer(modifier = Modifier.height(10.dp))

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "💾 Download: ${(animatedProgress * 100).toInt()}%",
      fontSize = 14.sp,
      fontWeight = FontWeight.Black,
      color = Color(0xFF38BDF8)
    )
    Spacer(modifier = Modifier.height(6.dp))
    LinearProgressIndicator(
      progress = { animatedProgress },
      modifier = Modifier
        .fillMaxWidth()
        .height(14.dp)
        .clip(RoundedCornerShape(7.dp)),
      color = Color(0xFF10B981),
      trackColor = Color(0xFF1E293B)
    )

    Spacer(modifier = Modifier.height(14.dp))

    Button(
      onClick = {
        if (!isDownloading) {
          isDownloading = true
        }
      },
      enabled = !isDownloading,
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
    ) {
      Text(
        text = if (isDownloading) "⬇️ Baixando Arquivos..." else "⬇️ Iniciar Download de Dados",
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        color = Color.White
      )
    }
  }
}

@Composable
private fun FilterCleanMiniGame(onFinished: () -> Unit) {
  val items = remember { mutableStateListOf(0, 1, 2, 3) }

  Text(
    text = "Toque em cada uma das 4 sujeiras para limpar o filtro:",
    fontSize = 12.sp,
    color = Color(0xFFCBD5E1),
    fontWeight = FontWeight.Bold
  )
  Spacer(modifier = Modifier.height(10.dp))

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
  ) {
    listOf("🍂", "🐾", "🧶", "Dust").forEachIndexed { idx, emoji ->
      val remaining = items.contains(idx)
      Surface(
        onClick = {
          if (remaining) {
            items.remove(idx)
            if (items.isEmpty()) {
              onFinished()
            }
          }
        },
        shape = CircleShape,
        color = if (remaining) Color(0xFF334155) else Color(0xFF064E3B),
        border = BorderStroke(2.dp, if (remaining) Color(0xFF94A3B8) else Color(0xFF10B981)),
        modifier = Modifier.size(56.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(text = if (remaining) emoji else "✨", fontSize = 22.sp)
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(12.dp))
  Text(
    text = if (items.isEmpty()) "✅ Filtro 100% Limpo!" else "Faltam ${items.size} para limpar",
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    color = if (items.isEmpty()) Color(0xFF34D399) else Color(0xFFFDE047)
  )
}

@Composable
private fun AntennaAlignMiniGame(onFinished: () -> Unit) {
  var sliderValue by remember { mutableFloatStateOf(70f) }
  val target = 88.5f
  val isAligned = abs(sliderValue - target) <= 2.5f

  Text(
    text = "Ajuste o sinal para 88.5 MHz (sinal verde):",
    fontSize = 12.sp,
    color = Color(0xFFCBD5E1),
    fontWeight = FontWeight.Bold
  )
  Spacer(modifier = Modifier.height(10.dp))

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "📡 Frequência: ${String.format("%.1f", sliderValue)} MHz",
      fontSize = 15.sp,
      fontWeight = FontWeight.Black,
      color = if (isAligned) Color(0xFF34D399) else Color(0xFFFDE047)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = if (isAligned) "🟢 SINAL SINTONIZADO! TOQUE EM FIXAR" else "🔴 FORA DE SINTONIA (Alvo: 88.5 MHz)",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = if (isAligned) Color(0xFF34D399) else Color(0xFFF87171)
    )

    Spacer(modifier = Modifier.height(8.dp))
    Slider(
      value = sliderValue,
      onValueChange = { sliderValue = it },
      valueRange = 70f..110f,
      colors = SliderDefaults.colors(
        thumbColor = if (isAligned) Color(0xFF10B981) else Color(0xFF38BDF8),
        activeTrackColor = if (isAligned) Color(0xFF10B981) else Color(0xFF38BDF8)
      ),
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))
    Button(
      onClick = {
        if (isAligned) {
          onFinished()
        }
      },
      enabled = isAligned,
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
      Text(
        text = if (isAligned) "📡 Fixar Frequência ✅" else "Alinhe o Slider até 88.5 MHz",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}
