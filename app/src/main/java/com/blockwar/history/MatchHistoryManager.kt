package com.blockwar.history

import android.content.Context
import android.content.SharedPreferences
import com.blockwar.model.AggregatedStats
import com.blockwar.model.MatchRecord
import com.blockwar.settings.ControlSettingsManager
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class MatchHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("block_war_match_history", Context.MODE_PRIVATE)
    private val historyKey = "saved_matches_list"
    private val settings = ControlSettingsManager(context)

    private fun ownerKey(): String {
        val owner = settings.getPlayerName().trim().lowercase(Locale.ROOT)
        return if (owner.isEmpty()) "history_guest_v2" else "history_profile_v2:$owner"
    }

    // Legacy records do not contain an owner: keep them intact, outside every profile's statistics.
    @Synchronized fun getLegacyHistory(): List<MatchRecord> = readHistory(historyKey)

    @Synchronized fun getHistory(): List<MatchRecord> = readHistory(ownerKey())

    private fun readHistory(key: String): List<MatchRecord> {
        val jsonStr = prefs.getString(key, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<MatchRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MatchRecord(
                        id = obj.optString("id", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        gameMode = obj.optString("gameMode", ""),
                        isVictory = obj.optBoolean("isVictory", false),
                        linesCleared = obj.optInt("linesCleared", 0),
                        attacksSent = obj.optInt("attacksSent", 0),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        score = obj.optInt("score", 0),
                        maxCombo = obj.optInt("maxCombo", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized fun saveMatch(record: MatchRecord) {
        val key = ownerKey()
        val currentHistory = readHistory(key).filterNot { record.id.isNotEmpty() && it.id == record.id }
        val trimmed = (currentHistory + record).sortedByDescending { it.timestamp }.take(50)

        val jsonArray = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("gameMode", item.gameMode)
                put("isVictory", item.isVictory)
                put("linesCleared", item.linesCleared)
                put("attacksSent", item.attacksSent)
                put("durationSeconds", item.durationSeconds)
                put("score", item.score)
                put("maxCombo", item.maxCombo)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }

    @Synchronized fun getAggregatedStats(): AggregatedStats {
        val list = getHistory()
        if (list.isEmpty()) {
            return AggregatedStats()
        }

        val totalMatches = list.size
        val wins = list.count { it.isVictory }
        val losses = totalMatches - wins
        val winRate = (wins.toFloat() / totalMatches.toFloat()) * 100f
        val totalLines = list.sumOf { it.linesCleared }
        val totalAttacks = list.sumOf { it.attacksSent }
        val highScore = list.maxOfOrNull { it.score } ?: 0
        val totalScore = list.sumOf { it.score }
        val totalTime = list.sumOf { it.durationSeconds }
        val avgTime = totalTime / totalMatches

        return AggregatedStats(
            totalMatches = totalMatches,
            totalWins = wins,
            totalLosses = losses,
            winRate = winRate,
            winRatePercent = winRate.toInt(),
            totalLinesCleared = totalLines,
            totalAttacksSent = totalAttacks,
            totalScore = totalScore,
            highScore = highScore,
            averageDurationSeconds = avgTime
        )
    }

    @Synchronized fun clearHistory() {
        prefs.edit().remove(ownerKey()).apply()
    }
}
