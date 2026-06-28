# Sprint 12 - Phase 0 Audit (Current Implementation)

Audit-first. Verified from source at HEAD 5309702 before any code was written.

## 1. Onboarding flow (current)
- `ui/onboarding/OnboardingScreen.kt`: a single scrolling `LazyColumn` of training
  questions (goal, level, equipment, days, style, units) + a generated-program
  preview. Two terminal actions: "Create my program" (`onPersonalize(answers)`)
  and "Skip - just set up logging" (`onComplete(useKg)`).
- `ui/onboarding/OnboardingViewModel.kt`: `finishWithPersonalization(answers)`
  persists units + training profile, installs a generated program, then sets
  onboarding complete. `finish(useKg)` is the units-only quick finish.
- NO display name, NO date of birth/height/weight, NO legal acceptance step.

## 2. Navigation gate (current)
- `ui/navigation/NavGraph.kt` line ~96:
  `if (!onboardingState.onboardingComplete) { OnboardingScreen(...); return }`
  then the main `NavHost(startDestination = Home)`.
- The gate is a SINGLE boolean read from DataStore. This is the one chokepoint to
  extend for legal version-locking (Priority 6). Home is NOT reachable while the
  gate is false (the NavHost is not composed at all), which is correct; but the
  gate today only checks a bool, not legal acceptance.

## 3. Settings architecture (current)
- `ui/settings/SettingsScreen.kt`: a flat `LazyColumn` of `RepLogCard`s (Units,
  Fast logging focus, Export folder, CSV export, JSON backup/restore, Templates,
  Demo data, Advanced/delete-all, Data ownership, Release checklist). NO formal
  "category" grouping, NO Legal section, NO visible app-version string, and NO
  navigation callbacks (it takes only `contentPadding`).
- To add a Legal Centre we will give `SettingsScreen` `onOpen...` callbacks wired
  in NavGraph to new secondary screens, mirroring the existing secondary-screen
  pattern (TopAppBar back arrow, no bottom bar).

## 4. Preferences / DataStore (current)
- `util/PreferencesManager.kt` (@Singleton), single Preferences DataStore named
  "settings". Holds units, onboarding_complete, rest timer config/persist, export
  location, auto-focus, training profile (goal/level/equipment/days/style),
  progression + coach markers. All flows + suspend setters. This is the correct
  home for the new user profile + legal acceptance (per the revised brief).

## 5. Room schema (current) - LEFT UNTOUCHED
- `data/db/AppDatabase.kt`: version 15, 17 entities, 14 migrations (1_2..14_15)
  all registered, no destructive fallback. 15 DAOs provided in `di/AppModule.kt`.
- DECISION: profile + legal acceptance go in DataStore. ZERO Room migrations.
  Room stays dedicated to workout/history/analytics/training data.

## 6. User profile storage (current)
- Only the DataStore training-profile keys exist. No identity (name/DOB/body
  metrics). Personalisation today is limited to program generation inputs.

## 7. Existing legal documentation (current)
- NONE in-app. No disclaimer, terms, or privacy policy screen or asset. The
  Settings "Data ownership" card is marketing copy, not a legal document.

## 8. Version handling (current)
- `app/build.gradle.kts`: versionName "1.0.0", versionCode 1. `buildConfig` is NOT
  enabled, so `BuildConfig.VERSION_NAME` is not currently generated. We will enable
  `buildFeatures { buildConfig = true }` so the app version can be read at runtime
  for acceptance records and the Settings "App Version" row. No legal-document
  versioning exists yet.

## 9. Home accessibility before onboarding (current)
- Not accessible: while `onboardingComplete == false` the main NavHost is never
  composed (early `return`). The risk for Sprint 12 is only that "complete" today
  means "bool set", with no legal-acceptance requirement. Priority 3/6 fix this by
  making the gate require BOTH a profile AND current-version legal acceptance.

## Audit conclusion / plan
- Add to DataStore: a `UserProfile` (displayName required; DOB/height/weight/units/
  level/goal/frequency/equipment optional) and a `LegalAcceptance` record
  (accepted versions per doc, timestamp, app version, display name, completed).
- Add `LegalDocuments` constants (version/effectiveDate/lastUpdated + body text)
  for Disclaimer/Terms/Privacy; bump a doc version to force re-acceptance.
- Replace the onboarding gate: require profile present AND all current legal
  versions accepted; otherwise show the multi-step onboarding+legal flow.
- Add a Settings Legal Centre (read-only doc viewers + Acceptance History + App
  Version + Open Source Licences).
- Zero Room migrations; 100% offline; existing user data preserved.
