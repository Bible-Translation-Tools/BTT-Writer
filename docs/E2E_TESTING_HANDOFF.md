# E2E Testing Handoff

Handoff for Kotlin Multiplatform E2E testing in BTT-Writer (desktop JVM + Android Maestro).

## Goal

Two-layer E2E strategy:

1. **Compose UI tests (JVM/desktop)** — fast, headless smoke test mirroring the Maestro profile flow; runs in `jvmTest` on Windows/Linux/macOS CI.
2. **Maestro (Android)** — black-box smoke test against a real debug APK on an emulator.

Desktop has no Maestro equivalent; the desktop test covers the same user journey via `runComposeUiTest`.

## What Is Implemented

### Gradle & dependencies

- Compose Multiplatform **1.11.1** in `gradle/libs.versions.toml`
- `composeApp/build.gradle.kts`:
  - `jvmTest` — `compose.desktop.currentOs` + `compose.uiTest`
  - Android `packaging.resources.pickFirsts` for duplicate `META-INF/*` in device/test APKs

### Desktop UI tests (`composeApp/src/jvmTest/.../uitest/`)

| File | Purpose |
|------|---------|
| `UiTestHarness.kt` | Koin + `TestDirectoryProvider`, mocked `UpdateApp`, `runComposeUiTest`, starts at `Config.Splash` by default |
| `SmokeFlow.kt` | Shared steps: `completeSmokeLaunch()`, `completeSmokeProfileToSettings()` |
| `SmokeProfileTest.kt` | Single test: cold start → profile → settings |

**Flow covered (matches Maestro `smoke.yaml`):**

1. Splash — dismiss migration (`No`) and hardware warning (`Don't show again` → `Continue`) if shown
2. Wait for profile index (`Create offline Account`)
3. Offline account form → privacy notice → terms (`I Agree`)
4. Home (`Your Translation Projects`) → More Options → Settings → assert `General` → back to home
5. New translation: `aaa` → `bible-nt` → `John` → assert `John` on home

**Notes:**

- Tests are **headless** — no visible window when running `jvmTest` (expected for `runComposeUiTest`).
- `UpdateApp` is **mocked** in the harness so splash completes reliably without network; all other UI interaction is real.
- Default `initialConfiguration` is `Config.Splash` (full user path, not skipped).

### Production code (minimal)

- `DefaultRootComponent`: optional `initialConfiguration` parameter (default `Config.Splash`) for test entry points.

No `UiTestTags`, `testTag` wiring, or separate `uiTest` / `androidDeviceTest` source sets in the repo today.

### Maestro (`.maestro/`)

```
.maestro/
  config.yaml              # appId, android disableAnimations
  flows/
    smoke.yaml             # parent: launch + settings + new translation
    smoke-launch.yaml      # subflow: cold launch → dismiss dialogs → wait for profile
    smoke-settings.yaml    # subflow: profile → home → settings → home
    smoke-new-translation.yaml  # subflow: create John project for aaa
```

`appId`: `org.bibletranslationtools.writer`

Most `extendedWaitUntil` and `scrollUntilVisible` steps use **`timeout: 5000`** (5s); splash waits are longer where library deploy runs on cold start.

**`smoke-launch.yaml`**

