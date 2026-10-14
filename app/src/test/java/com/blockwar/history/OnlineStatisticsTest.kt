package com.blockwar.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.blockwar.network.recordOnlineStatistics
import com.google.firebase.database.MutableData
import com.google.firebase.database.snapshot.Node
import com.google.firebase.database.snapshot.NodeUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class OnlineStatisticsTest {
    private lateinit var context:Context
    @Before fun setup() {
        context=ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pending_match_rewards",0).edit().clear().commit()
        context.getSharedPreferences("online_statistics_cache_v1",0).edit().clear().commit()
    }
    private fun data(value:Any?):MutableData {
        val ctor=MutableData::class.java.getDeclaredConstructor(Node::class.java)
        ctor.isAccessible=true
        return ctor.newInstance(NodeUtilities.NodeFromJSON(value))
    }
    private fun reward(id:String,won:Boolean,time:Long)=PendingMatchReward(id,"alice",if(won)36 else 6,50,won,
        isOnline=true,matchPoints=16,linesCleared=4,attacksSent=16,durationSeconds=50,finishedAtMillis=time)
    private fun add(user:MutableData,r:PendingMatchReward) {
        if(user.child("appliedMatchRewards").child(r.matchId).value==null)
            user.child("appliedMatchRewards").child(r.matchId).value=mapOf("isVictory" to r.isVictory)
        recordOnlineStatistics(user,r)
    }
    @Test fun cpuMatchesNeverEnterTheOnlinePanel() {
        val s=OnlineStatisticsPolicy.aggregate(listOf(
            OnlineMatchResult("cpu",true,999,99,999,1,1,online=false),
            OnlineMatchResult("online",false,8,2,8,50,2)))
        assertEquals(1,s.totalMatches);assertEquals(0,s.totalWins);assertEquals(1,s.totalLosses)
        assertEquals(8,s.highScore);assertEquals(2,s.totalLinesCleared);assertEquals(0L,s.fastestWinSeconds)
    }
    @Test fun winRateTimesAndBwUseOnlyTheRecordedOnlineResults() {
        val s=OnlineStatisticsPolicy.aggregate(listOf(
            OnlineMatchResult("a",true,32,8,40,50,1),OnlineMatchResult("b",false,5,1,5,70,2),
            OnlineMatchResult("c",true,12,3,12,30,3)))
        assertEquals(3,s.totalMatches);assertEquals(2,s.totalWins);assertEquals(66,s.winRatePercent)
        assertEquals(50L,s.averageDurationSeconds);assertEquals(30L,s.fastestWinSeconds)
        assertEquals(150L,s.totalDurationSeconds);assertEquals(32,s.highScore);assertEquals(49,s.totalScore)
        assertEquals(12,s.totalLinesCleared);assertEquals(57,s.totalAttacksSent)
    }
    @Test fun streaksAreChronologicalEvenWhenResultsArriveOutOfOrder() {
        val user=data(mapOf("coins" to 579))
        add(user,reward("late-loss",false,20))
        add(user,reward("early-win",true,10))
        val s=OnlineStatisticsPolicy.fromMap(user.child("onlineStats").value as Map<*,*>)
        assertEquals(2,s.totalMatches);assertEquals(0,s.currentWinStreak);assertEquals(1,s.bestWinStreak)
        assertEquals(579L,user.child("coins").value)
    }
    @Test fun duplicateReceiptsDoNotAddMatchesOrReplaceTheFirstScore() {
        val user=data(null);val r=reward("same",true,1)
        add(user,r);add(user,r.copy(matchPoints=999))
        val s=OnlineStatisticsPolicy.fromMap(user.child("onlineStats").value as Map<*,*>)
        assertEquals(1,s.totalMatches);assertEquals(16,s.highScore);assertEquals(16,s.totalScore)
    }
    @Test fun lifetimeStatsAreNotLimitedToFiftyMatches() {
        val results=(1..80).map {OnlineMatchResult("$it",true,5,1,5,10,it.toLong())}
        val s=OnlineStatisticsPolicy.aggregate(results)
        assertEquals(80,s.totalMatches);assertEquals(80,s.currentWinStreak);assertEquals(80,s.bestWinStreak)
    }
    @Test fun statisticsMetadataSurvivesAnAppRestart() {
        val r=reward("pending",true,123).copy(matchPoints=64,attacksSent=32,linesCleared=8,durationSeconds=45)
        assertTrue(PendingMatchRewardStore(context).save(r))
        assertEquals(r,PendingMatchRewardStore(context).pending("alice").single())
    }
    @Test fun oldPendingRewardsAreNotMistakenForOnlineMatches() {
        context.getSharedPreferences("pending_match_rewards",0).edit().putString("old",
            """{"matchId":"old","nickname":"alice","bwDelta":25,"blockoins":20,"isVictory":true}""").commit()
        val old=PendingMatchRewardStore(context).pending("alice").single()
        assertFalse(old.isOnline)
        val user=data(mapOf("coins" to 100))
        recordOnlineStatistics(user,old)
        assertNull(user.child("onlineStats").value)
    }
    @Test fun offlineCachesAreIsolatedByProfile() {
        val store=OnlineStatisticsStore(context)
        val s=OnlineStatisticsPolicy.aggregate(listOf(OnlineMatchResult("a",true,16,4,16,30,1)))
        store.save("alice",s)
        assertEquals(s,store.load(" ALICE "))
        assertEquals(0,store.load("bob").totalMatches)
        assertEquals(0,store.load("").totalMatches)
    }
}
