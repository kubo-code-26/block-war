package com.blockwar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.ui.components.HomeShowcaseCarousel
import com.blockwar.ui.components.homeSeason
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w393dp-h850dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeShowcaseTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun manualCarouselOpensCorrectDestinations() {
        var seasons=0; var banners=0
        compose.setContent { MaterialTheme {
            HomeShowcaseCarousel("banner_bot",{seasons++},{banners++},month=YearMonth.of(2026,10))
        } }
        compose.onNodeWithTag("season_button").performClick()
        compose.runOnIdle { assertEquals(1,seasons); assertEquals(0,banners) }
        compose.onNodeWithTag("home_showcase_carousel").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("home_banner_button").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1,banners) }
        compose.mainClock.advanceTimeBy(30000)
        compose.onNodeWithTag("home_banner_button").assertIsDisplayed()
        compose.onNodeWithTag("home_showcase_dot_0").performClick()
        compose.onNodeWithTag("season_button").assertIsDisplayed()
    }
    @Test fun seasonRotationMatchesMonthlyCalendar() {
        assertEquals("hero_forja",homeSeason(YearMonth.of(2026,10)).art)
        assertEquals("real_catedral",homeSeason(YearMonth.of(2026,11)).art)
        assertEquals("real_caverna",homeSeason(YearMonth.of(2026,12)).art)
        assertEquals("hero_forja",homeSeason(YearMonth.of(2027,1)).art)
    }
}