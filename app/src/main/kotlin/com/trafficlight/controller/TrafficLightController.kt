package com.trafficlight.controller

import com.trafficlight.data.TimingPreferences
import com.trafficlight.model.AnimationType
import com.trafficlight.model.LightState

/**
 * Drives the traffic-light state machine and delegates timed callbacks to a [Scheduler].
 *
 * ### Cycle semantics (FR-3.1 – FR-3.6, FR-4.4)
 * - [startCycle] always initialises state to RED and begins an infinite loop.
 * - Each phase runs for its configured duration (taken from [TimingPreferences]), then
 *   transitions to the next state via [LightState.next].
 * - Only one state is active at any moment; transitions are strictly
 *   RED → GREEN → YELLOW → RED (no skipping, no reverse).
 * - The cycle runs until [stopCycle] is called.
 *
 * ### Shutdown semantics (FR-8.1)
 * - [stopCycle] cancels the pending scheduled callback immediately.
 * - Once stopped, no further [onStateChanged] callbacks will fire.
 *
 * ### Background-kill / resume (FR-8.4)
 * - After a process kill, a new [TrafficLightController] instance is created by the screen
 *   and [startCycle] is called again, restarting from RED.
 *
 * ### Timer implementation
 * - All timing goes through the injected [Scheduler], which wraps [android.os.Handler.postDelayed]
 *   in production and a fake synchronous scheduler in unit tests.
 *   No [Thread.sleep] is used anywhere in this class.
 *
 * @param preferences  Timing configuration for each phase.
 * @param scheduler    Timing back-end; defaults to [HandlerScheduler] for production.
 * @param onStateChanged  Callback invoked on every state transition, including the initial RED.
 */
class TrafficLightController(
    private val preferences: TimingPreferences,
    private val scheduler: Scheduler = HandlerScheduler(),
    private val onStateChanged: (state: LightState, animation: AnimationType) -> Unit,
) {
    /**
     * The currently active light state. Exposed for observation; mutated only from the
     * scheduled callback so it is always consistent with the last [onStateChanged] delivery.
     */
    var currentState: LightState = LightState.RED
        private set

    /** True while the cycle is running; set to false atomically in [stopCycle]. */
    private var running: Boolean = false

    /** Token returned by the last [Scheduler.schedule] call; used to cancel on [stopCycle]. */
    private var pendingToken: Any? = null

    /**
     * Starts the continuous light cycle from RED (FR-3.1).
     *
     * If a cycle is already running it is stopped first so callers do not need to call
     * [stopCycle] explicitly before calling [startCycle] again (e.g., after a resume).
     */
    fun startCycle() {
        stopCycle()
        running = true
        currentState = LightState.RED
        onStateChanged(currentState, AnimationType.NONE)
        scheduleNext()
    }

    /**
     * Stops the cycle immediately (FR-8.1).
     * Cancels any pending callback; no further [onStateChanged] calls will be made after
     * this method returns.
     */
    fun stopCycle() {
        running = false
        pendingToken?.let { scheduler.cancel(it) }
        pendingToken = null
    }

    // ── private helpers ─────────────────────────────────────────────────────

    /** Returns the configured duration in milliseconds for [state]. */
    private fun durationMsFor(state: LightState): Long = when (state) {
        LightState.RED -> preferences.redDurationSeconds * 1_000L
        LightState.GREEN -> preferences.greenDurationSeconds * 1_000L
        LightState.YELLOW -> preferences.yellowDurationSeconds * 1_000L
    }

    /**
     * Schedules the transition that will fire after the current state's duration elapses.
     * The token is saved so it can be cancelled by [stopCycle].
     */
    private fun scheduleNext() {
        if (!running) return
        val delayMs = durationMsFor(currentState)
        pendingToken = scheduler.schedule(delayMs) { onPhaseComplete() }
    }

    /**
     * Invoked by the scheduler when the current phase duration has elapsed.
     * Advances the state machine and notifies the listener, then schedules the next phase.
     *
     * The [running] guard ensures this is a no-op if [stopCycle] was called between the
     * time the callback was scheduled and the time it fires.
     */
    private fun onPhaseComplete() {
        if (!running) return
        currentState = currentState.next()
        onStateChanged(currentState, AnimationType.FADE_IN)
        scheduleNext()
    }
}
