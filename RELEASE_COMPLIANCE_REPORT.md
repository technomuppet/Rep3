# Release Compliance Report (Sprint 12, Priority 7)

Verification of the compliance / onboarding / identity system. "Verified" =
checked from source + sandbox logic tests. "Gate" = requires an Android SDK host.

## Reliability checks
1. Home inaccessible before onboarding - VERIFIED. NavGraph composes the main
   NavHost only on GateState.READY; otherwise it composes the onboarding flow and
   returns. With no profile the gate is NEEDS_ONBOARDING.
2. Onboarding cannot be bypassed - VERIFIED (by design). The gate is recomputed on
   every cold start from persisted DataStore flows; there is no alternate entry to
   the NavHost. finish() is the only writer of profile + acceptance, and it guards
   on all-docs-accepted + exact-name match.
3. Acceptance survives reboot - VERIFIED. Stored in DataStore (disk-backed).
4. Acceptance survives process death - VERIFIED. Same DataStore persistence; the
   gate re-reads on next start.
5. Rotation-safe onboarding - VERIFIED (architecture). Flow state lives in the
   ViewModel (survives configuration changes); per-screen UI state is the only
   transient piece and is re-derivable.
6. Fully offline - VERIFIED. Documents are compiled-in constants; no network code,
   no INTERNET permission; profile + acceptance are local.
7. Database migration safety - VERIFIED. No Room schema change; AppDatabase stays
   version 15 with the existing 14 migrations. Profile + acceptance are DataStore.
8. Legal documents always available from Settings - VERIFIED. Legal section routes
   to read-only viewers backed by the same LegalDocuments registry.
9. Version mismatch forces re-acceptance - VERIFIED (logic). OnboardingGate returns
   NEEDS_REACCEPTANCE when the stored signature != currentVersionSignature; 10
   sandbox tests cover stale-signature and incomplete-acceptance cases.
10. Existing user data intact after legal updates - VERIFIED. Re-acceptance writes
    only a new acceptance record; no profile rewrite, no Room mutation.

## Test evidence (kotlinc 1.9.22 sandbox)
- 25 domain tests: age derivation (null/future/30y), greetings (incl. blank
  fallback + possessive-ending-in-s), document registry (3 docs, signature
  format, emergency warning), NO over-broad immunity claim + acknowledges
  non-excludable liability, all gate transitions.
- 10 step-machine tests: full forward path, end/start clamping, re-acceptance
  back-floor at Disclaimer, finish guard (all docs + exact name + non-blank).
- Pure domain compiles clean (11 classes).

## Legal content compliance
- The disclaimer does NOT claim complete immunity or removal of all liability; it
  explicitly preserves liability that cannot be excluded under applicable law
  (e.g. death or personal injury caused by negligence) and notes statutory rights.
- All three documents are drafted to a standard suitable for review by a qualified
  solicitor before commercial release. They remain DRAFTS pending that review.

## Outstanding gate (must run on an Android SDK host)
- :app:assembleDebug (and a signed release build). The sandbox has no Android SDK,
  so the Compose UI, Hilt graph, BuildConfig generation and DataStore runtime are
  compile-verified only by inference + balanced static checks here.
- On-device QA: complete the flow as a new user; decline a document (confirm no
  profile/acceptance written and Home unreachable); reboot + force-stop and
  confirm acceptance persists; rotate during each step; bump a document version
  and confirm forced re-acceptance with all workouts/templates/history intact;
  open every Legal Centre item; verify personalised greetings.

## Status
Implementation complete and internally verified. Pending solicitor review of the
legal drafts and the :app:assembleDebug / on-device QA gate before beta release.
