package com.blockwar.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.blockwar.model.MatchRecord
import com.blockwar.settings.ControlSettingsManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class MatchHistoryProfileTest {
    private lateinit var context: Context
    private lateinit var settings: ControlSettingsManager
    private lateinit var history: MatchHistoryManager
    @Before fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("block_war_match_history", 0).edit().clear().commit()
        context.getSharedPreferences("controls", 0).edit().clear().commit()
        settings = ControlSettingsManager(context)
        history = MatchHistoryManager(context)
    }
    private fun record(id: String, won: Boolean = true, time: Long = 1) = MatchRecord(
        id=id, timestamp=time, gameMode="VS CPU", isVictory=won,
        linesCleared=4, attacksSent=16, durationSeconds=30, score=16, maxCombo=2)

    @Test fun eachProfileHasItsOwnMatchesAndStatistics() {
        settings.savePlayerName("alice"); history.saveMatch(record("a"))
        settings.savePlayerName("bob")
        assertTrue(history.getHistory().isEmpty())
        assertEquals(0, history.getAggregatedStats().totalMatches)
        history.saveMatch(record("b",false))
        assertEquals(1,history.getAggregatedStats().totalLosses)
        settings.savePlayerName(" ALICE ")
        assertEquals(listOf("a"),history.getHistory().map {it.id})
        assertEquals(1,history.getAggregatedStats().totalWins)
    }
    @Test fun guestAndAnAccountNamedGuestAreDifferent() {
        settings.savePlayerName(""); history.saveMatch(record("anonymous"))
        settings.savePlayerName("guest"); assertTrue(history.getHistory().isEmpty())
        history.saveMatch(record("named"))
        settings.savePlayerName("")
        assertEquals("anonymous",history.getHistory().single().id)
    }
    @Test fun clearingOneProfileDoesNotClearAnother() {
        settings.savePlayerName("alice"); history.saveMatch(record("a"))
        settings.savePlayerName("bob"); history.saveMatch(record("b")); history.clearHistory()
        assertTrue(history.getHistory().isEmpty())
        settings.savePlayerName("alice"); assertEquals("a",history.getHistory().single().id)
    }
    @Test fun legacyHistoryIsPreservedWithoutAssigningItToAnyProfile() {
        val raw="""[{"id":"old","timestamp":1,"isVictory":true,"score":99}]"""
        context.getSharedPreferences("block_war_match_history",0).edit().putString("saved_matches_list",raw).commit()
        settings.savePlayerName("alice")
        assertTrue(history.getHistory().isEmpty())
        assertEquals(0,history.getAggregatedStats().totalMatches)
        history.saveMatch(record("a")); history.clearHistory()
        assertEquals("old",history.getLegacyHistory().single().id)
        assertEquals(raw,context.getSharedPreferences("block_war_match_history",0).getString("saved_matches_list",null))
    }
    @Test fun duplicateResultsDoNotInflateStatistics() {
        settings.savePlayerName("alice")
        history.saveMatch(record("same")); history.saveMatch(record("same"))
        assertEquals(1,history.getAggregatedStats().totalMatches)
        assertEquals(16,history.getAggregatedStats().totalScore)
    }
    @Test fun fiftyMatchLimitAndRestartAreIndependentForEachProfile() {
        settings.savePlayerName("alice")
        repeat(55) { history.saveMatch(record("a$it",time=it.toLong())) }
        assertEquals(50,history.getHistory().size)
        assertEquals("a54",history.getHistory().first().id)
        settings.savePlayerName("bob");history.saveMatch(record("b"))
        history=MatchHistoryManager(context)
        assertEquals("b",history.getHistory().single().id)
        settings.savePlayerName("alice");assertEquals(50,history.getHistory().size)
    }
}
