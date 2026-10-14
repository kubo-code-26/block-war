package com.blockwar

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
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
import com.blockwar.ui.components.AchievementsPanel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AchievementsScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun largeFontCanReachLastCategoryAndExpandAllSevenMilestones() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                MaterialTheme {
                    Column(Modifier.fillMaxSize().background(Color(0xFF121318))
                        .verticalScroll(rememberScrollState()).padding(16.dp)) {
                        AchievementsPanel(AggregatedStats(totalWins=50,totalMatches=250,totalAttacksSent=2100,
                            totalLinesCleared=600,totalCombos=30),PlayerInventory(),ShopCatalog.items)
                        Spacer(Modifier.height(64.dp))
                    }
                }
            }
        }
        compose.onNodeWithTag("achievement_wins").assertIsDisplayed()
        save("achievement-top.png")
        compose.onNodeWithTag("achievement_collector").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("COLECIONADOR").performClick()
        compose.onNodeWithText("73 itens diferentes").performScrollTo().assertIsDisplayed()
        save("achievement-last-tier.png")
        compose.onNodeWithTag("achievement_wins").performScrollTo()
        compose.onNodeWithText("VITÓRIAS").performClick()
        compose.onNodeWithText("5.000 vitórias").performScrollTo().assertIsDisplayed()
        save("achievement-victories-tiers.png")
    }
    private fun save(name:String) {
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null),name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
}
