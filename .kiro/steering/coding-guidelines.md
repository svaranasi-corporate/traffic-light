# Coding Guidelines

## Language and Platform

- **Language**: Kotlin (idiomatic, concise style)
- **Target**: Android API level 33+ (Android 13 Tiramisu)
- **Build system**: Gradle with Kotlin DSL
- **UI framework**: Jetpack Compose with Material Design 3

## Dependencies

- Android SDK (API 33+)
- AndroidX Core (lifecycle management, immersive mode helpers)
- Material Design 3 (buttons, sliders, app bars)
- SharedPreferences (built-in, no additional library)
- No third-party libraries unless strictly necessary

## Code Style

### Formatting
- Use ktlint for automated formatting enforcement
- 4-space indentation (no tabs)
- Max line length: 120 characters
- Trailing commas in multi-line parameter lists and collections

### Naming Conventions
- Classes: PascalCase (`TrafficLightController`)
- Functions/methods: camelCase (`getNextState()`)
- Constants: SCREAMING_SNAKE_CASE (`DIM_BRIGHTNESS`)
- Packages: lowercase, dot-separated (`com.trafficlight.controller`)

### Import Organization
- Android/system imports first
- Third-party imports second
- Project imports last
- Alphabetical within each group
- No wildcard imports

## Architecture Principles

### General
- Follow SOLID principles, particularly Single Responsibility
- DRY — avoid duplicating logic (e.g., one place for state transitions, one place for preference validation)
- Prefer composition over inheritance
- Keep classes small and focused

### Android-Specific
- No network permissions — this app is fully offline
- No sensitive data storage — only integer timing preferences
- Use `Handler.postDelayed()` or `CountDownTimer` for timing — never `Thread.sleep()` on the UI thread
- Use `FLAG_KEEP_SCREEN_ON` on the traffic light screen to prevent display sleep
- Use immersive sticky mode for full-screen display
- Lock orientation to portrait on the traffic light screen
- Clean up all timers and callbacks in lifecycle methods (onPause/onDestroy)

### Memory and Performance
- No bitmaps — all rendering is programmatic (shapes, fills)
- Minimal memory footprint
- No background services, no GPS, no sensors
- Battery impact is screen-only

## Error Handling

- Validate all preferences on load using clamp-to-range strategy
- Never crash on invalid stored data — silently recover with defaults
- Log errors for debugging but don't surface technical messages to the user

## Package Structure

```
com.trafficlight/
├── ui/
│   ├── menu/          # MenuScreen composable
│   ├── trafficlight/  # TrafficLightScreen, rendering logic
│   └── settings/      # SettingsScreen composable
├── controller/        # TrafficLightController, TimerEngine
├── data/              # PreferencesRepository, TimingPreferences model
└── model/             # LightState enum, AnimationType enum, data classes
```
