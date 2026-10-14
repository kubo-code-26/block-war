package com.blockwar.achievements

import com.blockwar.economy.ShopCatalog
import com.blockwar.history.OnlineMatchResult
import com.blockwar.history.OnlineStatisticsPolicy
import com.blockwar.model.*
import org.junit.Assert.*
import org.junit.Test

class AchievementCatalogTest {
    @Test fun eachOfSixCategoriesUnlocksExactlyAtAllSevenThresholds() {
        assertEquals(6, AchievementCatalog.categories.size)
        assertEquals(7, AchievementCatalog.tiers.size)
        for(c in AchievementCatalog.categories) {
            assertEquals(7,c.goals.distinct().size)
            assertEquals(c.goals.sorted(),c.goals)
            c.goals.forEachIndexed { index, goal ->
                assertEquals(index,AchievementProgress(c,goal-1).unlockedCount)
                val reached=AchievementProgress(c,goal)
                assertEquals(index+1,reached.unlockedCount)
                assertEquals(AchievementCatalog.tiers[index],reached.currentTier)
                assertEquals(c.goals.getOrNull(index+1),reached.nextGoal)
            }
        }
    }
    @Test fun categoriesDoNotBorrowProgressFromEachOther() {
        val p=AchievementCatalog.progress(AggregatedStats(totalWins=10,totalAttacksSent=10000),PlayerInventory(),ShopCatalog.items)
        assertEquals(1,p.first{it.category.id=="wins"}.unlockedCount)
        assertEquals(3,p.first{it.category.id=="attacks"}.unlockedCount)
        assertEquals(0,p.first{it.category.id=="matches"}.unlockedCount)
    }
    @Test fun collectorCountsKnownPaidItemsOnceAndAllLevelsAreReachable() {
        val paid=ShopCatalog.items.filter{it.price>0}.map{it.id}
        val inventory=PlayerInventory(boughtItems=PlayerInventory().boughtItems+paid+paid+"fake")
        val p=AchievementCatalog.progress(AggregatedStats(),inventory,ShopCatalog.items).last()
        assertEquals(paid.toSet().size,p.value)
        assertEquals(7,p.unlockedCount)
        assertEquals(0,AchievementCatalog.progress(AggregatedStats(),PlayerInventory(),ShopCatalog.items).last().value)
    }
    @Test fun emptyNegativeAndMaximumProgressStayBounded() {
        val c=AchievementCatalog.categories.first()
        assertEquals(0f,AchievementProgress(c,0).fraction)
        assertEquals(1f,AchievementProgress(c,Int.MAX_VALUE).fraction)
        assertNull(AchievementProgress(c,0).currentTier)
        assertNull(AchievementProgress(c,Int.MAX_VALUE).nextGoal)
    }
    @Test fun combosRoundTripWithoutCountingCpuOrDuplicateMatches() {
        val r=OnlineMatchResult("online",true,10,2,10,30,100,combos=3)
        val stats=OnlineStatisticsPolicy.aggregate(listOf(r,r,r.copy(id="cpu",online=false,combos=999)))
        assertEquals(3,stats.totalCombos)
        assertEquals(3,OnlineStatisticsPolicy.fromMap(OnlineStatisticsPolicy.toMap(stats)).totalCombos)
        assertEquals(0,OnlineStatisticsPolicy.fromMap(mapOf("totalMatches" to 4)).totalCombos)
    }
}
