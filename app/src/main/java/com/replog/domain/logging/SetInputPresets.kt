package com.replog.domain.logging

/**
 * Priority 2 — friendlier set-logging inputs (RPE + Tempo).
 *
 * Pure data/helpers so the UI can show plain-language choices while still
 * storing the same SetLog.rpe (Double?) and SetLog.tempo (String?) values the
 * rest of the app already understands. No schema or analytics changes.
 */

/** Labeled RPE options shown as a dropdown/chips instead of free-text entry. */
data class RpeOption(val value: Double?, val label: String)

object RpePresets {
    val OPTIONS: List<RpeOption> = listOf(
        RpeOption(null, "No RPE"),
        RpeOption(6.0, "RPE 6 · Easy"),
        RpeOption(7.0, "RPE 7 · Moderate"),
        RpeOption(8.0, "RPE 8 · Hard"),
        RpeOption(9.0, "RPE 9 · Very Hard"),
        RpeOption(10.0, "RPE 10 · Failure")
    )

    /** Map a stored rpe value back to the closest preset label (for display/selection). */
    fun labelFor(value: Double?): String =
        OPTIONS.firstOrNull { it.value == value?.let { v -> Math.round(v).toDouble() } }?.label
            ?: value?.let { "RPE ${trimmed(it)}" }
            ?: "No RPE"

    private fun trimmed(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
}

/**
 * Named tempo presets that translate to standard eccentric-pause-concentric
 * notation behind the scenes. CUSTOM allows manual notation entry.
 */
data class TempoOption(val key: String, val label: String, val notation: String?) {
    val isCustom: Boolean get() = key == "CUSTOM"
}

object TempoPresets {
    val OPTIONS: List<TempoOption> = listOf(
        TempoOption("NONE", "Not tracked", null),
        TempoOption("NORMAL", "Normal", "2-0-2"),
        TempoOption("CONTROLLED", "Controlled", "3-1-2"),
        TempoOption("SLOW_ECCENTRIC", "Slow Eccentric", "4-1-2"),
        TempoOption("PAUSE", "Pause Reps", "3-2-1"),
        TempoOption("CUSTOM", "Custom", null)
    )

    /**
     * Given a stored tempo notation string, choose which preset it represents.
     * A blank/null tempo -> NONE; a known notation -> its named preset;
     * any other non-blank notation -> CUSTOM.
     */
    fun optionForNotation(notation: String?): TempoOption {
        val n = notation?.trim().orEmpty()
        if (n.isBlank()) return OPTIONS.first { it.key == "NONE" }
        return OPTIONS.firstOrNull { it.notation == n } ?: OPTIONS.first { it.key == "CUSTOM" }
    }

    /** Resolve the tempo string to persist for a chosen preset + custom text. */
    fun resolveNotation(option: TempoOption, customText: String): String? = when {
        option.key == "NONE" -> null
        option.isCustom -> customText.trim().ifBlank { null }
        else -> option.notation
    }
}
