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
    private fun save(name:String) {
        val bitmap=compose.onNodeWithTag("achievements_screen").captureToImage().asAndroidBitmap()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null),name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
}
