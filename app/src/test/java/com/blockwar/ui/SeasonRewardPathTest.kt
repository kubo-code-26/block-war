package com.blockwar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.ui.components.SeasonRewardPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w320dp-h850dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SeasonRewardPathTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ids=listOf("season_frame_forja","real_obsidiana","real_fagulhas","real_fundicao","season_banner_forja","season_anim_forja","avatar_forja","fundo_forja","real_magma")
    private fun tiers()=listOf(
        mapOf("goal" to 100,"title" to "Aprendiz","items" to ids.take(1)),
        mapOf("goal" to 300,"title" to "Especialista","items" to ids.slice(1..1)),
        mapOf("goal" to 700,"title" to "Guardião","items" to ids.slice(2..2)),
        mapOf("goal" to 1500,"title" to "Mestre","items" to ids.slice(3..5)),
        mapOf("goal" to 2000,"title" to "Sentinela","items" to ids.slice(6..6)),
        mapOf("goal" to 2500,"title" to "Forja","items" to ids.slice(7..7)),
        mapOf("goal" to 3500,"title" to "Magma","items" to ids.takeLast(1)))
    @Test fun nineItemsRemainVisibleAndBundleClaimsOriginalTier() {
        var claim=-1
        compose.setContent { MaterialTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            SeasonRewardPath(tiers(),3500,false,androidx.compose.ui.graphics.Color(0xFFFFB244)){claim=it}
        } } }
        compose.onNodeWithText("9 itens para conquistar").assertIsDisplayed()
        ids.forEach { compose.onNodeWithTag("season_reward_item_$it").performScrollTo().assertIsDisplayed() }
        compose.onNodeWithTag("season_claim_3").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(3,claim) }
        compose.onNodeWithTag("season_claim_6").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(6,claim) }
    }
}