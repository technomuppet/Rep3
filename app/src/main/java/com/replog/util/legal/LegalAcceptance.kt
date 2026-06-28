package com.replog.util.legal

/**
 * An immutable record that the user accepted the legal documents. Stored in
 * DataStore (latest record) and appended to an on-device history list.
 */
data class LegalAcceptance(
    val acceptedSignature: String,
    val disclaimerVersion: String,
    val termsVersion: String,
    val privacyVersion: String,
    val acceptedAtEpochMillis: Long,
    val appVersion: String,
    val displayName: String,
    val completed: Boolean
) {
    /** Valid only while it matches the current registry signature. */
    fun isCurrent(requiredSignature: String): Boolean =
        completed && acceptedSignature == requiredSignature
}

/** Result of evaluating whether the app may be entered. */
enum class GateState {
    /** No usable profile yet -> run the full onboarding + legal flow. */
    NEEDS_ONBOARDING,
    /** Profile exists but legal versions changed -> require re-acceptance only. */
    NEEDS_REACCEPTANCE,
    /** Profile present and current legal versions accepted -> enter the app. */
    READY
}

/**
 * The single source of truth for whether Home may be shown. Pure and
 * deterministic so it can be unit-tested and recomputed on every cold start.
 */
object OnboardingGate {
    fun evaluate(
        hasProfile: Boolean,
        acceptance: LegalAcceptance?,
        requiredSignature: String
    ): GateState = when {
        !hasProfile -> GateState.NEEDS_ONBOARDING
        acceptance == null || !acceptance.isCurrent(requiredSignature) -> GateState.NEEDS_REACCEPTANCE
        else -> GateState.READY
    }
}
