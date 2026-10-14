package com.blockwar.tutorial

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class TutorialPreferencesTest {
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    @Before fun clear() {context.getSharedPreferences("tutorial_onboarding_v1",Context.MODE_PRIVATE).edit().clear().commit()}
    @Test fun skipIsRememberedWhenOpeningAgain() {
        assertFalse(TutorialPreferences(context).seen)
        TutorialPreferences(context).finish(false)
        assertTrue(TutorialPreferences(context).seen)
        assertFalse(context.getSharedPreferences("tutorial_onboarding_v1",Context.MODE_PRIVATE).getBoolean("completed",false))
    }
    @Test fun leavingReplayCannotErasePreviousCompletion() {
        TutorialPreferences(context).finish(true)
        TutorialPreferences(context).finish(false)
        assertTrue(context.getSharedPreferences("tutorial_onboarding_v1",Context.MODE_PRIVATE).getBoolean("completed",false))
    }
}
