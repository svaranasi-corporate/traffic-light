# Meeting Notes — 10 July 2026

## Attendees
*(not recorded)*

## Sketch Reference
`additional-details-sketch.jpeg` — whiteboard sketch photographed at the meeting.

---

## Details from the Sketch

### 1. App Name on Menu Screen
The app name ("Traffic Light") must appear on the first/menu screen, above the START and OPTIONS buttons. This was explicitly called out as a requirement on the sketch.

### 2. Visors / Cowls / Hoods on Each Light
Each of the three light circles needs a **visor** (also called a **cowl** or **hood**) rendered above it inside the housing. The visor is a curved/angled shade that sits above each bulb.

Behaviour noted on sketch:
- When a light is **lit (active)**, the visor reflects some of the light downward from the bulb below it.
- This is a visual realism detail — the reflected glow on the underside of the visor should be visible when the corresponding light is active.

This is a rendering-only change to the traffic light canvas; no logic changes required.

### 3. Manual Mode in Options/Settings
The Settings screen should include a **manual mode** option. When manual mode is enabled:
- The automatic timer/cycle is turned off.
- The user can **tap each light directly** on the traffic light screen to activate it manually.
- This lets users control the light sequence themselves rather than relying on the automatic cycle.

This is a new feature that touches both the Settings screen (toggle to enable manual mode) and the Traffic Light screen (tap targets on each light, cycle disabled when manual mode is active).

---

## Impact Assessment

| Detail | New Task or Existing? | Notes |
|---|---|---|
| App name on menu screen | Existing — Task 4 (Menu Screen) | Already implied by FR-1.2 ("app title/branding"); this confirms it must be "Traffic Light" |
| Visors/cowls on each light | Existing — Task 7 (Traffic Light Rendering) | Additional rendering detail; add visor shape above each circle + reflected glow when active |
| Manual mode | **New task** | Requires new UI in Settings (toggle), new state in controller (manual vs auto), and tap handling on TrafficLightScreen |
