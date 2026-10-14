package com.blockwar.history
import android.content.Context
import org.json.JSONObject

data class PendingMatchReward(
    val matchId: String,
    val nickname: String,
    val bwDelta: Int,
    val blockoins: Int,
    val isVictory: Boolean,
    val isOnline: Boolean = false,
    val matchPoints: Int = 0,
    val linesCleared: Int = 0,
    val attacksSent: Int = 0,
    val durationSeconds: Long = 0L,
    val finishedAtMillis: Long = 0L
)

object MatchRewardPolicy {
    fun bwDelta(matchPoints: Int, isVictory: Boolean): Int =
        matchPoints + if (isVictory) 20 else -10

    fun blockoins(isOnline: Boolean): Int = if (isOnline) 50 else 20

    fun applyDelta(balance: Int, delta: Int): Int =
        (balance.toLong() + delta).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}

class PendingMatchRewardStore(context: Context) {
    private val prefs = context.getSharedPreferences("pending_match_rewards", Context.MODE_PRIVATE)
    fun save(reward: PendingMatchReward): Boolean {
        val data = JSONObject().apply {
            put("matchId", reward.matchId); put("nickname", reward.nickname)
            put("bwDelta", reward.bwDelta); put("blockoins", reward.blockoins); put("isVictory", reward.isVictory)
            put("isOnline",reward.isOnline);put("matchPoints",reward.matchPoints);put("linesCleared",reward.linesCleared)
            put("attacksSent",reward.attacksSent);put("durationSeconds",reward.durationSeconds);put("finishedAtMillis",reward.finishedAtMillis)
        }
        return prefs.edit().putString(reward.matchId, data.toString()).commit()
    }
    fun pending(nickname: String): List<PendingMatchReward> = prefs.all.values.mapNotNull { raw ->
        runCatching {
            val data = JSONObject(raw as String)
            PendingMatchReward(data.getString("matchId"), data.getString("nickname"),
                data.getInt("bwDelta"), data.getInt("blockoins"), data.getBoolean("isVictory"),data.optBoolean("isOnline",false),data.optInt("matchPoints"),
                data.optInt("linesCleared"),data.optInt("attacksSent"),data.optLong("durationSeconds"),data.optLong("finishedAtMillis"))
        }.getOrNull()
    }.filter { it.nickname.equals(nickname, ignoreCase = true) }
    fun remove(matchId: String) { prefs.edit().remove(matchId).commit() }
}