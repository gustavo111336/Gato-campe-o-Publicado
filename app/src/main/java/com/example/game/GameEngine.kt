package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.GameAudio
import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

data class Obstacle(
  val x: Float,
  val y: Float,
  val radius: Float,
  val type: String, // "FOUNTAIN", "TREE", "FLOWER_PATCH", "YARN"
  val label: String
)

data class RemotePartnerState(
  val userId: String = "",
  val username: String,
  val position: Vector2D,
  val score: Int,
  val role: String = "cat",
  val skinId: String = "classic_orange"
)

class GameEngine(
  val worldWidth: Float = 3200f,
  val worldHeight: Float = 4800f,
  private val audio: GameAudio? = null
) {
  private val stateLock = Any()

  var heroPosition = Vector2D(worldWidth / 2f, worldHeight / 2f)
  var heroVelocity = Vector2D(0f, 0f)
  var heroAngle = 0f
  var heroSpeed = 340f
  var lives = 3
  var maxLives = 3

  var isInvincible = false
  var invincibilityTimer = 0f

  var isDashing = false
  var dashTimer = 0f
  var dashCooldownTimer = 0f
  val dashCooldown = 3.5f

  var sonicCooldownTimer = 0f
  val sonicCooldown = 7.5f

  // Item effect timers
  var catnipTimer = 0f
  var activeSpeedMult = 1.45f
  var goldenFishTimer = 0f
  var magnetTimer = 0f
  var giantMagnetTimer = 0f
  var freezeTimer = 0f
  var invisibilityTimer = 0f
  var doubleCatTimer = 0f
  var slowMotionTimer = 0f
  var turboOverdriveTimer = 0f
  var sonicOverdriveTimer = 0f
  var starInvincibleTimer = 0f
  var shieldActive = false

  // Combo & Match Level System
  var currentCombo = 0
  var maxComboThisRun = 0
  var comboTimer = 0f
  var matchLevel = 1

  // User Settings & Active Mode
  var godMode = false
  var ghostCount = 1
  var heroSpeedMultiplier = 1.0f
  var powerUpFrequency = PowerUpFrequency.NORMAL
  var arenaTheme = ArenaTheme.PARQUE_NOTURNO
  var difficulty = Difficulty.NORMAL
  var customGhostSpeed = 250 // 1..1000 used when difficulty == Difficulty.CUSTOM

  var customItemsPool: List<GameItemDef> = emptyList()
  var spawnCustomItemsEnabled: Boolean = true
  var activeCustomMap: CustomMapConfig? = null
  var remotePartner: RemotePartnerState? = null
  var remotePlayers: List<RemotePartnerState> = emptyList()

  // Elite / Multiplayer Ghost Role & Secret Mode State
  var isMultiplayerActive = false
  var multiplayerRole = "cat" // "cat" or "ghost"
  var multiplayerCaptureGoal = 3
  var isSecretMode = false
  var isPausedByMeeting = false
  var localPlayerAlive = true
  var eliminatedUserIds: Set<String> = emptySet()
  var secretKillCooldownTimer = 0f
  val secretKillCooldown = 8.0f
  var isBotSecretMode = false
  var botSecretLocalIsImpostor = false
  var pendingBotMeetingCaller: String? = null
  var isSpectating = false
  var secretTasksEnabled = true
  var secretVentsEnabled = true
  var secretRolesEnabled = true
  var secretShapeshifterEnabled = true
  var secretRolePreference: SecretRolePreference = SecretRolePreference.RANDOM
  var localSecretRole: SecretRole = SecretRole.INNOCENT
  val secretTaskStations = mutableListOf<SecretTaskStationDef>()
  val secretVents = mutableListOf<SecretVentDef>()
  var isInVent = false
  var currentVentId: String? = null
  var ventRemainTimer = 0f
  var ventCooldownTimer = 0f
  var morphedTargetUid: String? = null
  var morphedTargetName: String? = null
  var morphedTargetSkinId: String? = null
  var morphedTargetFurColor: Color? = null
  var morphedTimer = 0f
  var morphCooldownTimer = 0f
  var detectiveRadarTimer = 0f
  var detectiveRadarTargetPos: Vector2D? = null
  var detectiveRadarClueText: String = ""
  var detectiveRadarCooldownTimer = 0f
  var medicShieldCooldownTimer = 0f
  var localMedicShieldTimer = 0f
  var teamTasksCompletedPercent = 0f
  var totalBotTasksDoneCount = 0
  private var botTaskWorkTimer = 0f
  private var botReportCooldownTimer = 0f
  private var ghostCaptureCooldownTimer = 0f
  var aiRunnerCat: AiRunnerCat? = null

  var currentMode: GameModeDef = ALL_40_MODES.first()
  var timeLimitRemaining = 0f

  var status = GameStatus.READY
  var catsCollected = 0
  var goalCats = 10
  var endlessMode = false

  var gameTimeSeconds = 0f
  var damageTakenThisRun = 0
  var powerUpsCollectedThisRun = 0
  var ghostsScaredThisRun = 0
  var dashesUsedThisRun = 0
  var sonicsUsedThisRun = 0
  val collectedItemIdsThisRun = mutableListOf<String>()

  private val heroTrail = mutableListOf<Vector2D>()
  val followers = mutableListOf<FollowerCat>()
  val bots = mutableListOf<BotCatState>()
  var winnerBotName: String? = null

  val ghosts = mutableListOf(
    Ghost(
      id = 0,
      position = Vector2D(150f, 150f),
      velocity = Vector2D(0f, 0f)
    )
  )

  val ghost: Ghost
    get() = ghosts.first()

  val activeCollectibles = mutableListOf<CollectibleCat>()

  val activeCollectible: CollectibleCat?
    get() = activeCollectibles.firstOrNull()

  val activePowerUps = mutableListOf<PowerUpItem>()
  val activePowerUp: PowerUpItem?
    get() = activePowerUps.firstOrNull()

  val particles = mutableListOf<Particle>()
  val floatingTexts = mutableListOf<FloatingText>()
  val shockwaves = mutableListOf<ShockwaveEffect>()

  private var endlessSpawnTimer = 0f
  private var itemRainTimer = 0f
  private var nextCatSerial = 1

  private val defaultObstacles = listOf(
    Obstacle(worldWidth * 0.50f, worldHeight * 0.50f, 88f, "FOUNTAIN", "Fonte Central dos Peixinhos"),
    Obstacle(worldWidth * 0.22f, worldHeight * 0.16f, 65f, "FOUNTAIN", "Fonte Norte"),
    Obstacle(worldWidth * 0.78f, worldHeight * 0.84f, 65f, "FOUNTAIN", "Fonte Sul"),
    Obstacle(worldWidth * 0.25f, worldHeight * 0.30f, 58f, "TREE", "Arranhador Alto"),
    Obstacle(worldWidth * 0.75f, worldHeight * 0.30f, 58f, "TREE", "Arranhador Flor"),
    Obstacle(worldWidth * 0.18f, worldHeight * 0.52f, 58f, "TREE", "Torre Oeste"),
    Obstacle(worldWidth * 0.82f, worldHeight * 0.48f, 58f, "TREE", "Torre Leste"),
    Obstacle(worldWidth * 0.25f, worldHeight * 0.70f, 54f, "FLOWER_PATCH", "Jardim de Catnip"),
    Obstacle(worldWidth * 0.75f, worldHeight * 0.70f, 54f, "FLOWER_PATCH", "Canteiro dos Sonhos"),
    Obstacle(worldWidth * 0.50f, worldHeight * 0.14f, 52f, "FLOWER_PATCH", "Mirante Norte"),
    Obstacle(worldWidth * 0.50f, worldHeight * 0.86f, 52f, "FLOWER_PATCH", "Mirante Sul"),
    Obstacle(worldWidth * 0.36f, worldHeight * 0.40f, 46f, "YARN", "Novelo Gigante"),
    Obstacle(worldWidth * 0.64f, worldHeight * 0.60f, 46f, "YARN", "Novelo Lilás"),
    Obstacle(worldWidth * 0.68f, worldHeight * 0.18f, 48f, "YARN", "Novelo Dourado"),
    Obstacle(worldWidth * 0.32f, worldHeight * 0.82f, 48f, "YARN", "Novelo Azul"),
    Obstacle(worldWidth * 0.15f, worldHeight * 0.76f, 52f, "TREE", "Bosque Sudoeste"),
    Obstacle(worldWidth * 0.85f, worldHeight * 0.24f, 52f, "TREE", "Bosque Nordeste"),
    Obstacle(worldWidth * 0.50f, worldHeight * 0.32f, 46f, "YARN", "Praça dos Gatos")
  )

  var obstacles: List<Obstacle> = defaultObstacles

  init {
    resetGame(isEndless = false)
  }

  val isPlayerGhost: Boolean
    get() = if (isBotSecretMode) {
      botSecretLocalIsImpostor
    } else if (currentMode.isBotsMode) {
      false
    } else if (isMultiplayerActive) {
      multiplayerRole == "ghost"
    } else {
      difficulty == Difficulty.ELITE
    }

  val isEffectivelyGodMode: Boolean
    get() = !isPlayerGhost && (godMode || currentMode.isGodTrialMode || starInvincibleTimer > 0f)

  val isEffectivelyInvisible: Boolean
    get() = invisibilityTimer > 0f

  val effectiveArenaTheme: ArenaTheme
    get() = currentMode.arenaThemeOverride ?: arenaTheme

  // Safe snapshots for Compose DrawScope so Canvas never hits ConcurrentModificationException
  fun snapshotObstacles(): List<Obstacle> = synchronized(stateLock) { obstacles.toList() }
  fun snapshotShockwaves(): List<ShockwaveEffect> = synchronized(stateLock) { shockwaves.map { it.copy() } }
  fun snapshotPowerUps(): List<PowerUpItem> = synchronized(stateLock) { activePowerUps.map { it.copy() } }
  fun snapshotCollectibles(): List<CollectibleCat> = synchronized(stateLock) { activeCollectibles.map { it.copy() } }
  fun snapshotFollowers(): List<FollowerCat> = synchronized(stateLock) { followers.take(28).map { it.copy() } }
  fun snapshotBots(): List<BotCatState> = synchronized(stateLock) { bots.map { it.copy() } }
  fun snapshotGhosts(): List<Ghost> = synchronized(stateLock) { ghosts.map { it.copy() } }
  fun snapshotParticles(): List<Particle> = synchronized(stateLock) { particles.map { it.copy() } }
  fun snapshotFloatingTexts(): List<FloatingText> = synchronized(stateLock) { floatingTexts.map { it.copy() } }
  fun snapshotAiRunnerCat(): AiRunnerCat? = synchronized(stateLock) { aiRunnerCat?.copy() }
  fun snapshotRemotePlayers(): List<RemotePartnerState> = synchronized(stateLock) {
    if (remotePlayers.isNotEmpty()) {
      remotePlayers.map { it.copy() }
    } else {
      remotePartner?.let { listOf(it.copy()) } ?: emptyList()
    }
  }

  fun snapshotSecretTaskStations(): List<SecretTaskStationDef> = synchronized(stateLock) {
    secretTaskStations.map { it.copy() }
  }

  fun snapshotSecretVents(): List<SecretVentDef> = synchronized(stateLock) {
    secretVents.toList()
  }

  fun nearestVent(range: Float = 115f): SecretVentDef? = synchronized(stateLock) {
    if (!secretVentsEnabled || secretVents.isEmpty()) return null
    val candidate = secretVents.minByOrNull { (it.position - heroPosition).length() } ?: return null
    return if ((candidate.position - heroPosition).length() <= range) candidate else null
  }

  fun canLocalPlayerUseVents(): Boolean = synchronized(stateLock) {
    (isSecretMode || isBotSecretMode) &&
      secretVentsEnabled &&
      localPlayerAlive &&
      (isPlayerGhost || localSecretRole == SecretRole.ENGINEER)
  }

  fun canLocalPlayerMorph(): Boolean = synchronized(stateLock) {
    (isSecretMode || isBotSecretMode) &&
      secretShapeshifterEnabled &&
      isPlayerGhost &&
      localPlayerAlive &&
      localSecretRole == SecretRole.SHAPESHIFTER
  }

  fun getMorphCandidates(): List<MorphTargetInfo> = synchronized(stateLock) {
    if (isBotSecretMode) {
      bots.filter { it.isAlive && !it.isSecretImpostor }.map { bot ->
        MorphTargetInfo(
          uid = bot.uid,
          name = bot.name,
          skinId = bot.skinId,
          furColor = bot.furColor,
          accentEmoji = bot.accentEmoji
        )
      }
    } else {
      remotePlayers.filter { !eliminatedUserIds.contains(it.userId) && it.role != "ghost" }.map { rp ->
        val skin = ALL_HERO_SKINS.find { it.id == rp.skinId } ?: ALL_HERO_SKINS.first()
        MorphTargetInfo(
          uid = rp.userId,
          name = rp.username,
          skinId = rp.skinId,
          furColor = skin.furColor,
          accentEmoji = skin.badgeEmoji
        )
      }
    }
  }

  fun toggleEnterExitVent(): Boolean = synchronized(stateLock) {
    if (!canLocalPlayerUseVents()) return false
    if (isInVent) {
      isInVent = false
      ventRemainTimer = 0f
      ventCooldownTimer = if (isPlayerGhost) 1.2f else 5.5f
      audio?.playPowerUpSound()
      audio?.vibrateCollect()
      spawnVentBurstParticles(heroPosition, Color(0xFF38BDF8))
      addFloatingTextSafe(
        FloatingText(
          text = "🕳️ Saiu do duto!",
          position = heroPosition + Vector2D(0f, -40f),
          color = Color(0xFF38BDF8),
          lifetime = 1.6f
        )
      )
      return true
    }

    if (ventCooldownTimer > 0f) return false
    val vent = nearestVent(125f) ?: return false
    isInVent = true
    currentVentId = vent.id
    heroPosition = vent.position
    heroVelocity = Vector2D(0f, 0f)
    ventRemainTimer = if (isPlayerGhost) 999f else 10.0f
    audio?.playPowerUpSound()
    audio?.vibrateCollect()
    spawnVentBurstParticles(vent.position, Color(0xFF10B981))
    addFloatingTextSafe(
      FloatingText(
        text = "🕳️ Entrou no ${vent.name}!",
        position = vent.position + Vector2D(0f, -45f),
        color = Color(0xFF34D399),
        lifetime = 1.8f
      )
    )
    return true
  }

  fun hopToVent(direction: Int): SecretVentDef? = synchronized(stateLock) {
    if (!isInVent || secretVents.isEmpty()) return null
    val currentIndex = secretVents.indexOfFirst { it.id == currentVentId }.coerceAtLeast(0)
    val nextIndex = ((currentIndex + direction) % secretVents.size + secretVents.size) % secretVents.size
    val targetVent = secretVents[nextIndex]
    currentVentId = targetVent.id
    heroPosition = targetVent.position
    heroVelocity = Vector2D(0f, 0f)
    audio?.vibrateCollect()
    spawnVentBurstParticles(targetVent.position, Color(0xFF38BDF8))
    addFloatingTextSafe(
      FloatingText(
        text = "🕳️ ${targetVent.roomName}",
        position = targetVent.position + Vector2D(0f, -42f),
        color = Color(0xFF38BDF8),
        lifetime = 1.4f
      )
    )
    return targetVent
  }

  fun activateMorph(targetUid: String): String? = synchronized(stateLock) {
    if (!canLocalPlayerMorph() || morphCooldownTimer > 0f) return null
    val candidate = getMorphCandidates().firstOrNull { it.uid == targetUid } ?: return null
    morphedTargetUid = candidate.uid
    morphedTargetName = candidate.name
    morphedTargetSkinId = candidate.skinId
    morphedTargetFurColor = candidate.furColor
    morphedTimer = 16.0f
    morphCooldownTimer = 0f
    audio?.playPowerUpSound()
    audio?.vibrateCollect()
    spawnVentBurstParticles(heroPosition, Color(0xFFA855F7))
    addFloatingTextSafe(
      FloatingText(
        text = "🎭 Metamorfose em @${candidate.name}!",
        position = heroPosition + Vector2D(0f, -48f),
        color = Color(0xFFE879F9),
        lifetime = 2.4f
      )
    )
    return candidate.name
  }

  fun revertMorph() = synchronized(stateLock) {
    if (morphedTargetUid == null) return
    morphedTargetUid = null
    morphedTargetName = null
    morphedTargetSkinId = null
    morphedTargetFurColor = null
    morphedTimer = 0f
    morphCooldownTimer = 15.0f
    audio?.vibrateCollect()
    spawnVentBurstParticles(heroPosition, Color(0xFFC084FC))
    addFloatingTextSafe(
      FloatingText(
        text = "💨 Disfarce desfeito!",
        position = heroPosition + Vector2D(0f, -40f),
        color = Color(0xFFC084FC),
        lifetime = 1.8f
      )
    )
  }

  fun triggerDetectiveRadar(): String? = synchronized(stateLock) {
    if (!(isSecretMode || isBotSecretMode) || !localPlayerAlive || localSecretRole != SecretRole.DETECTIVE) return null
    if (detectiveRadarCooldownTimer > 0f) return null

    detectiveRadarCooldownTimer = 12.0f
    detectiveRadarTimer = 6.0f
    audio?.playSonicMeowSound()
    audio?.vibrateCollect()

    val clue: String
    if (isBotSecretMode) {
      val impostorBot = bots.firstOrNull { it.isAlive && it.isSecretImpostor }
      val unrepBody = bots.firstOrNull { !it.isAlive && !it.bodyReported }
      val target = impostorBot ?: unrepBody
      if (target != null) {
        detectiveRadarTargetPos = target.position
        val dist = (target.position - heroPosition).length().toInt()
        val morphAlert = if (impostorBot != null && impostorBot.morphedTimer > 0f) " ⚠️ METAMORFO ATIVO!" else ""
        clue = if (impostorBot != null) {
          "🔍 Radar: Suspeito detectado a ${dist}px!$morphAlert"
        } else {
          "🔍 Radar: Corpo não reportado a ${dist}px!"
        }
      } else {
        detectiveRadarTargetPos = null
        clue = "🔍 Radar limpo no momento!"
      }
    } else {
      val ghostPlayer = remotePlayers.firstOrNull { it.role == "ghost" && !eliminatedUserIds.contains(it.userId) }
      if (ghostPlayer != null) {
        detectiveRadarTargetPos = ghostPlayer.position
        val dist = (ghostPlayer.position - heroPosition).length().toInt()
        clue = "🔍 Radar: Fantasma detectado a ${dist}px!"
      } else {
        detectiveRadarTargetPos = null
        clue = "🔍 Radar: Nenhuma assinatura suspeita próxima!"
      }
    }
    detectiveRadarClueText = clue
    addFloatingTextSafe(
      FloatingText(
        text = clue,
        position = heroPosition + Vector2D(0f, -52f),
        color = Color(0xFFFBBF24),
        lifetime = 3.0f
      )
    )
    return clue
  }

  fun triggerMedicShield(): String? = synchronized(stateLock) {
    if (!(isSecretMode || isBotSecretMode) || !localPlayerAlive || localSecretRole != SecretRole.MEDIC) return null
    if (medicShieldCooldownTimer > 0f) return null

    medicShieldCooldownTimer = 14.0f
    audio?.playPowerUpSound()
    audio?.vibrateCollect()

    val nearbyBot = if (isBotSecretMode) {
      bots.filter { it.isAlive }.minByOrNull { (it.position - heroPosition).length() }
        ?.takeIf { (it.position - heroPosition).length() <= 170f }
    } else null

    val msg: String
    if (nearbyBot != null) {
      nearbyBot.shieldTimer = 11.0f
      localMedicShieldTimer = max(localMedicShieldTimer, 6.0f)
      msg = "🩺 Escudo aplicado em ${nearbyBot.name} (e em você)!"
      spawnVentBurstParticles(nearbyBot.position, Color(0xFF06B6D4))
    } else {
      localMedicShieldTimer = 10.0f
      msg = "🩺 Escudo Médico ativado em você (10s)!"
      spawnVentBurstParticles(heroPosition, Color(0xFF06B6D4))
    }

    addFloatingTextSafe(
      FloatingText(
        text = msg,
        position = heroPosition + Vector2D(0f, -50f),
        color = Color(0xFF22D3EE),
        lifetime = 2.6f
      )
    )
    return msg
  }

  private fun spawnVentBurstParticles(pos: Vector2D, color: Color) {
    for (i in 0 until 14) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val spd = Random.nextFloat() * 140f + 35f
      addParticleSafe(
        Particle(
          position = pos,
          velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
          color = color,
          size = 6.5f,
          life = 0.55f
        )
      )
    }
  }

  fun nearestTaskStation(range: Float = 95f): SecretTaskStationDef? = synchronized(stateLock) {
    if (!secretTasksEnabled || secretTaskStations.isEmpty()) return null
    val candidate = secretTaskStations.minByOrNull { (it.position - heroPosition).length() } ?: return null
    return if ((candidate.position - heroPosition).length() <= range) candidate else null
  }

  fun completeTask(taskId: String): Boolean = synchronized(stateLock) {
    val station = secretTaskStations.firstOrNull { it.id == taskId } ?: return false
    if (station.isCompletedByPlayer) return false
    station.isCompletedByPlayer = true
    recalculateTeamTasksProgress()
    audio?.vibrateCollect()
    addFloatingTextSafe(
      FloatingText(
        text = "✅ Tarefa Concluída: ${station.name}!",
        position = heroPosition + Vector2D(0f, -40f),
        color = Color(0xFF10B981),
        lifetime = 2.5f
      )
    )
    return true
  }

  fun recalculateTeamTasksProgress() = synchronized(stateLock) {
    if (!secretTasksEnabled || secretTaskStations.isEmpty()) return
    val totalStationCount = secretTaskStations.size.coerceAtLeast(1)
    val playerCompletedCount = secretTaskStations.count { it.isCompletedByPlayer }
    val targetBotCount = if (isBotSecretMode) bots.count { !it.isSecretImpostor }.coerceAtLeast(1) else 2
    val totalStepsTarget = (totalStationCount + targetBotCount * 2).toFloat()
    val totalDone = (playerCompletedCount + totalBotTasksDoneCount).toFloat()
    teamTasksCompletedPercent = (totalDone / totalStepsTarget).coerceIn(0f, 1f)

    if (teamTasksCompletedPercent >= 1.0f && status == GameStatus.PLAYING) {
      status = GameStatus.VICTORY
      winnerBotName = "Tripulação Inocente (Tarefas 100%)"
      audio?.vibrateVictory()
      addFloatingTextSafe(
        FloatingText(
          text = "🎉 TODAS AS TAREFAS COMPLETAS! A TRIPULAÇÃO VENCEU! 🚀",
          position = heroPosition + Vector2D(0f, -60f),
          color = Color(0xFF10B981),
          lifetime = 4.5f
        )
      )
    }
  }

  fun getBotRaceSummary(): Triple<Int, String, Int> = synchronized(stateLock) {
    if (bots.isEmpty()) return Triple(1, "", 0)
    val higherOrEqualBots = bots.count { it.catsCollected > catsCollected }
    val leaderBot = bots.maxByOrNull { it.catsCollected }
    Triple(higherOrEqualBots + 1, leaderBot?.name.orEmpty(), leaderBot?.catsCollected ?: 0)
  }

  fun setMultiplayerParticipants(
    active: Boolean,
    myRole: String,
    players: List<RemotePartnerState>,
    captureGoal: Int = 3,
    secretMode: Boolean = false,
    localAlive: Boolean = true,
    eliminatedIds: Set<String> = emptySet(),
    pausedByMeeting: Boolean = false
  ) = synchronized(stateLock) {
    val wasGhost = isPlayerGhost
    isMultiplayerActive = active
    isSecretMode = active && secretMode
    localPlayerAlive = !active || localAlive
    eliminatedUserIds = eliminatedIds
    isPausedByMeeting = active && pausedByMeeting
    multiplayerRole = if (myRole == "ghost") "ghost" else "cat"
    multiplayerCaptureGoal = captureGoal.coerceIn(1, 20)
    remotePlayers = players
    remotePartner = players.firstOrNull()
    if (active && multiplayerRole == "ghost" && !wasGhost) {
      ghosts.clear()
      goalCats = multiplayerCaptureGoal
    }
  }

  fun triggerSecretKillFeedback(victimName: String, victimPos: Vector2D) = synchronized(stateLock) {
    secretKillCooldownTimer = secretKillCooldown
    catsCollected++
    ghostsScaredThisRun++
    matchLevel = (catsCollected / 2) + 1
    audio?.playGhostHitSound()
    audio?.vibrateCollect()
    addFloatingTextSafe(
      FloatingText(
        text = "🗡️ Eliminou @$victimName!",
        position = victimPos + Vector2D(0f, -45f),
        color = Color(0xFFEF4444)
      )
    )
    for (i in 0 until 22) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val spd = Random.nextFloat() * 210f + 50f
      addParticleSafe(
        Particle(
          position = victimPos,
          velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
          color = if (i % 2 == 0) Color(0xFFEF4444) else Color(0xFFC084FC),
          size = 7.5f,
          life = 0.7f
        )
      )
    }
  }

  fun killBotInSecretMode(botUid: String): String? = synchronized(stateLock) {
    if (!isBotSecretMode || !botSecretLocalIsImpostor || !localPlayerAlive || secretKillCooldownTimer > 0f || isInVent) {
      return null
    }
    val bot = bots.firstOrNull { it.uid == botUid && it.isAlive && !it.isSecretImpostor && it.inVentTimer <= 0f } ?: return null
    if ((bot.position - heroPosition).length() > 260f) return null
    if (bot.shieldTimer > 0f) {
      bot.shieldTimer = 0f
      secretKillCooldownTimer = 5.0f
      spawnVentBurstParticles(bot.position, Color(0xFF22D3EE))
      addFloatingTextSafe(
        FloatingText(
          text = "🩺 ${bot.name} tinha Escudo Médico!",
          position = bot.position + Vector2D(0f, -45f),
          color = Color(0xFF22D3EE),
          lifetime = 2.0f
        )
      )
      return null
    }
    bot.isAlive = false
    bot.bodyReported = false
    triggerSecretKillFeedback(bot.name, bot.position)
    val aliveInnocents = bots.count { it.isAlive && !it.isSecretImpostor }
    if (aliveInnocents <= 1) {
      status = GameStatus.VICTORY
      audio?.vibrateVictory()
    }
    return bot.name
  }

  fun banishParticipantInBotSecretMode(targetUid: String): Boolean = synchronized(stateLock) {
    if (!isBotSecretMode) return false
    var wasImpostor = false
    if (targetUid == "LOCAL_PLAYER") {
      localPlayerAlive = false
      isSpectating = true
      isInVent = false
      morphedTargetUid = null
      morphedTargetName = null
      morphedTargetSkinId = null
      morphedTargetFurColor = null
      lives = 0
      wasImpostor = botSecretLocalIsImpostor
    } else {
      val bot = bots.firstOrNull { it.uid == targetUid }
      if (bot != null) {
        bot.isAlive = false
        bot.bodyReported = true
        wasImpostor = bot.isSecretImpostor
      }
    }
    return wasImpostor
  }

  fun resetBotSecretCooldownsAfterMeeting() = synchronized(stateLock) {
    isPausedByMeeting = false
    pendingBotMeetingCaller = null
    botReportCooldownTimer = 6.0f
    isInVent = false
    currentVentId = null
    if (morphedTargetUid != null) {
      morphedTargetUid = null
      morphedTargetName = null
      morphedTargetSkinId = null
      morphedTargetFurColor = null
      morphedTimer = 0f
    }
    bots.forEach { b ->
      if (!b.isAlive) {
        b.bodyReported = true
      }
      b.inVentTimer = 0f
      b.morphedTimer = 0f
      b.morphedAsUid = null
      b.morphedAsName = null
      b.morphedAsSkinId = null
      b.morphedAsFurColor = null
      b.killCooldownTimer = max(b.killCooldownTimer, 8.0f)
    }
    if (botSecretLocalIsImpostor) {
      secretKillCooldownTimer = max(secretKillCooldownTimer, 5.0f)
    }
  }

  fun resetGame(isEndless: Boolean = false) {
    val fallbackMode = if (isEndless) ALL_40_MODES[1] else ALL_40_MODES[0]
    resetGameForMode(fallbackMode, overrideGhostCount = ghostCount)
  }

  fun resetGameForCustomMap(mapConfig: CustomMapConfig) {
    val customModeDef = GameModeDef(
      id = "custom_map",
      number = 0,
      name = mapConfig.name.ifBlank { "Meu Mapa" },
      emoji = mapConfig.palette.emoji,
      description = "Mapa criado por você!",
      objectiveText = if (mapConfig.isEndless) "Resgate infinitos gatos no seu mapa" else "Resgate ${mapConfig.goalCats} gatos no seu mapa",
      unlockDescription = "Mapa Personalizado",
      unlockRule = ModeUnlockRule.ALWAYS_UNLOCKED,
      unlockTarget = 0,
      goalCats = if (mapConfig.isEndless) -1 else mapConfig.goalCats,
      ghostCount = mapConfig.ghostCount,
      startingCatsOnMap = if (mapConfig.isEndless) 12 else 3,
      isEndless = mapConfig.isEndless,
      isDarknessMode = mapConfig.isDarknessMode
    )
    resetGameForMode(customModeDef, overrideGhostCount = mapConfig.ghostCount, customMap = mapConfig)
  }

  fun resetGameForMode(
    mode: GameModeDef,
    overrideGhostCount: Int? = null,
    customMap: CustomMapConfig? = null
  ) = synchronized(stateLock) {
    currentMode = mode
    activeCustomMap = customMap
    obstacles = if (customMap != null) {
      customMap.obstacles.map { spec ->
        val radius = when (spec.type) {
          "FOUNTAIN" -> 75f
          "TREE" -> 55f
          "FLOWER_PATCH" -> 50f
          else -> 45f
        }
        Obstacle(
          x = worldWidth * spec.xRatio.coerceIn(0.08f, 0.92f),
          y = worldHeight * spec.yRatio.coerceIn(0.08f, 0.92f),
          radius = radius,
          type = spec.type,
          label = spec.type
        )
      }
    } else {
      defaultObstacles
    }
    isBotSecretMode = mode.id.startsWith("bots_secret_")
    val totalCustomBots = if (mode.isBotsMode && mode.botCount > 0) mode.botCount.coerceIn(1, 40) else 0
    val impostorSlot = if (isBotSecretMode && totalCustomBots > 2) {
      when {
        secretRolePreference == SecretRolePreference.SHAPESHIFTER && secretShapeshifterEnabled -> 0
        secretRolesEnabled && secretRolePreference in listOf(
          SecretRolePreference.ENGINEER,
          SecretRolePreference.DETECTIVE,
          SecretRolePreference.MEDIC,
          SecretRolePreference.INNOCENT
        ) -> 1 + Random.nextInt(totalCustomBots)
        else -> Random.nextInt(totalCustomBots + 1)
      }
    } else -1
    botSecretLocalIsImpostor = isBotSecretMode && impostorSlot == 0
    isSecretMode = isBotSecretMode || (isMultiplayerActive && isSecretMode && !mode.isBotsMode)
    localPlayerAlive = true
    isSpectating = false
    isInVent = false
    currentVentId = null
    ventRemainTimer = 0f
    ventCooldownTimer = 0f
    morphedTargetUid = null
    morphedTargetName = null
    morphedTargetSkinId = null
    morphedTargetFurColor = null
    morphedTimer = 0f
    morphCooldownTimer = if (botSecretLocalIsImpostor) 3.0f else 0f
    detectiveRadarTimer = 0f
    detectiveRadarTargetPos = null
    detectiveRadarClueText = ""
    detectiveRadarCooldownTimer = 0f
    medicShieldCooldownTimer = 0f
    localMedicShieldTimer = 0f
    pendingBotMeetingCaller = null
    botReportCooldownTimer = 0f
    totalBotTasksDoneCount = 0
    botTaskWorkTimer = 0f
    teamTasksCompletedPercent = 0f

    localSecretRole = when {
      !(isSecretMode || isBotSecretMode) -> SecretRole.INNOCENT
      isPlayerGhost -> if (secretShapeshifterEnabled) SecretRole.SHAPESHIFTER else SecretRole.IMPOSTOR
      !secretRolesEnabled -> SecretRole.INNOCENT
      secretRolePreference == SecretRolePreference.ENGINEER -> SecretRole.ENGINEER
      secretRolePreference == SecretRolePreference.DETECTIVE -> SecretRole.DETECTIVE
      secretRolePreference == SecretRolePreference.MEDIC -> SecretRole.MEDIC
      secretRolePreference == SecretRolePreference.INNOCENT -> SecretRole.INNOCENT
      else -> listOf(
        SecretRole.ENGINEER,
        SecretRole.DETECTIVE,
        SecretRole.MEDIC,
        SecretRole.INNOCENT
      ).random()
    }

    secretTaskStations.clear()
    if ((isSecretMode || isBotSecretMode) && secretTasksEnabled) {
      secretTaskStations.addAll(
        listOf(
          SecretTaskStationDef("task_wires", "Painel Elétrico", "Elétrica", "⚡", Vector2D(worldWidth * 0.22f, worldHeight * 0.22f), SecretTaskType.WIRES),
          SecretTaskStationDef("task_milk", "Estação de Leite", "Cafeteria", "🥛", Vector2D(worldWidth * 0.78f, worldHeight * 0.26f), SecretTaskType.MILK),
          SecretTaskStationDef("task_download", "Terminal Central", "Comunicações", "💻", Vector2D(worldWidth * 0.50f, worldHeight * 0.50f), SecretTaskType.DOWNLOAD),
          SecretTaskStationDef("task_filter", "Filtro de Ar", "Armazém", "🧹", Vector2D(worldWidth * 0.25f, worldHeight * 0.78f), SecretTaskType.FILTER),
          SecretTaskStationDef("task_antenna", "Antena Satélite", "Navegação", "📡", Vector2D(worldWidth * 0.80f, worldHeight * 0.82f), SecretTaskType.ANTENNA)
        )
      )
    }

    secretVents.clear()
    if ((isSecretMode || isBotSecretMode) && secretVentsEnabled) {
      secretVents.addAll(
        listOf(
          SecretVentDef("vent_elec", "Duto Elétrica", "Elétrica", clampAndResolveObstacles(Vector2D(worldWidth * 0.14f, worldHeight * 0.22f), 26f)),
          SecretVentDef("vent_cafe", "Duto Cafeteria", "Cafeteria", clampAndResolveObstacles(Vector2D(worldWidth * 0.84f, worldHeight * 0.36f), 26f)),
          SecretVentDef("vent_med", "Duto Enfermaria", "Enfermaria", clampAndResolveObstacles(Vector2D(worldWidth * 0.43f, worldHeight * 0.62f), 26f)),
          SecretVentDef("vent_storage", "Duto Armazém", "Armazém", clampAndResolveObstacles(Vector2D(worldWidth * 0.16f, worldHeight * 0.86f), 26f)),
          SecretVentDef("vent_nav", "Duto Navegação", "Navegação", clampAndResolveObstacles(Vector2D(worldWidth * 0.86f, worldHeight * 0.72f), 26f))
        )
      )
    }

    val ghostPlayerMode = isPlayerGhost
    endlessMode = !ghostPlayerMode && mode.isEndless
    goalCats = when {
      isBotSecretMode && botSecretLocalIsImpostor -> max(1, totalCustomBots - 1)
      isBotSecretMode -> max(10, mode.goalCats)
      ghostPlayerMode -> if (isMultiplayerActive) multiplayerCaptureGoal else 3
      mode.goalCats > 0 -> mode.goalCats
      else -> 999999
    }
    timeLimitRemaining = mode.timeLimitSec

    heroPosition = if (ghostPlayerMode && !isBotSecretMode) {
      Vector2D(worldWidth * 0.2f, worldHeight * 0.2f)
    } else {
      Vector2D(worldWidth / 2f, worldHeight * 0.65f)
    }
    heroVelocity = Vector2D(0f, 0f)
    heroAngle = if (ghostPlayerMode && !isBotSecretMode) 45f else -90f
    maxLives = if (ghostPlayerMode || isBotSecretMode) 3 else mode.heroLives
    lives = maxLives
    ghostCaptureCooldownTimer = 0f
    secretKillCooldownTimer = if (isBotSecretMode && botSecretLocalIsImpostor) 4.0f else 0f
    isPausedByMeeting = false

    isInvincible = false
    invincibilityTimer = 0f
    isDashing = false
    dashTimer = 0f
    dashCooldownTimer = 0f
    sonicCooldownTimer = 0f

    catnipTimer = 0f
    activeSpeedMult = 1.45f
    goldenFishTimer = 0f
    magnetTimer = 0f
    giantMagnetTimer = 0f
    freezeTimer = 0f
    invisibilityTimer = 0f
    doubleCatTimer = 0f
    slowMotionTimer = 0f
    turboOverdriveTimer = 0f
    sonicOverdriveTimer = 0f
    starInvincibleTimer = 0f
    shieldActive = false

    currentCombo = 0
    maxComboThisRun = 0
    comboTimer = 0f
    matchLevel = 1
    catsCollected = 0
    gameTimeSeconds = 0f
    damageTakenThisRun = 0
    powerUpsCollectedThisRun = 0
    ghostsScaredThisRun = 0
    dashesUsedThisRun = 0
    sonicsUsedThisRun = 0
    collectedItemIdsThisRun.clear()
    endlessSpawnTimer = 0f
    itemRainTimer = 0f
    nextCatSerial = 1

    heroTrail.clear()
    for (i in 0 until 220) {
      heroTrail.add(heroPosition)
    }
    followers.clear()
    bots.clear()
    winnerBotName = null

    ghosts.clear()
    aiRunnerCat = null

    if (ghostPlayerMode && !isBotSecretMode) {
      if (!isMultiplayerActive) {
        aiRunnerCat = AiRunnerCat(
          position = Vector2D(worldWidth * 0.52f, worldHeight * 0.68f),
          lives = 3,
          maxLives = 3,
          catsCollected = 0,
          goalCats = 10
        )
      }
    } else if (!isBotSecretMode) {
      val spawnCorners = listOf(
        Vector2D(worldWidth * 0.15f, worldHeight * 0.15f),
        Vector2D(worldWidth * 0.85f, worldHeight * 0.15f),
        Vector2D(worldWidth * 0.15f, worldHeight * 0.85f)
      )
      val effectiveGhosts = if (mode.id == "classic_champion" || mode.id == "endless_infinite") {
        (overrideGhostCount ?: mode.ghostCount).coerceIn(1, 3)
      } else {
        mode.ghostCount.coerceIn(1, 3)
      }
      for (i in 0 until effectiveGhosts) {
        ghosts.add(
          Ghost(
            id = i,
            position = spawnCorners[i % spawnCorners.size],
            velocity = Vector2D(0f, 0f)
          )
        )
      }
    }

    if (mode.isBotsMode && totalCustomBots > 0) {
      val botNames = listOf(
        "Bot Alfa", "Bot Turbo", "Bot Ninja", "Bot Pixel", "Bot Miau",
        "Bot Raio", "Bot Estrela", "Bot Cometa", "Bot Sombra", "Bot Lua",
        "Bot Cristal", "Bot Foguete", "Bot Tigre", "Bot Safira", "Bot Rubi",
        "Bot Vento", "Bot Flash", "Bot Cyber", "Bot Ouro", "Bot Lenda"
      )
      val botColors = listOf(
        Color(0xFF38BDF8), Color(0xFFFB7185), Color(0xFF34D399), Color(0xFFFBBF24),
        Color(0xFFA78BFA), Color(0xFF2DD4BF), Color(0xFFF472B6), Color(0xFF60A5FA),
        Color(0xFFFB923C), Color(0xFFC084FC)
      )
      val botEmojis = listOf("🤖", "⚡", "🥷", "👾", "🐱", "🚀", "🌟", "☄️", "🕶️", "🌙")
      for (i in 0 until totalCustomBots) {
        val angle = (i.toFloat() / totalCustomBots.toFloat()) * 2f * PI.toFloat()
        val ringRadius = 420f + (i % 4) * 240f
        val rawPos = Vector2D(
          worldWidth * 0.5f + cos(angle) * ringRadius,
          worldHeight * 0.5f + sin(angle) * ringRadius
        )
        val assignedSkin = ALL_HERO_SKINS[i % ALL_HERO_SKINS.size]
        val botIsImp = isBotSecretMode && impostorSlot == (i + 1)
        val botRole = when {
          !isBotSecretMode -> SecretRole.INNOCENT
          botIsImp -> if (secretShapeshifterEnabled) SecretRole.SHAPESHIFTER else SecretRole.IMPOSTOR
          !secretRolesEnabled -> SecretRole.INNOCENT
          i % 4 == 0 -> SecretRole.ENGINEER
          i % 4 == 1 -> SecretRole.DETECTIVE
          i % 4 == 2 -> SecretRole.MEDIC
          else -> SecretRole.INNOCENT
        }
        bots.add(
          BotCatState(
            id = i + 1,
            name = botNames[i % botNames.size],
            furColor = if (isBotSecretMode) assignedSkin.furColor else botColors[i % botColors.size],
            accentEmoji = if (isBotSecretMode) assignedSkin.badgeEmoji else botEmojis[i % botEmojis.size],
            position = clampAndResolveObstacles(rawPos, 24f),
            catsCollected = 0,
            uid = "bot_${i + 1}",
            skinId = assignedSkin.id,
            isAlive = true,
            isSecretImpostor = botIsImp,
            killCooldownTimer = if (isBotSecretMode) 9.5f else 0f,
            bodyReported = false,
            secretRole = botRole,
            morphCooldownTimer = if (botIsImp) 8.0f else 0f,
            ventCooldownTimer = 6.0f + (i % 5) * 2f
          )
        )
      }
    }

    particles.clear()
    floatingTexts.clear()
    shockwaves.clear()
    activeCollectibles.clear()
    activePowerUps.clear()

    val catsToSpawn = when {
      isBotSecretMode -> max(14, mode.startingCatsOnMap)
      ghostPlayerMode && isMultiplayerActive -> 0
      ghostPlayerMode -> 4
      mode.isBotsMode -> max(18, mode.startingCatsOnMap)
      mode.isEndless -> max(12, mode.startingCatsOnMap)
      else -> max(1, mode.startingCatsOnMap)
    }
    repeat(catsToSpawn) {
      spawnNextCat()
    }

    val startingItems = if (mode.isEndless || mode.isBotsMode || mode.powerUpSpawnMult >= 2.0f) 3 else 1
    repeat(startingItems) {
      spawnPowerUp()
    }

    status = GameStatus.READY
  }

  fun revivePlayer(): Boolean = synchronized(stateLock) {
    if (status != GameStatus.GAME_OVER || winnerBotName != null) return false
    lives = max(3, maxLives)
    isInvincible = true
    invincibilityTimer = 3.5f
    if (currentMode.timeLimitSec > 0f && timeLimitRemaining <= 0f) {
      timeLimitRemaining = 15f
    }
    for (g in ghosts) {
      val diff = g.position - heroPosition
      val dist = diff.length()
      if (dist < 260f) {
        val pushDir = if (dist > 0.1f) diff.normalized() else Vector2D(1f, 0f)
        g.position = Vector2D(
          (heroPosition.x + pushDir.x * 280f).coerceIn(50f, worldWidth - 50f),
          (heroPosition.y + pushDir.y * 280f).coerceIn(50f, worldHeight - 50f)
        )
      }
      g.stunTimerSec = max(g.stunTimerSec, 1.8f)
    }
    shockwaves.add(
      ShockwaveEffect(
        origin = heroPosition,
        radius = 24f,
        maxRadius = 380f,
        color = Color(0xFFF472B6),
        alpha = 1f
      )
    )
    addFloatingTextSafe(FloatingText("💖 REVIVEU! (+3.5s invencível)", heroPosition + Vector2D(0f, -55f), Color(0xFFF472B6)))
    audio?.playPowerUpSound()
    status = GameStatus.PLAYING
    return true
  }

  fun startPlaying() = synchronized(stateLock) {
    if (status == GameStatus.READY || status == GameStatus.PAUSED) {
      status = GameStatus.PLAYING
    }
  }

  fun togglePause() = synchronized(stateLock) {
    if (status == GameStatus.PLAYING) {
      status = GameStatus.PAUSED
    } else if (status == GameStatus.PAUSED) {
      status = GameStatus.PLAYING
    }
  }

  fun triggerDash(): Boolean = synchronized(stateLock) {
    if (status != GameStatus.PLAYING) return false
    val freeDash = currentMode.infiniteTurbo || turboOverdriveTimer > 0f
    if ((dashCooldownTimer <= 0f || freeDash) && !isDashing) {
      isDashing = true
      dashTimer = 0.35f
      dashCooldownTimer = if (freeDash) 0.4f else dashCooldown
      dashesUsedThisRun++
      audio?.playDashSound()
      audio?.vibrateDash()

      for (i in 0 until 12) {
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val spd = Random.nextFloat() * 120f + 40f
        addParticleSafe(
          Particle(
            position = heroPosition,
            velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
            color = Color(0xFFFACC15),
            size = Random.nextFloat() * 8f + 4f,
            life = 0.5f
          )
        )
      }
      return true
    }
    return false
  }

  fun triggerSonicWave(): Boolean = synchronized(stateLock) {
    if (status != GameStatus.PLAYING) return false
    val freeSonic = currentMode.infiniteSonic || sonicOverdriveTimer > 0f
    if (sonicCooldownTimer <= 0f || freeSonic) {
      sonicCooldownTimer = if (freeSonic) 0.7f else sonicCooldown
      sonicsUsedThisRun++
      emitSonicShockwave(heroPosition, maxRadius = 380f, stunSec = 2.5f)
      audio?.playSonicMeowSound()
      audio?.vibrateDash()
      return true
    }
    return false
  }

  private fun emitSonicShockwave(origin: Vector2D, maxRadius: Float, stunSec: Float) {
    if (shockwaves.size < 12) {
      shockwaves.add(
        ShockwaveEffect(
          origin = origin,
          radius = 24f,
          maxRadius = maxRadius,
          color = Color(0xFFC084FC),
          alpha = 1f
        )
      )
    }
    var hitAny = false
    aiRunnerCat?.let { ai ->
      val dist = (ai.position - origin).length()
      if (dist < maxRadius) {
        ai.stunTimer = max(ai.stunTimer, 1.8f)
        hitAny = true
      }
    }
    for (g in ghosts) {
      val diff = g.position - origin
      val dist = diff.length()
      if (dist < maxRadius) {
        val push = if (dist > 0.1f) diff.normalized() else Vector2D(1f, 0f)
        g.position = Vector2D(
          (g.position.x + push.x * 260f).coerceIn(40f, worldWidth - 40f),
          (g.position.y + push.y * 260f).coerceIn(40f, worldHeight - 40f)
        )
        g.stunTimerSec = max(g.stunTimerSec, stunSec)
        hitAny = true
        ghostsScaredThisRun++
      }
    }
    if (hitAny) {
      val msg = if (isPlayerGhost) "👻 Grito Sônico atordoou o Runner!" else "🔔 Onda Felina afastou o fantasma!"
      addFloatingTextSafe(FloatingText(msg, origin + Vector2D(0f, -50f), Color(0xFFE879F9)))
    } else {
      val msg = if (isPlayerGhost) "👻 Buuu Sônico!" else "🔔 Miau Sônico!"
      addFloatingTextSafe(FloatingText(msg, origin + Vector2D(0f, -45f), Color(0xFFC084FC)))
    }
  }

  fun update(deltaTime: Float, inputDirection: Vector2D) = synchronized(stateLock) {
    if (status != GameStatus.PLAYING || isPausedByMeeting) return

    val safeDelta = deltaTime.coerceIn(0.001f, 0.05f)
    gameTimeSeconds += safeDelta

    if (currentMode.timeLimitSec > 0f) {
      timeLimitRemaining = max(0f, timeLimitRemaining - safeDelta)
      if (timeLimitRemaining <= 0f) {
        status = GameStatus.GAME_OVER
        addFloatingTextSafe(FloatingText("⏱️ Tempo esgotado!", heroPosition, Color(0xFFEF4444)))
        return
      }
    }

    if (dashTimer > 0f) {
      dashTimer -= safeDelta
      if (dashTimer <= 0f) isDashing = false
    }
    if (dashCooldownTimer > 0f) dashCooldownTimer = max(0f, dashCooldownTimer - safeDelta)
    if (sonicCooldownTimer > 0f) sonicCooldownTimer = max(0f, sonicCooldownTimer - safeDelta)
    if (invincibilityTimer > 0f) {
      invincibilityTimer -= safeDelta
      if (invincibilityTimer <= 0f) isInvincible = false
    }
    if (catnipTimer > 0f) catnipTimer = max(0f, catnipTimer - safeDelta)
    if (magnetTimer > 0f) magnetTimer = max(0f, magnetTimer - safeDelta)
    if (giantMagnetTimer > 0f) giantMagnetTimer = max(0f, giantMagnetTimer - safeDelta)
    if (freezeTimer > 0f) freezeTimer = max(0f, freezeTimer - safeDelta)
    if (invisibilityTimer > 0f) invisibilityTimer = max(0f, invisibilityTimer - safeDelta)
    if (doubleCatTimer > 0f) doubleCatTimer = max(0f, doubleCatTimer - safeDelta)
    if (slowMotionTimer > 0f) slowMotionTimer = max(0f, slowMotionTimer - safeDelta)
    if (turboOverdriveTimer > 0f) turboOverdriveTimer = max(0f, turboOverdriveTimer - safeDelta)
    if (sonicOverdriveTimer > 0f) sonicOverdriveTimer = max(0f, sonicOverdriveTimer - safeDelta)
    if (starInvincibleTimer > 0f) starInvincibleTimer = max(0f, starInvincibleTimer - safeDelta)

    if (ghostCaptureCooldownTimer > 0f) ghostCaptureCooldownTimer = max(0f, ghostCaptureCooldownTimer - safeDelta)
    if (secretKillCooldownTimer > 0f) secretKillCooldownTimer = max(0f, secretKillCooldownTimer - safeDelta)
    if (botReportCooldownTimer > 0f) botReportCooldownTimer = max(0f, botReportCooldownTimer - safeDelta)
    if (ventCooldownTimer > 0f) ventCooldownTimer = max(0f, ventCooldownTimer - safeDelta)
    if (morphCooldownTimer > 0f) morphCooldownTimer = max(0f, morphCooldownTimer - safeDelta)
    if (detectiveRadarCooldownTimer > 0f) detectiveRadarCooldownTimer = max(0f, detectiveRadarCooldownTimer - safeDelta)
    if (medicShieldCooldownTimer > 0f) medicShieldCooldownTimer = max(0f, medicShieldCooldownTimer - safeDelta)
    if (localMedicShieldTimer > 0f) localMedicShieldTimer = max(0f, localMedicShieldTimer - safeDelta)

    if (detectiveRadarTimer > 0f) {
      detectiveRadarTimer = max(0f, detectiveRadarTimer - safeDelta)
      if (detectiveRadarTimer <= 0f) {
        detectiveRadarTargetPos = null
      }
    }

    if (morphedTimer > 0f) {
      morphedTimer -= safeDelta
      if (morphedTimer <= 0f) {
        revertMorph()
      }
    }

    if (isInVent) {
      if (!isPlayerGhost) {
        ventRemainTimer -= safeDelta
        if (ventRemainTimer <= 0f) {
          toggleEnterExitVent()
        }
      }
    }

    if (comboTimer > 0f) {
      comboTimer -= safeDelta
      if (comboTimer <= 0f) currentCombo = 0
    }
    if (goldenFishTimer > 0f) {
      goldenFishTimer = max(0f, goldenFishTimer - safeDelta)
      ghosts.forEach { it.isFleeing = (goldenFishTimer > 0f) }
    } else {
      ghosts.forEach { it.isFleeing = false }
    }

    var currentSpeed = heroSpeed * heroSpeedMultiplier * currentMode.heroSpeedMult
    if (isPlayerGhost) currentSpeed *= 1.15f
    if (isSpectating) currentSpeed *= 1.30f
    if (catnipTimer > 0f) currentSpeed *= activeSpeedMult
    if (isDashing) currentSpeed *= 1.9f

    if ((isBotSecretMode || isSecretMode) && secretTasksEnabled && status == GameStatus.PLAYING) {
      val aliveInnocentBots = bots.count { it.isAlive && !it.isSecretImpostor }
      if (aliveInnocentBots > 0) {
        botTaskWorkTimer += safeDelta
        if (botTaskWorkTimer >= 9.0f) {
          botTaskWorkTimer = 0f
          totalBotTasksDoneCount++
          recalculateTeamTasksProgress()
        }
      }
    }

    if (isInVent) {
      heroVelocity = Vector2D(0f, 0f)
    } else {
      if (inputDirection.length() > 0.05f) {
        val norm = inputDirection.normalized()
        heroVelocity = norm * currentSpeed
        heroAngle = (atan2(norm.y, norm.x) * 180f / PI.toFloat())
      } else {
        heroVelocity = heroVelocity * 0.85f
      }

      val newPos = heroPosition + heroVelocity * safeDelta
      heroPosition = clampAndResolveObstacles(newPos, 32f)
    }

    heroTrail.add(0, heroPosition)
    if (heroTrail.size > 400) {
      heroTrail.removeAt(heroTrail.size - 1)
    }

    val visibleFollowers = followers.take(28)
    visibleFollowers.forEachIndexed { index, follower ->
      val trailIndex = min((index + 1) * 13, heroTrail.size - 1)
      val targetPos = heroTrail[trailIndex]
      val diff = targetPos - follower.position
      follower.position = follower.position + diff * (10f * safeDelta).coerceAtMost(1f)
      if (diff.length() > 2f) {
        follower.angle = atan2(diff.y, diff.x) * 180f / PI.toFloat()
      }
      follower.hopOffset = sin((gameTimeSeconds * 8f) + index * 0.7f) * 6f
    }

    if (!localPlayerAlive) {
      // Eliminated / banished player can still roam freely as a spectator without collecting or hitting ghosts
      if (isBotSecretMode && bots.isNotEmpty() && status == GameStatus.PLAYING) {
        updateBots(safeDelta)
      }
    } else if (isPlayerGhost) {
      updateGhostPlayerRole(safeDelta)
      if (isBotSecretMode && bots.isNotEmpty() && status == GameStatus.PLAYING) {
        updateBots(safeDelta)
      }
      val pickedPowerUps = mutableListOf<PowerUpItem>()
      val powerUpIter = activePowerUps.iterator()
      while (powerUpIter.hasNext()) {
        val item = powerUpIter.next()
        if ((item.position - heroPosition).length() < 56f) {
          powerUpIter.remove()
          pickedPowerUps.add(item)
        }
      }
      for (item in pickedPowerUps) {
        collectPowerUp(item)
      }
    } else {
      for (g in ghosts) {
        updateGhost(g, safeDelta)
      }

      if (endlessMode || currentMode.isBotsMode) {
        val targetCount = max(16, currentMode.startingCatsOnMap)
        endlessSpawnTimer += safeDelta
        if (activeCollectibles.size < targetCount || (endlessMode && endlessSpawnTimer >= 2.0f && activeCollectibles.size < targetCount + 6)) {
          spawnNextCat()
          endlessSpawnTimer = 0f
        }
      } else {
        val targetNormalCount = max(1, currentMode.startingCatsOnMap)
        val remainingNeeded = goalCats - catsCollected
        while (activeCollectibles.size < min(targetNormalCount, remainingNeeded)) {
          spawnNextCat()
        }
      }

      itemRainTimer += safeDelta
      val maxItemsOnMap = if (currentMode.powerUpSpawnMult >= 2.5f) 7 else 3
      val rainPeriod = (4.5f / currentMode.powerUpSpawnMult).coerceAtLeast(0.8f)
      if (itemRainTimer >= rainPeriod) {
        itemRainTimer = 0f
        if (activePowerUps.size < maxItemsOnMap && Random.nextFloat() < powerUpFrequency.spawnChance) {
          spawnPowerUp()
        }
      }

      val collectedList = mutableListOf<CollectibleCat>()
      for (cat in activeCollectibles) {
        cat.bobOffset = sin(gameTimeSeconds * 5f + cat.id) * 8f

        if ((endlessMode || currentMode.startingCatsOnMap >= 5) && cat.wanderDir.length() > 0.01f) {
          val nextCatPos = cat.position + cat.wanderDir * (38f * safeDelta)
          if (nextCatPos.x < 80f || nextCatPos.x > worldWidth - 80f) {
            cat.wanderDir = Vector2D(-cat.wanderDir.x, cat.wanderDir.y)
          }
          if (nextCatPos.y < 80f || nextCatPos.y > worldHeight - 80f) {
            cat.wanderDir = Vector2D(cat.wanderDir.x, -cat.wanderDir.y)
          }
          cat.position = clampAndResolveObstacles(nextCatPos, 24f)
        }

        val toHero = heroPosition - cat.position
        val dist = toHero.length()
        if (giantMagnetTimer > 0f && dist > 1f) {
          cat.position = cat.position + toHero.normalized() * (560f * safeDelta)
        } else if (magnetTimer > 0f && dist < 540f && dist > 1f) {
          cat.position = cat.position + toHero.normalized() * (460f * safeDelta)
        }

        if ((cat.position - heroPosition).length() < 55f) {
          collectedList.add(cat)
        }
      }

      for (cat in collectedList) {
        if (status == GameStatus.PLAYING) {
          collectCat(cat)
        }
      }

      val pickedPowerUps = mutableListOf<PowerUpItem>()
      val powerUpIter = activePowerUps.iterator()
      while (powerUpIter.hasNext()) {
        val item = powerUpIter.next()
        val dist = (item.position - heroPosition).length()
        if (dist < 56f) {
          powerUpIter.remove()
          pickedPowerUps.add(item)
        }
      }
      for (item in pickedPowerUps) {
        collectPowerUp(item)
      }

      for (g in ghosts) {
        val ghostDist = (g.position - heroPosition).length()
        if (ghostDist < 58f) {
          if (g.isFleeing || g.freezeTimerSec > 0f) {
            g.position = g.position + (g.position - heroPosition).normalized() * 280f
            addFloatingTextSafe(FloatingText("Fantasma espantado! 👻💨", g.position, Color(0xFF38BDF8)))
            audio?.playPowerUpSound()
            ghostsScaredThisRun++
          } else if (isEffectivelyInvisible) {
            // Ghosts pass harmlessly when invisible
          } else if (isEffectivelyGodMode) {
            val pushDir = (g.position - heroPosition).normalized()
            g.position = Vector2D(
              (g.position.x + pushDir.x * 220f).coerceIn(40f, worldWidth - 40f),
              (g.position.y + pushDir.y * 220f).coerceIn(40f, worldHeight - 40f)
            )
            g.stunTimerSec = 1.0f
            val msg = if (starInvincibleTimer > 0f) "🌟 INVENCÍVEL!" else "👑 MODO DEUS: IMORTAL!"
            addFloatingTextSafe(FloatingText(msg, heroPosition + Vector2D(0f, -45f), Color(0xFFFDE047)))
            for (i in 0 until 8) {
              val angle = Random.nextFloat() * 2f * PI.toFloat()
              addParticleSafe(
                Particle(
                  position = heroPosition,
                  velocity = Vector2D(cos(angle) * 160f, sin(angle) * 160f),
                  color = Color(0xFFFDE047),
                  size = 7f,
                  life = 0.5f
                )
              )
            }
          } else if (!isInvincible) {
            onHeroHitByGhost(g)
          }
        }
      }

      // Remote player who was voted as Ghost in Multiplayer Elite (non-secret) can catch local Cat player
      if (isMultiplayerActive && !isSecretMode && !isEffectivelyGodMode && !isEffectivelyInvisible && !isInvincible) {
        val remoteGhost = remotePlayers.firstOrNull { it.role == "ghost" && (it.position - heroPosition).length() < 58f }
        if (remoteGhost != null) {
          onHeroHitByGhost(Ghost(id = 99, position = remoteGhost.position))
        }
      }

      if (currentMode.isBotsMode && bots.isNotEmpty() && status == GameStatus.PLAYING) {
        updateBots(safeDelta)
      }
    }

    val swIter = shockwaves.iterator()
    while (swIter.hasNext()) {
      val sw = swIter.next()
      sw.radius += 850f * safeDelta
      sw.alpha = (1f - (sw.radius / max(1f, sw.maxRadius))).coerceIn(0f, 1f)
      if (sw.radius >= sw.maxRadius) swIter.remove()
    }

    val pIter = particles.iterator()
    while (pIter.hasNext()) {
      val p = pIter.next()
      p.position = p.position + p.velocity * safeDelta
      p.life -= safeDelta * 1.5f
      p.alpha = p.life.coerceIn(0f, 1f)
      if (p.life <= 0f) pIter.remove()
    }

    val tIter = floatingTexts.iterator()
    while (tIter.hasNext()) {
      val txt = tIter.next()
      txt.position = txt.position + Vector2D(0f, -65f * safeDelta)
      txt.lifetime -= safeDelta
      txt.alpha = (txt.lifetime / 1.2f).coerceIn(0f, 1f)
      if (txt.lifetime <= 0f) tIter.remove()
    }

    if (isDashing || catnipTimer > 0f || isEffectivelyGodMode) {
      if (Random.nextFloat() < (if (isEffectivelyGodMode && !isDashing) 0.35f else 0.9f)) {
        addParticleSafe(
          Particle(
            position = heroPosition + Vector2D(Random.nextFloat() * 24f - 12f, Random.nextFloat() * 24f - 12f),
            velocity = Vector2D(-heroVelocity.x * 0.2f, -heroVelocity.y * 0.2f),
            color = when {
              isEffectivelyGodMode -> Color(0xFFFDE047)
              isDashing -> Color(0xFFFBBF24)
              else -> Color(0xFF34D399)
            },
            size = Random.nextFloat() * 6f + 3f,
            life = 0.45f
          )
        )
      }
    }
  }

  private fun addParticleSafe(p: Particle) {
    if (particles.size >= 160) {
      particles.removeAt(0)
    }
    particles.add(p)
  }

  private fun addFloatingTextSafe(txt: FloatingText) {
    if (floatingTexts.size >= 20) {
      floatingTexts.removeAt(0)
    }
    floatingTexts.add(txt)
  }

  private fun updateBots(deltaTime: Float) {
    val botPickedCats = mutableSetOf<CollectibleCat>()
    val speedDiffFactor = when (difficulty) {
      Difficulty.CALMO -> 0.85f
      Difficulty.DESAFIO -> 1.15f
      else -> 1.0f
    }

    for (bot in bots) {
      if (!bot.isAlive) continue
      if (bot.invincibleTimer > 0f) {
        bot.invincibleTimer = max(0f, bot.invincibleTimer - deltaTime)
      }
      if (bot.killCooldownTimer > 0f) {
        bot.killCooldownTimer = max(0f, bot.killCooldownTimer - deltaTime)
      }
      if (bot.shieldTimer > 0f) {
        bot.shieldTimer = max(0f, bot.shieldTimer - deltaTime)
      }
      if (bot.ventCooldownTimer > 0f) {
        bot.ventCooldownTimer = max(0f, bot.ventCooldownTimer - deltaTime)
      }
      if (bot.morphCooldownTimer > 0f) {
        bot.morphCooldownTimer = max(0f, bot.morphCooldownTimer - deltaTime)
      }
      if (bot.morphedTimer > 0f) {
        bot.morphedTimer -= deltaTime
        if (bot.morphedTimer <= 0f) {
          bot.morphedAsUid = null
          bot.morphedAsName = null
          bot.morphedAsSkinId = null
          bot.morphedAsFurColor = null
          spawnVentBurstParticles(bot.position, Color(0xFFC084FC))
        }
      }

      if (bot.inVentTimer > 0f) {
        bot.inVentTimer -= deltaTime
        if (bot.inVentTimer <= 0f && secretVents.isNotEmpty()) {
          val destVent = secretVents[Random.nextInt(secretVents.size)]
          bot.position = destVent.position
          bot.ventCooldownTimer = 16.0f + Random.nextFloat() * 10f
          spawnVentBurstParticles(destVent.position, if (bot.isSecretImpostor) Color(0xFFEF4444) else Color(0xFF10B981))
        }
        continue
      }

      // Shapeshifter Impostor Bot ability: disguises as another alive bot or even the player!
      if (isBotSecretMode && bot.secretRole == SecretRole.SHAPESHIFTER && secretShapeshifterEnabled &&
        gameTimeSeconds >= 9f && bot.morphCooldownTimer <= 0f
      ) {
        val targetPool = bots.filter { it.isAlive && !it.isSecretImpostor && it.id != bot.id }
        if (targetPool.isNotEmpty()) {
          val chosen = targetPool[Random.nextInt(targetPool.size)]
          bot.morphedAsUid = chosen.uid
          bot.morphedAsName = chosen.name
          bot.morphedAsSkinId = chosen.skinId
          bot.morphedAsFurColor = chosen.furColor
          bot.morphedTimer = 13.0f
          bot.morphCooldownTimer = 26.0f
          spawnVentBurstParticles(bot.position, Color(0xFFA855F7))
        }
      }

      // Medic Bot ability: periodically puts shield on self or nearby alive innocent
      if (isBotSecretMode && bot.secretRole == SecretRole.MEDIC && secretRolesEnabled &&
        bot.shieldTimer <= 0f && Random.nextFloat() < deltaTime * 0.08f
      ) {
        bot.shieldTimer = 8.0f
      }

      // Vent traversal AI for Impostor & Engineer bots
      if (isBotSecretMode && secretVentsEnabled && secretVents.isNotEmpty() &&
        (bot.isSecretImpostor || bot.secretRole == SecretRole.ENGINEER) && bot.ventCooldownTimer <= 0f
      ) {
        val nearVent = secretVents.firstOrNull { (it.position - bot.position).length() < 78f }
        if (nearVent != null && Random.nextFloat() < 0.45f) {
          bot.inVentTimer = 1.8f
          bot.ventCooldownTimer = 18.0f
          spawnVentBurstParticles(nearVent.position, if (bot.isSecretImpostor) Color(0xFFEF4444) else Color(0xFF10B981))
          continue
        }
      }

      // In Bot Secret Mode, innocent bots discovering a freshly eliminated body trigger an emergency meeting
      if (isBotSecretMode && !bot.isSecretImpostor && botReportCooldownTimer <= 0f && pendingBotMeetingCaller == null) {
        val freshBody = bots.firstOrNull { !it.isAlive && !it.bodyReported && (it.position - bot.position).length() < 130f }
        if (freshBody != null) {
          freshBody.bodyReported = true
          pendingBotMeetingCaller = bot.name
          botReportCooldownTimer = 14.0f
          isPausedByMeeting = true
          addFloatingTextSafe(
            FloatingText(
              text = "🚨 ${bot.name} encontrou ${freshBody.name} e chamou reunião!",
              position = heroPosition + Vector2D(0f, -55f),
              color = Color(0xFFEA580C)
            )
          )
          break
        }
      }

      val nearestCat = activeCollectibles
        .filter { !botPickedCats.contains(it) }
        .minByOrNull { (it.position - bot.position).length() }

      val nearestGhost = ghosts.minByOrNull { (it.position - bot.position).length() }
      val ghostDist = nearestGhost?.let { (it.position - bot.position).length() } ?: 9999f

      // Impostor Bot stalking logic in Bot Secret Mode
      var huntTargetPos: Vector2D? = null
      var huntTargetBot: BotCatState? = null
      var huntTargetIsHero = false
      if (isBotSecretMode && bot.isSecretImpostor && gameTimeSeconds >= 10f && bot.killCooldownTimer <= 0f) {
        val victimBot = bots
          .filter { it.isAlive && !it.isSecretImpostor && it.id != bot.id && it.inVentTimer <= 0f }
          .minByOrNull { (it.position - bot.position).length() }
        val botVictimDist = victimBot?.let { (it.position - bot.position).length() } ?: 99999f
        val heroEligible = !botSecretLocalIsImpostor && localPlayerAlive && !isInVent && !isEffectivelyGodMode && !isEffectivelyInvisible
        val heroDist = if (heroEligible) (heroPosition - bot.position).length() else 99999f

        if (heroEligible && heroDist <= botVictimDist && heroDist < 720f) {
          huntTargetPos = heroPosition
          huntTargetIsHero = true
        } else if (victimBot != null && botVictimDist < 760f) {
          huntTargetPos = victimBot.position
          huntTargetBot = victimBot
        }
      }

      val moveDir = when {
        huntTargetPos != null -> (huntTargetPos - bot.position).normalized()
        ghostDist < 190f && nearestGhost != null && !nearestGhost.isFleeing && nearestGhost.freezeTimerSec <= 0f -> {
          val flee = (bot.position - nearestGhost.position).normalized()
          val toCat = nearestCat?.let { (it.position - bot.position).normalized() } ?: Vector2D(0f, 0f)
          (flee * 0.72f + toCat * 0.28f).normalized()
        }
        nearestCat != null -> (nearestCat.position - bot.position).normalized()
        else -> {
          val orbit = gameTimeSeconds * 1.1f + bot.id * 0.7f
          Vector2D(cos(orbit), sin(orbit))
        }
      }

      val botBaseSpeed = ((210f + (bot.id % 6) * 14f) * speedDiffFactor) * (if (huntTargetPos != null) 1.18f else 1.0f)
      val targetVel = moveDir * botBaseSpeed
      bot.velocity = bot.velocity + (targetVel - bot.velocity) * (6f * deltaTime).coerceAtMost(1f)
      bot.position = clampAndResolveObstacles(bot.position + bot.velocity * deltaTime, 22f)
      if (bot.velocity.length() > 8f) {
        bot.angle = atan2(bot.velocity.y, bot.velocity.x) * 180f / PI.toFloat()
      }

      // Execute kill if Impostor Bot caught victim
      if (isBotSecretMode && bot.isSecretImpostor && gameTimeSeconds >= 10f && bot.killCooldownTimer <= 0f && status == GameStatus.PLAYING) {
        if (huntTargetIsHero && !isInVent && (heroPosition - bot.position).length() < 64f) {
          if (localMedicShieldTimer > 0f || shieldActive) {
            if (localMedicShieldTimer > 0f) localMedicShieldTimer = 0f else shieldActive = false
            bot.killCooldownTimer = 7.5f
            isInvincible = true
            invincibilityTimer = 2.0f
            spawnVentBurstParticles(heroPosition, Color(0xFF22D3EE))
            addFloatingTextSafe(
              FloatingText(
                text = "🩺 Escudo Médico bloqueou o ataque!",
                position = heroPosition + Vector2D(0f, -55f),
                color = Color(0xFF22D3EE)
              )
            )
            audio?.playPowerUpSound()
            continue
          }
          bot.killCooldownTimer = 12.0f
          localPlayerAlive = false
          isSpectating = true
          isInVent = false
          lives = 0
          audio?.playGhostHitSound()
          audio?.vibrateGhostHit()
          addFloatingTextSafe(
            FloatingText(
              text = "👻 VOCÊ FOI ELIMINADO! MODO ESPECTADOR ATIVO",
              position = heroPosition + Vector2D(0f, -55f),
              color = Color(0xFFEF4444)
            )
          )
          for (i in 0 until 16) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val spd = Random.nextFloat() * 160f + 40f
            addParticleSafe(
              Particle(
                position = heroPosition,
                velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
                color = Color(0xFFC084FC),
                size = 7f,
                life = 0.7f
              )
            )
          }
          val aliveInnocents = bots.count { it.isAlive && !it.isSecretImpostor }
          if (aliveInnocents <= 1) {
            winnerBotName = bot.name
            status = GameStatus.GAME_OVER
            break
          }
        } else if (huntTargetBot != null && (huntTargetBot.position - bot.position).length() < 68f) {
          if (huntTargetBot.shieldTimer > 0f) {
            huntTargetBot.shieldTimer = 0f
            bot.killCooldownTimer = 7.0f
            spawnVentBurstParticles(huntTargetBot.position, Color(0xFF22D3EE))
            continue
          }
          bot.killCooldownTimer = 11.5f
          huntTargetBot.isAlive = false
          huntTargetBot.bodyReported = false
          for (i in 0 until 16) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val spd = Random.nextFloat() * 180f + 40f
            addParticleSafe(
              Particle(
                position = huntTargetBot.position,
                velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
                color = Color(0xFFEF4444),
                size = 6.5f,
                life = 0.65f
              )
            )
          }
          val aliveInnocents = bots.count { it.isAlive && !it.isSecretImpostor } +
            if (!botSecretLocalIsImpostor && localPlayerAlive) 1 else 0
          if (aliveInnocents <= 1) {
            winnerBotName = bot.name
            status = GameStatus.GAME_OVER
            audio?.playGhostHitSound()
            break
          }
        }
      }

      if (nearestCat != null && (nearestCat.position - bot.position).length() < 48f) {
        botPickedCats.add(nearestCat)
        bot.catsCollected++
        if (!isBotSecretMode && bot.catsCollected >= goalCats && status == GameStatus.PLAYING) {
          winnerBotName = bot.name
          status = GameStatus.GAME_OVER
          addFloatingTextSafe(
            FloatingText(
              text = "🤖 ${bot.name} atingiu $goalCats/$goalCats gatos!",
              position = heroPosition + Vector2D(0f, -60f),
              color = Color(0xFFEF4444)
            )
          )
          audio?.playGhostHitSound()
          break
        } else if (isBotSecretMode && !bot.isSecretImpostor && bot.catsCollected >= currentMode.goalCats && status == GameStatus.PLAYING) {
          if (botSecretLocalIsImpostor) {
            winnerBotName = bot.name
            status = GameStatus.GAME_OVER
            audio?.playGhostHitSound()
          } else {
            status = GameStatus.VICTORY
            audio?.vibrateVictory()
          }
          break
        }
      }

      if (nearestGhost != null && ghostDist < 52f && bot.invincibleTimer <= 0f &&
        !nearestGhost.isFleeing && nearestGhost.freezeTimerSec <= 0f && nearestGhost.stunTimerSec <= 0f
      ) {
        bot.invincibleTimer = 2.0f
        val push = (bot.position - nearestGhost.position).normalized()
        bot.position = clampAndResolveObstacles(bot.position + push * 130f, 22f)
      }
    }

    if (botPickedCats.isNotEmpty()) {
      activeCollectibles.removeAll(botPickedCats)
      if (status == GameStatus.PLAYING) {
        val targetCount = if (isBotSecretMode) max(14, currentMode.startingCatsOnMap) else max(16, currentMode.startingCatsOnMap)
        while (activeCollectibles.size < targetCount) {
          spawnNextCat()
        }
      }
    }
  }

  private fun updateGhostPlayerRole(deltaTime: Float) {
    if (isBotSecretMode) {
      // Disguised bot-mode impostor hunts via the Matar button while collectibles stay stocked
      while (activeCollectibles.size < max(14, currentMode.startingCatsOnMap)) {
        spawnNextCat()
      }
      return
    }
    if (isMultiplayerActive) {
      if (isSecretMode) {
        // In Secret Mode the impostor is disguised and attacks deliberately via the Matar button
        return
      }
      for (p in remotePlayers) {
        if (p.role == "cat" && (p.position - heroPosition).length() < 62f && ghostCaptureCooldownTimer <= 0f) {
          ghostCaptureCooldownTimer = 1.6f
          catsCollected++
          ghostsScaredThisRun++
          matchLevel = (catsCollected / 2) + 1
          val isWin = catsCollected >= goalCats
          audio?.playCatCollectSound(isWin)
          audio?.vibrateCollect()
          addFloatingTextSafe(
            FloatingText(
              text = "👻 Pegou @${p.username}! ($catsCollected/$goalCats)",
              position = heroPosition + Vector2D(0f, -45f),
              color = if (isWin) Color(0xFFFDE047) else Color(0xFFE879F9)
            )
          )
          for (i in 0 until 16) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val spd = Random.nextFloat() * 180f + 50f
            addParticleSafe(
              Particle(
                position = p.position,
                velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
                color = Color(0xFFE879F9),
                size = 7f,
                life = 0.65f
              )
            )
          }
          if (isWin) {
            status = GameStatus.VICTORY
            audio?.vibrateVictory()
          }
          break
        }
      }
      return
    }

    // Solo Elite Mode: player is the Ghost chasing an AI Runner Cat
    while (activeCollectibles.size < 4) {
      spawnNextCat()
    }
    val ai = aiRunnerCat ?: return
    if (ai.invincibleTimer > 0f) ai.invincibleTimer = max(0f, ai.invincibleTimer - deltaTime)
    if (ai.stunTimer > 0f) ai.stunTimer = max(0f, ai.stunTimer - deltaTime)

    val distToGhost = (heroPosition - ai.position).length()
    val nearestCat = activeCollectibles.minByOrNull { (it.position - ai.position).length() }
    val desiredDir = when {
      ai.stunTimer > 0f -> Vector2D(0f, 0f)
      distToGhost < 360f -> {
        val flee = (ai.position - heroPosition).normalized()
        val toCat = nearestCat?.let { (it.position - ai.position).normalized() } ?: Vector2D(0f, 0f)
        (flee * 0.78f + toCat * 0.22f).normalized()
      }
      nearestCat != null -> (nearestCat.position - ai.position).normalized()
      else -> Vector2D(cos(gameTimeSeconds), sin(gameTimeSeconds))
    }

    val runnerSpeed = if (ai.stunTimer > 0f) 0f else 310f + (ai.catsCollected * 6f)
    val targetVel = desiredDir * runnerSpeed
    ai.velocity = ai.velocity + (targetVel - ai.velocity) * (6f * deltaTime).coerceAtMost(1f)
    ai.position = clampAndResolveObstacles(ai.position + ai.velocity * deltaTime, 24f)
    if (ai.velocity.length() > 10f) {
      ai.angle = atan2(ai.velocity.y, ai.velocity.x) * 180f / PI.toFloat()
    }

    // Check if AI Runner Cat rescued a collectible cat
    val rescuedCat = activeCollectibles.firstOrNull { (it.position - ai.position).length() < 52f }
    if (rescuedCat != null) {
      activeCollectibles.remove(rescuedCat)
      ai.catsCollected++
      addFloatingTextSafe(
        FloatingText(
          text = "🐱 Runner resgatou ${ai.catsCollected}/${ai.goalCats}!",
          position = ai.position + Vector2D(0f, -35f),
          color = Color(0xFFFB923C)
        )
      )
      if (ai.catsCollected >= ai.goalCats) {
        status = GameStatus.GAME_OVER
        audio?.playGhostHitSound()
        return
      } else {
        spawnNextCat()
      }
    }

    // Check if Player Ghost caught AI Runner Cat
    if ((heroPosition - ai.position).length() < 58f && ai.invincibleTimer <= 0f) {
      ai.lives--
      ai.invincibleTimer = 2.0f
      catsCollected++
      ghostsScaredThisRun++
      matchLevel = (catsCollected / 2) + 1
      val pushDir = (ai.position - heroPosition).normalized()
      ai.position = clampAndResolveObstacles(ai.position + pushDir * 140f, 24f)

      val isWin = ai.lives <= 0
      audio?.playCatCollectSound(isWin)
      audio?.vibrateCollect()
      addFloatingTextSafe(
        FloatingText(
          text = if (isWin) "🏆 ELITE VENCIDO! Você pegou o Runner!" else "👻 Pegou o Runner! ($catsCollected/$goalCats)",
          position = ai.position + Vector2D(0f, -45f),
          color = if (isWin) Color(0xFFFDE047) else Color(0xFFE879F9)
        )
      )
      for (i in 0 until 18) {
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val spd = Random.nextFloat() * 200f + 60f
        addParticleSafe(
          Particle(
            position = ai.position,
            velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
            color = Color(0xFFE879F9),
            size = 7f,
            life = 0.7f
          )
        )
      }
      if (isWin) {
        status = GameStatus.VICTORY
        audio?.vibrateVictory()
      }
    }
  }

  private fun updateGhost(g: Ghost, deltaTime: Float) {
    g.floatOffset = sin(gameTimeSeconds * 3.5f + g.id * 1.3f) * 12f

    if (g.freezeTimerSec > 0f) {
      g.freezeTimerSec = max(0f, g.freezeTimerSec - deltaTime)
      g.velocity = Vector2D(0f, 0f)
      return
    }
    if (g.stunTimerSec > 0f) {
      g.stunTimerSec = max(0f, g.stunTimerSec - deltaTime)
      g.velocity = g.velocity * 0.85f
      return
    }

    val scaledCats = min(catsCollected, 25)
    val levelBoost = (matchLevel - 1) * 10f
    var baseGhostSpeed = if (difficulty == Difficulty.CUSTOM) {
      val clamped = customGhostSpeed.coerceIn(1, 1000)
      (10f + clamped * 0.72f) * currentMode.ghostSpeedMult * (1f + g.id * 0.05f)
    } else {
      (175f + (scaledCats * 8.0f) + levelBoost) * difficulty.ghostSpeedMult * currentMode.ghostSpeedMult * (1f + g.id * 0.06f)
    }
    if (slowMotionTimer > 0f) baseGhostSpeed *= 0.35f

    val actualDist = (heroPosition - g.position).length()

    if (actualDist > 1400f && Random.nextFloat() < 0.0025f) {
      g.position = Vector2D(
        (heroPosition.x + if (Random.nextBoolean()) 550f else -550f).coerceIn(60f, worldWidth - 60f),
        (heroPosition.y + if (Random.nextBoolean()) 550f else -550f).coerceIn(60f, worldHeight - 60f)
      )
      addFloatingTextSafe(FloatingText("Buuu! O fantasma surgiu! 👻", g.position, Color(0xFFA855F7)))
      audio?.playGhostAlertSound()
    }

    val targetPos = if (difficulty == Difficulty.DESAFIO && !isEffectivelyInvisible && !g.isFleeing) {
      val predicted = heroPosition + heroVelocity * 0.42f
      Vector2D(
        predicted.x.coerceIn(60f, worldWidth - 60f),
        predicted.y.coerceIn(60f, worldHeight - 60f)
      )
    } else {
      heroPosition
    }
    val toHero = targetPos - g.position

    val dir: Vector2D = when {
      g.isFleeing -> (g.position - heroPosition).normalized()
      isEffectivelyInvisible -> {
        val angle = gameTimeSeconds * 1.2f + g.id * 2f
        Vector2D(cos(angle), sin(angle))
      }
      else -> toHero.normalized()
    }

    val targetVel = dir * (if (g.isFleeing) baseGhostSpeed * 0.78f else baseGhostSpeed)
    g.velocity = g.velocity + (targetVel - g.velocity) * (4f * deltaTime)
    val nextGhostPos = g.position + g.velocity * deltaTime

    g.position = Vector2D(
      nextGhostPos.x.coerceIn(40f, worldWidth - 40f),
      nextGhostPos.y.coerceIn(40f, worldHeight - 40f)
    )

    if (Random.nextFloat() < 0.20f) {
      addParticleSafe(
        Particle(
          position = g.position + Vector2D(Random.nextFloat() * 20f - 10f, Random.nextFloat() * 20f - 10f),
          velocity = Vector2D(Random.nextFloat() * 20f - 10f, Random.nextFloat() * 20f - 10f),
          color = if (g.isFleeing) Color(0xFF38BDF8) else Color(0xFFC084FC),
          size = Random.nextFloat() * 8f + 4f,
          life = 0.65f
        )
      )
    }
  }

  private fun collectCat(cat: CollectibleCat) {
    activeCollectibles.remove(cat)
    val value = if (doubleCatTimer > 0f) 2 else 1
    catsCollected += value
    currentCombo++
    if (currentCombo > maxComboThisRun) maxComboThisRun = currentCombo
    comboTimer = 4.0f

    val isChampion = !endlessMode && catsCollected >= goalCats

    // Match Level-Up every 5 cats collected!
    val computedLevel = (catsCollected / 5) + 1
    if (computedLevel > matchLevel) {
      matchLevel = computedLevel
      addFloatingTextSafe(
        FloatingText(
          text = "⬆️ SUBIU PARA O NÍVEL $matchLevel! ⭐",
          position = heroPosition + Vector2D(0f, -80f),
          color = Color(0xFF38BDF8)
        )
      )
      if (!isChampion) {
        audio?.playAchievementSound()
        if (activePowerUps.size < 6) {
          spawnPowerUp()
        }
      }
    }

    followers.add(
      FollowerCat(
        breed = cat.breed,
        position = cat.position
      )
    )

    audio?.playCatCollectSound(isChampion)
    audio?.vibrateCollect()

    val comboSuffix = if (currentCombo >= 2) " (Combo x$currentCombo! 🔥)" else ""
    val doubleTag = if (value > 1) " x2!" else ""
    val celebratoryMsg = when {
      isChampion -> "🏆 VITÓRIA: VOCÊ É O CAMPEÃO! 🏆"
      endlessMode -> "+$value ${cat.breed.name}$doubleTag ($catsCollected ♾️)$comboSuffix"
      else -> "+$value ${cat.breed.name}$doubleTag ($catsCollected/$goalCats)$comboSuffix"
    }

    addFloatingTextSafe(
      FloatingText(
        text = celebratoryMsg,
        position = heroPosition + Vector2D(0f, -45f),
        color = if (isChampion) Color(0xFFFDE047) else Color(0xFF4ADE80)
      )
    )

    for (i in 0 until 20) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val spd = Random.nextFloat() * 220f + 50f
      addParticleSafe(
        Particle(
          position = cat.position,
          velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
          color = if (isChampion) Color(0xFFFACC15) else cat.breed.furColor,
          size = Random.nextFloat() * 8f + 5f,
          life = 0.8f
        )
      )
    }

    if (isChampion) {
      status = GameStatus.VICTORY
      audio?.vibrateVictory()
      activeCollectibles.clear()
    } else {
      if (endlessMode || currentMode.isBotsMode) {
        spawnNextCat()
        if (activeCollectibles.size < max(16, currentMode.startingCatsOnMap)) {
          spawnNextCat()
        }
      } else {
        val remaining = goalCats - catsCollected
        if (activeCollectibles.size < remaining) {
          spawnNextCat()
        }
      }

      if (Random.nextFloat() < powerUpFrequency.spawnChance && activePowerUps.size < 5) {
        spawnPowerUp()
      }
    }
  }

  private fun collectPowerUp(powerUp: PowerUpItem) {
    val def = powerUp.itemDef
    powerUpsCollectedThisRun++
    collectedItemIdsThisRun.add(def.id)
    audio?.vibrateCollect()

    when (def.effectType) {
      ItemEffectType.SPEED_BOOST -> {
        audio?.playPowerUpSound()
        catnipTimer = max(catnipTimer, def.durationSec)
        activeSpeedMult = max(activeSpeedMult, def.powerMagnitude)
      }
      ItemEffectType.GHOST_FLEE -> {
        audio?.playPowerUpSound()
        goldenFishTimer = max(goldenFishTimer, def.durationSec)
        ghosts.forEach { it.isFleeing = true }
        ghostsScaredThisRun++
      }
      ItemEffectType.SHIELD -> {
        audio?.playPowerUpSound()
        shieldActive = true
      }
      ItemEffectType.MAGNET -> {
        audio?.playPowerUpSound()
        magnetTimer = max(magnetTimer, def.durationSec)
      }
      ItemEffectType.FREEZE_GHOSTS -> {
        audio?.playFreezeSound()
        freezeTimer = max(freezeTimer, def.durationSec)
        ghosts.forEach { it.freezeTimerSec = max(it.freezeTimerSec, def.durationSec) }
      }
      ItemEffectType.HEAL_HEART -> {
        audio?.playPowerUpSound()
        if (lives < maxLives) {
          lives++
        } else {
          shieldActive = true
        }
        if (def.durationSec > 0f) {
          catnipTimer = max(catnipTimer, def.durationSec)
        }
      }
      ItemEffectType.SONIC_BLAST -> {
        audio?.playSonicMeowSound()
        emitSonicShockwave(heroPosition, maxRadius = 520f, stunSec = 3.0f)
      }
      ItemEffectType.INVISIBILITY -> {
        audio?.playPowerUpSound()
        invisibilityTimer = max(invisibilityTimer, def.durationSec)
      }
      ItemEffectType.DOUBLE_CAT_VALUE -> {
        audio?.playPowerUpSound()
        doubleCatTimer = max(doubleCatTimer, def.durationSec)
      }
      ItemEffectType.SLOW_MOTION_GHOSTS -> {
        audio?.playFreezeSound()
        slowMotionTimer = max(slowMotionTimer, def.durationSec)
      }
      ItemEffectType.TURBO_OVERDRIVE -> {
        audio?.playDashSound()
        turboOverdriveTimer = max(turboOverdriveTimer, def.durationSec)
        dashCooldownTimer = 0f
      }
      ItemEffectType.SONIC_OVERDRIVE -> {
        audio?.playSonicMeowSound()
        sonicOverdriveTimer = max(sonicOverdriveTimer, def.durationSec)
        sonicCooldownTimer = 0f
      }
      ItemEffectType.GIANT_MAGNET -> {
        audio?.playPowerUpSound()
        giantMagnetTimer = max(giantMagnetTimer, def.durationSec)
      }
      ItemEffectType.SPAWN_EXTRA_CATS -> {
        audio?.playCatCollectSound(false)
        repeat(3) { spawnNextCat() }
      }
      ItemEffectType.TELEPORT_SAFE -> {
        audio?.playDashSound()
        val corners = listOf(
          Vector2D(250f, 250f),
          Vector2D(worldWidth - 250f, 250f),
          Vector2D(250f, worldHeight - 250f),
          Vector2D(worldWidth - 250f, worldHeight - 250f)
        )
        val bestSpot = corners.maxByOrNull { spot ->
          ghosts.minOfOrNull { (it.position - spot).length() } ?: 0f
        } ?: heroPosition
        heroPosition = bestSpot
        isInvincible = true
        invincibilityTimer = max(invincibilityTimer, 1.5f)
      }
      ItemEffectType.INVINCIBILITY_STAR -> {
        audio?.playPowerUpSound()
        starInvincibleTimer = max(starInvincibleTimer, def.durationSec)
        if (def.id == "champion_elixir") {
          catnipTimer = max(catnipTimer, def.durationSec)
          magnetTimer = max(magnetTimer, def.durationSec)
        }
      }
      ItemEffectType.TIME_BONUS_15S -> {
        audio?.playPowerUpSound()
        if (currentMode.timeLimitSec > 0f) {
          timeLimitRemaining += 15f
        } else {
          catnipTimer = max(catnipTimer, 4f)
        }
      }
      ItemEffectType.GHOST_REPEL_RING -> {
        audio?.playSonicMeowSound()
        emitSonicShockwave(heroPosition, maxRadius = 700f, stunSec = 3.5f)
      }
      ItemEffectType.RECHARGE_SKILLS -> {
        audio?.playDashSound()
        dashCooldownTimer = 0f
        sonicCooldownTimer = 0f
      }
      ItemEffectType.LUCKY_RAIN -> {
        audio?.playPowerUpSound()
        repeat(2) { spawnNextCat() }
        repeat(3) { spawnPowerUp() }
      }
    }

    addFloatingTextSafe(
      FloatingText(
        text = "${def.emoji} ${def.name}: ${def.effectSummary}!",
        position = heroPosition + Vector2D(0f, -35f),
        color = def.color
      )
    )

    for (i in 0 until 18) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val spd = Random.nextFloat() * 150f + 40f
      addParticleSafe(
        Particle(
          position = powerUp.position,
          velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
          color = def.color,
          size = 7f,
          life = 0.6f
        )
      )
    }
  }

  private fun onHeroHitByGhost(hitGhost: Ghost) {
    if (shieldActive) {
      shieldActive = false
      isInvincible = true
      invincibilityTimer = 1.5f
      addFloatingTextSafe(FloatingText("Escudo de Leite protegeu você! 🥛✨", heroPosition, Color(0xFF38BDF8)))
      audio?.playPowerUpSound()
      return
    }

    lives--
    damageTakenThisRun++
    currentCombo = 0
    isInvincible = true
    invincibilityTimer = 2.0f
    audio?.playGhostHitSound()
    audio?.vibrateGhostHit()

    val pushDir = (heroPosition - hitGhost.position).normalized()
    heroPosition = clampAndResolveObstacles(heroPosition + pushDir * 80f, 32f)
    hitGhost.position = Vector2D(
      (hitGhost.position.x - pushDir.x * 120f).coerceIn(40f, worldWidth - 40f),
      (hitGhost.position.y - pushDir.y * 120f).coerceIn(40f, worldHeight - 40f)
    )

    addFloatingTextSafe(FloatingText("Ai! O fantasma te pegou! 💔", heroPosition, Color(0xFFEF4444)))

    for (i in 0 until 20) {
      val angle = Random.nextFloat() * 2f * PI.toFloat()
      val spd = Random.nextFloat() * 180f + 60f
      addParticleSafe(
        Particle(
          position = heroPosition,
          velocity = Vector2D(cos(angle) * spd, sin(angle) * spd),
          color = Color(0xFFEF4444),
          size = 8f,
          life = 0.6f
        )
      )
    }

    if (lives <= 0) {
      status = GameStatus.GAME_OVER
    }
  }

  fun spawnNextCat() {
    val allBreeds = if (endlessMode) CHAMPION_CATS + ENDLESS_EXTRA_BREEDS else CHAMPION_CATS
    val breed = if (endlessMode || currentMode.goalCats > 10) {
      allBreeds[Random.nextInt(allBreeds.size)]
    } else {
      CHAMPION_CATS[catsCollected % CHAMPION_CATS.size]
    }

    var spawnPos = Vector2D(worldWidth * 0.5f, worldHeight * 0.35f)
    for (attempt in 0 until 25) {
      val margin = 110f
      val rx = Random.nextFloat() * (worldWidth - margin * 2) + margin
      val ry = Random.nextFloat() * (worldHeight - margin * 2) + margin
      val candidate = Vector2D(rx, ry)
      val distToHero = (candidate - heroPosition).length()
      val minGhostDist = ghosts.minOfOrNull { (candidate - it.position).length() } ?: 999f
      val minCatDist = activeCollectibles.minOfOrNull { (candidate - it.position).length() } ?: 999f

      if (distToHero > 240f && minGhostDist > 180f && minCatDist > 120f) {
        var clearOfObstacles = true
        for (obs in obstacles) {
          if ((candidate - Vector2D(obs.x, obs.y)).length() < obs.radius + 40f) {
            clearOfObstacles = false
            break
          }
        }
        if (clearOfObstacles) {
          spawnPos = candidate
          break
        }
      }
    }

    val wanderAngle = Random.nextFloat() * 2f * PI.toFloat()
    val wander = if (endlessMode || currentMode.startingCatsOnMap >= 5) {
      Vector2D(cos(wanderAngle), sin(wanderAngle))
    } else {
      Vector2D(0f, 0f)
    }

    activeCollectibles.add(
      CollectibleCat(
        id = nextCatSerial++,
        breed = breed,
        position = spawnPos,
        wanderDir = wander
      )
    )
  }

  fun spawnPowerUp() {
    val useCustom = spawnCustomItemsEnabled && customItemsPool.isNotEmpty() &&
      Random.nextFloat() < (if (activeCustomMap != null) 0.50f else 0.25f)
    val chosenItem = if (useCustom) {
      customItemsPool[Random.nextInt(customItemsPool.size)]
    } else {
      ALL_40_ITEMS[Random.nextInt(ALL_40_ITEMS.size)]
    }
    val margin = 120f
    val rx = Random.nextFloat() * (worldWidth - margin * 2) + margin
    val ry = Random.nextFloat() * (worldHeight - margin * 2) + margin
    activePowerUps.add(
      PowerUpItem(
        itemDef = chosenItem,
        position = Vector2D(rx, ry)
      )
    )
  }

  private fun clampAndResolveObstacles(pos: Vector2D, radius: Float): Vector2D {
    var cur = Vector2D(
      pos.x.coerceIn(radius + 20f, worldWidth - radius - 20f),
      pos.y.coerceIn(radius + 20f, worldHeight - radius - 20f)
    )

    for (obs in obstacles) {
      val obsPos = Vector2D(obs.x, obs.y)
      val diff = cur - obsPos
      val dist = diff.length()
      val minAllowed = obs.radius + radius
      if (dist < minAllowed && dist > 0.001f) {
        val push = diff.normalized() * (minAllowed - dist)
        cur = cur + push
      }
    }
    return cur
  }

  fun calculateScore(isVictory: Boolean = (status == GameStatus.VICTORY)): Int {
    if (catsCollected <= 0 && !isVictory) return 0
    val catPoints = catsCollected * 150
    val levelPoints = (matchLevel - 1) * 100
    val comboPoints = maxComboThisRun * 35
    val timeBudget = if (isVictory) 600 else 240
    val timeBonus = max(0, timeBudget - (gameTimeSeconds * 3f).toInt())
    val livesBonus = if (isVictory) lives * 100 else 0
    return (catPoints + levelPoints + comboPoints + timeBonus + livesBonus).coerceAtLeast(0)
  }
}
