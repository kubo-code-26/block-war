package com.blockwar.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OnlineSessionStoreTest {
    private lateinit var context: Context
    private lateinit var store: OnlineSessionStore

    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("interrupted_online_session", Context.MODE_PRIVATE).edit().clear().commit()
        store = OnlineSessionStore(context)
    }

    private fun session(id: String, nickname: String, score: Int = 5) =
        InterruptedOnlineSession("ABCD", "player_$id", true, nickname, id, 1000, score, 1, 5, 0)

    @Test
    fun switchingProfilesKeepsEachPendingMatch() {
        store.save(session("a", "alice"))
        assertTrue(store.pending("bob").isEmpty())
        store.save(session("b", "bob"))
        assertEquals(listOf("a"), store.pending("alice").map { it.matchId })
        assertEquals(listOf("b"), store.pending("bob").map { it.matchId })
        store.clear("b")
        assertEquals("a", store.pending("alice").single().matchId)
        assertTrue(store.pending("bob").isEmpty())
    }

    @Test
    fun updatingOneMatchPreservesOtherProfiles() {
        store.save(session("a", "alice"))
        store.save(session("b", "bob"))
        store.save(session("a", " ALICE ", score = 16))
        assertEquals(16, store.pending("alice").single().score)
        assertEquals(5, store.pending("bob").single().score)
        val reopened = OnlineSessionStore(context)
        assertEquals(16, reopened.pending("Alice").single().score)
        assertEquals("a", reopened.load()?.matchId)
    }

    @Test
    fun aMatchCannotMoveToAnotherOwner() {
        store.save(session("a", "alice"))
        store.save(session("a", "bob", score = 100))
        assertEquals(5, store.pending("alice").single().score)
        assertTrue(store.pending("bob").isEmpty())
    }

    @Test
    fun legacyRecordIsMigratedWithoutBlockingAnotherProfile() {
        val old = JSONObject().put("room", "ABCD").put("player", "old_player").put("p1", true)
            .put("nickname", "alice").put("match", "legacy").put("started", 1000L)
            .put("score", 12).put("lines", 3).put("attacks", 12).put("combo", 2)
        val prefs = context.getSharedPreferences("interrupted_online_session", Context.MODE_PRIVATE)
        prefs.edit().putString("session", old.toString()).commit()
        assertTrue(store.pending("bob").isEmpty())
        assertFalse(prefs.contains("session"))
        assertEquals(12, store.pending("alice").single().score)
        store.save(session("b", "bob"))
        store.clear("b")
        assertEquals("legacy", OnlineSessionStore(context).pending("alice").single().matchId)
        store.clear("legacy")
        assertTrue(store.pending("alice").isEmpty())
    }

    @Test
    fun guestsAreDistinctFromNamedProfilesAndUnknownClearsDoNothing() {
        store.save(session("guest-match", ""))
        store.save(session("named-match", "guest"))
        store.clear("unknown")
        assertEquals("guest-match", store.pending("").single().matchId)
        assertEquals("named-match", store.pending("guest").single().matchId)
        store.clear("guest-match")
        assertEquals("named-match", store.pending("guest").single().matchId)
    }
}
