package com.blockwar.missions

import com.blockwar.model.Mission
import com.blockwar.model.MissionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class MissionEvent(val id: String, val missionId: String, val period: String,
                        val amount: Int, val isSet: Boolean)

object MissionCalendar {
    @Volatile var serverOffsetMillis: Long = 0
    private val zone = TimeZone.getTimeZone("America/Sao_Paulo")

    fun period(type: MissionType, now: Long = System.currentTimeMillis() + serverOffsetMillis): String {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = now }
        val pattern = when (type) {
            MissionType.DAILY -> "yyyy-MM-dd"
            MissionType.WEEKLY -> {
                val days = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
                calendar.add(Calendar.DAY_OF_MONTH, -days)
                "yyyy-MM-dd"
            }
            MissionType.MONTHLY -> "yyyy-MM"
        }
        return SimpleDateFormat(pattern, Locale.ROOT).apply { timeZone = zone }.format(calendar.time)
    }

    fun advance(mission: Mission, event: MissionEvent): Mission {
        if (mission.isCompleted || mission.isClaimed) return mission
        val progress = if (event.isSet) maxOf(mission.progress, event.amount)
            else (mission.progress.toLong() + event.amount.coerceAtLeast(0)).coerceAtMost(mission.goal.toLong()).toInt()
        return mission.copy(progress = progress.coerceIn(0, mission.goal),
            isCompleted = progress >= mission.goal)
    }
}
