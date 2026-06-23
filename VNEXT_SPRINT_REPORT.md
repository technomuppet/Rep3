# RepLog VNext - UX Activation, Navigation Repair & Feature Integration Sprint

**Final deliverable report**

Branch: `sync/apply-improvements`
Sprint commit range: `e8ea98f` (sync point) .. `3a4ce2a` (HEAD)
Sprint commits: `694ac06` (A1/A2/P2/P6) -> `da06d31` (P4) -> `140a097` (P7) -> `3a4ce2a` (P8)

This sprint's mandate was activation and integration, not new engines: connect and surface
the systems the app already shipped (Goal Engine, Training Genome, Coach Dashboard, Recovery,
516-exercise library, program generator), repair navigation, and polish the UX. No domain
engines were added; one small pure-domain filter helper (`ExerciseFilter`) and a `WorkoutStyle`
enum were introduced purely to drive existing capabilities into the UI.

---

## 1. Navigation audit

### Findings (pre-sprint)
- The six primary destinations (Home, Training, Progress, History, Exercises, Settings) were
  reachable via the bottom navigation bar.
- Three secondary destinations - **Goals**, **Training DNA**, **Coach History** - were
  navigable to, but had **no back affordance**: once a user drilled in, the bottom bar was the
  only way out, and on the secondary routes it offered no obvious "up" action. This was the most
  serious navigation defect (logged as issue **A1**).
- Training DNA had no entry point from Home; it could only be reached indirectly.

### Repairs (commit `694ac06`)
`ui/navigation/NavGraph.kt` was rewritten:

- Added a `secondaryTitles` map and `tabRoutes` / `bottomTabs` lists so the shell knows which
  routes are primary tabs and which are secondary detail screens.
- Secondary screens (`Goals`, `TrainingDna`, `CoachHistory`) now render inside a `Scaffold`
  with a Material 3 `TopAppBar` carrying a back arrow
  (`Icons.AutoMirrored.Filled.ArrowBack` -> `navController.navigateUp()`).
- The bottom navigation bar is **hidden** on secondary screens and shown only on the six
  primary tabs, removing the ambiguous double-navigation surface.
- Bottom-tab reselection uses the correct state-preserving pattern
  (`popUpTo(startDestination){ saveState = true }; launchSingleTop; restoreState`).
- The `Workout` tab was relabelled **"Training"** to match product language.
- Added an `onOpenTrainingDna` callback so Home can deep-link into Training DNA.

### Post-sprint navigation map
| Route | Type | Reachable from | Back affordance |
|---|---|---|---|
| `home` (Home) | Primary tab | Bottom bar | n/a (root) |
| `workout` (Training) | Primary tab | Bottom bar, Coach "Start" | n/a |
| `progress` (Progress) | Primary tab | Bottom bar | n/a |
| `history` (History) | Primary tab | Bottom bar, Home buttons | n/a |
| `exercises` (Exercises) | Primary tab | Bottom bar | n/a |
| `settings` (Settings) | Primary tab | Bottom bar | n/a |
| `goals` (Goals) | Secondary | Home goal card, Home "Goals" button | TopAppBar back arrow |
| `training_dna` (Training DNA) | Secondary | Home genome card | TopAppBar back arrow |
| `coach_history` (Coach History) | Secondary | Home "Coach History" button | TopAppBar back arrow |

**Result:** every screen is reachable, and every secondary screen now has an unambiguous way back.

---

## 2. Feature activation report

The goal was to surface engines that already existed in the domain layer but were under-exposed.

