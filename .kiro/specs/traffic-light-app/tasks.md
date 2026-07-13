# Implementation Tasks: Traffic Light App

Tasks are ordered from scaffolding foundation to full feature completion.
Each task references the FRD requirements it satisfies.

---

## Task 1: Project Setup
*Scaffolding — no FRD requirement, prerequisite for all tasks*

Set up the Android project skeleton with Gradle (Kotlin DSL), Material Design 3, and the package structure defined in coding-guidelines.md.

### Acceptance Criteria
- [x] Android project created targeting API 33+
- [x] Kotlin DSL Gradle build configured
- [x] Material Design 3 dependency added
- [x] Package structure: `com.trafficlight/{ui/menu, ui/trafficlight, ui/settings, controller, data, model}`
- [x] ktlint configured and passing on an empty project
- [x] App builds and launches without errors

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
- [x] Single `MainActivity` with `NavHost` containing three routes: `menu`, `trafficlight`, `settings`
- [x] "Start" on MenuScreen navigates to `trafficlight` route (FR-1.3)
- [x] "Options" on MenuScreen navigates to `settings` route (FR-1.4)
- [x] Back from SettingsScreen returns to MenuScreen (FR-6.7)
- [x] Back from TrafficLightScreen returns to MenuScreen (FR-8.3)
- [x] No duplicate screen instances on rapid taps
- [x] App launches and shows MenuScreen as the first screen (FR-1.1)

---

## Task 4: Menu Screen
*Satisfies: FR-1.1, FR-1.2, FR-1.3, FR-1.4*

Implement the `MenuScreen` composable with branding and navigation buttons.

### Acceptance Criteria
- [x] App title/branding displayed above the buttons (FR-1.2)
- [x] Two Material 3 filled buttons vertically centered: "Start" and "Options" (FR-1.2)
- [x] "Start" triggers navigation to TrafficLightScreen (FR-1.3)
- [x] "Options" triggers navigation to SettingsScreen (FR-1.4)
- [x] Dark theme applied per ui-guidelines.md

---

## Task 5: Settings Screen
*Satisfies: FR-6.1, FR-6.2, FR-6.3, FR-6.4, FR-6.5, FR-6.6, FR-6.7*

Implement the `SettingsScreen` composable with per-phase duration sliders.

### Acceptance Criteria
- [x] Material 3 slider for RED duration, range 3–60s (FR-6.1)
- [x] Material 3 slider for GREEN duration, range 3–60s (FR-6.2)
- [x] Material 3 slider for YELLOW duration, range 1–10s (FR-6.3)
- [x] Current numeric value displayed beside each slider (FR-6.4)
- [x] Each slider change immediately persisted via `PreferencesRepository` (FR-6.5)
- [x] "Reset to Defaults" button restores all three defaults (FR-6.6)
- [x] Material 3 top app bar with back arrow; back returns to MenuScreen (FR-6.7)

---

## Task 6: Traffic Light Controller and Timer Engine
*Satisfies: FR-3.1, FR-3.2, FR-3.3, FR-3.4, FR-3.5, FR-3.6, FR-4.4, FR-8.1, FR-8.4*

Implement the state machine and timing logic that drives the light cycle.

### Acceptance Criteria
- [x] `startCycle()` initialises state to RED (FR-3.1)
- [x] Cycle advances RED → GREEN → YELLOW → RED continuously (FR-3.2)
- [x] No state is skipped; no reverse transitions possible (FR-3.3, FR-3.4)
- [x] At all times exactly one state is active (FR-3.5)
- [x] Cycle runs indefinitely until explicitly stopped (FR-3.6)
- [x] Each phase runs for its configured duration ±100ms (FR-4.4)
- [x] `stopCycle()` cancels all timers immediately; no further callbacks fire after stop (FR-8.1)
- [x] Timer implemented with `Handler.postDelayed()` — no `Thread.sleep()`
- [x] On resume after background kill, cycle restarts from RED (FR-8.4)
- [x] Unit tests: state transitions deterministic, clean shutdown, timing tolerance
- [x] Property tests: transitions always follow the correct sequence

---

## Task 7: Traffic Light Rendering
*Satisfies: FR-2.4, FR-2.5, FR-2.6*

Implement the custom Canvas composable that draws the housing and three light circles.

