package com.blockwar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.ui.components.SeasonPageContent
import com.blockwar.ui.components.seasonDaysRemaining
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w393dp-h850dp") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class SeasonPageTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun season(xp:Int=820):Map<String,Any?> = mapOf("id" to "forja","name" to "Forja da Arena","period" to "2026-10",
        "board" to "real_fundicao","skin" to "real_obsidiana","xp" to xp,"leaderboard" to emptyList<Any>(),
        "tiers" to listOf(
            mapOf("goal" to 100,"title" to "Aprendiz","items" to listOf("season_frame_forja"),"claimed" to (xp>=100)),
            mapOf("goal" to 300,"title" to "Especialista","items" to listOf("real_obsidiana"),"claimed" to (xp>=300)),
            mapOf("goal" to 700,"title" to "Guardião","items" to listOf("real_fagulhas"),"claimed" to false),
            mapOf("goal" to 1500,"title" to "Mestre da temporada","items" to listOf("real_fundicao","season_banner_forja","season_anim_forja"),"claimed" to false)))
    @Test fun onlyAvailableRewardCanBeClaimedAndCollectionOpens() {
        var selected:Int?=null;var opened=false
        compose.setContent{MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
            SeasonPageContent(season(),"jogador",false,{selected=it},{opened=true})
        }}}
        compose.onNodeWithTag("season_claim_0").assertDoesNotExist()
        compose.onNodeWithTag("season_claim_3").assertDoesNotExist()
        compose.onNodeWithTag("season_claim_2").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(2,selected)}
        compose.onNodeWithTag("season_collection").performScrollTo().performClick()
        compose.runOnIdle{assertTrue(opened)}
    }
    @Test fun loadingPreventsRepeatedClaimsAndEmptyRankingUsesNoFakePlayers() {
        compose.setContent{MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
            SeasonPageContent(season(),"jogador",true,{fail("Busy claim must not execute")},{})
        }}}
        compose.onNodeWithTag("season_claim_2").assertIsNotEnabled()
        compose.onNodeWithText("VOCÊ · SEM POSIÇÃO NO TOP 50").assertExists()
        compose.onNodeWithText("Fênix").assertDoesNotExist()
    }
    @Test fun countdownUsesBrasiliaMonthBoundary() {
        assertEquals(1L,seasonDaysRemaining("2026-10",Instant.parse("2026-11-01T02:59:59Z")))
        assertEquals(0L,seasonDaysRemaining("2026-10",Instant.parse("2026-11-01T03:00:00Z")))
        assertNull(seasonDaysRemaining("invalid"))
    }
}
