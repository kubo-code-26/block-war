package com.blockwar.network

import android.content.Context
import org.json.JSONObject
import java.util.Locale
import android.util.Log
import com.blockwar.storage.commit

data class InterruptedOnlineSession(val roomCode: String, val playerId: String, val isPlayer1: Boolean,
    val nickname: String, val matchId: String, val startedAt: Long, val score: Int,
    val lines: Int, val attacks: Int, val combo: Int)

class OnlineSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("interrupted_online_session", Context.MODE_PRIVATE)
    private fun owner(nickname: String) = nickname.trim().lowercase(Locale.ROOT)
    private fun key(matchId: String) = "match:$matchId"

    private fun decode(raw: String): InterruptedOnlineSession? = try {
            val j = JSONObject(raw)
            InterruptedOnlineSession(j.getString("room"), j.getString("player"), j.getBoolean("p1"),
                j.getString("nickname"), j.getString("match"), j.getLong("started"),
                j.getInt("score"), j.getInt("lines"), j.getInt("attacks"), j.getInt("combo"))
    } catch (_: Exception) { null }

    private fun migrateLegacy() {
        val raw = prefs.getString("session", null) ?: return
        val legacy = decode(raw) ?: return
        // The conversion and removal happen in the same preference commit.
        prefs.commit {
            if (!prefs.contains(key(legacy.matchId))) putString(key(legacy.matchId), raw)
            remove("session")
        }
    }

    private fun allSessions(): List<InterruptedOnlineSession> {
        migrateLegacy()
        val sessions = prefs.all.entries.filter { it.key.startsWith("match:") }
            .mapNotNull { (it.value as? String)?.let(::decode) }
        // If a migration could not be committed, the legacy record must remain recoverable.
        val legacy = prefs.getString("session", null)?.let(::decode)
        return (sessions + listOfNotNull(legacy)).distinctBy { it.matchId }
            .sortedWith(compareBy<InterruptedOnlineSession> { it.startedAt }.thenBy { it.matchId })
    }

    @Synchronized
    fun load(): InterruptedOnlineSession? = allSessions().firstOrNull()

    @Synchronized
    fun pending(nickname: String): List<InterruptedOnlineSession> =
        allSessions().filter { owner(it.nickname) == owner(nickname) }

    @Synchronized
    fun save(s: InterruptedOnlineSession) {
        migrateLegacy()
        val existing = prefs.getString(key(s.matchId), null)?.let(::decode)
            ?: prefs.getString("session", null)?.let(::decode)?.takeIf { it.matchId == s.matchId }
        if (existing != null && owner(existing.nickname) != owner(s.nickname)) {
            Log.w("OnlineSessionStore", "Uma partida pendente não pode mudar de perfil.")
            return
        }
        prefs.commit {
            putString(key(s.matchId), JSONObject().put("room", s.roomCode).put("player", s.playerId)
                .put("p1", s.isPlayer1).put("nickname", s.nickname).put("match", s.matchId)
                .put("started", s.startedAt).put("score", s.score).put("lines", s.lines)
                .put("attacks", s.attacks).put("combo", s.combo).toString())
        }
    }

    @Synchronized
    fun clear(matchId: String) {
        migrateLegacy()
        prefs.commit {
            remove(key(matchId))
            if (prefs.getString("session", null)?.let(::decode)?.matchId == matchId) remove("session")
        }
    }
}