### Acceptance Criteria
- [x] Goldenrod rounded rectangle housing centered horizontally (FR-2.6)
- [x] Three circles stacked vertically: red (top), yellow (middle), green (bottom) (FR-2.4)
- [x] Circles evenly spaced within the housing
- [x] Active light rendered at brightness 1.0; inactive lights at 0.15 (FR-2.5)
- [x] Color rendered as `blend(#2A2A2A, activeColor, brightness)` per ui-guidelines.md
- [x] Each light circle has a visor (cowl/hood) rendered above it — a curved shade shape drawn programmatically using Canvas
- [x] When a light is active, the underside of its visor shows a reflected glow blended from the active light's color at reduced opacity (simulating light bouncing off the shade)
- [x] When a light is inactive, the visor underside shows no glow
- [x] No bitmaps — all drawing programmatic using Canvas
- [x] Housing and circles scale proportionally across different screen sizes

---

## Task 8: Fade Transition Animations
*Satisfies: FR-5.1, FR-5.2, FR-5.3, FR-5.4*

Implement incandescent-style brightness animations using `ValueAnimator`.

### Acceptance Criteria
- [x] Fade-out uses decelerate curve: `1.0 - (1.0 - progress)²` (FR-5.1)
- [x] Fade-in uses accelerate curve: `progress²` (FR-5.2)
- [x] Total transition duration ~300ms (FR-5.3)
- [x] No two lights simultaneously at full brightness (1.0) during any transition (FR-5.4)
- [x] Brightness values stay within [0.15, 1.0] throughout animation
- [x] 60fps target via `ValueAnimator` tied to choreographer
- [x] Property test: brightness always in [DIM, FULL] for any progress value in [0.0, 1.0]

---

## Task 9: Traffic Light Screen — Full Integration
*Satisfies: FR-2.1, FR-2.2, FR-2.3, FR-8.1, FR-8.2, FR-8.3, FR-8.4, FR-8.5, UC-1, UC-3*

Assemble `TrafficLightScreen` integrating rendering, animation, controller, and lifecycle management.

### Acceptance Criteria
- [x] Immersive sticky mode entered on screen open — status bar and nav bar hidden (FR-2.1)
- [x] Portrait orientation locked while screen is active (FR-2.2)
- [x] `FLAG_KEEP_SCREEN_ON` set on entry; cleared on exit (FR-2.3, UC-3 step 4)
- [x] Timing preferences loaded from `PreferencesRepository` on screen entry
- [x] Controller initialised and cycle started automatically on entry
- [x] Back button: `stopCycle()` called → immersive mode exited → screen-keep-on released → navigate to MenuScreen (FR-8.1, FR-8.2, FR-8.3)
- [x] `onResume`: if timer was killed while backgrounded, restart cycle from RED (FR-8.4)
- [x] `onDestroy`: all timers and animation callbacks cancelled; no memory leaks (FR-8.5)
- [x] App does not crash when returning from background (FR-8.5)

---

## Task 9.1: Realistic Incandescent Signal Rendering
*Satisfies: TRAFFIC-SIGNAL-VISUAL-SPEC.md — team review meeting, 13 July 2026*

Replace the current single-circle light rendering with a layered, physically accurate North American incandescent traffic signal. No logic, animation timing, or state machine changes are required — this is a rendering-only update to the Canvas drawing in `TrafficLightComposable`.

### Acceptance Criteria

