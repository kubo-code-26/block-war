package com.blockwar.auth

import com.blockwar.network.FirebaseRealtimeManager
import com.google.firebase.database.MutableData
import com.google.firebase.database.snapshot.Node
import com.google.firebase.database.snapshot.NodeUtilities
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PlayGamesProfileTest {
    private val identity = PlayGamesIdentity("verified-firebase-uid", "verified-player-id")
    private fun data(value: Any?): MutableData {
        val ctor = MutableData::class.java.getDeclaredConstructor(Node::class.java)
        ctor.isAccessible = true
        return ctor.newInstance(NodeUtilities.NodeFromJSON(value))
    }
    private fun legacyProfile(): Map<String, Any> = mapOf(
        "nickname" to "Kender", "pin_hash" to FirebaseRealtimeManager.hashPin("1234", "salt"),
        "pin_salt" to "salt", "pontos_bw" to 579, "coins" to 1420,
        "inventory" to mapOf("purchasedItems" to listOf("avatar_robot")),
        "onlineStats" to mapOf("totalMatches" to 20),
        "appliedMatchRewards" to mapOf("match1" to mapOf("bw" to 20)))

    @Test fun verifiedLinkPreservesProgressInventoryStatisticsAndReceipts() {
        val before = legacyProfile()
        val profile = data(before)
        assertTrue(attachPlayGamesProfile(profile, identity, "1234").isSuccess)
        before.forEach { (key, _) -> assertEquals(data(before).child(key).value, profile.child(key).value) }
        assertEquals(identity.firebaseUid, profile.child("firebaseAuthUid").value)
        assertEquals(identity.playerId, profile.child("google_play_id").value)
    }
    @Test fun wrongPinLeavesAllProfileDataUnchanged() {
        val profile = data(legacyProfile()); val before = profile.value
        assertFalse(attachPlayGamesProfile(profile, identity, "0000").isSuccess)
        assertEquals(before, profile.value)
    }
    @Test fun anotherGoogleAccountCannotTakeAnAlreadyLinkedProfileEvenWithThePin() {
        val profile = data(legacyProfile() + ("firebaseAuthUid" to "another-uid"))
        val before = profile.value
        assertFalse(attachPlayGamesProfile(profile, identity, "1234").isSuccess)
        assertEquals(before, profile.value)
    }
    @Test fun sameVerifiedAccountCanRetryAfterRestartWithoutDuplicatingAnything() {
        val profile = data(legacyProfile())
        attachPlayGamesProfile(profile, identity, "1234")
        val first = profile.value
        assertTrue(attachPlayGamesProfile(profile, identity, "").isSuccess)
        assertEquals(first, profile.value)
    }
    @Test fun oneGoogleAccountCannotReserveTwoNicknames() {
        val account = data(null)
        assertTrue(reservePlayGamesAccount(account, "Kender").isSuccess)
        val before = account.value
        assertFalse(reservePlayGamesAccount(account, "Other").isSuccess)
        assertEquals(before, account.value)
        assertTrue(reservePlayGamesAccount(account, "Kender").isSuccess)
        assertEquals(before, account.value)
    }
    @Test fun missingProfileIsNeverRecreatedByTheLinkTransaction() {
        val profile = data(null)
        attachPlayGamesProfile(profile, identity, "1234")
        assertNull(profile.value)
    }
    @Test fun legacyPlainPinWorksButAnUnprotectedOrBlankIdentityCannotBeClaimed() {
        assertTrue(PlayGamesProfilePolicy.canLink("", identity.firebaseUid, "1234", "", "1234", ""))
        assertFalse(PlayGamesProfilePolicy.canLink("", identity.firebaseUid, "1234", "", "", ""))
        assertFalse(PlayGamesProfilePolicy.canLink("", "", "1234", "", "1234", ""))
        assertFalse(PlayGamesProfilePolicy.canReserve("", ""))
    }
}