- `launchApp` with `clearState: true`
- Hardware warning (`Slow Device`) → `Don't show again` → `Continue` (string is **Don't**, not "Do not") — handled **before** migration (matches app startup order)
- Migration dialog → required `extendedWaitUntil` + `tapOn: "No"` + `notVisible` check (always shown after `clearState`; do **not** use `optional: true` on the tap)
- Wait up to **30s** for profile title `Please create or login to your account.` (cold start deploys the bundled ~158 MB library on splash)

**`smoke.yaml`**

- `runFlow: smoke-launch.yaml` then `runFlow: smoke-settings.yaml` then `runFlow: smoke-new-translation.yaml`

**`smoke-new-translation.yaml`**

- `Start a new translation` → language `aaa` → category `bible-nt` → book `John` → assert `John` on home → open project → `Edit Source` → `English (en) - Unlocked Literal Bible`
- `scrollUntilVisible` for `aaa` and `John` when list items are off-screen

**`smoke-settings.yaml`**

- `scrollUntilVisible` for offline card (may be below fold on emulator)
- Full profile → home → settings path
- `waitToSettleTimeoutMs: 200` on taps caps Maestro’s post-action settle wait (default can be several seconds)

Former `smoke-offline-profile.yaml` and an earlier settings-only flow were merged into `smoke-settings.yaml`.

### CI (`.github/workflows/build.yml`)

**Job order:**

1. **`test-android-e2e`** — Maestro on API 34 emulator (runs first)
2. **`test`** — `jvmTest` with Xvfb (includes desktop smoke test); `needs: test-android-e2e`
3. **Build jobs** — `needs: test`

**E2E job details:**

- Free disk space + enable KVM
- `android-emulator-runner@v2`: API 34, `ram-size: 4096M`, `disk-size: 6000M`, KVM, `swiftshader_indirect` GPU, `setup-android-emulator.sh` (Vulkan off)
- Assemble a normal debug APK, install Maestro, run **`smoke.yaml`**
- Use **`"$HOME/.maestro/bin/maestro"`** — `android-emulator-runner` runs each script line in a separate shell, so `export PATH` does not persist

## How to Run

```bash
# Desktop smoke test only (fast, headless)
gradlew :composeApp:jvmTest --tests org.bibletranslationtools.writer.uitest.SmokeProfileTest

# All JVM tests (unit + integration + desktop smoke)
gradlew :composeApp:jvmTest

# Maestro locally (emulator/device + debug APK)
gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
maestro test .maestro/flows/smoke.yaml

# Optional: individual subflows
maestro test .maestro/flows/smoke-launch.yaml
maestro test .maestro/flows/smoke-settings.yaml
maestro test .maestro/flows/smoke-new-translation.yaml
```

On Linux CI desktop tests, the `test` job starts Xvfb automatically. On Windows locally, `jvmTest` runs headless without Xvfb for the smoke test.

## Debugging Lessons

### Desktop (`runComposeUiTest`)

1. **`@OptIn(ExperimentalTestApi::class)`** on harness and tests
2. **`DefaultRootComponent`** created inside `setContent` on the main thread
3. **Essenty `LifecycleRegistry`** + `resume()` for Decompose
4. **`LocalLifecycleOwner`** provided by `runComposeUiTest` (Compose 1.11+)
5. **Mock `UpdateApp` only** — use real `Preference` from Koin; do not skip splash unless testing a narrower path
6. **Two `Continue` buttons** when privacy dialog is open — use `onAllNodesWithText("Continue")[1]` for the dialog button
7. **No visible UI** during `jvmTest` is normal

### Maestro / CI emulator

1. **Emulator boot timeout** — free disk space, KVM, tuned `emulator-options`; avoid heavy `pixel_6` profile on CI
2. **`maestro: not found`** — use full path `$HOME/.maestro/bin/maestro`, not `export PATH` in a prior script line
3. **Hardware dialog** — UI string is `Don't show again`, not `Do not show again`
4. **Splash → profile on CI** — cold start deploys the bundled ~158 MB library; `smoke-launch.yaml` waits up to 30s for the profile screen
5. **Profile card off-screen** — `scrollUntilVisible` before tapping `Create offline Account`
6. **Migration dialog** — with `clearState: true` the prompt always appears; use required `extendedWaitUntil` + `tapOn: "No"` (not `optional: true`)
7. **`when` condition `timeout`** — only supported on newer Maestro; use `extendedWaitUntil` for long waits instead
8. **`Failed to find ColorBuffer`** — benign emulator GPU stderr during dialog transitions; not a Maestro failure
9. **`qemu-system-x86_64-headless: I/O thread spun`** — benign emulator warning
10. **Post-tap delay** — Maestro waits for the UI to settle after each action by default; use `waitToSettleTimeoutMs` on `tapOn` to cap that wait (200ms in our flows). Prefer `assertVisible` / `extendedWaitUntil` over manual sleeps.

## Known Issues / Follow-ups

| Issue | Notes |
|-------|--------|
| **Desktop vs Maestro parity** | Desktop mocks `UpdateApp` for fast headless runs; Maestro exercises the real splash path including library deploy |
| **Pre-existing `jvmTest` failures** | Unrelated unit/integration tests may still fail in full `jvmTest` |
| **Android instrumented UI tests** | Not set up; only Maestro for Android E2E today |
| **`runComposeUiTest` v1 deprecation** | Consider migrating to `androidx.compose.ui.test.v2.runComposeUiTest` |

## Architecture

```mermaid
flowchart LR
  subgraph desktop [Desktop JVM]
    smokeTest["SmokeProfileTest"]
    jvmTest["jvmTest / Xvfb CI"]
  end
  subgraph android [Android]
    maestro["Maestro smoke.yaml"]
    emulator["API 34 emulator"]
  end
  smokeTest --> jvmTest
  maestro --> emulator
```

## Files Touched (summary)

- `gradle/libs.versions.toml`
- `composeApp/build.gradle.kts`
- `.github/workflows/build.yml`
- `.maestro/**`
- `composeApp/src/jvmTest/.../uitest/*`
- `composeApp/src/commonMain/.../RootComponent.kt` (`initialConfiguration`)
