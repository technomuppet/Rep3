# Remaining Issues (Sprint 12)

All severities below are LOW and NONE block `:app:assembleDebug`.

## Trivial (warning-level, optional cleanup)
1. Unused import: `kotlinx.coroutines.flow.map` in
   app/src/main/java/com/replog/ui/navigation/RootGateViewModel.kt (the VM uses
   `combine`, not `map`). Kotlin warning only; does not fail the build. Safe to
   remove in a follow-up commit.

## By design (documented, intentionally retained)
2. Legacy `ONBOARDING_COMPLETE` DataStore key + `onboardingComplete` flow +
   `setOnboardingComplete()` are kept and set on finish(), but the entry gate now
   uses profile + legal acceptance instead. Retained for backward compatibility
   and any external readers; no behaviour depends on it for gating.
3. Bottom navigation bar remains hidden on secondary screens (including the new
   legal screens) - consistent with the existing Material detail-screen idiom
   (back arrow). Unchanged from prior sprints.

## Process (non-code, pre-release)
4. Legal documents (Health & Safety Disclaimer, Terms of Use, Privacy Policy) are
   DRAFTS and must be reviewed by a qualified solicitor before commercial release.
5. `:app:assembleDebug` + signed release build + on-device QA must be run on an
   Android SDK host (see RELEASE_COMPLIANCE_REPORT.md for the QA checklist).

## Deferred from earlier sprints (still open, unrelated to Sprint 12)
6. Shared SectionHeader, spacing/radius scale unification, Gson-streamed JSON
   backup, typed combine wrappers, large-composable extraction. None affect
   compliance/onboarding.
