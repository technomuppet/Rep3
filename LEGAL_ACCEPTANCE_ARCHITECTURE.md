# Legal Acceptance Architecture (Sprint 12, Priorities 2/3/6)

## Documents
`util/legal/LegalDocuments.kt` - a pure registry of the three documents, each:

```
data class LegalDocument(
    val id: LegalDocId,           // DISCLAIMER | TERMS | PRIVACY
    val title: String,
    val version: String,          // e.g. "1.0.0" - the lock key
    val effectiveDate: String,    // ISO date
    val lastUpdated: String,      // ISO date
    val body: String              // full text shown in-app
)
```

- `LegalDocuments.all` = [disclaimer, terms, privacy].
- `LegalDocuments.currentVersionSignature` = a stable concatenation of the three
  versions, e.g. `"disclaimer=1.0.0;terms=1.0.0;privacy=1.0.0"`. This single
  string is what acceptance is compared against (Priority 6 version lock).
- App version is read from `BuildConfig.VERSION_NAME` (buildConfig enabled).

## Acceptance record (DataStore)
`util/legal/LegalAcceptance.kt`:

```
data class LegalAcceptance(
    val acceptedSignature: String,  // matches currentVersionSignature when valid
    val disclaimerVersion: String,
    val termsVersion: String,
    val privacyVersion: String,
    val acceptedAtEpochMillis: Long,
    val appVersion: String,
    val displayName: String,
    val completed: Boolean
)
```

DataStore keys (in PreferencesManager): `legal_accepted_signature`,
`legal_disclaimer_version`, `legal_terms_version`, `legal_privacy_version`,
`legal_accepted_at`, `legal_accepted_app_version`, `legal_accepted_name`,
`legal_completed`. Plus an append-only acceptance history (JSON list under
`legal_acceptance_history`) so Settings can show every acceptance, not just the
latest.

API: `val legalAcceptance: Flow<LegalAcceptance?>`,
`val legalAcceptanceHistory: Flow<List<LegalAcceptance>>`,
`suspend fun recordLegalAcceptance(record)`.

## The single gate (Priority 3 + 6)
`util/legal/OnboardingGate.kt` - a pure function:

```
fun gateState(profile: UserProfile?, acceptance: LegalAcceptance?,
              requiredSignature: String): GateState
// = NEEDS_ONBOARDING (no profile),
//   NEEDS_REACCEPTANCE (profile ok but acceptance.signature != required),
//   READY
```

`NavGraph` reads profile + acceptance flows via a small `RootGateViewModel` and:
- `READY` -> compose the main NavHost (Home reachable).
- otherwise -> compose the onboarding+legal flow and `return` (Home NOT composed,
  so it cannot be reached or deep-linked around).

Because the gate compares the CURRENT required signature against the stored one,
bumping any document version in `LegalDocuments` automatically forces
re-acceptance on next launch, while ALL Room data (workouts, history, templates,
progress) and other settings are preserved untouched.

## Onboarding + legal flow (Priority 3)
A step machine in `OnboardingViewModel`:
Welcome -> CreateProfile -> TrainingPreferences -> Disclaimer -> Terms ->
Privacy -> FinalConfirmation -> (commit) -> Home.

- Each legal step: must scroll to the bottom (tracked via list state) to enable
  the confirmation checkbox; checkbox must be ticked to enable Continue.
- Decline on any document: cancel the flow, write NOTHING (no profile, no
  acceptance), return to Welcome. Home remains unreachable.
- Final confirmation: user must type the display name EXACTLY (case-sensitive)
  to enable Finish. Finish atomically writes the profile, the acceptance record
  (with the current signature, timestamp, app version, name, completed=true),
  appends to history, and (for new users) installs the generated program.

## Reliability (Priority 7)
- Persistence is DataStore -> survives reboot and process death.
- Gate is recomputed on every cold start from persisted flows -> cannot be
  bypassed; rotation-safe because flow state is held in ViewModels.
- 100% offline: documents are compiled-in constants; no network.
- No Room migration -> migration safety preserved; existing data intact.
