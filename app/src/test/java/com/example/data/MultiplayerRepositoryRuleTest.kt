package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MultiplayerRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun randomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val raw = UUID.randomUUID().toString().replace("-", "").uppercase()
    return raw.take(6).map { if (it in 'A'..'Z' || it in '0'..'9') it else chars[0] }.joinToString("")
  }

  @Test
  fun upsertUserProfile_and_observeOwnProfile_succeeds() = runBlocking {
    val uid = signInTestUser("alice_cat@test.com")
    val repo = MultiplayerRepository(firestore)

    val saved = withTimeout(5000L) {
      repo.upsertUserProfile(uid, "AliceGato", "alice_cat@test.com", level = 3L, wins = 2L).getOrThrow()
    }
    assertEquals("AliceGato", saved.username)
    assertEquals(3L, saved.level)

    val observed = withTimeout(3000L) {
      repo.observeUserProfile(uid).first { it != null }
    }
    assertNotNull(observed)
    assertEquals("AliceGato", observed?.username)
  }

  @Test
  fun saveAndLoadCloudProgress_roundTripsAllFields() = runBlocking {
    val uid = signInTestUser("cloud_cat@test.com")
    val repo = MultiplayerRepository(firestore)

    val payload = CloudSavePayload(
      championCount = 12,
      bestTimeSec = 28.5f,
      bestHighScore = 4500,
      spendablePoints = 820,
      playerLevel = 7,
      playerXp = 40,
      totalCatsSaved = 150,
      bestEndlessCats = 42,
      totalGhostsScared = 19,
      totalItemsUsed = 33,
      totalDashesUsed = 24,
      totalSonicsUsed = 11,
      selectedSkinId = "cyber_neon",
      discoveredItemsCsv = "catnip,milk_shield",
      unlockedAchievementsCsv = "first_win,amor_felino",
      purchasedSkinsCsv = "cyber_neon,cosmic_galaxy",
      purchasedModesCsv = "turbo_frenzy",
      purchasedItemsCsv = "golden_collar",
      tradedAwaySkinsCsv = "snow_mingau",
      reviveCount = 4
    )

    val saved = withTimeout(5000L) {
      repo.saveCloudProgress(uid, "CloudMiau", "cloud_cat@test.com", payload).getOrThrow()
    }
    assertEquals(4500L, saved.bestHighScore)
    assertEquals(820L, saved.spendablePoints)
    assertEquals("cyber_neon", saved.selectedSkinId)
    assertEquals(4L, saved.reviveCount)

    val loaded = withTimeout(5000L) {
      repo.loadCloudProgress(uid).getOrThrow()
    }
    assertNotNull(loaded)
    assertEquals(12L, loaded?.championCount)
    assertEquals(4L, loaded?.reviveCount)
    assertEquals("cyber_neon,cosmic_galaxy", loaded?.purchasedSkinsCsv)
    assertEquals("snow_mingau", loaded?.tradedAwaySkinsCsv)
  }

  @Test
  fun directorySearch_friends_and_sharedSkins_succeeds() = runBlocking {
    val aliceUid = signInTestUser("alice_friend@test.com")
    val aliceRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      aliceRepo.upsertUserProfile(aliceUid, "AliceAmiga", "alice_friend@test.com", 5L, 4L, "cyber_neon").getOrThrow()
    }

    val bobUid = signInTestUser("bob_friend@test.com")
    val bobRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      bobRepo.upsertUserProfile(bobUid, "BobFelino", "bob_friend@test.com", 2L, 1L, "classic_orange").getOrThrow()
    }

    val results = withTimeout(5000L) {
      bobRepo.searchPlayersByUsername("alice", bobUid).getOrThrow()
    }
    assertTrue(results.any { it.userId == aliceUid && it.username == "AliceAmiga" })

    val aliceEntry = results.first { it.userId == aliceUid }
    withTimeout(5000L) {
      bobRepo.addFriend(bobUid, aliceEntry).getOrThrow()
    }
    val friends = withTimeout(3000L) {
      bobRepo.observeFriends(bobUid).first { list -> list.any { it.friendUserId == aliceUid } }
    }
    assertEquals("AliceAmiga", friends.first().friendUsername)

    val post = withTimeout(5000L) {
      bobRepo.shareSkin(
        ownerUserId = bobUid,
        ownerUsername = "BobFelino",
        skinId = "cyber_neon",
        skinName = "Cyber Gato Neon",
        badgeEmoji = "🤖",
        customNote = "Presente de amigo!",
        recipientUsername = "AliceAmiga"
      ).getOrThrow()
    }
    val shared = withTimeout(3000L) {
      bobRepo.observeSharedSkins().first { list -> list.any { it.shareId == post.shareId } }
    }
    assertTrue(shared.any { it.shareId == post.shareId && it.recipientUsername == "AliceAmiga" })

    withTimeout(5000L) {
      bobRepo.likeSharedSkin(post.shareId, post.likes).getOrThrow()
    }

    withTimeout(5000L) {
      bobRepo.removeFriend(bobUid, aliceUid).getOrThrow()
    }
    val friendsAfter = withTimeout(3000L) {
      bobRepo.observeFriends(bobUid).first { list -> list.none { it.friendUserId == aliceUid } }
    }
    assertTrue(friendsAfter.none { it.friendUserId == aliceUid })
  }

  @Test
  fun createAndJoinRoom_150Players_eliteVote_and_leaveRemovesImmediately() = runBlocking {
    val hostUid = signInTestUser("host_elite@test.com")
    val hostRepo = MultiplayerRepository(firestore)
    val code = randomCode()

    val created = withTimeout(5000L) {
      hostRepo.createRoom(
        hostUserId = hostUid,
        hostUsername = "HostElite",
        isPublic = true,
        targetCats = 10,
        maxPlayers = 150,
        difficulty = "ELITE",
        skinId = "classic_orange",
        explicitCode = code
      ).getOrThrow()
    }
    assertEquals(code, created.code)
    assertEquals(150L, created.maxPlayers)
    assertEquals("ELITE", created.difficulty)

    val guestUid = signInTestUser("guest_elite@test.com")
    val guestRepo = MultiplayerRepository(firestore)

    val (joinedRoom, joinedExisting) = withTimeout(5000L) {
      guestRepo.findAndJoinOrCreatePublicRoom(
        userId = guestUid,
        username = "GuestElite",
        skinId = "cyber_neon"
      ).getOrThrow()
    }
    assertTrue(joinedExisting)
    assertEquals("playing", joinedRoom.status)

    val chosenGhost = withTimeout(5000L) {
      guestRepo.castGhostVoteAndResolve(
        roomCode = joinedRoom.code,
        voterUserId = guestUid,
        voterUsername = "GuestElite",
        voteForUserId = hostUid,
        skinId = "cyber_neon"
      ).getOrThrow()
    }
    assertEquals(hostUid, chosenGhost)

    withTimeout(5000L) {
      guestRepo.syncPlayerProgress(
        roomCode = joinedRoom.code,
        userId = guestUid,
        username = "GuestElite",
        isHost = false,
        isGuestSlot = true,
        x = 640f,
        y = 1200f,
        score = 5,
        role = "cat",
        skinId = "cyber_neon",
        voteGhostForUid = hostUid
      ).getOrThrow()
    }

    val playersBeforeLeave = withTimeout(3000L) {
      guestRepo.observeRoomPlayers(joinedRoom.code).first { list -> list.any { it.userId == guestUid && it.score == 5L } }
    }
    assertTrue(playersBeforeLeave.any { it.userId == guestUid })

    withTimeout(5000L) {
      guestRepo.leaveRoomPlayer(joinedRoom.code, guestUid).getOrThrow()
    }

    val playersAfterLeave = withTimeout(3000L) {
      guestRepo.observeRoomPlayers(joinedRoom.code).first { list -> list.none { it.userId == guestUid } }
    }
    assertFalse(playersAfterLeave.any { it.userId == guestUid })
  }

  @Test
  fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = MultiplayerRepository(firestore)

    var thrown: Exception? = null
    try {
      withTimeout(3000L) {
        repo.observePublicWaitingRooms().first()
      }
    } catch (e: Exception) {
      thrown = e
    }
    val fsEx = checkNotNull(thrown?.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
  }

  @Test
  fun crossUserProfileRead_rejectedWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_pii@test.com")
    val aliceRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      aliceRepo.upsertUserProfile(aliceUid, "AlicePriv", "alice_pii@test.com", 1L, 0L).getOrThrow()
    }

    signInTestUser("bob_pii@test.com")
    val bobRepo = MultiplayerRepository(firestore)
    var thrown: Exception? = null
    try {
      withTimeout(3000L) {
        bobRepo.observeUserProfile(aliceUid).first()
      }
    } catch (e: Exception) {
      thrown = e
    }
    val fsEx = checkNotNull(thrown?.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
  }

  @Test
  fun postGameplayVideo_watchByOthers_like_comment_and_blockUser_succeeds() = runBlocking {
    val aliceUid = signInTestUser("alice_vid@test.com")
    val aliceRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      aliceRepo.upsertUserProfile(aliceUid, "AliceStreamer", "alice_vid@test.com", 2L, 1L).getOrThrow()
    }

    val sampleFrames = com.example.model.ReplayFrame.listToJson(
      listOf(
        com.example.model.ReplayFrame(timeSec = 0f, heroX = 800f, heroY = 1200f, ghostX = 300f, ghostY = 400f, catX = 600f, catY = 800f, catsCollected = 0, score = 0),
        com.example.model.ReplayFrame(timeSec = 0.2f, heroX = 760f, heroY = 1140f, ghostX = 340f, ghostY = 460f, catX = 600f, catY = 800f, catsCollected = 1, score = 120, isDashing = true)
      )
    )
    val postId = "vid_${UUID.randomUUID().toString().take(8)}"
    withTimeout(5000L) {
      aliceRepo.postGameplayVideo(
        GameplayVideoPost(
          postId = postId,
          userId = aliceUid,
          username = "AliceStreamer",
          userSkinId = "cyber_neon",
          title = "Minha gravação real",
          score = 120L,
          modeName = "Clássico",
          durationSec = 12.4f,
          replayFramesJson = sampleFrames
        )
      ).getOrThrow()
    }

    // Bob signs in and sees Alice's posted replay, likes it, and comments
    val bobUid = signInTestUser("bob_vid@test.com")
    val bobRepo = MultiplayerRepository(firestore)
    val feed = withTimeout(5000L) {
      bobRepo.observeGameplayVideos().first { list -> list.any { it.postId == postId } }
    }
    val seenPost = feed.first { it.postId == postId }
    assertEquals("AliceStreamer", seenPost.username)
    assertTrue(seenPost.replayFramesJson.isNotBlank())

    withTimeout(5000L) {
      bobRepo.likeGameplayVideo(postId, bobUid, seenPost.likes, seenPost.likedByUserIds).getOrThrow()
      bobRepo.commentOnVideo(
        postId,
        VideoComment(
          commentId = "cmt_${UUID.randomUUID().toString().take(8)}",
          userId = bobUid,
          username = "BobViewer",
          text = "Incrível!"
        )
      ).getOrThrow()
    }

    val updatedFeed = withTimeout(5000L) {
      bobRepo.observeGameplayVideos().first { list ->
        val p = list.find { it.postId == postId }
        p != null && p.likes == 1L && p.commentsCount == 1L
      }
    }
    assertTrue(updatedFeed.any { it.postId == postId && it.likedByUserIds.contains(bobUid) })

    // Bob can block Alice on his own profile
    withTimeout(5000L) {
      bobRepo.upsertUserProfile(bobUid, "BobViewer", "bob_vid@test.com", 1L, 0L).getOrThrow()
      bobRepo.blockUser(bobUid, aliceUid, "").getOrThrow()
    }

    // Alice deletes her own video
    signInTestUser("alice_vid@test.com")
    withTimeout(5000L) {
      aliceRepo.deleteGameplayVideo(postId).getOrThrow()
    }
  }

  @Test
  fun secretMode_randomGhost_kill_meeting_accuseVote_and_banishReveal_succeeds() = runBlocking {
    val code = randomCode()
    val aliceUid = signInTestUser("alice_sec@test.com")
    val aliceRepo = MultiplayerRepository(firestore)
    val roomCreated = withTimeout(5000L) {
      aliceRepo.createRoom(
        hostUserId = aliceUid,
        hostUsername = "AliceSec",
        isPublic = true,
        targetCats = 10,
        maxPlayers = 20,
        difficulty = "NORMAL",
        isSecretMode = true,
        skinId = "classic_orange",
        explicitCode = code
      ).getOrThrow()
    }
    assertTrue(roomCreated.isSecretMode)

    val bobUid = signInTestUser("bob_sec@test.com")
    val bobRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      bobRepo.joinRoomByCode(code, bobUid, "BobSec", "cyber_neon").getOrThrow()
    }

    val charlieUid = signInTestUser("charlie_sec@test.com")
    val charlieRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      charlieRepo.joinRoomByCode(code, charlieUid, "CharlieSec", "cosmic_galaxy").getOrThrow()
    }

    val dianaUid = signInTestUser("diana_sec@test.com")
    val dianaRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      dianaRepo.joinRoomByCode(code, dianaUid, "DianaSec", "snow_mingau").getOrThrow()
    }

    val assignedGhostUid = withTimeout(5000L) {
      aliceRepo.ensureSecretGhostAssigned(code).getOrThrow()
    }
    val allParticipants = listOf(
      Triple(aliceUid, "AliceSec", "alice_sec@test.com"),
      Triple(bobUid, "BobSec", "bob_sec@test.com"),
      Triple(charlieUid, "CharlieSec", "charlie_sec@test.com"),
      Triple(dianaUid, "DianaSec", "diana_sec@test.com")
    )
    assertTrue(allParticipants.any { it.first == assignedGhostUid })

    // Verify expanded map coordinates sync cleanly (3200 x 4800)
    signInTestUser("bob_sec@test.com")
    withTimeout(5000L) {
      bobRepo.syncPlayerProgress(
        roomCode = code,
        userId = bobUid,
        username = "BobSec",
        isHost = false,
        isGuestSlot = true,
        x = 2950f,
        y = 4500f,
        score = 2,
        role = "cat"
      ).getOrThrow()
    }

    val ghostEntry = allParticipants.first { it.first == assignedGhostUid }
    val innocents = allParticipants.filter { it.first != assignedGhostUid }
    val firstVictim = innocents[0]
    val secondInnocent = innocents[1]
    val thirdInnocent = innocents[2]

    // Secret ghost kills first innocent player
    signInTestUser(ghostEntry.third)
    val ghostRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      ghostRepo.killPlayerInSecretMatch(code, ghostEntry.first, ghostEntry.second, firstVictim.first).getOrThrow()
    }

    val afterKillRoom = withTimeout(3000L) {
      ghostRepo.observeRoom(code).first { it != null && it.eliminatedUserIdsCsv.contains(firstVictim.first) }
    }!!
    assertEquals("playing", afterKillRoom.status)

    // Innocent calls emergency meeting (Round 1)
    signInTestUser(thirdInnocent.third)
    val callerRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      callerRepo.callEmergencyMeeting(code, thirdInnocent.first, thirdInnocent.second).getOrThrow()
    }

    val meetingRoom1 = withTimeout(3000L) {
      callerRepo.observeRoom(code).first { it != null && it.meetingStatus == "active" && it.meetingRound == 1L }
    }!!
    assertEquals(thirdInnocent.second, meetingRoom1.meetingCallerUsername)

    // Accuse wrong player (secondInnocent) -> not an impostor reveal
    withTimeout(5000L) {
      callerRepo.castBanishVote(code, thirdInnocent.first, thirdInnocent.second, secondInnocent.first, 1L).getOrThrow()
    }
    signInTestUser(secondInnocent.third)
    val secondRepo = MultiplayerRepository(firestore)
    withTimeout(5000L) {
      secondRepo.castBanishVote(code, secondInnocent.first, secondInnocent.second, thirdInnocent.first, 1L).getOrThrow()
    }
    signInTestUser(ghostEntry.third)
    withTimeout(5000L) {
      ghostRepo.castBanishVote(code, ghostEntry.first, ghostEntry.second, secondInnocent.first, 1L).getOrThrow()
    }

    val round1Reveal = withTimeout(5000L) {
      callerRepo.resolveSecretMeeting(code).getOrThrow()
    }
    assertEquals("**${secondInnocent.second}** não era um impostor", round1Reveal)

    withTimeout(5000L) {
      callerRepo.dismissMeetingResult(code).getOrThrow()
    }

    // Round 2: survivor accuses actual Secret Ghost -> impostor revealed & match finishes
    signInTestUser(thirdInnocent.third)
    withTimeout(5000L) {
      callerRepo.callEmergencyMeeting(code, thirdInnocent.first, thirdInnocent.second).getOrThrow()
      callerRepo.castBanishVote(code, thirdInnocent.first, thirdInnocent.second, ghostEntry.first, 2L).getOrThrow()
    }
    val round2Reveal = withTimeout(5000L) {
      callerRepo.resolveSecretMeeting(code).getOrThrow()
    }
    assertEquals("**${ghostEntry.second}** realmente era um impostor", round2Reveal)

    val finalRoom = withTimeout(3000L) {
      callerRepo.observeRoom(code).first { it != null && it.status == "finished" }
    }!!
    assertTrue(finalRoom.lastBanishedWasImpostor)
    assertEquals(ghostEntry.first, finalRoom.lastBanishedUserId)
  }
}
