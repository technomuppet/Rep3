package com.replog.domain.templates

/**
 * Offline rules-based program generator (Priority 1 — onboarding personalization,
 * also covers the "Workout Builder AI (offline rules)" idea).
 *
 * No LLM. Given the user's goal, available days, equipment and location, it
 * selects an appropriate split from the curated BuiltInTemplates catalog.
 * Pure, deterministic, unit-testable.
 */
enum class TrainingGoal { STRENGTH, HYPERTROPHY, FAT_LOSS, GENERAL }
enum class EquipmentAccess { FULL_GYM, DUMBBELLS_ONLY, BODYWEIGHT_ONLY }
enum class TrainingLevel { BEGINNER, INTERMEDIATE, ADVANCED }

data class ProgramRequest(
    val goal: TrainingGoal,
    val daysPerWeek: Int,
    val equipment: EquipmentAccess,
    val level: TrainingLevel
)

data class GeneratedProgram(
    val name: String,
    val rationale: String,
    val templateNames: List<String>
)

object ProgramGenerator {

    private fun byName(vararg names: String): List<String> = names.toList()

    fun generate(request: ProgramRequest): GeneratedProgram {
        val days = request.daysPerWeek.coerceIn(2, 6)

        // Equipment-constrained programs take priority — no point recommending
        // barbell splits to a bodyweight-only user.
        when (request.equipment) {
            EquipmentAccess.BODYWEIGHT_ONLY -> return GeneratedProgram(
                name = "Bodyweight Program",
                rationale = "Built from bodyweight movements you can do anywhere.",
                templateNames = if (request.level == TrainingLevel.ADVANCED)
                    byName("Calisthenics", "Home Beginner (Bodyweight)")
                else
                    byName("Home Beginner (Bodyweight)", "Calisthenics")
            )
            EquipmentAccess.DUMBBELLS_ONLY -> return GeneratedProgram(
                name = "Dumbbell Program",
                rationale = "Dumbbell-only split matched to your available days.",
                templateNames = if (days <= 3)
                    byName("Dumbbells Only - Full Body")
                else
                    byName("Adjustable Dumbbells - Upper", "Adjustable Dumbbells - Lower")
            )
            EquipmentAccess.FULL_GYM -> { /* fall through to goal-based selection */ }
        }

        return when (request.goal) {
            TrainingGoal.FAT_LOSS -> GeneratedProgram(
                name = "Fat Loss Program",
                rationale = "Full-body strength plus conditioning to preserve muscle while losing fat.",
                templateNames = if (days >= 4)
                    byName("Full Body Fat Loss", "Conditioning Circuit", "Full Body Fat Loss", "Conditioning Circuit")
                else
                    byName("Full Body Fat Loss", "Conditioning Circuit")
            )

            TrainingGoal.STRENGTH -> when {
                request.level == TrainingLevel.BEGINNER || days <= 3 -> GeneratedProgram(
                    name = "Strength 5x5",
                    rationale = "Linear 5x5 strength progression on the main barbell lifts, 3 days/week.",
                    templateNames = byName("5x5 Workout A", "5x5 Workout B", "5x5 Workout A")
                )
                days >= 4 -> GeneratedProgram(
                    name = "5/3/1 Strength",
                    rationale = "4-day 5/3/1 main-lift focus for intermediate/advanced lifters.",
                    templateNames = byName("5/3/1 Squat Day", "5/3/1 Bench Day", "5/3/1 Deadlift Day", "5/3/1 Press Day")
                )
                else -> GeneratedProgram(
                    name = "Strength 5x5",
                    rationale = "5x5 strength template.",
                    templateNames = byName("5x5 Workout A", "5x5 Workout B")
                )
            }

            // HYPERTROPHY and GENERAL share split logic driven by days/week.
            else -> when (days) {
                2 -> GeneratedProgram(
                    name = "2-Day Full Body",
                    rationale = "Two full-body sessions cover every muscle on a low-frequency schedule.",
                    templateNames = byName("Full Body A", "Full Body B")
                )
                3 -> GeneratedProgram(
                    name = "3-Day Full Body",
                    rationale = "Three full-body sessions — ideal frequency for most lifters.",
                    templateNames = byName("Full Body A", "Full Body B", "3 Day Strength")
                )
                4 -> GeneratedProgram(
                    name = "Upper / Lower",
                    rationale = "4-day Upper/Lower split balances volume and recovery.",
                    templateNames = byName("Upper", "Lower", "Upper", "Lower")
                )
                5 -> GeneratedProgram(
                    name = "Push / Pull / Legs + Upper / Lower",
                    rationale = "5-day hybrid maximises weekly volume for hypertrophy.",
                    templateNames = byName("Push (PPL)", "Pull (PPL)", "Legs (PPL)", "Upper", "Lower")
                )
                else -> GeneratedProgram(
                    name = "Push / Pull / Legs",
                    rationale = "6-day PPL run twice through the week for maximum hypertrophy frequency.",
                    templateNames = byName("Push (PPL)", "Pull (PPL)", "Legs (PPL)", "Push (PPL)", "Pull (PPL)", "Legs (PPL)")
                )
            }
        }
    }
}