| Engine / system | Status before sprint | Activation this sprint |
|---|---|---|
| Goal Engine (`GoalRepository.forecastFor`) | Only on Goals screen | **Surfaced on Home** as a top-goal card with progress %, ETA and summary, tapping into Goals (P6, `694ac06`) |
| Training Genome (`TrainingGenomeEngine.analyze`) | Only on Training DNA screen | **Surfaced on Home** as a one-line "how you grow best" headline, tapping into Training DNA (P2, `694ac06`) |
| Coach Dashboard (`CoachDashboardCard`) | Already on Home | Verified wired; confirmed loading/empty handling |
| Exercise library (516 exercises, fully tagged) | Single-select category filter | **Replaced with multi-select combinable filters** across muscle / equipment / difficulty / movement pattern, with live result count (P4, `da06d31`) |
| Program Generator (`ProgramGenerator`) | Fixed split logic | **Personalized by preferred split** (full body / upper-lower / push-pull-legs) chosen at onboarding (P7, `140a097`) |
| RPE / Tempo presets (`SetInputPresets`) | Wired in prior sprint | Confirmed reachable (P5, prior) |
| Recovery / Muscle-Gap / Forecast | On Training DNA screen | Confirmed reachable; Home mirroring deferred (see gaps) |

**Orphan check:** every domain engine has at least one UI consumer. No dead engines remain.

---

## 3. UX improvements

### Onboarding (P7, `140a097`)
- Added a **"Preferred workout style?"** question (No preference / Full body / Upper-Lower /
  Push-Pull-Legs).
- The answer flows through `OnboardingAnswers -> toRequest() -> ProgramRequest.style` and is
  applied in the 3-4 day hypertrophy/general branch of `ProgramGenerator`, and is persisted via
  `PreferencesManager` (`PROFILE_STYLE` key + `profileStyle` flow) so the choice survives.

### Exercise Library 2.0 (P4, `da06d31`)
- New pure-domain `domain/library/ExerciseFilter.kt`: `ExerciseFilterState` (muscles, equipment,
  difficulties, patterns, query), muscle-group chips that map to the library's fine-grained
  muscle vocabulary (e.g. "Shoulders" -> front/side/rear deltoids), and `apply()` semantics:
  muscle chips combine with **OR**, filter dimensions combine with **AND**, search is a further
  **AND**.
- `ExerciseViewModel` rewritten around a single `MutableStateFlow<ExerciseFilterState>` with
  `toggleMuscle/Equipment/Difficulty/Pattern`, `clearFilters`, and a live `resultCount`.
- Filter UI is a collapsible `FilterSection` (muscles always visible; equipment, difficulty and
  movement pattern revealed on expand) with a live result count and a Clear action. Exercise
  cards now show "Primary:" and "Secondary:" muscle lines.
- Filter logic was validated against the real 516-exercise dataset.

### Product polish (P8, `3a4ce2a`)
- Two shared components added to `CommonComponents.kt`: `LoadingState` (centered spinner +
  caption) and `InlineEmpty` (consistent muted inline empty text).
- **Loading-gated empty states:** Goals, History, Progress and Exercise Library no longer flash
  their "empty" state on first composition while the data flow is still emitting. The empty
  state now only renders once loading has completed and the data is genuinely empty.
- Exercise Library distinguishes "still loading" from "no matches": a non-empty filter that
  yields zero results correctly shows "No exercises found" rather than a spinner.
- `HistoryViewModel` gained an `isLoading` flag (true until its sessions flow first emits).
- Muted inline empty messages were standardized to `InlineEmpty` for visual consistency
  (Training DNA progression / plateau / no-data sections, the exercise insight empty, and the
  Home last-PR card).

---

## 4. Newly connected systems (summary)
- Goal Engine -> Home (goal card -> Goals).
- Training Genome -> Home (genome headline -> Training DNA).
- Training DNA -> reachable from Home (new entry point + back arrow).
- Multi-dimensional exercise filtering -> Exercise Library.
- Onboarding split preference -> Program Generator -> persisted profile.

---

