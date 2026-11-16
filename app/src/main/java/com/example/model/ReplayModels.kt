package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class ReplayFrame(
  val timeSec: Float = 0f,
  val heroX: Float = 800f,
  val heroY: Float = 1200f,
  val ghostX: Float = 800f,
  val ghostY: Float = 400f,
  val catX: Float = 800f,
  val catY: Float = 800f,
  val catsCollected: Int = 0,
  val score: Int = 0,
  val isDashing: Boolean = false,
  val ghost2X: Float = -1f,
  val ghost2Y: Float = -1f,
  val ghost3X: Float = -1f,
  val ghost3Y: Float = -1f,
  val isSonicActive: Boolean = false,
  val isGhostFleeing: Boolean = false,
  val isPlayerGhost: Boolean = false,
  val lives: Int = 3
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("t", (timeSec * 100).toInt() / 100f)
    put("hx", heroX.toInt())
    put("hy", heroY.toInt())
    put("gx", ghostX.toInt())
    put("gy", ghostY.toInt())
    put("cx", catX.toInt())
    put("cy", catY.toInt())
    put("c", catsCollected)
    put("s", score)
    if (isDashing) put("d", 1)
    if (ghost2X >= 0f && ghost2Y >= 0f) {
      put("g2x", ghost2X.toInt())
      put("g2y", ghost2Y.toInt())
    }
    if (ghost3X >= 0f && ghost3Y >= 0f) {
      put("g3x", ghost3X.toInt())
      put("g3y", ghost3Y.toInt())
    }
    if (isSonicActive) put("sw", 1)
    if (isGhostFleeing) put("gf", 1)
    if (isPlayerGhost) put("pg", 1)
    if (lives != 3) put("lv", lives)
  }

  companion object {
    fun fromJson(json: JSONObject): ReplayFrame = ReplayFrame(
      timeSec = json.optDouble("t", 0.0).toFloat(),
      heroX = json.optInt("hx", 800).toFloat(),
      heroY = json.optInt("hy", 1200).toFloat(),
      ghostX = json.optInt("gx", 800).toFloat(),
      ghostY = json.optInt("gy", 400).toFloat(),
      catX = json.optInt("cx", 800).toFloat(),
      catY = json.optInt("cy", 800).toFloat(),
      catsCollected = json.optInt("c", 0),
      score = json.optInt("s", 0),
      isDashing = json.optInt("d", 0) == 1,
      ghost2X = if (json.has("g2x")) json.optInt("g2x", -1).toFloat() else -1f,
      ghost2Y = if (json.has("g2y")) json.optInt("g2y", -1).toFloat() else -1f,
      ghost3X = if (json.has("g3x")) json.optInt("g3x", -1).toFloat() else -1f,
      ghost3Y = if (json.has("g3y")) json.optInt("g3y", -1).toFloat() else -1f,
      isSonicActive = json.optInt("sw", 0) == 1,
      isGhostFleeing = json.optInt("gf", 0) == 1,
      isPlayerGhost = json.optInt("pg", 0) == 1,
      lives = json.optInt("lv", 3)
    )

    fun listToJson(frames: List<ReplayFrame>, maxBytes: Int = 60_000): String {
      if (frames.isEmpty()) return "[]"
      var candidate = frames
      var encoded = encodeArray(candidate)
      while (encoded.length > maxBytes && candidate.size > 4) {
        val last = candidate.last()
        val thinned = candidate.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
        if (thinned.lastOrNull() != last) {
          thinned.add(last)
        }
        candidate = thinned
        encoded = encodeArray(candidate)
      }
      return encoded
    }

    private fun encodeArray(frames: List<ReplayFrame>): String {
      val arr = JSONArray()
      frames.forEach { arr.put(it.toJson()) }
      return arr.toString()
    }

    fun listFromJson(raw: String): List<ReplayFrame> {
      if (raw.isBlank()) return emptyList()
      return try {
        val arr = JSONArray(raw)
        val list = mutableListOf<ReplayFrame>()
        for (i in 0 until arr.length()) {
          list.add(fromJson(arr.getJSONObject(i)))
        }
        list
      } catch (_: Exception) {
        emptyList()
      }
    }
  }
}
