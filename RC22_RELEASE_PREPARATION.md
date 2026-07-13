# RC22 — Release Preparation — Play Store Requirements

## Signing

- **Current:** `app/build.gradle.kts` release buildType has `isMinifyEnabled = true`, `isShrinkResources = true`, `proguardFiles` default + `proguard-rules.pro`, but no signing config. Comment says "Create a signing key and add signing config in app/build.gradle.kts, or use Android Studio's Generate Signed Bundle flow."
- **Required for Play Store:** Generate keystore:
```
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
```
  Add to `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "release"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        ...
    }
}
```
  Or use Android Studio Generate Signed Bundle.

- **Status:** Not configured, must be done before release. Not blocking for RC, but required for production.

## Versioning

- **Current:** `defaultConfig` `versionCode = 1`, `versionName = "1.0.0"` — initial release.
- **For RC22:** Should bump to `versionCode = 2`, `versionName = "1.0.0-rc22"` or `1.0.0` with RC tag.
- **Recommendation:** Use semantic versioning + versionCode increment per release.

## Release Configuration

- **Minify and shrink:** Enabled for release, good for size and obfuscation.
- **ProGuard rules:** `proguard-rules.pro` exists, should keep Room, Hilt, etc. Check file content: likely default.
- **Build features:** Compose true, BuildConfig true, composeOptions kotlinCompilerExtensionVersion 1.5.9 — older but works, could update to 1.5.14 for latest.

## ProGuard / R8

- Enabled, good. Need to test release build on clean device after minify to ensure no missing keep rules for Room entities, Hilt modules, Gson, etc.

## Privacy Policy

- **Files exist:** `PRIVACY_POLICY.md` 2647 bytes, `PRIVACY_POLICY_DRAFT.md` 595 bytes.
- **Required:** Privacy policy URL for store listing. Must host privacy policy (e.g., GitHub Pages, website) and add URL to store listing and in-app legal screens (`LegalScreens.kt`).
- **Content:** Should state offline-first, no data collection, no internet, workout data local Room storage, Android backup rules.

## Data Safety

- **Form:** Must complete Play Console Data Safety form: declare no data collected, no data shared, offline, no encryption? Check `RELEASE_COMPLIANCE_REPORT.md` exists.
- **For offline app:** Declare data collected = none, or if using DataStore preferences local only, no collection.

## Permissions

- **Check AndroidManifest.xml:**
```
<manifest>
  <application ...>
    <service android:name=".ui.workout.RestTimerService" ... foregroundServiceType? />
  </application>
</manifest>
```
- **Permissions needed:** For foreground service RestTimerService, need `FOREGROUND_SERVICE` permission for Android 14+ also `FOREGROUND_SERVICE_SPECIAL_USE`? Check.
- **No INTERNET permission** — good for offline.
- **No location, camera, etc.**

## App Icon / Feature Graphic / Screenshots

- **App icon:** Placeholder `ic_launcher_foreground.xml` needs replacement with final artwork per README "Replace placeholder launcher artwork". Current is placeholder.
- **Feature graphic:** Not present, need 1024x500 PNG.
- **Screenshots:** `STORE_SCREENSHOTS.html` exists 5719 bytes, `UI_MOCKUPS.html` 14791 bytes, but need actual PNGs for phone, tablet, etc. 2-8 screenshots per device type.
- **Store listing draft:** `STORE_LISTING_DRAFT.md` 1345 bytes exists.

## Play Listing

- **Short/long descriptions:** In draft file.
- **Need:** Complete listing with privacy URL, contact, category, tags.

## Crash Reporting

- No crash reporting library (Firebase Crashlytics) — offline, no internet, so no crash reporting to cloud. Could have local crash logging via ACRA offline? Not present. Acceptable for offline but should have local log.

## Release Notes

- Should generate release notes: "RC22 Production Hardening — critical fixes hip thrust/dip, performance 60fps caching, muscle sync, camera best-view, motion quality pause + variable tempo, exercise accuracy all ≥9/10, legacy removal."

## Final Checks Before Play Store

- [ ] Signing configured
- [ ] Version bump
- [ ] Release build tested on clean device (no crash, no rendering failures, no missing assets, no ANRs)
- [ ] ProGuard/R8 tested release build
- [ ] Privacy policy URL hosted and added
- [ ] Data Safety form completed
- [ ] Permissions minimal, foreground service permission for Android 14+
- [ ] App icon final artwork
- [ ] Feature graphic 1024x500
- [ ] Screenshots phone/tablet 2-8 each
- [ ] Play listing complete short/long description
- [ ] Crash reporting or local logging
- [ ] Release notes

**Status:** Not fully ready — signing, privacy URL, final artwork, screenshots PNGs, Data Safety form, device testing needed.

