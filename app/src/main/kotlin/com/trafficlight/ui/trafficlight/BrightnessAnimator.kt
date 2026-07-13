package com.trafficlight.ui.trafficlight

import android.animation.ValueAnimator
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.trafficlight.model.AnimationType
import com.trafficlight.model.LightState

// ── Constants ─────────────────────────────────────────────────────────────────

/** Brightness of a fully-active light (FR-2.5). */
const val DIM_BRIGHTNESS = 0.15f

/** Brightness of a fully-inactive (dim) light (FR-2.5). */
const val FULL_BRIGHTNESS = 1.0f

/** Duration of a single fade-in or fade-out animation in milliseconds (FR-5.3). */
private const val FADE_DURATION_MS = 150L

// ── Interpolation functions ───────────────────────────────────────────────────

/**
 * Pure-Kotlin interpolation functions for incandescent light transitions.
 *
 * All functions are stateless and have no Android framework dependencies so
 * they can be exercised directly in JVM unit / property tests.
 */
object FadeInterpolator {

    /**
     * Decelerate curve — simulates a cooling filament (FR-5.1).
     *
     * Maps animation [progress] in [0.0, 1.0] to an interpolated value in the
     * same range using `1 - (1 - progress)²`.  The curve is fast at the
     * beginning and slow at the end.
     *
     * @param progress Raw linear animation progress in [0.0, 1.0].
     * @return Interpolated progress in [0.0, 1.0].
     */
    fun decelerateOut(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return 1f - (1f - p) * (1f - p)
    }

    /**
     * Accelerate curve — simulates a heating filament (FR-5.2).
     *
     * Maps animation [progress] in [0.0, 1.0] to an interpolated value using
     * `progress²`.  The curve is slow at the beginning and fast at the end.
     *
     * @param progress Raw linear animation progress in [0.0, 1.0].
     * @return Interpolated progress in [0.0, 1.0].
     */
    fun accelerateIn(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return p * p
    }

    /**
     * Returns the instantaneous brightness for a light that is **fading out**
     * (transitioning from [FULL_BRIGHTNESS] to [DIM_BRIGHTNESS]).
     *
     * @param progress Linear animation progress in [0.0, 1.0].
     * @return Brightness clamped to [[DIM_BRIGHTNESS], [FULL_BRIGHTNESS]].
     */
    fun fadeOutBrightness(progress: Float): Float {
        val interpolated = decelerateOut(progress)
        return (FULL_BRIGHTNESS + (DIM_BRIGHTNESS - FULL_BRIGHTNESS) * interpolated)
            .coerceIn(DIM_BRIGHTNESS, FULL_BRIGHTNESS)
    }

    /**
     * Returns the instantaneous brightness for a light that is **fading in**
     * (transitioning from [DIM_BRIGHTNESS] to [FULL_BRIGHTNESS]).
     *
     * @param progress Linear animation progress in [0.0, 1.0].
     * @return Brightness clamped to [[DIM_BRIGHTNESS], [FULL_BRIGHTNESS]].
     */
    fun fadeInBrightness(progress: Float): Float {
        val interpolated = accelerateIn(progress)
        return (DIM_BRIGHTNESS + (FULL_BRIGHTNESS - DIM_BRIGHTNESS) * interpolated)
            .coerceIn(DIM_BRIGHTNESS, FULL_BRIGHTNESS)
    }
}

// ── BrightnessAnimator ────────────────────────────────────────────────────────

/**
 * Drives per-light brightness animations using [ValueAnimator] tied to the
 * Android Choreographer for 60fps rendering (FR-5.3, FR-5.4).
 *
 * ### Transition strategy
 * Each light transition is split into two sequential phases (total ~300ms):
 *
 * 1. **Fade-out** (~150ms): The **outgoing** light decelerates from 1.0 → 0.15
 *    using [FadeInterpolator.decelerateOut] (FR-5.1).
 * 2. **Fade-in** (~150ms): The **incoming** light accelerates from 0.15 → 1.0
 *    using [FadeInterpolator.accelerateIn] (FR-5.2).
 *
 * This two-phase approach guarantees that the outgoing light reaches
 * [DIM_BRIGHTNESS] before the incoming light reaches [FULL_BRIGHTNESS],
 * so no two lights are ever simultaneously at full brightness (FR-5.4).
 *
 * ### Usage
 * ```kotlin
 * val animator = BrightnessAnimator()
 * // Read the current brightness map in Compose:
 * val brightnesses by animator.brightnessState
 * // Trigger a transition when the controller delivers a new state:
 * animator.transition(oldState = LightState.RED, newState = LightState.GREEN)
 * // Cancel on exit:
 * animator.cancel()
 * ```
 *
 * @param initialState The light that should start at full brightness.
 */
