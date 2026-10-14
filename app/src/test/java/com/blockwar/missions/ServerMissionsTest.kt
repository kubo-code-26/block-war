package com.blockwar.missions

import org.junit.Assert.*
import org.junit.Test

class ServerMissionsTest {
    private fun row() = mapOf("id" to "rot_daily_lines", "title" to "Abrindo Caminho",
        "description" to "Limpe 40 linhas", "type" to "DAILY", "goal" to 40,
        "reward" to 100, "progress" to 12, "isCompleted" to false, "isClaimed" to false)
    @Test fun acceptsNewDefinitionsInsteadOfDiscardingUnknownIds() {
        val mission = ServerMissions.parse(listOf(row())).single()
        assertEquals("rot_daily_lines", mission.id)
        assertEquals(40, mission.goal)
        assertEquals(12, mission.progress)
        assertEquals(100, mission.reward)
    }
    @Test fun malformedAndDuplicateRowsCannotCrashOrDuplicateTasks() {
        val result = ServerMissions.parse(listOf(null, "wrong", row(), row(),
            row()+mapOf("id" to "invalid", "goal" to 0),
            row()+mapOf("id" to "unknown", "type" to "OTHER")))
        assertEquals(1, result.size)
    }
}
