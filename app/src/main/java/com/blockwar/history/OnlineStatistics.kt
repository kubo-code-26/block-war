package com.blockwar.history

import android.content.Context
import androidx.core.content.edit
import com.blockwar.model.AggregatedStats
import org.json.JSONObject
import java.util.Locale

data class OnlineMatchResult(val id: String, val won: Boolean, val bw: Int, val lines: Int,
    val attacks: Int, val durationSeconds: Long, val finishedAt: Long, val online: Boolean = true, val combos: Int = 0)

object OnlineStatisticsPolicy {
    fun aggregate(results: List<OnlineMatchResult>): AggregatedStats {
        val ordered=results.filter {it.online && it.id.isNotBlank()}.distinctBy {it.id}
            .sortedWith(compareBy<OnlineMatchResult> {it.finishedAt}.thenBy {it.id})
        var streak=0; var best=0
        ordered.forEach { if(it.won) {streak++;best=maxOf(best,streak)} else streak=0 }
        val wins=ordered.count {it.won}; val count=ordered.size
        fun total(field: (OnlineMatchResult)->Int)=ordered.sumOf {field(it).coerceAtLeast(0).toLong()}.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val seconds=ordered.sumOf {it.durationSeconds.coerceIn(0, Int.MAX_VALUE.toLong())}
        val rate=if(count==0)0f else wins*100f/count
        return AggregatedStats(totalMatches=count,totalWins=wins,totalLosses=count-wins,
            winRate=rate,winRatePercent=rate.toInt(),totalLinesCleared=total {it.lines},totalAttacksSent=total {it.attacks},totalCombos=total {it.combos},
            totalScore=total {it.bw},highScore=ordered.maxOfOrNull {it.bw.coerceAtLeast(0)}?:0,
            averageDurationSeconds=if(count==0)0 else seconds/count,currentWinStreak=streak,bestWinStreak=best,
            totalDurationSeconds=seconds,fastestWinSeconds=ordered.filter {it.won && it.durationSeconds>0}.minOfOrNull {it.durationSeconds}?:0)
    }

    fun toMap(s: AggregatedStats): Map<String,Any> = mapOf("totalMatches" to s.totalMatches,"totalWins" to s.totalWins,
        "totalLosses" to s.totalLosses,"totalLinesCleared" to s.totalLinesCleared,"totalAttacksSent" to s.totalAttacksSent,
        "totalCombos" to s.totalCombos,"totalScore" to s.totalScore,"highScore" to s.highScore,"currentWinStreak" to s.currentWinStreak,
        "bestWinStreak" to s.bestWinStreak,"totalDurationSeconds" to s.totalDurationSeconds,"fastestWinSeconds" to s.fastestWinSeconds)

    fun fromMap(map: Map<*,*>?): AggregatedStats {
        fun number(key:String)=(map?.get(key) as? Number)?.toLong()?.coerceAtLeast(0)?:0L
        fun int(key:String)=number(key).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val n=int("totalMatches"); val w=int("totalWins").coerceAtMost(n);val rate=if(n==0)0f else w*100f/n
        val seconds=number("totalDurationSeconds")
        return AggregatedStats(totalMatches=n,totalWins=w,totalLosses=n-w,winRate=rate,winRatePercent=rate.toInt(),
            totalLinesCleared=int("totalLinesCleared"),totalAttacksSent=int("totalAttacksSent"),totalScore=int("totalScore"),totalCombos=int("totalCombos"),
            highScore=int("highScore"),averageDurationSeconds=if(n==0)0 else seconds/n,
            currentWinStreak=int("currentWinStreak"),bestWinStreak=int("bestWinStreak"),totalDurationSeconds=seconds,fastestWinSeconds=number("fastestWinSeconds"))
    }
}

class OnlineStatisticsStore(context: Context) {
    private val prefs=context.getSharedPreferences("online_statistics_cache_v1",Context.MODE_PRIVATE)
    private fun key(name:String)="profile:"+name.trim().lowercase(Locale.ROOT)
    fun load(name:String): AggregatedStats {
        if(name.isBlank())return AggregatedStats()
        return runCatching {
            val json=JSONObject(prefs.getString(key(name),"{}")?:"{}")
            OnlineStatisticsPolicy.fromMap(json.keys().asSequence().associateWith {json.get(it)})
        }.getOrDefault(AggregatedStats())
    }
    fun save(name:String,stats:AggregatedStats) {
        if(name.isNotBlank()) prefs.edit {
            putString(key(name), JSONObject(OnlineStatisticsPolicy.toMap(stats)).toString())
        }
    }
}
