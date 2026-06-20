# RepLog V2 Product Map

This map defines what moves RepLog from a first-pass MVP to a product that feels deliberate, reliable, and gym-ready.

## V2 North Star

RepLog should feel fast enough to use between sets, reliable enough to trust with months of training data, and rewarding enough to open after every session.

## 10/10 App Pillars

### 1. Gym-speed logging
- Start a workout in one tap.
- Start from templates in one tap.
- Add a set with previous values prefilled.
- Repeat the last set instantly.
- Tap any logged set to edit it.

### 2. Trust and recovery
- Active workout ID is persisted in DataStore.
- If Android kills the app, RepLog restores the active session.
- Every set is saved immediately to Room.

### 3. Meaningful feedback
- Workout summary appears immediately after finishing.
- Summary includes duration, exercises, sets, volume, and PR count.
- PR badges are visible inline.

### 4. Smart context
- Each exercise card shows recent completed sets for that movement.
- Users know what they did last time without opening history.
- Suggested set values come from the current workout or last completed workout.

### 5. Reduced friction
- Built-in templates are visible before starting a session.
- Exercise picker supports search by name, category, or equipment.
- Duplicate exercise additions are ignored automatically.

## Implemented in this V2 pass

- Persistent active workout recovery via DataStore.
- Template quick-start on Workout screen.
- Workout summary dialog after finishing.
- Rest timer card after logging a set.
- Inline previous completed sets per exercise.
- Suggested set prefill.
- One-tap repeat set.
- Set editing.
- PR recalculation when editing sets.
- Cleaner active workout dashboard stats.
- DAO/repository methods for recent exercise sets and PR exclusion during edits.

## Implemented in the next V2 pass

- Exercise detail analytics dialog from the Exercise Library.
- Exercise-level recent set history.
- Best weight, estimated 1RM, total set count, total volume and PR stats per exercise.
- Custom Compose estimated 1RM trend chart.
- Barbell plate calculator in Settings.
- DAO/repository analytics query for completed exercise set history.

## Implemented in the next build pass

- Dedicated Progress tab in bottom navigation.
- Weekly volume chart across the last 12 training weeks.
- Estimated 1RM leaderboard across exercises.
- Aggregate progress stats: workouts, sets, volume and PRs.
- Custom template creation from the Workout screen.
- Custom template deletion.
- CSV export to local app files for data ownership.

## Implemented in the high-standard V2.2 pass

- First-launch onboarding with unit selection and product positioning.
- Calendar-style history overview with monthly training-day highlights.
- Bodyweight tracking in the Progress hub.
- Bodyweight trend chart.
- Strength-to-bodyweight ratio on exercise rankings.
- JSON backup export containing workouts, sets, exercises and bodyweight logs.
- Local JSON restore/import from the latest generated backup.
- Room bodyweight entity, DAO, repository and DI wiring.

## Implemented in the V2.3 polish pass

- Android FileProvider wiring for safe export sharing.
- Share sheet support for CSV exports.
- Share sheet support for JSON backup exports.
- Android document picker integration for importing external JSON backups.
- Local restore retained for quick device-only backup testing.
- Removed monetisation UX in favour of a fully accessible feature set.
- Busy/export state surfaced in Settings.
- Timestamped export filenames while preserving a latest local JSON backup.

## Implemented in the V2.4 training polish pass

- Advanced custom template editor.
- Custom template exercise reorder using Up/Down controls.
- Custom template rename and save flow.
- Bodyweight goal preference stored in DataStore.
- Bodyweight goal line in the Progress chart.
- Bodyweight rolling-average trend line.
- More useful bodyweight actions: Log, Goal, Delete.
- Repository support for replacing template exercises cleanly.

## Implemented in the V2.5 open-access pass

- Removed the paywall and all Pro upgrade UI.
- Removed Google Play Billing dependency and billing permission.
- Removed BillingManager source package.
- Updated Settings to clearly communicate that every local feature is included from first launch.
- Updated release/privacy/store documentation to match the no-paywall product direction.

## Implemented in the roadmap-alignment phase

- Added strategic roadmap document for RepLog as a local-first training operating system.
- Added smart exercise metadata: primary muscles, secondary muscles, movement pattern and difficulty.
- Upgraded bundled exercise JSON with structured metadata.
- Added advanced set type model: working, warmup, drop set, failure, AMRAP, cluster, rest-pause and tempo.
- Added RPE and tempo fields to set logs.
- Added superset group data model and active-workout assignment UI.
- Added Room v3 migration for structured exercise metadata, set metadata and supersets.
- Extended CSV and JSON backup formats with new training metadata.

