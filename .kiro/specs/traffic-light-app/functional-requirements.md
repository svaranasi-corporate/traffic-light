# Functional Requirements Document: Traffic Light App

## 1. Introduction

### 1.1 Purpose
This document defines the functional requirements for a traffic light simulator Android application intended for children's play. It describes what the system shall do, its inputs and outputs, behavioral rules, and operational constraints.

### 1.2 Scope
The system is a standalone Android application distributed as an APK. It simulates a three-light traffic signal cycling continuously through red, green, and yellow phases. An adult operator configures timing; no accounts, cloud connectivity, or Play Store integration are required.

### 1.3 Definitions
| Term | Definition |
|------|-----------|
| Active light | The light currently illuminated at full brightness |
| Inactive light | A light not in the current phase, shown dimmed |
| Phase | One complete period of a single light state (e.g., the RED phase) |
| Cycle | One complete pass through all three phases: RED → GREEN → YELLOW |
| Housing | The enclosure graphic surrounding the three light circles |

---

## 2. System Overview

The application consists of three screens:

1. **Menu Screen** — entry point with navigation to the traffic light display or settings
2. **Traffic Light Screen** — full-screen display of the cycling traffic light
3. **Settings Screen** — configuration of per-phase timing durations

All state (timing preferences) is stored locally on the device. The application requires no network access, no user authentication, and retains no personal data.

---

## 3. Functional Requirements

### FR-1: Application Launch

**FR-1.1** The system shall display the Menu Screen as the initial screen upon launch.

**FR-1.2** The Menu Screen shall present exactly two action controls: "Start" and "Options".

**FR-1.3** The system shall navigate to the Traffic Light Screen when the user activates the "Start" control.

**FR-1.4** The system shall navigate to the Settings Screen when the user activates the "Options" control.

### FR-2: Traffic Light Display

**FR-2.1** The Traffic Light Screen shall occupy the full display area, hiding the system status bar and navigation bar (immersive sticky mode).

**FR-2.2** The system shall lock screen orientation to portrait while the Traffic Light Screen is active.

**FR-2.3** The system shall prevent the display from sleeping while the Traffic Light Screen is active.

**FR-2.4** The Traffic Light Screen shall render three light circles arranged vertically within a housing graphic: red at the top, yellow in the middle, green at the bottom.

**FR-2.5** The active light shall be rendered at full brightness. All other lights shall be rendered at a reduced brightness indicating an inactive state; they shall remain visible, not hidden.

**FR-2.6** The housing shall be rendered as a rounded rectangle in the goldenrod color defined in ui-guidelines.md.

### FR-3: Light Cycle Behavior

**FR-3.1** Upon entering the Traffic Light Screen, the system shall initialize the cycle in the RED state.

**FR-3.2** The system shall advance light states in the fixed sequence: RED → GREEN → YELLOW → RED, repeating indefinitely.

**FR-3.3** The system shall not skip any state in the cycle sequence.

**FR-3.4** The system shall not transition in reverse order (e.g., GREEN → RED directly is prohibited).

**FR-3.5** At all times during steady-state operation, exactly one light shall be in the active state.

**FR-3.6** The cycle shall continue without interruption until the user explicitly exits the Traffic Light Screen.

### FR-4: Phase Timing

**FR-4.1** The duration of the RED phase shall be configurable. The default value shall be 10 seconds. The valid range shall be 3–60 seconds inclusive.

**FR-4.2** The duration of the GREEN phase shall be configurable. The default value shall be 20 seconds. The valid range shall be 3–60 seconds inclusive.

**FR-4.3** The duration of the YELLOW phase shall be configurable. The default value shall be 3 seconds. The valid range shall be 1–10 seconds inclusive.

**FR-4.4** Each phase shall remain active for its configured duration before the system initiates a transition to the next phase.

### FR-5: Light Transition Behavior

**FR-5.1** When a light deactivates, the system shall animate a fade-out transition using a decelerate interpolation curve (fast start, slow end) simulating an incandescent bulb cooling.

**FR-5.2** When a light activates, the system shall animate a fade-in transition using an accelerate interpolation curve (slow start, fast end) simulating an incandescent bulb warming.

