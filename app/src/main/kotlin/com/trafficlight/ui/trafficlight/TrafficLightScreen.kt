package com.trafficlight.ui.trafficlight

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.trafficlight.controller.TrafficLightController
import com.trafficlight.data.PreferencesRepository
import com.trafficlight.model.AnimationType
import com.trafficlight.model.LightState

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Walks the [Context] wrapper chain to find the hosting [Activity], or null if not found. */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Returns the state that was active just before [incomingState] in the fixed cycle.
 * Used to derive the "outgoing" light for the brightness animator.
 *
 * The reverse mapping of RED→GREEN→YELLOW→RED is:
 *   GREEN was preceded by RED
 *   YELLOW was preceded by GREEN
 *   RED was preceded by YELLOW
 */
internal fun previousStateFor(incomingState: LightState): LightState = when (incomingState) {
    LightState.GREEN -> LightState.RED
    LightState.YELLOW -> LightState.GREEN
    LightState.RED -> LightState.YELLOW
}

// ── TrafficLightScreen ────────────────────────────────────────────────────────

/**
 * Full-screen composable for the traffic light (FR-2.1 – FR-2.3, FR-8.1 – FR-8.5, UC-1, UC-3).
 *
 * ### Lifecycle
 * - **Entry**: enters immersive sticky mode, locks portrait, sets keep-screen-on, loads
 *   timing preferences from [PreferencesRepository], and starts the controller cycle.
 * - **Resume after background**: if the controller reports it is no longer running
 *   (timer was killed while the app was backgrounded), the cycle is restarted from RED (FR-8.4).
 * - **Back button**: the [BackHandler] calls [onBack]; [DisposableEffect]'s onDispose
 *   stops the cycle, cancels animations, restores system bars, releases keep-screen-on,
 *   and unlocks orientation before navigation completes (FR-8.1, FR-8.2, FR-8.3, FR-8.5).
 * - **Disposal**: all timers and animation callbacks are cancelled (FR-8.5).
 *
 * @param onBack  Called when the user presses back; navigates to MenuScreen.
 */
@Composable
fun TrafficLightScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Load timing preferences once — they are stable for the lifetime of this screen.
    val preferences = remember {
        PreferencesRepository(context).getTimingPreferences()
    }

    // BrightnessAnimator drives the per-light brightness state observed by the composable.
    val brightnessAnimator = remember { BrightnessAnimator(initialState = LightState.RED) }
    val brightnesses by brightnessAnimator.brightnessState

    // Controller drives state transitions; its callbacks update the animator.
    val controller = remember {
        TrafficLightController(
            preferences = preferences,
            onStateChanged = { newState: LightState, animationType: AnimationType ->
                if (animationType == AnimationType.NONE) {
                    // Initial state — snap directly without animation.
                    brightnessAnimator.snapTo(newState)
                } else {
                    // Transition: derive the outgoing (previous) state and animate.
                    brightnessAnimator.transition(
                        outgoing = previousStateFor(newState),
                        incoming = newState,
                        animationType = animationType,
                    )
                }
            },
        )
    }

    // ── Immersive mode + keep-screen-on + orientation lock + cycle start ─────
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        var insetsController: WindowInsetsControllerCompat? = null

        if (activity != null) {
            // 1. Immersive sticky mode: hide status bar and navigation bar (FR-2.1)
            insetsController = WindowCompat.getInsetsController(
                activity.window,
                activity.window.decorView,
            )
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            // 2. Keep the screen on while the traffic light is running (FR-2.3, UC-3 step 4)
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // 3. Lock portrait orientation (FR-2.2)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // 4. Initialise and start the light cycle automatically on entry (FR-3.1)
        controller.startCycle()

        onDispose {
            // Stop the cycle and cancel all pending callbacks (FR-8.1, FR-8.5)
            controller.stopCycle()
            brightnessAnimator.cancel()

            if (activity != null) {
                // Restore system bars (FR-8.2)
                insetsController?.show(WindowInsetsCompat.Type.systemBars())

                // Release keep-screen-on (FR-2.3)
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                // Restore unspecified orientation so other screens are unconstrained
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // ── onResume: restart cycle if the timer was killed while backgrounded (FR-8.4) ──
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !controller.isRunning) {
                controller.startCycle()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Back button (FR-8.1, FR-8.2, FR-8.3) ──────────────────────────────
    // Cleanup (stopCycle, immersive exit, keep-screen-on release) is handled
    // by onDispose above, which fires when this composable leaves the composition.
    BackHandler(onBack = onBack)

    // ── Render the traffic light ─────────────────────────────────────────────
    TrafficLightComposable(
        activeState = controller.currentState,
        brightnesses = brightnesses,
    )
}
