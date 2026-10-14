package com.blockwar.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.ui.components.*
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
class SocialPresentationTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun bottomNavigationOpensFriendsAndNoLongerDuplicatesProfile() {
  var friends=false
  compose.setContent{MyApplicationTheme{HomeBottomNav({},{},{},{},{friends=true},pendingRequests=2)}}
  compose.onNodeWithTag("nav_perfil_button").assertDoesNotExist()
  compose.onNodeWithTag("nav_amigos_button").performClick()
  compose.runOnIdle{assertTrue(friends)}
 }
 @Test fun friendsAvatarInvitesAndOverflowActionsUseTheRealContact() {
  var profile="";var invite="";var action=""
  compose.setContent{MyApplicationTheme{FriendsPageContent(listOf(FriendContact("luna","Luna","friend","avatar_default",null)),false,"",{},{},{},
   {a,n->action="$a:$n"},{invite=it},{},{profile=it})}}
  compose.onNodeWithTag("nav_amigos_button").assertDoesNotExist()
  compose.onNodeWithTag("nav_loja_button").assertDoesNotExist()
  compose.onNodeWithTag("friend_avatar_luna").performScrollTo().performClick()
  compose.runOnIdle{assertEquals("luna",profile)}
  compose.onNodeWithText("CONVIDAR").performScrollTo().performClick()
  compose.runOnIdle{assertEquals("luna",invite)}
  compose.onNodeWithTag("friend_menu_luna").performScrollTo().performClick()
  compose.onNodeWithText("Remover amizade").performClick()
  compose.runOnIdle{assertEquals("remove:luna",action)}
 }
 @Test fun playerHistoryIsLimitedToFive() {
  val rows=(0..6).map{mapOf<String,Any?>("won" to true,"score" to it,"finishedAt" to 10000L,"duration" to 90,"opponent" to "Rival$it")}
  compose.setContent{MyApplicationTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
   PublicPlayerContent(mapOf("nickname" to "Luna","history" to rows))
  }}}
  compose.onNodeWithTag("public_history_4").performScrollTo().assertIsDisplayed()
  compose.onNodeWithTag("public_history_5").assertDoesNotExist()
 }
 @Test fun newPlayersShowEmptyHistoryWithoutInventedRows() {
  compose.setContent{MyApplicationTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
   PublicPlayerContent(mapOf("nickname" to "Novo","history" to emptyList<Any>()))
  }}}
  compose.onNodeWithTag("public_history_empty").performScrollTo().assertIsDisplayed()
  compose.onNodeWithTag("public_history_0").assertDoesNotExist()
 }

}
