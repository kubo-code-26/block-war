package com.blockwar.missions

import android.content.Context
import com.blockwar.model.Mission
import org.json.JSONArray
import org.json.JSONObject

class MissionStore(context: Context) {
    private val prefs = context.getSharedPreferences("mission_progress_v1", Context.MODE_PRIVATE)
    private fun key(name: String) = name.trim().lowercase(java.util.Locale.ROOT).ifEmpty { "guest" }
    private fun read(name: String): JSONObject = try {
        JSONObject(prefs.getString(key(name), "{}") ?: "{}")
    } catch (_: Exception) { JSONObject() }

    fun load(name: String, definitions: List<Mission>): List<Mission> {
        val saved = read(name).optJSONObject("missions") ?: JSONObject()
        return definitions.map { m ->
            val entry = saved.optJSONObject(m.id)
            if (entry == null || entry.optString("period") != MissionCalendar.period(m.type)) m
            else m.copy(progress = entry.optInt("progress").coerceIn(0, m.goal),
                isCompleted = entry.optBoolean("completed"), isClaimed = entry.optBoolean("claimed"))
        }
    }

    fun pending(name: String): List<MissionEvent> {
        val array = read(name).optJSONArray("pending") ?: JSONArray()
        return (0 until array.length()).map { i ->
            val e = array.getJSONObject(i)
            MissionEvent(e.getString("id"), e.getString("missionId"), e.getString("period"),
                e.getInt("amount"), e.getBoolean("isSet"))
        }
    }

    fun save(name: String, missions: List<Mission>, events: List<MissionEvent>) {
        val entries = JSONObject()
        missions.forEach { m -> entries.put(m.id, JSONObject()
            .put("period", MissionCalendar.period(m.type)).put("progress", m.progress)
            .put("completed", m.isCompleted).put("claimed", m.isClaimed)) }
        val pending = JSONArray()
        events.forEach { e -> pending.put(JSONObject().put("id", e.id).put("missionId", e.missionId)
            .put("period", e.period).put("amount", e.amount).put("isSet", e.isSet)) }
        check(prefs.edit().putString(key(name), JSONObject().put("missions", entries)
            .put("pending", pending).toString()).commit()) { "Não foi possível salvar as missões." }
    }
}
