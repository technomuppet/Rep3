package com.replog.domain.recovery

import com.replog.util.profile.UserProfile
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Offline-only nutrition guidance for recovery surfaces.
 *
 * This is general training guidance, not medical or dietary advice. It deliberately
 * returns no estimate when the profile weight is missing or outside a conservative
 * validation range, rather than guessing from incomplete data.
 */
object NutritionGuidelines {

    private const val MIN_VALID_WEIGHT_KG = 30.0
    private const val MAX_VALID_WEIGHT_KG = 300.0
    private const val PROTEIN_MIN_G_PER_KG = 1.6
    private const val PROTEIN_MAX_G_PER_KG = 2.2
    private const val WATER_LITRES_PER_KG = 0.033
    private const val MIN_WATER_LITRES = 2.0
    private const val FAT_G_PER_KG = 1.0
    private const val LONG_SESSION_MINUTES = 60
    private const val VERY_LONG_SESSION_MINUTES = 90

    /**
     * Returns one concise guideline suitable for the Recovery Centre, or null
     * when the profile does not contain a trustworthy weight.
     */
    fun recoveryGuideline(profile: UserProfile): String? {
        val weightKg = profile.weightKg ?: return null
        if (weightKg !in MIN_VALID_WEIGHT_KG..MAX_VALID_WEIGHT_KG) return null

        val proteinMin = (weightKg * PROTEIN_MIN_G_PER_KG).roundToInt()
        val proteinMax = (weightKg * PROTEIN_MAX_G_PER_KG).roundToInt()
        val waterLitres = (weightKg * WATER_LITRES_PER_KG)
            .coerceAtLeast(MIN_WATER_LITRES)
        val context = goalContext(profile.primaryGoal)

        return "$context, consider roughly ${proteinMin}-${proteinMax}g of protein and ~${formatLitres(waterLitres)}L of water today."
    }

    /**
     * Structured daily macro and hydration targets, goal-aware, with an optional
     * hydration bump for longer sessions. Null when the weight is not usable.
     */
    fun dailyTargets(profile: UserProfile, workoutDurationMinutes: Int? = null): MacroTargets? {
        val weightKg = profile.weightKg ?: return null
        if (weightKg !in MIN_VALID_WEIGHT_KG..MAX_VALID_WEIGHT_KG) return null

        val (proteinMin, proteinMax) = proteinRangeForGoal(profile.primaryGoal, weightKg)
        val carbGrams = carbGramsForGoal(profile.primaryGoal, weightKg)
        val fatGrams = (weightKg * FAT_G_PER_KG).roundToInt()
        val durationBonus = when {
            workoutDurationMinutes == null -> 0.0
            workoutDurationMinutes >= VERY_LONG_SESSION_MINUTES -> 1.0
            workoutDurationMinutes >= LONG_SESSION_MINUTES -> 0.5
            else -> 0.0
        }
        val waterLitres = (weightKg * WATER_LITRES_PER_KG)
            .coerceAtLeast(MIN_WATER_LITRES) + durationBonus

        return MacroTargets(
            proteinMinGrams = proteinMin,
            proteinMaxGrams = proteinMax,
            carbGrams = carbGrams,
            fatGrams = fatGrams,
            waterLitres = waterLitres
        )
    }

    /**
     * Everything a nutrition surface needs: the daily targets summary, a
     * pre-workout fuel tip and a post-workout refuel tip. Null when the profile
     * weight is not usable.
     */
    fun dailyGuidance(profile: UserProfile, workoutDurationMinutes: Int? = null): NutritionGuidance? {
        val targets = dailyTargets(profile, workoutDurationMinutes) ?: return null
        val summary = buildString {
            append("Daily recovery targets: ${targets.proteinMinGrams}–${targets.proteinMaxGrams}g protein")
            append(", ~${targets.carbGrams}g carbs")
            append(", ~${targets.fatGrams}g fat")
            append(" and ~${formatLitres(targets.waterLitres)}L water.")
        }
        return NutritionGuidance(
            macroTargets = targets,
            summary = summary,
            preWorkoutTip = preWorkoutTip(workoutDurationMinutes),
            postWorkoutTip = postWorkoutTip(profile, workoutDurationMinutes)
        )
    }

