package com.blockwar.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.model.*
import com.blockwar.ui.components.GameOverDialog
import com.blockwar.ui.theme.MyApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MatchResultPresentationTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun cpuNeverOffersFriendRequestAndAllActionsRemainReachableOnSmallScreen() {
  var restarted=false;var menu=false
  compose.setContent {MyApplicationTheme {GameOverDialog(VersusGameState(matchStatus=MatchStatus.VICTORY),91,50,{restarted=true},onOpenHistory={},onMenu={menu=true})}}
  compose.onNodeWithTag("add_opponent_friend").assertDoesNotExist()
  compose.onNodeWithTag("result_rewards_confirmed").assertDoesNotExist()
  compose.onNodeWithTag("result_rewards_pending").assertExists()
  compose.onNodeWithTag("restart_button").performScrollTo().performClick()
  compose.runOnIdle{assertTrue(restarted)}
  compose.onNodeWithTag("menu_button").performScrollTo().performClick()
  compose.runOnIdle{assertTrue(menu)}
 }
 @Test fun confirmedReceiptShowsActualServerAmountsEvenOnDefeat() {
  val state=VersusGameState(matchStatus=MatchStatus.DEFEAT,matchRewardSummary=MatchRewardSummary(0,20,0))
  compose.setContent {MyApplicationTheme {GameOverDialog(state,20,0,{},onOpenHistory={},onMenu={})}}
  compose.onNodeWithTag("result_rewards_confirmed").assertExists()
  compose.onNodeWithTag("result_rewards_pending").assertDoesNotExist()
  compose.onNodeWithText("+20").assertExists()
  compose.onNodeWithText("TENTAR NOVAMENTE").performScrollTo().assertIsDisplayed()
 }
}
