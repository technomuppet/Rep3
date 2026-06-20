# RepLog

RepLog is a native Android workout logger built with Kotlin, Jetpack Compose, Room, Hilt, and DataStore.

RepLog is now designed as a fully accessible app: no paywall, no billing dependency, and no locked feature tiers. Every local feature is available from first launch.

## Features

- Smart exercise library seeded from `assets/exercises.json`
- Exercise metadata for primary muscles, secondary muscles, movement patterns, equipment and difficulty
- Custom exercises
- First-launch onboarding with unit setup
- One-tap empty workout start
- One-tap template quick-start
- Custom workout template creation, editing, reorder and deletion
- Start/discard/finish workouts
- Persistent active workout recovery through DataStore
- Add exercises and log weight/reps sets
- Advanced set types: warmup, drop set, failure, AMRAP, cluster, rest-pause and tempo
- RPE and tempo logging
- Superset group support with grouped active-workout layout
- Suggested set prefill from current or previous performance
- One-tap repeat set
- Tap-to-edit logged sets
- Automatic same-rep personal-record detection
- PR recalculation when editing sets
- Inline previous completed sets per exercise
- In-workout rest timer card
- Post-workout summary with duration, set count, volume and PRs
- Workout history with PR badges
- Calendar-style workout history overview
- Exercise detail analytics with recent sets and estimated 1RM trend chart
- Dedicated Progress hub with weekly volume chart and estimated 1RM leaderboard
- Lifetime analytics for workouts, sets, reps, PRs, total load and tonnes lifted
- Movement-pattern and muscle-volume balance analysis
- Basic recovery score and plateau watch
- Strength forecasts with confidence ranges, next-best-action recommendations and Training DNA™ summary
- Home screen intelligence card for recent training signals
- Adaptive vNext workout suggestions based on recent performance and fatigue
- Persisted adaptive/template prescriptions shown inside workouts with post-workout target hit/miss summary and next-progression recommendations
- Template editor supports target sets, reps and optional target load per exercise
- PR forecast target-date estimates for next rounded strength target
- Muscle balance recommendations with specific exercise suggestions
- Bodyweight tracking with trend chart, rolling average, goal line and strength-to-bodyweight ratios
- Advanced plate calculator with custom gym plate inventories
- CSV export for workout data ownership
- Android share sheet for CSV and JSON exports
- JSON backup, local restore and external file import with restore confirmation and duplicate skipping
- Workout prescriptions included in CSV/JSON data ownership tools
- Preferences for units and rest timer length
- Local demo-data generator for QA and store screenshots
- Foreground rest-timer service scaffold

## Open and run

1. Use Android Studio with JDK 17 or newer.
2. Open this folder in Android Studio: `RepLog/`.
3. Let Android Studio sync Gradle.
4. Run the `app` configuration on an emulator or device.

## Release build

1. Replace placeholder launcher artwork in `app/src/main/res/drawable/ic_launcher_foreground.xml`.
2. Create a signing key and add signing config in `app/build.gradle.kts`, or use Android Studio's Generate Signed Bundle flow.
3. Build an Android App Bundle:

```bash
./gradlew bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Store checklist before submission

- Add privacy policy URL.
- Complete Data Safety form.
- Upload screenshots, feature graphic, app icon, and short/long descriptions.
- Test release build on a clean device.
- Test onboarding, workout logging, recovery, exports and import/restore.
- Verify target SDK matches the current Play Console requirement.

## Notes

The project is intentionally offline-first. Workout data is local Room storage and included in Android backup rules. All features are available without subscription or in-app purchase.
