package com.blockwar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.economy.ShopCatalog
import com.blockwar.ui.components.ShopItemCard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h850dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShopResponsiveTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun narrowCardWithLargeFontKeepsPurchaseInsideAndClickable() {
        var bought = false
        val item = ShopCatalog.find("gold")!!.copy(price=999999)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                Box(Modifier.width(168.dp)) {
                    ShopItemCard(item, false, false, { bought=true }, {}, 1f)
                }
            }
        }
        val card = compose.onNodeWithTag("shop_card_${item.id}").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText("COMPRAR").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(button.left >= card.left && button.right <= card.right)
        assertTrue(button.top >= card.top && button.bottom <= card.bottom)
        compose.onNodeWithText("COMPRAR").performClick()
        compose.runOnIdle { assertTrue(bought) }
    }
}
