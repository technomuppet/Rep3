package com.replog.util.legal

/** The three legal documents the user must accept before using RepLog. */
enum class LegalDocId { DISCLAIMER, TERMS, PRIVACY }

/**
 * A version-controlled legal document. Bumping [version] on any document changes
 * the registry signature and forces re-acceptance on next launch (Priority 6),
 * without touching any stored workout/history/template data.
 */
data class LegalDocument(
    val id: LegalDocId,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val lastUpdated: String,
    val body: String
)

/**
 * Compiled-in registry of legal documents (100% offline; no network fetch). The
 * texts are drafted to a standard suitable for review by a qualified solicitor
 * before commercial release. They deliberately do NOT claim that the disclaimer
 * removes all liability or grants immunity; they acknowledge that liability
 * cannot be excluded where prohibited by applicable law.
 */
object LegalDocuments {

    const val APP_NAME = "RepLog"
    private const val EFFECTIVE = "2026-06-28"
    private const val UPDATED = "2026-06-28"

    val disclaimer = LegalDocument(
        id = LegalDocId.DISCLAIMER,
        title = "Health & Safety Disclaimer",
        version = "1.0.0",
        effectiveDate = EFFECTIVE,
        lastUpdated = UPDATED,
        body = DISCLAIMER_BODY
    )

    val terms = LegalDocument(
        id = LegalDocId.TERMS,
        title = "Terms of Use",
        version = "1.0.0",
        effectiveDate = EFFECTIVE,
        lastUpdated = UPDATED,
        body = TERMS_BODY
    )

    val privacy = LegalDocument(
        id = LegalDocId.PRIVACY,
        title = "Privacy Policy",
        version = "1.0.0",
        effectiveDate = EFFECTIVE,
        lastUpdated = UPDATED,
        body = PRIVACY_BODY
    )

    /** Order matters: this is the onboarding acceptance order. */
    val all: List<LegalDocument> = listOf(disclaimer, terms, privacy)

    fun byId(id: LegalDocId): LegalDocument = when (id) {
        LegalDocId.DISCLAIMER -> disclaimer
        LegalDocId.TERMS -> terms
        LegalDocId.PRIVACY -> privacy
    }

    /**
     * A stable signature of all current document versions. Acceptance is valid
     * only while a stored signature equals this value. Changing any [version]
     * changes this signature and triggers re-acceptance.
     */
    val currentVersionSignature: String
        get() = "disclaimer=${disclaimer.version};terms=${terms.version};privacy=${privacy.version}"

    /**
     * A prominent emergency warning shown immediately before the disclaimer can be
     * accepted (Priority 4).
     */
    const val EMERGENCY_WARNING: String =
        "STOP IMMEDIATELY AND SEEK URGENT MEDICAL HELP if during or after exercise " +
            "you experience chest pain or pressure, pain spreading to the arm, neck or " +
            "jaw, severe shortness of breath, dizziness, fainting, an irregular or " +
            "racing heartbeat, sudden severe pain, or any other symptom that concerns " +
            "you. In an emergency call your local emergency number immediately."
}
