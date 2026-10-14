package com.blockwar.auth

import android.os.Handler
import android.os.Looper

/** Delivers a result at most once, including when an SDK replies after timeout. */
internal class ResultDeadline<T>(
    timeoutMillis: Long,
    private val onSuccess: (T) -> Unit,
    private val onError: (String) -> Unit,
    timeoutMessage: String,
    private val handler: Handler = Handler(Looper.getMainLooper())
) {
    private var finished = false
    private val timeout = Runnable { fail(timeoutMessage) }
    init { handler.postDelayed(timeout, timeoutMillis) }
    val active: Boolean get() = !finished
    fun succeed(value: T) { if (finish()) onSuccess(value) }
    fun fail(message: String) { if (finish()) onError(message) }
    private fun finish(): Boolean {
        if (finished) return false
        finished = true
        handler.removeCallbacks(timeout)
        return true
    }
}
