package com.blockwar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blockwar.ui.components.TutorialScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TutorialScreenTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun skipExitsWithoutReportingCompletion() {
        var result:Boolean?=null
        compose.setContent { MaterialTheme {TutorialScreen {result=it}} }
        compose.onNodeWithTag("tutorial_skip").performClick()
        compose.runOnIdle {assertEquals(false,result)}
    }
    private fun press(tag:String,times:Int=1) {
        repeat(times) {compose.onNodeWithTag(tag).performScrollTo().performClick()}
    }
    @Test fun allThreeLessonsCanBeCompletedUsingVisibleControls() {
        var result:Boolean?=null
        compose.setContent {MaterialTheme {TutorialScreen {result=it}}}
        press("tutorial_start")
        press("tutorial_right",3)
        press("tutorial_drop")
        compose.onNodeWithTag("tutorial_success").assertExists()
        press("tutorial_next")
        compose.onNodeWithText("PASSO 2 DE 3").assertIsDisplayed()
        press("tutorial_right",2)
        press("tutorial_rotate")
        press("tutorial_drop")
        press("tutorial_next")
        compose.onNodeWithText("PASSO 3 DE 3").assertIsDisplayed()
        press("tutorial_right",3)
        press("tutorial_drop")
        press("tutorial_next")
        compose.runOnIdle {assertEquals(true,result)}
    }
    @Test fun boardGesturesMoveRotateAndDropTheTrainingPiece() {
        compose.setContent {MaterialTheme {TutorialScreen {}}}
        press("tutorial_start")
        press("tutorial_right",3)
        press("tutorial_drop")
        press("tutorial_next")
        repeat(2) {compose.onNodeWithTag("tutorial_board").performScrollTo().performTouchInput {swipeRight(durationMillis=150)}}
        compose.onNodeWithTag("tutorial_board").performTouchInput {click()}
        compose.onNodeWithTag("tutorial_board").performTouchInput {swipeDown(durationMillis=150)}
        compose.onNodeWithTag("tutorial_success").assertExists()
    }
}
