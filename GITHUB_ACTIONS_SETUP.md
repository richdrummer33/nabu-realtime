# GitHub Actions CI/CD Setup

## Overview

This repository now has automated CI/CD pipelines via GitHub Actions. The workflows will automatically build, test, and validate your Android app on every push and pull request.

## Workflows

### 1. Main CI Workflow (`android-ci.yml`)

**Triggers:**
- Push to `latest` or `main` branches
- Pull requests to `latest` or `main` branches

**Steps:**
1. Checkout code (including git submodules)
2. Set up JDK 11 (Temurin distribution)
3. Grant execute permissions to gradlew
4. Build with Gradle
5. Run unit tests
6. Assemble debug APK
7. Upload APK artifact (retained for 7 days)
8. Upload test results (retained for 7 days)

**Artifacts:**
- `app-debug`: Debug APK from `app/build/outputs/apk/debug/app-debug.apk`
- `test-results`: Test results XML files from all modules

### 2. PR Checks Workflow (`pr-checks.yml`)

**Triggers:**
- Pull requests to `latest` or `main` branches

**Steps:**
1. Checkout code (including git submodules)
2. Set up JDK 11 (Temurin distribution)
3. Grant execute permissions to gradlew
4. Run lint checks (continues on error)
5. Run unit tests
6. Upload lint results (retained for 7 days)
7. Comment on PR if tests fail

**Features:**
- Automatic PR comments on test failures
- Lint results available as artifacts
- Parallel execution with main CI workflow

## Configuration

### JDK Version
- **Version**: 11
- **Distribution**: Eclipse Temurin
- **Reason**: Matches the configuration in `app/build.gradle.kts`

### Gradle Configuration
- **Version**: 8.10.2 (defined in `gradle/wrapper/gradle-wrapper.properties`)
- **Wrapper**: Included in repository as `gradle/wrapper/gradle-wrapper.jar`
- **Cache**: Enabled via GitHub Actions for faster builds

### Build Options
- `--no-daemon`: Prevents Gradle daemon from running (saves CI resources)
- `--stacktrace`: Provides detailed error information on failures

## Viewing Results

### GitHub UI
1. Go to your repository on GitHub
2. Click the "Actions" tab
3. Select a workflow run to view details
4. Download artifacts from the workflow run page

### Status Badges
You can add status badges to your README.md:

```markdown
![Android CI](https://github.com/richdrummer33/nabu-realtime/workflows/Android%20CI/badge.svg)
![PR Checks](https://github.com/richdrummer33/nabu-realtime/workflows/PR%20Checks/badge.svg)
```

## Artifacts

### Downloading APKs
1. Go to Actions → Select a workflow run
2. Scroll to "Artifacts" section
3. Click "app-debug" to download the APK
4. Install on device: `adb install app-debug.apk`

### Viewing Test Results
1. Go to Actions → Select a workflow run
2. Download "test-results" artifact
3. Extract and open XML files with your preferred viewer

### Viewing Lint Results
1. Go to Actions → Select a PR checks run
2. Download "lint-results" artifact
3. Open HTML files in browser

## Local Development

To run the same checks locally before pushing:

```bash
# Build
./gradlew build --no-daemon

# Run tests
./gradlew test --no-daemon

# Run lint
./gradlew lint --no-daemon

# Assemble APK
./gradlew assembleDebug --no-daemon
```

## Troubleshooting

### Build Failures

**Problem**: Missing Android SDK components
**Solution**: The GitHub Actions runner has Android SDK pre-installed. No additional setup needed.

**Problem**: Gradle download fails
**Solution**: The gradle wrapper jar is now included in the repository. If issues persist, check network connectivity.

**Problem**: Out of memory during build
**Solution**: Gradle daemon is disabled with `--no-daemon`. If needed, adjust memory settings in `gradle.properties`.

### Test Failures

**Problem**: Tests fail in CI but pass locally
**Solution**: Check if there are environment-specific assumptions (file paths, resources, etc.)

**Problem**: Flaky tests
**Solution**: Re-run the workflow. Consider adding retry logic or fixing timing issues in tests.

### Permission Issues

**Problem**: gradlew permission denied
**Solution**: The workflow includes `chmod +x gradlew` step. This should not happen.

## Future Enhancements

Potential improvements to consider:

1. **Release Workflow**: Automate APK signing and release to Play Store
2. **Instrumentation Tests**: Add workflow for running instrumented tests on emulator
3. **Code Coverage**: Add Jacoco for test coverage reports
4. **Dependency Updates**: Add Dependabot or Renovate for dependency updates
5. **Branch Protection**: Require workflow success before merging PRs
6. **Caching**: Further optimize build times with custom caching strategies

## Files Added/Modified

### New Files
- `.github/workflows/android-ci.yml` - Main CI workflow
- `.github/workflows/pr-checks.yml` - PR validation workflow
- `gradle/wrapper/gradle-wrapper.jar` - Gradle wrapper for CI

### Modified Files
- `.gitignore` - Added exception for gradle wrapper jar

## Notes

- Workflows use GitHub-hosted Ubuntu runners
- Build cache is shared between workflow runs for the same branch
- Artifacts are automatically cleaned up after 7 days
- Workflow runs can be manually triggered from the Actions tab
- Failed workflow runs will send email notifications to watchers
# CI Build Fix
