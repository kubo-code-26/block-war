package com.blockwar

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blockwar.economy.ShopCatalog
import com.blockwar.model.*
import com.blockwar.ui.components.AchievementsScreen
import com.blockwar.ui.components.AchievementsEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AchievementsScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun compactEntryOpensSeparateScreenAndBackPreservesNavigationWithLargeFont() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                MaterialTheme {
                    var open by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxSize().background(Color(0xFF121318)).padding(16.dp)) {
                        AchievementsEntry(9) { open=true }
                        if(open) AchievementsScreen(AggregatedStats(totalWins=50,totalMatches=250,totalAttacksSent=2100,
                            totalLinesCleared=600,totalCombos=30),PlayerInventory(),ShopCatalog.items) { open=false }
                    }
                }
            }
        }
        compose.onNodeWithTag("achievement_categories").assertDoesNotExist()
        compose.onNodeWithTag("profile_section_achievements").performClick()
        compose.onNodeWithTag("achievement_wins").assertIsDisplayed()
        save("achievement-top.png")
        compose.onNodeWithTag("achievement_categories").performScrollToIndex(5)
        compose.onNodeWithTag("achievement_collector").assertIsDisplayed().performClick()
        compose.onNodeWithTag("achievement_details").performScrollToIndex(7)
        compose.onNodeWithText("73 itens diferentes").assertIsDisplayed()
        save("achievement-last-tier.png")
        compose.onNodeWithTag("achievements_back").performClick()
        compose.onNodeWithTag("achievement_categories").assertIsDisplayed()
        compose.onNodeWithTag("achievement_details").assertDoesNotExist()
        compose.onNodeWithTag("achievement_categories").performScrollToIndex(0)
        compose.onNodeWithTag("achievement_wins").performClick()
        compose.onNodeWithTag("achievement_details").performScrollToIndex(7)
        compose.onNodeWithText("5.000 vitórias").assertIsDisplayed()
        save("achievement-victories-tiers.png")
        compose.onNodeWithTag("achievements_back").performClick()
        compose.onNodeWithTag("achievements_back").performClick()
        compose.onNodeWithTag("achievements_screen").assertDoesNotExist()
        compose.onNodeWithTag("profile_section_achievements").assertIsDisplayed()
    }
    @Test fun sevenTierSilhouettesRenderInCompactGallery() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().background(Color(0xFF121318)).padding(16.dp).testTag("achievements_screen")) {
                    androidx.compose.material3.Text("ESCUDOS · SETE NÍVEIS",color=Color.White)
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns=androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        modifier=Modifier.weight(1f).testTag("badge_gallery"),
                        contentPadding=PaddingValues(bottom=32.dp),
                        verticalArrangement=Arrangement.spacedBy(12.dp)
                    ) {
                        items(7) { level ->
                            val tier=com.blockwar.achievements.AchievementCatalog.tiers[level]
                            Column(Modifier.padding(12.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                                com.blockwar.ui.components.AchievementShield("wins",level,true,Modifier.size(96.dp),allowAnimation=false)
                                androidx.compose.material3.Text(tier.name,color=Color(tier.color))
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Bronze").assertIsDisplayed()
        save("escudos-sete-niveis.png")
        compose.onNodeWithTag("badge_gallery").performScrollToIndex(6)
        compose.onNodeWithText("Lendário").assertIsDisplayed()
    }

    @Test fun legendaryLightAnimatesWithoutChangingBadgeSize() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assume.assumeTrue(android.provider.Settings.Global.getFloat(context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f)
        compose.mainClock.autoAdvance=false
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF121318))) {
                com.blockwar.ui.components.AchievementShield("wins",6,true,Modifier.size(120.dp).testTag("animated_badge"))
            }
        }
        compose.mainClock.advanceTimeBy(32)
        val before=compose.onNodeWithTag("animated_badge").captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1500)
        val after=compose.onNodeWithTag("animated_badge").captureToImage().asAndroidBitmap()
        org.junit.Assert.assertEquals(before.width,after.width)
        org.junit.Assert.assertEquals(before.height,after.height)
        org.junit.Assert.assertFalse("O brilho precisa mudar com o tempo",before.sameAs(after))
    }

    private fun save(name:String) {
        val bitmap=compose.onNodeWithTag("achievements_screen").captureToImage().asAndroidBitmap()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null),name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
}