**FR-5.3** The total transition animation duration shall be approximately 300 milliseconds.

**FR-5.4** During a transition, no two lights shall simultaneously reach full brightness.

### FR-6: Settings Screen

**FR-6.1** The Settings Screen shall provide a control to adjust the RED phase duration within its valid range.

**FR-6.2** The Settings Screen shall provide a control to adjust the GREEN phase duration within its valid range.

**FR-6.3** The Settings Screen shall provide a control to adjust the YELLOW phase duration within its valid range.

**FR-6.4** The Settings Screen shall display the current numeric value of each duration alongside its control.

**FR-6.5** The system shall persist each duration change immediately when the user adjusts a control, without requiring a separate save action.

**FR-6.6** The Settings Screen shall provide a "Reset to Defaults" action that restores all durations to their default values.

**FR-6.7** The system shall navigate back to the Menu Screen when the user activates the back control from the Settings Screen.

### FR-7: Preference Persistence

**FR-7.1** The system shall persist all timing preferences to local device storage.

**FR-7.2** The system shall load saved preferences on each application launch.

**FR-7.3** If a stored preference value falls outside the valid range (e.g., due to data corruption), the system shall silently clamp it to the nearest valid boundary and continue operation. No error shall be shown to the user.

**FR-7.4** Preferences shall be stored on-device only. No cross-device synchronization or cloud storage shall be implemented.

### FR-8: Exit and Lifecycle Behavior

**FR-8.1** The system shall stop the light cycle and cancel all active timers when the user activates the back control from the Traffic Light Screen.

**FR-8.2** The system shall restore system bars (exit immersive mode) when returning from the Traffic Light Screen to the Menu Screen.

**FR-8.3** The system shall return the user to the Menu Screen after exiting the Traffic Light Screen.

**FR-8.4** If the application is sent to the background while the Traffic Light Screen is active and the system terminates the active timer, the system shall restart the cycle from the RED state when the application returns to the foreground.

**FR-8.5** The application shall not crash when returning from background under any circumstances.

---

## 4. Use Cases

### UC-1: Start Traffic Light Simulation

**Actor:** User (adult)
**Precondition:** App is launched; Menu Screen is displayed.

1. User taps "Start"
2. System loads saved timing preferences (or defaults if none saved)
3. System navigates to Traffic Light Screen
4. System enters full-screen immersive mode and locks portrait orientation
5. System activates screen-keep-on
6. System initializes cycle in RED state and starts the phase timer
7. RED light displays at full brightness; yellow and green display dimmed
8. After the RED phase duration elapses, system transitions to GREEN with fade animation
9. Cycle continues indefinitely

**Postcondition:** Traffic light is cycling. User cannot change settings without exiting.

---

### UC-2: Adjust Timing Settings

**Actor:** User (adult)
**Precondition:** Menu Screen is displayed.

1. User taps "Options"
2. System navigates to Settings Screen
3. System loads current preference values and displays them on the sliders
4. User adjusts one or more sliders
5. System saves each change immediately as the slider is adjusted
6. User taps the back arrow
7. System navigates back to Menu Screen

**Postcondition:** Updated preferences are persisted and will be used on the next "Start."

---

### UC-3: Exit Traffic Light Screen

**Actor:** User (adult)
**Precondition:** Traffic Light Screen is active; cycle is running.

1. User presses the Android back button
2. System stops the cycle and cancels all timers
3. System exits immersive mode (system bars restored)
4. System releases screen-keep-on
5. System navigates to Menu Screen

**Postcondition:** No timers, callbacks, or memory references remain from the Traffic Light Screen.

---

## 5. Constraints and Assumptions

### Constraints
- The application targets Android API level 33 (Android 13) and above
- The application shall be distributed as a standalone APK; Play Store publishing is out of scope
- The application shall request no internet permission
- No user authentication or account system shall be implemented
- Preferences are device-local only; no cloud sync

### Assumptions
- The device running the application has a portrait-capable display
- An adult is responsible for configuring settings and initiating/stopping the simulation
- The device has sufficient battery and display capability to run continuously during a play session