**Housing**
- [x] Housing background is solid black (#111111–#1A1A1A), filling the full screen behind the signal
- [x] Housing color is traffic-signal yellow (#D9A520); matte painted appearance with subtle texture
- [x] Housing uses rounded corners of 14–18 px radius
- [x] Housing width-to-height ratio is approximately 1 : 3.05
- [x] Housing has minimal reflections (no shiny surface effect)

**Module Construction — Layered Rendering**
- [x] Each of the three signal modules is rendered as stacked layers in this order: Housing backing → Visor → Bezel → Reflector → Fresnel Lens → Incandescent Glow → Glass Highlight
- [x] No module is drawn as a single flat colored circle
- [x] Gap between adjacent modules is 6–10 px

**Visor**
- [x] Each module has a visor that extends 40–45% of the lens diameter outward from the housing
- [x] Visor has a thick curved profile with an elliptical opening at the bottom
- [x] Visor casts a visible shadow over the upper portion of its lens
- [x] Visor is rendered for all three modules regardless of active/inactive state

**Bezel**
- [x] Each module has a thick black bezel ring, 12–16 px wide, surrounding the lens
- [x] Bezel has a slight inward shadow to convey depth

**Lens**
- [x] Lens diameter is approximately 80% of housing width
- [x] Lens has a convex appearance with visible depth
- [x] Lens surface shows concentric Fresnel ring texture (not a smooth gradient)
- [x] Lens has fine prism texture with slight imperfections — no perfectly smooth gradients

**Incandescent Light Model**
- [x] Active light: center is near-white (color-tinted), transitions through the mid color, darkens to the edge color — simulating a single bulb in a mirrored reflector
- [x] Active red: center #FFEFB0, middle #FF3030, edge #A00000
- [x] Active yellow: center #FFF3B0, middle #FFBF1C, edge #A65B00
- [x] Active green: center #D7FFF0, middle #2BE060, edge #007B2D
- [x] Inactive red: #3C0D0D (dim, no glow)
- [x] Inactive yellow: #49380F (dim, no glow)
- [x] Inactive green: #14361C (dim, no glow)
- [x] No LED pixel pattern; no neon glow; no flat-fill color

**Glass Highlight**
- [x] A small, soft, low-opacity white elliptical highlight is drawn in the upper-left quadrant of each lens, even when the light is active
- [x] Highlight simulates convex glass reflection

**Shadows and Ambient Occlusion**
- [x] Shadow rendered under each visor
- [x] Shadow rendered around each bezel
- [x] Shadow rendered between adjacent modules (in the inter-module gap)
- [x] Shadow rendered inside the visor cavity

**Animation (Turn-on / Turn-off)**
- [x] Turn-on brightness ramp follows keyframes: 0% → 10% → 40% → 75% → 100%
- [x] Turn-on total duration is 120–180 ms
- [x] Turn-off brightness ramp follows keyframes: 100% → 60% → 25% → 5% → 0%
- [x] No instant on/off switching — all transitions use the incandescent ramp

**General**
- [x] All rendering is programmatic using Canvas — no bitmaps or image assets
- [x] Signal scales proportionally across different screen sizes and densities
- [x] Rendering does not introduce new dependencies beyond the existing Canvas/Compose stack
- [x] Existing state machine, controller, animation timing, and lifecycle behaviour are unchanged

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

---

## Task 12: Bugfix — Housing Glow Spill and Visor Shadow
*Satisfies: bugfix.md §2.1, §2.2*

The active bulb's glow halo and the visor's cast shadow both stop at the bezel edge. They should extend onto the surrounding goldenrod housing.

### Acceptance Criteria
- [ ] When a bulb is active (brightness > 0.5), a subtle colored tint appears on the goldenrod housing around that module's bezel, intensity proportional to brightness, fading to transparent
- [ ] The visor's downward shadow extends onto the goldenrod housing above/around the module (not clipped to the lens circle)
- [ ] Inactive lights (brightness ≤ 0.5) produce no housing tint
- [ ] Existing module layers (bezel, reflector, Fresnel, glow, highlight) render identically to current behavior
- [ ] Inter-module spacing and adjacent modules remain unaffected
- [ ] No regressions in transition animations or brightness range

---

## Task 13: HDR Enhanced Rendering
*Satisfies: FR-9.1, FR-9.2, FR-9.3, FR-9.4, FR-9.5*

Use HDR extended-range colors to render brighter bulbs on capable displays. Non-HDR devices continue rendering as-is.

### Acceptance Criteria
- [ ] Runtime HDR capability check using `Display.isHdrSdrRatioAvailable()` (API 34+)
- [ ] When HDR available: window color mode set to `COLOR_MODE_HDR`; headroom multiplier = 2.0
- [ ] When HDR unavailable: headroom multiplier = 1.0 (no-op, identical to current rendering)
- [ ] Incandescent glow gradient color values multiplied by headroom (extended sRGB, values > 1.0)
- [ ] Outer glow halo color values multiplied by headroom
- [ ] Non-HDR device renders pixel-identically to current behavior (no visual diff)
- [ ] No user-facing toggle or setting for HDR — fully automatic
- [ ] No new dependencies required beyond existing Android SDK (API 33+ min, HDR APIs in API 34)
- [ ] Unit test: headroom provider returns 1.0 when HDR unsupported, 2.0 when supported
