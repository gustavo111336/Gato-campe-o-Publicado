package com.example.model

import androidx.compose.ui.graphics.Color

enum class SecretTaskType {
  WIRES,      // Conectar Fios Elétricos
  MILK,       // Abastecer Leite / Ração
  DOWNLOAD,   // Baixar Dados de Segurança
  FILTER,     // Limpar Tubos de Ventilação
  ANTENNA     // Alinhar Frequência do Satélite
}

data class SecretTaskStationDef(
  val id: String,
  val name: String,
  val roomName: String,
  val emoji: String,
  val position: Vector2D,
  val type: SecretTaskType,
  var isCompletedByPlayer: Boolean = false
)

enum class SecretRole(
  val title: String,
  val emoji: String,
  val summary: String,
  val isImpostor: Boolean,
  val badgeColorHex: Long
) {
  INNOCENT(
    title = "Gato Inocente",
    emoji = "🐱",
    summary = "Faça tarefas, resgate gatos e denuncie suspeitos!",
    isImpostor = false,
    badgeColorHex = 0xFF0EA5E9
  ),
  ENGINEER(
    title = "Engenheiro",
    emoji = "🔧",
    summary = "Pode entrar e viajar pelos Dutos (Vents) pelo mapa!",
    isImpostor = false,
    badgeColorHex = 0xFF10B981
  ),
  DETECTIVE(
    title = "Detetive",
    emoji = "🔍",
    summary = "Use o Radar para rastrear o Impostor e pistas!",
    isImpostor = false,
    badgeColorHex = 0xFFF59E0B
  ),
  MEDIC(
    title = "Médico",
    emoji = "🩺",
    summary = "Crie um Escudo protetor em você ou em aliados!",
    isImpostor = false,
    badgeColorHex = 0xFF06B6D4
  ),
  IMPOSTOR(
    title = "Fantasma Impostor",
    emoji = "👻",
    summary = "Elimine os inocentes e fuja pelos Dutos!",
    isImpostor = true,
    badgeColorHex = 0xFFEF4444
  ),
  SHAPESHIFTER(
    title = "Impostor Metamorfo",
    emoji = "🎭",
    summary = "Transforme-se em qualquer jogador vivo e use Dutos!",
    isImpostor = true,
    badgeColorHex = 0xFFA855F7
  )
}

enum class SecretRolePreference(val label: String, val emoji: String) {
  RANDOM("Aleatório", "🎲"),
  SHAPESHIFTER("Metamorfo", "🎭"),
  ENGINEER("Engenheiro", "🔧"),
  DETECTIVE("Detetive", "🔍"),
  MEDIC("Médico", "🩺"),
  INNOCENT("Inocente", "🐱")
}

data class SecretVentDef(
  val id: String,
  val name: String,
  val roomName: String,
  val position: Vector2D,
  val connectedVentIds: List<String> = emptyList()
)

data class MorphTargetInfo(
  val uid: String,
  val name: String,
  val skinId: String,
  val furColor: Color,
  val accentEmoji: String
)

