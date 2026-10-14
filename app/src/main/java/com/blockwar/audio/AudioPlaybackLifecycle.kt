package com.blockwar.audio

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A replacement music track waits until the previous track has been released. */
internal class AudioPlaybackLifecycle {
    private val mutex = Mutex()
    suspend fun <T> play(create: () -> T, stop: (T) -> Unit, release: (T) -> Unit,
                         playback: suspend (T) -> Unit) {
        mutex.withLock { useAudioResource(create, stop, release, playback) }
    }
}

/** Independent cleanup steps: a failed stop must never skip release. */
internal suspend fun <T> useAudioResource(create: () -> T, stop: (T) -> Unit, release: (T) -> Unit,
                                         playback: suspend (T) -> Unit) {
    val resource = create()
    try {
        playback(resource)
    } finally {
        try { stop(resource) } catch (_: Exception) { }
        try { release(resource) } catch (_: Exception) { }
    }
}