class BrightnessAnimator(initialState: LightState = LightState.RED) {

    /** Mutable backing store for the current per-light brightness snapshot. */
    private val _brightnessState = mutableStateOf(defaultBrightnesses(initialState))

    /**
     * Observable snapshot of per-light brightness values.
     * Read this in Compose with `val brightnesses by animator.brightnessState`.
     */
    val brightnessState: State<Map<LightState, Float>> = _brightnessState

    /** The ValueAnimator currently running (either fade-out or fade-in phase), or null. */
    private var activeAnimator: ValueAnimator? = null

    // ── State tracking ──────────────────────────────────────────────────────

    /**
     * Current per-light brightness values, mutable only within this class.
     * Stored separately from [_brightnessState] so we can batch-update the map.
     */
    private val currentBrightnesses: MutableMap<LightState, Float> =
        defaultBrightnesses(initialState).toMutableMap()

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Animates the brightness transition from [outgoing] to [incoming].
     *
     * The animation is two-phase:
     * - Phase 1 (fade-out): [outgoing] fades from 1.0 → 0.15 over [FADE_DURATION_MS].
     * - Phase 2 (fade-in): [incoming] fades from 0.15 → 1.0 over [FADE_DURATION_MS].
     *
     * Any currently-running animation is cancelled before the new one starts.
     *
     * If [animationType] is [AnimationType.NONE] the brightnesses snap to their
     * final steady-state values immediately without animation.
     *
     * @param outgoing     The light that is turning off.
     * @param incoming     The light that is turning on.
     * @param animationType The type of animation requested by the controller.
     */
    fun transition(
        outgoing: LightState,
        incoming: LightState,
        animationType: AnimationType,
    ) {
        cancel()

        if (animationType == AnimationType.NONE) {
            snapTo(incoming)
            return
        }

        // Phase 1: fade out the outgoing light, then chain phase 2.
        activeAnimator = buildFadeOutAnimator(outgoing) {
            // Phase 2: fade in the incoming light.
            activeAnimator = buildFadeInAnimator(incoming)
            activeAnimator?.start()
        }
        activeAnimator?.start()
    }

    /**
     * Snaps all lights to the steady-state brightness for [activeState]
     * without any animation.
     *
     * @param activeState The light that should be at [FULL_BRIGHTNESS]; all others go to [DIM_BRIGHTNESS].
     */
    fun snapTo(activeState: LightState) {
        cancel()
        LightState.entries.forEach { state ->
            currentBrightnesses[state] = if (state == activeState) FULL_BRIGHTNESS else DIM_BRIGHTNESS
        }
        publishSnapshot()
    }

    /**
     * Cancels any running animation immediately, leaving brightnesses at their
     * current intermediate values.  Call this in onDestroy / onPause to avoid
     * callbacks firing after the composable is gone (FR-8.5).
     */
    fun cancel() {
        activeAnimator?.cancel()
        activeAnimator = null
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /** Builds a [ValueAnimator] that fades [light] from its current brightness down to [DIM_BRIGHTNESS]. */
    private fun buildFadeOutAnimator(
        light: LightState,
        onComplete: () -> Unit,
    ): ValueAnimator =
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FADE_DURATION_MS
            // Linear animator; interpolation is applied manually via FadeInterpolator.
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { anim ->
                val rawProgress = anim.animatedValue as Float
                currentBrightnesses[light] = FadeInterpolator.fadeOutBrightness(rawProgress)
                publishSnapshot()
            }
            addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // Snap to exact dim value to avoid floating-point drift.
                        currentBrightnesses[light] = DIM_BRIGHTNESS
                        publishSnapshot()
                        onComplete()
                    }
                },
            )
        }

    /** Builds a [ValueAnimator] that fades [light] from [DIM_BRIGHTNESS] up to [FULL_BRIGHTNESS]. */
    private fun buildFadeInAnimator(light: LightState): ValueAnimator =
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FADE_DURATION_MS
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { anim ->
                val rawProgress = anim.animatedValue as Float
                currentBrightnesses[light] = FadeInterpolator.fadeInBrightness(rawProgress)
                publishSnapshot()
            }
            addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // Snap to exact full value to avoid floating-point drift.
                        currentBrightnesses[light] = FULL_BRIGHTNESS
                        publishSnapshot()
                    }
                },
            )
        }

    /** Copies [currentBrightnesses] into the observable [_brightnessState]. */
    private fun publishSnapshot() {
        _brightnessState.value = currentBrightnesses.toMap()
    }
}

// defaultBrightnesses is declared in TrafficLightComposable.kt (same package) and reused here.
