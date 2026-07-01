# Traffic Light App

A full-screen Android app that simulates a realistic traffic light for kids playing with ride-on toys or scale model vehicles. Three lights cycle continuously through red, green, and yellow. Timing for each phase is configurable via a settings screen.

---

## Prerequisites

Everything you need before opening the project.

### 1. Android Studio

**Minimum**: Android Studio Jellyfish | 2023.3.1 Patch 2  
**Recommended**: Latest stable release

Download: https://developer.android.com/studio

> Android Studio bundles a JDK — see the JDK section below before installing a separate one.

### 2. JDK 17

This project compiles with Java 17 (`sourceCompatibility = JavaVersion.VERSION_17`).

Android Studio Jellyfish and newer ship with a bundled JDK 17. You do **not** need to install one separately unless you plan to build from the command line outside of Android Studio.

If you build from the terminal, make sure `java -version` reports 17:

```bash
java -version
# openjdk version "17.x.x" ...
```

If not, install JDK 17 from [Adoptium](https://adoptium.net/) or via your package manager:

```bash
# macOS (Homebrew)
brew install --cask temurin@17

# Ubuntu / Debian
sudo apt install openjdk-17-jdk
```

### 3. Android SDK

The SDK is installed and managed through Android Studio's SDK Manager (`Tools → SDK Manager`).

| Component | Required version |
|-----------|-----------------|
| Android SDK Platform | API 33 (Android 13 Tiramisu) — **minimum** |
| Android SDK Build-Tools | 34.0.0 or higher |
| Android SDK Platform-Tools | Latest |
| Android Emulator | Latest (if running on a virtual device) |

To install: open Android Studio → **Tools → SDK Manager → SDK Platforms**, check **Android 13 (API 33)**, and apply.

### 4. Gradle

Gradle is managed by the Gradle Wrapper — **no separate installation needed**. When you build, the wrapper downloads Gradle 8.7 automatically.

```bash
./gradlew --version
# Gradle 8.7
```

### 5. Git

Any recent version of Git works. Used for cloning the repo and version control.

```bash
git --version   # 2.x or higher is fine
```

---

## Getting Started

```bash
# Clone the repo
git clone <repo-url>
cd traffic-light

# Open in Android Studio
# File → Open → select the traffic-light folder
# Android Studio will sync Gradle automatically on first open
```

---

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

---

## Run

- **Emulator**: Create a virtual device in Android Studio (API 33+, portrait-capable) via **Device Manager**, then press Run (▶).
- **Physical device**: Enable Developer Options and USB Debugging on an Android 13+ device, connect via USB, then press Run (▶).

---

## Test

```bash
# Unit tests (JVM — Kotest, no device required)
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest
```

---

## Code Style

This project uses [ktlint](https://pinterest.github.io/ktlint/) for formatting, applied via the Gradle plugin. No separate installation needed.

```bash
# Check for style violations
./gradlew ktlintCheck

# Auto-fix violations
./gradlew ktlintFormat
```

---

## Project Structure

```
app/src/main/kotlin/com/trafficlight/
├── MainActivity.kt           # Single activity, Compose NavHost
├── ui/
│   ├── theme/                # Material 3 dark theme
│   ├── menu/                 # Menu screen (Start / Options)
│   ├── trafficlight/         # Full-screen traffic light display
│   └── settings/             # Per-phase duration sliders
├── controller/               # State machine and timer logic
├── data/                     # PreferencesRepository (SharedPreferences)
└── model/                    # LightState, AnimationType, TimingPreferences
```

---

## Key Versions (pinned in `gradle/libs.versions.toml`)

| Dependency | Version |
|------------|---------|
| Android Gradle Plugin | 8.4.2 |
| Kotlin | 1.9.24 |
| Gradle Wrapper | 8.7 |
| Compose BOM | 2024.06.00 |
| Target / Compile SDK | API 34 |
| Min SDK | API 33 |
| Kotest | 5.9.1 |
| ktlint Gradle plugin | 12.1.1 |
