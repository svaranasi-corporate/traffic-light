# Requirements Document

## Introduction

This feature adds GitHub Actions CI/CD pipelines to the Traffic Light Android application. The goal is two simple pipelines: one that validates pull requests before merge, and one that builds a release APK and publishes it as a GitHub Release artifact when code lands on `main`. The release versioning uses a date-based format (`YYYYMMDD_<run_number>`). APK signing uses a self-signed release key stored as GitHub repository secrets.

## Glossary

- **PR_Validation_Pipeline**: A GitHub Actions workflow triggered on pull requests targeting the `main` branch that compiles, lints, and tests the application.
- **Release_Pipeline**: A GitHub Actions workflow triggered when a pull request is merged to `main` that builds a release APK and publishes it as a GitHub Release.
- **GitHub_Release**: A GitHub feature that associates downloadable assets (such as APK files) with a tagged commit in the repository.
- **APK**: Android Package Kit — the installable artifact for Android applications.
- **Workflow_File**: A YAML file in `.github/workflows/` that defines a GitHub Actions pipeline.
- **Runner**: The GitHub-hosted virtual machine (ubuntu-latest) that executes workflow steps.

## Requirements

### Requirement 1: PR Validation Pipeline Trigger

**User Story:** As a developer, I want the validation pipeline to run automatically on pull requests to main, so that I get feedback on code quality before merging.

#### Acceptance Criteria

1. WHEN a pull request is opened or reopened targeting the `main` branch, THE PR_Validation_Pipeline SHALL execute.
2. WHEN a pull request is synchronized (new commits pushed) targeting the `main` branch, THE PR_Validation_Pipeline SHALL execute.
3. THE PR_Validation_Pipeline SHALL execute on a GitHub-hosted `ubuntu-latest` Runner.
4. THE PR_Validation_Pipeline SHALL NOT execute for pull requests targeting branches other than `main`.
5. WHEN a new pipeline run is triggered for a pull request that already has a run in progress, THE PR_Validation_Pipeline SHALL cancel the in-progress run and execute only the latest triggered run.

### Requirement 2: PR Validation Pipeline Steps

**User Story:** As a developer, I want the validation pipeline to compile, lint, and run unit tests, so that broken code is caught before it reaches main.

#### Acceptance Criteria

1. THE PR_Validation_Pipeline SHALL check out the repository source code at the pull request merge ref with the Gradle wrapper file retaining executable permissions.
2. THE PR_Validation_Pipeline SHALL configure JDK 17 (Temurin distribution) for the build environment.
3. THE PR_Validation_Pipeline SHALL execute `./gradlew build` to compile the application, run ktlint checks, and run unit tests, with a maximum step timeout of 10 minutes.
4. IF the `./gradlew build` command exits with a non-zero status, THEN THE PR_Validation_Pipeline SHALL report a failure status on the pull request.
5. WHEN `./gradlew build` completes successfully, THE PR_Validation_Pipeline SHALL report a success status on the pull request.

### Requirement 3: Release Pipeline Trigger

**User Story:** As a developer, I want the release pipeline to run automatically when code is merged to main, so that a release APK is built without manual intervention.

#### Acceptance Criteria

1. WHEN a push event occurs on the `main` branch, THE Release_Pipeline SHALL execute.
2. THE Release_Pipeline SHALL execute on a GitHub-hosted `ubuntu-latest` Runner.
3. THE Release_Pipeline SHALL NOT define any `pull_request` event triggers in its Workflow_File.
4. WHEN a commit is pushed directly to `main` (not via a pull request merge), THE Release_Pipeline SHALL also execute.

### Requirement 4: Release APK Build

**User Story:** As a developer, I want the release pipeline to produce a release APK, so that I have a distributable artifact for each merge to main.

#### Acceptance Criteria

1. THE Release_Pipeline SHALL check out the repository source code.
2. THE Release_Pipeline SHALL configure JDK 17 (Temurin distribution) for the build environment.
3. THE Release_Pipeline SHALL execute `./gradlew assembleRelease` to produce a release APK.
4. IF the `./gradlew assembleRelease` command exits with a non-zero status, THEN THE Release_Pipeline SHALL fail the workflow run and not proceed to artifact publication.
5. IF all four repository secrets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD are present, THEN THE Release_Pipeline SHALL decode the keystore, configure signing with those credentials, and produce a signed release APK.
6. IF any of the four signing secrets (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD) is missing, THEN THE Release_Pipeline SHALL produce a debug APK via `./gradlew assembleDebug` as a fallback.

### Requirement 5: GitHub Release Publication

**User Story:** As a developer, I want the release APK to be published as a GitHub Release asset, so that team members can download it directly from the repository.

#### Acceptance Criteria

1. WHEN the release APK is built successfully, THE Release_Pipeline SHALL create a GitHub_Release on the repository using a workflow token with `contents: write` permission.
2. THE Release_Pipeline SHALL attach the release APK file as a downloadable asset on the GitHub_Release.
3. THE Release_Pipeline SHALL generate a tag name for the GitHub_Release using the format `YYYYMMDD_<run_number>` where `YYYYMMDD` is the current UTC date and `<run_number>` is the GitHub Actions workflow run number (e.g., `20260713_07`).
4. THE Release_Pipeline SHALL set the GitHub_Release title to match the tag name.
5. THE Release_Pipeline SHALL populate the GitHub_Release body with auto-generated release notes derived from the commit messages since the last release tag.
6. IF the GitHub_Release creation or asset upload fails, THEN THE Release_Pipeline SHALL report a failure status on the workflow run.
7. THE Release_Pipeline SHALL mark the GitHub_Release as a non-draft, non-prerelease publication.

### Requirement 6: Gradle Caching

**User Story:** As a developer, I want Gradle dependencies and build outputs to be cached between pipeline runs, so that builds complete faster.

#### Acceptance Criteria

1. THE PR_Validation_Pipeline SHALL cache the `~/.gradle/caches` and `~/.gradle/wrapper` directories between runs.
2. THE Release_Pipeline SHALL cache the `~/.gradle/caches` and `~/.gradle/wrapper` directories between runs.
3. WHEN cache entries matching the current cache key exist from a previous run, THE pipelines SHALL restore them before executing Gradle commands.
4. THE pipelines SHALL derive the cache key from a hash of the Gradle dependency files (`**/*.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`) so that the cache is invalidated when dependencies change.
5. IF no cache entry matches the current cache key, THEN THE pipeline SHALL proceed with a full dependency download without failing the build.

### Requirement 7: Pipeline Scope Constraint

**User Story:** As a developer, I want only two pipelines with no additional complexity, so that the CI/CD setup remains simple and maintainable.

#### Acceptance Criteria

1. THE `.github/workflows/` directory SHALL contain exactly two YAML files: one defining the PR_Validation_Pipeline and one defining the Release_Pipeline.
2. THE PR_Validation_Pipeline SHALL NOT include any steps that create a GitHub_Release, upload assets to a GitHub_Release, sign an APK, or publish artifacts outside of the workflow run.
3. THE Release_Pipeline SHALL NOT define `pull_request` or `pull_request_target` as trigger events in its `on:` configuration.
