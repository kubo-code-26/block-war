package com.blockwar.ui
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.model.AggregatedStats
import com.blockwar.model.PlayerInventory
import com.blockwar.ui.components.AchievementsScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w393dp-h850dp") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FeaturedBadgeTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun onlyEarnedBadgeOffersFeatureAction() {
        var selected:Pair<String,Int>?=null
        compose.setContent {MaterialTheme {AchievementsScreen(AggregatedStats(totalWins=10),PlayerInventory(),emptyList(),{},onFeature={c,t->selected=c to t})}}
        compose.onNodeWithTag("achievement_wins").performScrollTo().performClick()
        compose.onAllNodesWithText("Exibir no perfil").assertCountEquals(1)
        compose.onNodeWithText("Exibir no perfil").performScrollTo().performClick()
        compose.runOnIdle{assertEquals("wins" to 0,selected)}
    }
}
