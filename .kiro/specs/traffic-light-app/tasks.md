# Implementation Tasks: Traffic Light App

Tasks are ordered from scaffolding foundation to full feature completion.
Each task references the FRD requirements it satisfies.

---

## Task 1: Project Setup
*Scaffolding — no FRD requirement, prerequisite for all tasks*

Set up the Android project skeleton with Gradle (Kotlin DSL), Material Design 3, and the package structure defined in coding-guidelines.md.

### Acceptance Criteria
- [ ] Android project created targeting API 33+
- [ ] Kotlin DSL Gradle build configured
- [ ] Material Design 3 dependency added
- [ ] Package structure: `com.trafficlight/{ui/menu, ui/trafficlight, ui/settings, controller, data, model}`
- [ ] ktlint configured and passing on an empty project
- [ ] App builds and launches without errors

---

## Task 2: Data Models and Preferences
*Satisfies: FR-4.1, FR-4.2, FR-4.3, FR-7.1, FR-7.2, FR-7.3, FR-7.4*

Implement core data types and local persistence layer.

### Acceptance Criteria
- [x] `LightState` enum: RED, YELLOW, GREEN
- [x] `AnimationType` enum: FADE_IN, FADE_OUT, NONE
- [x] `TimingPreferences` data class with defaults: red=10s, green=20s, yellow=3s
- [x] `validateAndClamp()` clamps red/green to 3–60s, yellow to 1–10s
- [x] `PreferencesRepository` reads/writes all three durations to SharedPreferences
- [x] `resetToDefaults()` clears saved values and restores defaults
- [x] No network permission in manifest; no cloud sync code (FR-7.4)
- [x] Unit tests: clamping idempotency, boundary values (min/max/out-of-range), default fallback when no prefs saved

---

## Task 3: Navigation Skeleton
*Satisfies: FR-1.1, FR-1.3, FR-1.4, FR-6.7*

Wire up single-Activity Compose navigation between all three screens with placeholder composables.

### Acceptance Criteria
- [ ] Single `MainActivity` with `NavHost` containing three routes: `menu`, `trafficlight`, `settings`
- [ ] "Start" on MenuScreen navigates to `trafficlight` route (FR-1.3)
- [ ] "Options" on MenuScreen navigates to `settings` route (FR-1.4)
- [ ] Back from SettingsScreen returns to MenuScreen (FR-6.7)
- [ ] Back from TrafficLightScreen returns to MenuScreen (FR-8.3)
- [ ] No duplicate screen instances on rapid taps
- [ ] App launches and shows MenuScreen as the first screen (FR-1.1)

---

## Task 4: Menu Screen
*Satisfies: FR-1.1, FR-1.2, FR-1.3, FR-1.4*

Implement the `MenuScreen` composable with branding and navigation buttons.

### Acceptance Criteria
- [ ] App title/branding displayed above the buttons (FR-1.2)
- [ ] Two Material 3 filled buttons vertically centered: "Start" and "Options" (FR-1.2)
- [ ] "Start" triggers navigation to TrafficLightScreen (FR-1.3)
- [ ] "Options" triggers navigation to SettingsScreen (FR-1.4)
- [ ] Dark theme applied per ui-guidelines.md

---

## Task 5: Settings Screen
*Satisfies: FR-6.1, FR-6.2, FR-6.3, FR-6.4, FR-6.5, FR-6.6, FR-6.7*

Implement the `SettingsScreen` composable with per-phase duration sliders.

### Acceptance Criteria
- [ ] Material 3 slider for RED duration, range 3–60s (FR-6.1)
- [ ] Material 3 slider for GREEN duration, range 3–60s (FR-6.2)
- [ ] Material 3 slider for YELLOW duration, range 1–10s (FR-6.3)
- [ ] Current numeric value displayed beside each slider (FR-6.4)
- [ ] Each slider change immediately persisted via `PreferencesRepository` (FR-6.5)
- [ ] "Reset to Defaults" button restores all three defaults (FR-6.6)
- [ ] Material 3 top app bar with back arrow; back returns to MenuScreen (FR-6.7)

---

## Task 6: Traffic Light Controller and Timer Engine
*Satisfies: FR-3.1, FR-3.2, FR-3.3, FR-3.4, FR-3.5, FR-3.6, FR-4.4, FR-8.1, FR-8.4*

Implement the state machine and timing logic that drives the light cycle.

### Acceptance Criteria
- [ ] `startCycle()` initialises state to RED (FR-3.1)
- [ ] Cycle advances RED → GREEN → YELLOW → RED continuously (FR-3.2)
- [ ] No state is skipped; no reverse transitions possible (FR-3.3, FR-3.4)
- [ ] At all times exactly one state is active (FR-3.5)
- [ ] Cycle runs indefinitely until explicitly stopped (FR-3.6)
- [ ] Each phase runs for its configured duration ±100ms (FR-4.4)
- [ ] `stopCycle()` cancels all timers immediately; no further callbacks fire after stop (FR-8.1)
- [ ] Timer implemented with `Handler.postDelayed()` — no `Thread.sleep()`
- [ ] On resume after background kill, cycle restarts from RED (FR-8.4)
- [ ] Unit tests: state transitions deterministic, clean shutdown, timing tolerance
- [ ] Property tests: transitions always follow the correct sequence

---

## Task 7: Traffic Light Rendering
*Satisfies: FR-2.4, FR-2.5, FR-2.6*

Implement the custom Canvas composable that draws the housing and three light circles.

