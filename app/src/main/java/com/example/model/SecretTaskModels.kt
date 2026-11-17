package com.example.model

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
