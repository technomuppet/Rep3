package com.replog.util.profile

/**
 * The local user identity captured during onboarding. Stored in DataStore (never
 * Room) per the Sprint 12 architecture. Only [displayName] is required; every
 * other field is optional.
 *
 * Age is intentionally NOT a field: it is always derived from
 * [dateOfBirthEpochDay] via [ProfileMath.ageFrom] so it can never drift out of
 * date. Weight is stored canonically in kilograms and converted for display.
 */
data class UserProfile(
    val displayName: String,
    val dateOfBirthEpochDay: Long? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val useKg: Boolean = true,
    val experienceLevel: String? = null,
    val primaryGoal: String? = null,
    val weeklyFrequency: Int? = null,
    val equipment: String? = null
) {
    /** A profile is usable once a non-blank display name exists. */
    val isComplete: Boolean get() = displayName.isNotBlank()

    /** Trimmed, display-ready name; falls back to a neutral label if blank. */
    val safeName: String get() = displayName.trim().ifBlank { "Athlete" }
}

/** Pure profile maths kept out of the data class for testability. */
object ProfileMath {
    /**
     * Whole-years age from a date-of-birth epoch-day relative to [todayEpochDay].
     * Returns null when [dobEpochDay] is null or in the future. Uses a 365.2425
     * day year only as a guard; the precise calendar calc is done by the caller
     * passing epoch days computed from LocalDate, so this stays Android-free.
     */
    fun ageFrom(dobEpochDay: Long?, todayEpochDay: Long): Int? {
        if (dobEpochDay == null) return null
        if (dobEpochDay > todayEpochDay) return null
        val days = todayEpochDay - dobEpochDay
        return (days / 365.2425).toInt()
    }
}
