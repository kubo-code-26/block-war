package com.blockwar.network

import com.blockwar.history.*
import com.google.firebase.database.MutableData

/** Called inside the SAME profile transaction as BW/Blockoins, with an immutable per-match receipt. */
internal fun recordOnlineStatistics(user: MutableData, reward: PendingMatchReward) {
    if(!reward.isOnline)return
    val receipt=user.child("appliedMatchRewards").child(reward.matchId)
    val detail=receipt.child("onlineStatistics")
    if(detail.value==null) {
        detail.value=mapOf("online" to true,"bw" to reward.matchPoints.coerceAtLeast(0),
            "lines" to reward.linesCleared.coerceAtLeast(0),"attacks" to reward.attacksSent.coerceAtLeast(0),
            "durationSeconds" to reward.durationSeconds.coerceAtLeast(0),"finishedAt" to reward.finishedAtMillis)
    }
    val results=user.child("appliedMatchRewards").children.mapNotNull {entry ->
        val values=entry.child("onlineStatistics").value as? Map<*,*> ?: return@mapNotNull null
        if(values["online"]!=true)return@mapNotNull null
        fun n(key:String)=(values[key] as? Number)?.toLong()?:0
        OnlineMatchResult(entry.key?:"",entry.child("isVictory").value==true,n("bw").toInt(),n("lines").toInt(),
            n("attacks").toInt(),n("durationSeconds"),n("finishedAt"),combos=n("combos").toInt())
    }
    user.child("onlineStats").value=OnlineStatisticsPolicy.toMap(OnlineStatisticsPolicy.aggregate(results))
}