### Acceptance Criteria
- [ ] Goldenrod rounded rectangle housing centered horizontally (FR-2.6)
- [ ] Three circles stacked vertically: red (top), yellow (middle), green (bottom) (FR-2.4)
- [ ] Circles evenly spaced within the housing
- [ ] Active light rendered at brightness 1.0; inactive lights at 0.15 (FR-2.5)
- [ ] Color rendered as `blend(#2A2A2A, activeColor, brightness)` per ui-guidelines.md
- [ ] Each light circle has a visor (cowl/hood) rendered above it — a curved shade shape drawn programmatically using Canvas
- [ ] When a light is active, the underside of its visor shows a reflected glow blended from the active light's color at reduced opacity (simulating light bouncing off the shade)
- [ ] When a light is inactive, the visor underside shows no glow
- [ ] No bitmaps — all drawing programmatic using Canvas
- [ ] Housing and circles scale proportionally across different screen sizes

---

## Task 8: Fade Transition Animations
*Satisfies: FR-5.1, FR-5.2, FR-5.3, FR-5.4*

Implement incandescent-style brightness animations using `ValueAnimator`.

### Acceptance Criteria
- [ ] Fade-out uses decelerate curve: `1.0 - (1.0 - progress)²` (FR-5.1)
- [ ] Fade-in uses accelerate curve: `progress²` (FR-5.2)
- [ ] Total transition duration ~300ms (FR-5.3)
- [ ] No two lights simultaneously at full brightness (1.0) during any transition (FR-5.4)
- [ ] Brightness values stay within [0.15, 1.0] throughout animation
- [ ] 60fps target via `ValueAnimator` tied to choreographer
- [ ] Property test: brightness always in [DIM, FULL] for any progress value in [0.0, 1.0]

---

## Task 9: Traffic Light Screen — Full Integration
*Satisfies: FR-2.1, FR-2.2, FR-2.3, FR-8.1, FR-8.2, FR-8.3, FR-8.4, FR-8.5, UC-1, UC-3*

Assemble `TrafficLightScreen` integrating rendering, animation, controller, and lifecycle management.

### Acceptance Criteria
- [ ] Immersive sticky mode entered on screen open — status bar and nav bar hidden (FR-2.1)
- [ ] Portrait orientation locked while screen is active (FR-2.2)
- [ ] `FLAG_KEEP_SCREEN_ON` set on entry; cleared on exit (FR-2.3, UC-3 step 4)
- [ ] Timing preferences loaded from `PreferencesRepository` on screen entry
- [ ] Controller initialised and cycle started automatically on entry
- [ ] Back button: `stopCycle()` called → immersive mode exited → screen-keep-on released → navigate to MenuScreen (FR-8.1, FR-8.2, FR-8.3)
- [ ] `onResume`: if timer was killed while backgrounded, restart cycle from RED (FR-8.4)
- [ ] `onDestroy`: all timers and animation callbacks cancelled; no memory leaks (FR-8.5)
- [ ] App does not crash when returning from background (FR-8.5)

---

## Task 10: Integration and Property-Based Tests
*Validates: all FR sections*

Write the full test suite covering unit, property-based, and integration tests.

### Acceptance Criteria
- [ ] 100% branch coverage on `getNextState()` (FR-3.2–3.4)
- [ ] Boundary and out-of-range cases covered for `validateAndClamp()` (FR-7.3)
- [ ] Property test: `validateAndClamp` is idempotent
- [ ] Property test: brightness always in [DIM, FULL] for any animation progress (FR-5)
- [ ] Integration test: full RED→GREEN→YELLOW→RED cycle with correct timing (FR-3, FR-4)
- [ ] Integration test: settings roundtrip — save values, relaunch, verify loaded (FR-7.1, FR-7.2)
- [ ] Integration test: back button exits cleanly from all three light states (FR-8.1–8.3)
- [ ] Integration test: immersive mode active on TrafficLightScreen, restored on exit (FR-2.1, FR-8.2)

---

## Task 11: Manual Mode
*Satisfies: meeting notes — 10 July 2026*

Add a manual mode that lets the user disable the automatic timer and tap each light directly to activate it. This covers a Settings toggle, persistence of the mode preference, controller behaviour changes, and tap handling on the Traffic Light Screen.

### Acceptance Criteria
- [ ] `TimingPreferences` extended with a `manualMode: Boolean` field (default: `false`)
- [ ] `PreferencesRepository` reads and writes `manualMode` to SharedPreferences
- [ ] Settings Screen includes a clearly labelled toggle (switch) for "Manual mode"; its state is persisted immediately on change
- [ ] When `manualMode` is `false`, the app behaves exactly as before (automatic cycle)
- [ ] When `manualMode` is `true`, `TrafficLightController` does not start the automatic timer cycle on entry
- [ ] In manual mode, each of the three light areas on the Traffic Light Screen is individually tappable
- [ ] Tapping a light in manual mode activates that light (full brightness) and deactivates the other two (dim), using the same fade transition animation as the automatic cycle
- [ ] Only one light can be active at a time in manual mode; tapping the already-active light has no effect
- [ ] Manual mode initial state on screen entry: RED light active (same as automatic mode)
- [ ] Back button behaviour is unchanged in manual mode: stop, exit immersive, return to Menu
- [ ] `stopCycle()` on the controller is a no-op (or safe to call) when in manual mode — no crashes
- [ ] Unit tests: controller does not fire timer callbacks when manual mode is active; tapping each light produces correct state
