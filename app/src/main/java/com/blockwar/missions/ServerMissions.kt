package com.blockwar.missions

import com.blockwar.model.Mission
import com.blockwar.model.MissionType

/** Definitions and rewards come from the protected backend, including rotating tasks. */
internal object ServerMissions {
    fun parse(raw: List<*>): List<Mission> = raw.mapNotNull { value ->
        val row = value as? Map<*, *> ?: return@mapNotNull null
        val id = row["id"] as? String ?: return@mapNotNull null
        val title = row["title"] as? String ?: return@mapNotNull null
        val type = runCatching { MissionType.valueOf(row["type"] as? String ?: "") }.getOrNull()
            ?: return@mapNotNull null
        val goal = (row["goal"] as? Number)?.toInt()?.takeIf { it > 0 } ?: return@mapNotNull null
        Mission(id, title, row["description"] as? String ?: "", (row["reward"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
            (row["progress"] as? Number)?.toInt()?.coerceIn(0, goal) ?: 0, goal,
            row["isCompleted"] == true, row["isClaimed"] == true, type,
            row["rewardType"] as? String ?: "BLOCKOIN")
    }.distinctBy { it.id }
}
