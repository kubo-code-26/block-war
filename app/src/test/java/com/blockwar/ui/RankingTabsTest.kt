package com.blockwar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.ui.components.RankingTabs
import com.blockwar.ui.components.SeasonalRankingContent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w393dp-h850dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RankingTabsTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun tabsSelectIndependentRankingsAndReturnToGlobal() {
        compose.setContent { MaterialTheme {
            var tab by remember { mutableStateOf(0) }
            Column { RankingTabs(tab) { tab=it }; Text(if(tab==0) "Dados globais PBW" else "Dados mensais XP") }
        } }
        compose.onNodeWithTag("ranking_tab_global").assertIsSelected()
        compose.onNodeWithTag("ranking_tab_season").performClick().assertIsSelected()
        compose.onNodeWithText("Dados mensais XP").assertIsDisplayed()
        compose.onNodeWithText("Dados globais PBW").assertDoesNotExist()
        compose.onNodeWithTag("ranking_tab_global").performClick().assertIsSelected()
        compose.onNodeWithText("Dados globais PBW").assertIsDisplayed()
    }
    @Test fun seasonalRankingUsesMonthlyXpAndCurrentPlayer() {
        val data=mapOf<String,Any?>("name" to "Forja da Arena", "period" to "2026-10", "xp" to 120,
            "leaderboard" to listOf(mapOf("nickname" to "Campeao", "xp" to 900),mapOf("nickname" to "Jogador", "xp" to 120)))
        compose.setContent { MaterialTheme { SeasonalRankingContent(data,"Jogador",false,"",{}) } }
        compose.onNodeWithText("Forja da Arena · 2026-10").assertIsDisplayed()
        compose.onNodeWithText("900 XP").assertIsDisplayed()
        compose.onNodeWithText("VOCÊ · 2º").assertIsDisplayed()
        compose.onNodeWithText("Vaga aberta").assertExists()
    }
    @Test fun seasonalErrorAllowsRetryWithoutShowingStaleRanking() {
        var retried=false
        compose.setContent { MaterialTheme { SeasonalRankingContent(mapOf("name" to "Stale"),"Jogador",false,"Falha de conexão",{retried=true}) } }
        compose.onNodeWithText("Falha de conexão").assertIsDisplayed()
        compose.onNodeWithText("Stale").assertDoesNotExist()
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.runOnIdle { assertTrue(retried) }
    }
}