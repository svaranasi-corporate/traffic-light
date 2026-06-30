# Testing Strategy

## Test Framework

- **Unit & Property-Based Testing**: Kotest (Kotlin-native, supports property-based testing out of the box)
- **Android Instrumented Tests**: AndroidX Test + Espresso (for UI and integration tests)
- **Assertion Library**: Kotest matchers

## Unit Testing

### What to Unit Test
- **State machine logic**: `getNextState()` returns correct transitions for all states
- **Preference validation**: `validateAndClamp()` with boundary values, out-of-range values, and valid values
- **Duration lookup**: `getDurationForState()` returns correct durations for each state
- **Brightness interpolation**: Animation algorithm produces values within bounds at all progress points

### Unit Test Conventions
- One test class per source class
- Test method names describe the behavior: `should return GREEN when current state is RED`
- Arrange-Act-Assert structure
- No Android framework dependencies in unit tests (pure Kotlin logic)

## Property-Based Testing

Use Kotest's property testing to verify correctness properties:

- **State transitions are deterministic** — For any valid LightState, `getNextState()` always returns the same result
- **Clamping is idempotent** — `validateAndClamp(validateAndClamp(x)) == validateAndClamp(x)`
- **Brightness always in bounds** — For any progress value in [0.0, 1.0], interpolated brightness is in [DIM_BRIGHTNESS, FULL_BRIGHTNESS]
- **Cycle never terminates on its own** — Given isRunning=true and valid preferences, the cycle produces state changes indefinitely

### Timing Tolerance
- Phase duration accuracy: ±100ms of configured value (accounting for animation overlay)
- Animation frame timing: ±1 frame (16ms) tolerance

## Integration Testing

- **Full cycle test**: Start the light, verify it transitions through RED → GREEN → YELLOW → RED at least once with correct timing
- **Settings roundtrip**: Save preferences, kill app, restart, verify preferences loaded correctly
- **Back button test**: Start light, press back at various states, verify clean return to menu
- **Immersive mode test**: Verify system bars are hidden on TrafficLightScreen and restored on return

## Test Organization

```
src/
├── test/              # Unit tests (JVM, no Android framework)
│   └── kotlin/
│       └── com/trafficlight/
│           ├── controller/
│           │   ├── TrafficLightControllerTest.kt
│           │   └── TimerEngineTest.kt
│           ├── data/
│           │   └── PreferencesRepositoryTest.kt
│           └── model/
│               └── LightStateTest.kt
└── androidTest/       # Instrumented tests (require device/emulator)
    └── kotlin/
        └── com/trafficlight/
            ├── ui/
            │   ├── MenuScreenTest.kt
            │   ├── TrafficLightScreenTest.kt
            │   └── SettingsScreenTest.kt
            └── integration/
                └── FullCycleTest.kt
```

## Coverage Expectations

- State machine logic: 100% branch coverage
- Preference validation: 100% boundary coverage
- UI screens: smoke tests for navigation and rendering
- Animation: property tests for bounds, not pixel-perfect assertions
