package com.blockwar.social
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class GameInvitationTest {
    @Test fun linksRoundTripAndRejectForeignOrMalformedInput() {
        assertEquals("ABCD",GameInvitation.parse(Uri.parse(GameInvitation.link("abcd"))))
        assertEquals("ABCD",GameInvitation.parse(Uri.parse("blockwar://join?sala=ABCD")))
        assertNull(GameInvitation.parse(Uri.parse("https://evil.example/?sala=ABCD")))
        assertNull(GameInvitation.parse(Uri.parse("https://${GameInvitation.HOST}/?sala=../x")))
        assertNull(GameInvitation.parse(null))
    }
}
