# RC21 — Release Readiness — Play Store Checks

## Versioning

- Check `app/build.gradle.kts` defaultConfig: `versionCode = 1`, `versionName = "1.0.0"` — initial release, okay but should be bumped for RC.
- No versioning file for RC21 — should tag git.

## Signing

- `buildTypes.release` has `isMinifyEnabled = true`, `isShrinkResources = true`, `proguardFiles` default + `proguard-rules.pro`.
- No signing config in `build.gradle.kts` — placeholder comment "Create a signing key and add signing config in app/build.gradle.kts, or use Android Studio's Generate Signed Bundle flow." So signing not configured, must be done before release via env variables or local properties.
- **Not ready for Play Store until signing configured.**

## ProGuard / R8

- Release build minify enabled, shrink resources enabled, proguard-rules.pro exists (282 bytes? Check).
- Check proguard-rules.pro: should keep Room, Hilt, etc. Might need rules for Compose.

## Permissions

- Check `AndroidManifest.xml`: No internet permission? For offline app should have no INTERNET. Check manifest.
- Permissions needed: None? Maybe for foreground service RestTimerService? Check manifest.

## Privacy

- `PRIVACY_POLICY.md` exists (2647 bytes), `PRIVACY_POLICY_DRAFT.md` exists. Need privacy policy URL for store listing.
- Data Safety form: Must declare offline-first, no data collection? Check `RELEASE_COMPLIANCE_REPORT.md` etc. Exists.
- No analytics? Check `AnalyticsEngine` — offline? Should be okay.

## Offline Behaviour

- Requirement: Everything must remain offline, no APIs, no cloud. Verified via code: no import of internet, no Retrofit, no Firebase Analytics? Check dependencies: only Room, Hilt, DataStore, Gson, no network libs. Good.
- Exercises.json bundled in assets, 211KB, offline.
- No cloud services, no GIF, MP4, Lottie, OpenGL, Unity, external rendering libs — verified only Kotlin + Compose Canvas + local assets.

## Crash Handling

- VisualEngineAdapter catches exceptions and falls back to vector or commercial generic bench, logs via Log.e/w, avoids crash. Good.
- No global crash handler? Check `RepLogApplication.kt`? Might have.
- Need to verify no uncaught exceptions in animation loop: LaunchedEffect withFrameNanos loop while true, no try/catch, could crash if exception inside draw? Should have try/catch.

## Logging

- Uses `android.util.Log` with TAG VisualEngineAdapter. Should be removed or set to not log in release? Could be okay but should not log sensitive.

## Accessibility

- ExerciseAnimationView Canvas semantics contentDescription present.
- MuscleBodyDiagram contentDescription primary/secondary muscles.
- Touch targets IconButton default 48dp.
- Font sizes Material typography.
- Color contrast: red #EF4444 on slate #1E293B may fail AAA, need check.

## Play Store Compliance

- Target SDK 34, compileSdk 34, minSdk 26 — matches current Play Console requirement (target 34+). Good.
- Data Safety form not checked, need to complete.
- Store listing draft exists `STORE_LISTING_DRAFT.md` (1345 bytes), screenshots HTML `STORE_SCREENSHOTS.html` exists (5719 bytes), feature graphic, app icon placeholder `ic_launcher_foreground.xml` needs replacement with final artwork per README.

## Android 12+ / 13+ / 14+ / 15+

- CompileSdk 34 supports Android 14. Android 15 (SDK 35) not yet? Latest is 34/35? Should test on Android 12+ devices for foreground service, notification, etc. RestTimerService foreground service scaffold exists, needs permission for Android 14.

## Tablet Compatibility / Large Screen

- ReferenceSize min(width,height)*0.32 scales, Canvas fillMaxWidth height 240dp responsive, but fixed height 240dp may be small on tablet large screen, could be larger.
- No specific large screen layout (e.g., two-pane).

## Landscape / Portrait

- No specific landscape handling, Canvas uses fillMaxWidth height 240dp works in both, but landscape wider may have extra horizontal space.

## Permissions for Android 12+

- Check Android 12+ foreground service permission? RestTimerService maybe needs FOREGROUND_SERVICE.

## Overall Release Readiness

- **Versioning:** Needs bump for RC.
- **Signing:** Not configured, must be done.
- **ProGuard/R8:** Enabled, rules exist, need verification.
- **Permissions:** Likely minimal, check manifest.
- **Privacy:** Policy exists, need URL and Data Safety form.
- **Offline:** Good, no network.
- **Crash handling:** Fallback via adapter, but animation loop no try/catch.
- **Logging:** Uses Log, should be okay.
- **Accessibility:** Good contentDescription, touch targets, but color contrast may need check.
- **Play Store Compliance:** Target 34 good, store listing draft exists, screenshots HTML exists, but need final artwork, privacy URL, Data Safety.
- **Android 12-15:** CompileSdk 34 good, need testing on devices.
- **Tablet/Large screen:** Scaling via referenceSize, but no dedicated large screen layout.
- **Landscape/Portrait:** Works but not optimized.

**Not fully ready for Play Store until signing configured, privacy URL, Data Safety form, final artwork, and testing on physical devices/emulators across Android versions.**

