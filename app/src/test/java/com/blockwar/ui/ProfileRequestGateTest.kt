package com.blockwar.ui

import org.junit.Assert.*
import org.junit.Test

class ProfileRequestGateTest {
    @Test fun oldProfileResponsesCannotReplaceTheNewSelection() {
        val gate = ProfileRequestGate()
        val first = gate.select("alice")
        val second = gate.select("bob")
        assertFalse(gate.complete(first, "alice"))
        assertTrue(gate.complete(second, "alice"))
        assertFalse(gate.complete(first, "bob"))
    }
    @Test fun backgroundRefreshCannotOverrideAnExplicitSelection() {
        val gate = ProfileRequestGate()
        val old = gate.refresh("alice", "alice")!!
        val selected = gate.select("bob")
        assertNull(gate.refresh("alice", "alice"))
        assertFalse(gate.complete(old, "alice"))
        assertTrue(gate.complete(selected, "alice"))
    }
    @Test fun newerRefreshInvalidatesTheOlderResponse() {
        val gate = ProfileRequestGate()
        val old = gate.refresh("alice", "alice")!!
        val newer = gate.refresh("alice", "alice")!!
        assertFalse(gate.complete(old, "alice"))
        assertTrue(gate.complete(newer, "alice"))
    }
    @Test fun refreshCannotSwitchAccountsAndNormalizesTheNickname() {
        val gate = ProfileRequestGate()
        assertNull(gate.refresh("alice", "bob"))
        val ticket = gate.refresh(" ALICE ", "Alice")!!
        assertFalse(gate.complete(ticket, "bob"))
    }
    @Test fun terminalErrorsReleaseTheSelectionAndDuplicateCallbacksAreIgnored() {
        val gate = ProfileRequestGate()
        val ticket = gate.select("bob")
        assertTrue(gate.complete(ticket, "alice")) // Same guard used by the current request's error.
        assertFalse(gate.complete(ticket, "alice"))
        assertNotNull(gate.refresh("alice", "alice"))
    }
    @Test fun cancellingOrDestroyingTheViewModelInvalidatesPendingCallbacks() {
        val gate = ProfileRequestGate()
        val ticket = gate.select("alice")
        gate.invalidate()
        assertFalse(gate.complete(ticket, "alice"))
        val newTicket = gate.select("alice")
        assertTrue(gate.complete(newTicket, ""))
    }
}
