package com.trafficlight.controller

import android.os.Handler
import android.os.Looper

/**
 * Abstraction over Android's [Handler.postDelayed] mechanism so the controller can be
 * tested on the JVM without any Android framework dependency.
 *
 * Production code uses [HandlerScheduler]; tests supply a fake implementation.
 */
interface Scheduler {
    /**
     * Schedule [action] to run after [delayMs] milliseconds.
     * Returns a token that can be passed to [cancel] to remove the pending callback.
     */
    fun schedule(delayMs: Long, action: () -> Unit): Any

    /** Cancel a previously scheduled [token]. No-op if the token is not pending. */
    fun cancel(token: Any)
}

/**
 * Production [Scheduler] that delegates to Android's main-thread [Handler].
 * Each scheduled [action] is wrapped in a [Runnable] — the runnable itself acts as the token.
 */
class HandlerScheduler(private val handler: Handler = Handler(Looper.getMainLooper())) : Scheduler {

    override fun schedule(delayMs: Long, action: () -> Unit): Any {
        val runnable = Runnable { action() }
        handler.postDelayed(runnable, delayMs)
        return runnable
    }

    override fun cancel(token: Any) {
        if (token is Runnable) {
            handler.removeCallbacks(token)
        }
    }
}
