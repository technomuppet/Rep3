package com.replog.domain.recovery

import com.replog.util.profile.UserProfile
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Small, offline-only nutrition guideline helper for recovery surfaces.
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

    private fun formatLitres(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            "%.0f".format(Locale.US, rounded)
        } else {
            "%.1f".format(Locale.US, rounded)
        }
    }
}