## Implemented in the V3 Analytics Foundation block

- Exercise media architecture advanced with a persisted `mediaAsset` field and Room v4 migration.
- Workout streaks and milestone system.
- Lifetime analytics expanded with total reps and tonnes lifted.
- Recovery score based on recent training frequency, volume trend and bodyweight trend.
- Plateau watch for exercises with no recent estimated 1RM improvement.
- Movement-pattern balance using smart exercise metadata.
- Muscle-volume distribution using primary and secondary muscle metadata.
- Progress screen now surfaces recovery, plateau, movement balance and muscle balance cards.
- Advanced plate calculator now persists custom kg/lb plate inventories.

## Implemented in the V3.1 Intelligence block

- Strength forecasting for exercises with enough longitudinal data.
- Forecast confidence labels and projected estimated 1RM over 5 weeks.
- Next-best-action recommendations from recovery, plateau, balance and forecast signals.
- Training DNA™ first draft with preferred rep range, average RPE, hard sets/week, top movement pattern and top exercise.
- Plateau intervention suggestions now consider recent hard-set volume and RPE.
- Home screen intelligence card surfaces recent training signals immediately on app open.

## Implemented in V3.2 / V3.3 Adaptive Prototype

- Strength forecast confidence ranges.
- Adaptive vNext workout plan generated from the last completed workout.
- Exercise-level progression targets: add load, add reps, add/remove set, or maintain.
- Fatigue modifier from recent volume and high-RPE work.
- Workout screen adaptive plan card with one-tap start.
- Superset visual labels now show A1/A2-style numbering.
- Home screen intelligence card remains the quick daily signal.

## Implemented in V3.4 Adaptive Execution Loop

- Adaptive workout targets now display inside active workout exercise cards.
- Workout completion summary evaluates adaptive targets hit vs missed.
- Adaptive summary notes show which exercise targets were hit or missed.
- Template exercise schema now supports target reps and target weight.
- Room v5 migration added for template target programming fields.
- Adaptive vNext starts preserve superset grouping and now close more of the suggestion → execution → evaluation loop.

## Implemented in V3.5 Programming Targets block

- PR forecast cards now include target dates for the next rounded strength target.
- Muscle balance recommendations now include specific exercise suggestions.
- Template exercise schema supports target sets, target reps and optional target load.
- Template editor UI can edit sets, reps and target load per exercise.
- Room v5 migration added for template target programming fields.
- Adaptive workout target evaluation remains connected to the completion summary.

## Implemented in V3.6 Prescription Persistence block

- Added `WorkoutPrescription` entity to persist exact programmed targets per workout.
- Added PrescriptionDao, repository methods and Hilt wiring.
- Room v6 migration creates the `workout_prescriptions` table and indexes.
- Adaptive vNext workouts now save their exact exercise prescriptions at creation time.
- Template-started workouts now save their exact template targets as prescriptions.
- Active workout cards read persisted prescriptions instead of recalculating targets.
- Workout completion summary compares actual work against saved prescriptions.
- This closes the suggestion → execution → evaluation loop more reliably.

## Implemented in V3.7 Stabilisation and Safety

- Restore confirmation dialogs for local and external JSON restores.
- Simple duplicate skipping for restored sessions and bodyweight logs.
- Delete confirmations for workouts, sets, templates, custom exercises and bodyweight entries.
- JSON backup/restore includes workout prescriptions.
- CSV export includes prescription source, target sets/reps/weight and target-hit status.
- Added architecture, migration and QA documentation.
- Gradle settings cleaned up for KSP plugin marker resolution.

## Implemented in V3.8 Polish and Progression

- Added progression engine for target hit/miss outcomes.
- Workout summary now recommends next progression after prescribed workouts.
- Exercise detail now renders a bundled local media placeholder asset.
- Added adaptive launcher icon XML resources and updated manifest icon references.
- Added exercise media placeholder vector.

## Implemented in V3.9 UI Polish block

- Active workout now renders grouped superset blocks for linked A/B/C/D exercises.
- Settings includes a local demo-data generator for QA and store screenshots.
- Exercise detail renders bundled local media placeholder imagery.
- Added accessibility notes and QA checklist.

## Next V4.0 Targets

- Shared superset rest timer behavior.
- Real bundled exercise GIFs/images.
- Full TalkBack/content-description audit.
- Larger-screen layouts and dialog-to-screen conversions.
- Store screenshot generation using demo data.
- Final local compile cleanup and UI QA pass.
