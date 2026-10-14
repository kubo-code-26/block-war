package com.blockwar.auth

import android.os.Looper
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class ResultDeadlineTest {
    @Test fun timeoutReleasesBusyFlowAndIgnoresLateSuccess() {
        val results = mutableListOf<String>()
        val pending = ResultDeadline<String>(1000, { results += it }, { results += it }, "timeout")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))
        pending.succeed("late")
        pending.fail("duplicate")
        assertEquals(listOf("timeout"), results)
        assertFalse(pending.active)
    }
    @Test fun successfulResultCancelsTimeoutAndDuplicates() {
        val results = mutableListOf<String>()
        val pending = ResultDeadline<String>(1000, { results += it }, { results += it }, "timeout")
        pending.succeed("ok")
        pending.succeed("duplicate")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(listOf("ok"), results)
    }
    @Test fun failureIsDeliveredOnce() {
        val results = mutableListOf<String>()
        val pending = ResultDeadline<String>(1000, { results += it }, { results += it }, "timeout")
        pending.fail("cancelled")
        pending.succeed("late")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(listOf("cancelled"), results)
    }
}
