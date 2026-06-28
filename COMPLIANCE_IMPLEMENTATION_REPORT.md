# Compliance Implementation Report (Sprint 12)

Branch sync/apply-improvements. Audit-first; DataStore for profile + legal
acceptance; Room untouched (still v15, zero new migrations); 100% offline.

## Phase 0 - Audit (committed first, no code)
COMPLIANCE_PHASE0_AUDIT.md, USER_PROFILE_ARCHITECTURE.md,
LEGAL_ACCEPTANCE_ARCHITECTURE.md. Confirmed: single-boolean nav gate, no profile
identity, no in-app legal docs, buildConfig disabled, Home unreachable while the
gate is unsatisfied.

## Priority 1 - User Identity (DataStore)
- util/profile/UserProfile.kt: displayName REQUIRED; optional DOB (epoch day),
  height, weight (canonical kg), units, experience, goal, weekly frequency,
  equipment. Age is ALWAYS derived from DOB (ProfileMath.ageFrom), never stored.
- PreferencesManager: userProfile / displayName / hasProfile flows; atomic
  setUserProfile(); units + training prefs reuse existing keys (no duplication).
- Personalisation: util/profile/Greetings.kt (deterministic, hour passed in).
  Home header shows "Good morning, <Name>." + "<Name>'s training dashboard";
  Recovery Centre shows "<Name>'s Recovery Centre". Neutral fallback if no name.

## Priority 2 - Versioned Legal Framework
- util/legal/LegalDocuments.kt + LegalTexts.kt: registry of Disclaimer/Terms/
  Privacy, each with version, effectiveDate, lastUpdated and full body. App
  version read at runtime via BuildConfig (buildConfig now enabled) through
  util/AppInfo.kt. currentVersionSignature is the lock key.
- LegalAcceptance record: accepted signature + per-doc versions, timestamp,
  app version, display name, completed flag. Stored in DataStore plus an
  append-only history (line/tab-delimited, capped 50) for the Settings screen.

## Priority 3 - Mandatory Legal Acceptance
- OnboardingViewModel step machine: Welcome -> Create Profile -> Training
  Preferences -> Disclaimer -> Terms -> Privacy -> Final Confirmation.
- Each legal step: must scroll to the bottom (derivedState on list layout) to
  enable the checkbox; checkbox enables Continue; Decline cancels the whole flow
  (nothing persisted: no profile, no acceptance) and returns to the start.
- Final Confirmation: Finish enabled only when the typed name matches the chosen
  display name EXACTLY and all three documents are accepted. finish() then writes
  the profile (new users), records acceptance, installs the generated program.
- The gate (RootGateViewModel + pure OnboardingGate) is evaluated at the single
  NavGraph chokepoint; Home (the NavHost) is never composed until READY.

## Priority 4 - Health & Safety Disclaimer
- Drafted for solicitor review. States: educational tool; not medical advice;
  does not diagnose/treat/prevent disease; user decides suitability; consult
  professionals; inherent risks; voluntary/own risk; limits responsibility only
  to the extent permitted by law and does NOT exclude non-excludable liability
  (e.g. death/personal injury by negligence); stop-and-seek-help symptom list.
  A prominent EMERGENCY_WARNING is shown immediately before acceptance.

## Priority 5 - Legal Centre (Settings)
- New "Legal" section: Health & Safety Disclaimer, Terms of Use, Privacy Policy
  (read-only viewers), Acceptance History (date/time/accepted versions/app
  version, read-only), App Version, Open Source Licences.

## Priority 6 - Future Version Handling
- Bumping any LegalDocument.version changes currentVersionSignature; on next cold
  start OnboardingGate returns NEEDS_REACCEPTANCE; NavGraph starts the flow in
  re-acceptance mode (profile pre-filled, starts at Disclaimer). All Room data
  (workouts, history, templates, progress) and other settings are preserved; only
  a new acceptance record is written.

## Verification (Priority 7)
See RELEASE_COMPLIANCE_REPORT.md. Sandbox: 25 domain tests + 10 step-machine
tests pass under kotlinc 1.9.22; pure domain compiles (11 classes). The Compose
UI and Hilt graph require :app:assembleDebug on an Android SDK host - the single
outstanding gate.

## Commits
Phase 0 docs; Phase 1 domain; Phase 2 DataStore; Phase 3 gated flow; Phase 4/5
Legal Centre + greetings; Phase 5 Recovery Centre title; Phase 6 deliverables.
