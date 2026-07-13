# Implementation Plan: GitHub Actions CI/CD

## Overview

Create two GitHub Actions workflow files for the Traffic Light Android app: a PR validation pipeline that compiles, lints, and tests on pull requests to main, and a release pipeline that builds a signed APK (or debug fallback) and publishes it as a GitHub Release on push to main.

## Tasks

- [ ] 1. Create PR Validation Pipeline
  - [ ] 1.1 Create `.github/workflows/pr-validation.yml`
    - Define `pull_request` trigger targeting `main` branch (types: opened, reopened, synchronize)
    - Configure concurrency group keyed to the PR number with `cancel-in-progress: true` to cancel stale runs
    - Set runner to `ubuntu-latest`
    - Add checkout step with proper permissions for Gradle wrapper
    - Add JDK 17 Temurin setup using `actions/setup-java@v4`
    - Add Gradle caching using `actions/cache@v4` for `~/.gradle/caches` and `~/.gradle/wrapper` with cache key derived from `**/*.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`
    - Add `./gradlew build` step with `timeout-minutes: 10`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 6.1, 6.3, 6.4, 6.5, 7.1, 7.2_

- [ ] 2. Create Release Pipeline
  - [ ] 2.1 Create `.github/workflows/release.yml`
    - Define `push` trigger on `main` branch only (no `pull_request` triggers)
    - Set `permissions: contents: write` at workflow level for GitHub Release creation
    - Set runner to `ubuntu-latest`
    - Add checkout step
    - Add JDK 17 Temurin setup using `actions/setup-java@v4`
    - Add Gradle caching using `actions/cache@v4` for `~/.gradle/caches` and `~/.gradle/wrapper` with cache key derived from `**/*.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`
    - Add conditional signing logic: check if `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` secrets are all present; if yes, decode keystore and run `./gradlew assembleRelease` with signing config; if any are missing, run `./gradlew assembleDebug` as fallback
    - Add GitHub Release publication step using `softprops/action-gh-release@v2` (or equivalent): tag format `YYYYMMDD_<run_number>` (UTC date), attach built APK, auto-generated release notes from commits, mark as non-draft and non-prerelease, release title matches tag name
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 6.2, 6.3, 6.4, 6.5, 7.1, 7.3_

- [ ] 3. Final checkpoint
  - Ensure both workflow files are syntactically valid YAML, ask the user if questions arise.

## Notes

- No property-based tests are applicable — this is infrastructure-as-code (YAML workflow definitions)
- Both tasks are independent and can be implemented in parallel since they create separate files
- The signing logic in the release pipeline uses a conditional step pattern: an `env` check determines whether secrets are available, then branches to signed or debug build accordingly
- Cache restore-keys should include a fallback prefix so partial cache hits still speed up builds
- Each task references specific requirement acceptance criteria for traceability

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] }
  ]
}
```
