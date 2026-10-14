package com.blockwar.audio

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AudioPlaybackLifecycleTest {
    @Test fun cancellationAlwaysStopsAndReleasesTheTrack() = runBlocking {
        val log = mutableListOf<String>()
        val started = CompletableDeferred<Unit>()
        val job = launch {
            AudioPlaybackLifecycle().play({ "track" }, { log.add("stop") }, { log.add("release") }) {
                started.complete(Unit)
                awaitCancellation()
            }
        }
        started.await()
        job.cancelAndJoin()
        assertEquals(listOf("stop", "release"), log)
    }

    @Test fun playbackFailureStillReleasesTheTrack() = runBlocking {
        val log = mutableListOf<String>()
        try {
            useAudioResource({ "track" }, { log.add("stop") }, { log.add("release") }) {
                throw IllegalStateException("playback failed")
            }
            fail("Playback error must propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("playback failed", expected.message)
        }
        assertEquals(listOf("stop", "release"), log)
    }

    @Test fun stopFailureCannotSkipRelease() = runBlocking {
        var released = 0
        useAudioResource({ "track" }, { throw IllegalStateException("stop failed") }, { released++ }) { }
        assertEquals(1, released)
    }

    @Test fun replacementStartsOnlyAfterThePreviousTrackIsReleased() = runBlocking {
        val log = mutableListOf<String>()
        val lifecycle = AudioPlaybackLifecycle()
        val started = CompletableDeferred<Unit>()
        val first = launch {
            lifecycle.play({ log.add("create A"); "A" }, { log.add("stop A") }, { log.add("release A") }) {
                started.complete(Unit)
                awaitCancellation()
            }
        }
        started.await()
        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            lifecycle.play({ log.add("create B"); "B" }, { log.add("stop B") }, { log.add("release B") }) { }
        }
        assertEquals(listOf("create A"), log)
        first.cancelAndJoin()
        second.join()
        assertEquals(listOf("create A", "stop A", "release A", "create B", "stop B", "release B"), log)
    }

    @Test fun cancelledReplacementDoesNotAllocateAnAudioTrack() = runBlocking {
        val lifecycle = AudioPlaybackLifecycle()
        val started = CompletableDeferred<Unit>()
        val first = launch { lifecycle.play({ "A" }, {}, {}) { started.complete(Unit); awaitCancellation() } }
        started.await()
        var allocated = false
        val next = launch(start = CoroutineStart.UNDISPATCHED) {
            lifecycle.play({ allocated = true; "B" }, {}, {}) { }
        }
        next.cancelAndJoin()
        first.cancelAndJoin()
        assertFalse(allocated)
    }

    @Test fun failedReleaseDoesNotBlockFutureMusic() = runBlocking {
        val lifecycle = AudioPlaybackLifecycle()
        lifecycle.play({ "A" }, {}, { throw IllegalStateException("release failed") }) { }
        var played = false
        lifecycle.play({ "B" }, {}, {}) { played = true }
        assertTrue(played)
    }
}