    /** What to eat before training; adds a mid-workout carb tip for long sessions. */
    fun preWorkoutTip(workoutDurationMinutes: Int? = null): String {
        val base = "Have a light meal or snack with some carbs and protein 1–2 hours before training."
        return if (workoutDurationMinutes != null && workoutDurationMinutes >= VERY_LONG_SESSION_MINUTES) {
            "$base Keep a quick carb option (e.g. a banana or sports drink) handy mid-workout."
        } else {
            base
        }
    }

    /** Post-workout refuel guidance; reminds about fluids for longer sessions. */
    fun postWorkoutTip(profile: UserProfile, workoutDurationMinutes: Int? = null): String? {
        val weightKg = profile.weightKg ?: return null
        if (weightKg !in MIN_VALID_WEIGHT_KG..MAX_VALID_WEIGHT_KG) return null
        val (proteinMin, proteinMax) = proteinRangeForGoal(profile.primaryGoal, weightKg)
        val base = "Within 1–2 hours of training, eat a protein-rich meal (~${proteinMin}–${proteinMax}g protein across the day) with carbs to refill glycogen."
        return if (workoutDurationMinutes != null && workoutDurationMinutes >= LONG_SESSION_MINUTES) {
            "$base Replenish fluids and electrolytes after longer sessions."
        } else {
            base
        }
    }

    /** Goal-specific protein range (g/kg). Fat-loss users get more to spare lean mass. */
    private fun proteinRangeForGoal(goal: String?, weightKg: Double): Pair<Int, Int> {
        val normalized = goal.orEmpty().lowercase(Locale.US)
        val (low, high) = when {
            normalized.contains("fat") || normalized.contains("loss") -> 2.0 to 2.4
            normalized.contains("endur") || normalized.contains("cardio") -> 1.4 to 1.8
            normalized.contains("hypertrophy") || normalized.contains("muscle") -> 1.6 to 2.2
            else -> 1.6 to 2.0
        }
        return (weightKg * low).roundToInt() to (weightKg * high).roundToInt()
    }

    /** Goal-specific daily carbohydrate target (g/kg). */
    private fun carbGramsForGoal(goal: String?, weightKg: Double): Int {
        val normalized = goal.orEmpty().lowercase(Locale.US)
        val perKg = when {
            normalized.contains("endur") || normalized.contains("cardio") -> 5.0
            normalized.contains("hypertrophy") || normalized.contains("muscle") -> 4.0
            normalized.contains("fat") || normalized.contains("loss") -> 2.5
            else -> 3.5
        }
        return (weightKg * perKg).roundToInt()
    }

    private fun goalContext(goal: String?): String {
        val normalized = goal.orEmpty().lowercase(Locale.US)
        return when {
            normalized.contains("hypertrophy") || normalized.contains("muscle") ->
                "To support muscle repair"
            normalized.contains("fat") || normalized.contains("loss") ->
                "To support recovery while preserving lean mass"
            else ->
                "To support daily recovery"
        }
    }

    /** Formats litres with one decimal, omitting a trailing zero (2.0 → "2", 2.6 → "2.6"). */
    fun formatLitres(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            "%.0f".format(Locale.US, rounded)
        } else {
            "%.1f".format(Locale.US, rounded)
        }
    }
}

/** Daily protein/carb/fat/water targets for one user. */
data class MacroTargets(
    val proteinMinGrams: Int,
    val proteinMaxGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int,
    val waterLitres: Double
)

/** Bundle used by nutrition surfaces: daily targets plus pre/post-workout tips. */
data class NutritionGuidance(
    val macroTargets: MacroTargets,
    val summary: String,
    val preWorkoutTip: String?,
    val postWorkoutTip: String?
)