## 5. Screens changed this sprint
| File | Change |
|---|---|
| `ui/navigation/NavGraph.kt` | Back-nav scaffold for secondary screens; bottom-bar gating; Training relabel; Training DNA entry point |
| `ui/home/HomeScreen.kt` + `HomeViewModel.kt` | Goal card, genome headline card, `onOpenTrainingDna`; consistent empty text |
| `ui/exercise/ExerciseLibraryScreen.kt` + `ExerciseViewModel.kt` | Multi-select combinable filters, muscle detail, loading gate |
| `domain/library/ExerciseFilter.kt` | NEW pure filter model + logic |
| `domain/templates/ProgramGenerator.kt` | `WorkoutStyle` enum + split personalization |
| `ui/onboarding/OnboardingScreen.kt` + `OnboardingViewModel.kt` | Preferred-style question, threaded to request + prefs |
| `util/PreferencesManager.kt` | `PROFILE_STYLE` key + `profileStyle` flow |
| `ui/components/CommonComponents.kt` | NEW `LoadingState` + `InlineEmpty` |
| `ui/goals/GoalsScreen.kt`, `ui/history/HistoryScreen.kt` (+ ViewModel), `ui/progress/ProgressScreen.kt`, `ui/trainingdna/TrainingDnaInsightScreen.kt` | Loading-gated / standardized empty states |
| `util/timer/RestTimerPrefs.kt` | DELETED (dead, comment-only, unreferenced) |
| `AUDIT_ERRORS_AND_GAPS.md` | NEW audit document |

---

## 6. Verification status
- Pure domain + model layer compiles clean under standalone `kotlinc 1.9.22` (with stubbed
  `androidx.room` annotations); post-sprint class count is consistent with the established
  baseline.
- Exercise filter logic was validated with logic tests against the real 516-exercise dataset
  (run in a throwaway sandbox; **no test files were committed** to keep CI test-free).
- UI Compose changes were verified statically: imports resolve, brace balance holds, shared
  components are defined once with matching usage/import counts, and no stale bare empty-text
  references remain.
- **Not verifiable here:** a real `:app:assembleDebug`. There is no Android SDK in this
  environment. **Action for the user: run `:app:assembleDebug` on an SDK host before release.**

---

## 7. Remaining launch gaps
1. **`:app:assembleDebug` not run** - the one verification that cannot be done in this
   environment. Highest-priority pre-release check.
2. **Recovery / Muscle-Gap / Forecast not mirrored on Home** - these still live only on the
   Training DNA screen. Home could surface a compact recovery/next-focus card.
3. **Per-goal-type workflow depth (P6 deeper)** - the Home goal card surfaces the top goal, but
   goal-type-specific coaching (strength vs reps vs bodyweight) could be richer.
4. **Empty-state copy** is functional but could be made more motivating / action-oriented with
   inline CTAs (e.g. "Add a goal" directly inside the Goals empty state).
5. **No automated UI tests** - by design (CI kept test-free); logic is validated in sandboxes
   only. A future hardening pass could add instrumented tests on an SDK host.

---

## 8. Ranked roadmap (next)
1. **Build verification** - run `:app:assembleDebug` on an SDK host; fix any
   environment-specific issues that only surface with the real toolchain. (Blocking for release.)
2. **Home recovery / next-focus card** - mirror the Recovery dashboard summary onto Home so the
   single most useful "what should I do today" signal is visible without drilling into DNA.
3. **Goal workflow depth** - per-goal-type detail and inline next-action on the Home goal card
   and Goals screen.
4. **Action-oriented empty states** - add inline CTAs to empty states (Goals, Exercises) to
   shorten time-to-first-value for new users.
5. **Onboarding completeness** - feed the persisted `profileStyle` into the Coach/recommendation
   path so the chosen split also shapes ongoing recommendations, not just the initial program.
6. **Test hardening** - instrumented UI smoke tests on an SDK host (kept out of the lightweight CI).

---

*All work was done locally on `sync/apply-improvements`; nothing was pushed. Regenerate the
patch bundle / changelog only once you confirm updates are complete.*
