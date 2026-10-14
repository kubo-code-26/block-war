package com.blockwar.network

import com.google.firebase.database.*
import com.google.firebase.database.snapshot.Node
import com.google.firebase.database.snapshot.NodeUtilities
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CreatePlayerProfileTransactionTest {
    private val initial = mapOf<String, Any>("nickname" to "alice", "pin_hash" to "creator-a",
        "pontos_bw" to 0, "coins" to 100000, "inventory" to mapOf("boughtItems" to listOf("neon")))

    // Use Firebase's actual MutableData/Node implementation without contacting the server.
    private fun data(value: Any?): MutableData {
        val constructor = MutableData::class.java.getDeclaredConstructor(Node::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(NodeUtilities.NodeFromJSON(value))
    }

    @Test fun anEmptyNicknameIsCreated() {
        val current = data(null)
        val handler = CreatePlayerProfileTransaction(initial) { _, _, _ -> fail("Callback inside transaction") }
        assertTrue(handler.doTransaction(current).isSuccess)
        assertEquals("alice", current.child("nickname").value)
        assertEquals("creator-a", current.child("pin_hash").value)
        assertEquals(100000L, current.child("coins").value)
    }

    @Test fun existingBalanceInventoryMissionsAndPinArePreserved() {
        val current = data(mapOf("nickname" to "alice", "coins" to 321L, "pontos_bw" to 1700L,
            "pin_hash" to "original", "missions" to mapOf("claimed" to true),
            "inventory" to mapOf("boughtItems" to listOf("neon", "skin_ouro_macico")),
            "appliedMatchRewards" to mapOf("match-1" to true)))
        val before = current.value
        assertTrue(CreatePlayerProfileTransaction(initial) { _, _, _ -> }.doTransaction(current).isSuccess)
        assertEquals(before, current.value)
    }

    @Test fun aRetryAfterAnotherCreatorWinsKeepsTheWinningProfile() {
        val handler = CreatePlayerProfileTransaction(initial) { _, _, _ -> }
        handler.doTransaction(data(null)) // Optimistic first invocation with an empty local cache.
        val winner = data(initial + mapOf("pin_hash" to "creator-b", "coins" to 93000L))
        val before = winner.value
        handler.doTransaction(winner) // Firebase retry with the authoritative server contents.
        assertEquals(before, winner.value)
        assertEquals("creator-b", winner.child("pin_hash").value)
    }

    @Test fun aPartialExistingRecordIsNeverReset() {
        val current = data(mapOf("coins" to 50L))
        val before = current.value
        CreatePlayerProfileTransaction(initial) { _, _, _ -> }.doTransaction(current)
        assertEquals(before, current.value)
    }

    @Test fun completionPropagatesTheActualFailure() {
        var calls = 0
        val error = DatabaseError.fromCode(DatabaseError.PERMISSION_DENIED)
        val handler = CreatePlayerProfileTransaction(initial) { actual, committed, snapshot ->
            calls++
            assertSame(error, actual)
            assertFalse(committed)
            assertNull(snapshot)
        }
        handler.onComplete(error, false, null)
        assertEquals(1, calls)
    }
}
