package com.replog.domain.templates

/**
 * Curated built-in workout templates (Priority 1 — "Templates are weak").
 *
 * Pure data. Each entry references exercises by their exact name in
 * exercises.json, so the seeder can resolve them to ids. Exercises that
 * cannot be resolved are skipped gracefully by the seeder.
 *
 * Categories map to the spec: Beginner, Hypertrophy, Strength, Bodyweight,
 * Home Gym, Fat Loss.
 */
data class TemplateExerciseSpec(
    val exerciseName: String,
    val sets: Int,
    val reps: Int
)

data class BuiltInTemplate(
    val name: String,
    val group: String,
    val exercises: List<TemplateExerciseSpec>
)

object BuiltInTemplates {

    private fun e(name: String, sets: Int, reps: Int) = TemplateExerciseSpec(name, sets, reps)

    val ALL: List<BuiltInTemplate> = listOf(
        // ---------------- BEGINNER ----------------
        BuiltInTemplate("Full Body A", "Beginner", listOf(
            e("Barbell Back Squat", 3, 8),
            e("Barbell Bench Press", 3, 8),
            e("Barbell Bent Over Row", 3, 8),
            e("Dumbbell Shoulder Press", 3, 10),
            e("Plank", 3, 30)
        )),
        BuiltInTemplate("Full Body B", "Beginner", listOf(
            e("Barbell Deadlift", 3, 6),
            e("Barbell Overhead Press", 3, 8),
            e("Lat Pulldown", 3, 10),
            e("Leg Press", 3, 12),
            e("Hanging Leg Raise", 3, 12)
        )),
        BuiltInTemplate("3 Day Strength", "Beginner", listOf(
            e("Barbell Back Squat", 3, 5),
            e("Barbell Bench Press", 3, 5),
            e("Barbell Deadlift", 1, 5),
            e("Barbell Bent Over Row", 3, 6),
            e("Barbell Overhead Press", 3, 6)
        )),

        // ---------------- HYPERTROPHY ----------------
        BuiltInTemplate("Push (PPL)", "Hypertrophy", listOf(
            e("Barbell Bench Press", 4, 8),
            e("Incline Dumbbell Press", 3, 10),
            e("Dumbbell Shoulder Press", 3, 10),
            e("Cable Lateral Raise", 3, 15),
            e("Cable Chest Fly", 3, 12),
            e("Cable Rope Pushdown", 3, 12)
        )),
        BuiltInTemplate("Pull (PPL)", "Hypertrophy", listOf(
            e("Pull Ups", 3, 8),
            e("Barbell Bent Over Row", 4, 8),
            e("Seated Cable Row", 3, 10),
            e("Face Pulls", 3, 15),
            e("Incline Dumbbell Curl", 3, 12),
            e("Hammer Curls", 3, 12)
        )),
        BuiltInTemplate("Legs (PPL)", "Hypertrophy", listOf(
            e("Barbell Back Squat", 4, 8),
            e("Barbell Romanian Deadlift", 3, 10),
            e("Leg Press", 3, 12),
            e("Seated Leg Curl", 3, 12),
            e("Standing Calf Raise", 4, 15)
        )),
        BuiltInTemplate("Upper", "Hypertrophy", listOf(
            e("Barbell Bench Press", 4, 8),
            e("Barbell Bent Over Row", 4, 8),
            e("Dumbbell Shoulder Press", 3, 10),
            e("Lat Pulldown", 3, 10),
            e("Incline Dumbbell Curl", 3, 12),
            e("Cable Rope Pushdown", 3, 12)
        )),
        BuiltInTemplate("Lower", "Hypertrophy", listOf(
            e("Barbell Back Squat", 4, 8),
            e("Barbell Romanian Deadlift", 3, 10),
            e("Bulgarian Split Squat", 3, 10),
            e("Leg Extension", 3, 15),
            e("Standing Calf Raise", 4, 15)
        )),
        BuiltInTemplate("Chest & Triceps (Bro Split)", "Hypertrophy", listOf(
            e("Barbell Bench Press", 4, 8),
            e("Incline Dumbbell Press", 3, 10),
            e("Cable Chest Fly", 3, 12),
            e("Chest Dips", 3, 10),
            e("Cable Rope Pushdown", 3, 12),
            e("Dumbbell Overhead Tricep Extension", 3, 12)
        )),
        BuiltInTemplate("Back & Biceps (Bro Split)", "Hypertrophy", listOf(
            e("Pull Ups", 3, 8),
            e("Barbell Bent Over Row", 4, 8),
            e("Lat Pulldown", 3, 10),
            e("Seated Cable Row", 3, 12),
            e("Dumbbell Bicep Curl", 3, 12),
            e("Hammer Curls", 3, 12)
        )),
        BuiltInTemplate("Shoulders & Arms (Bro Split)", "Hypertrophy", listOf(
            e("Barbell Overhead Press", 4, 8),
            e("Cable Lateral Raise", 4, 15),
            e("Face Pulls", 3, 15),
            e("Incline Dumbbell Curl", 3, 12),
            e("Cable Rope Pushdown", 3, 12)
        )),
        BuiltInTemplate("Legs (Bro Split)", "Hypertrophy", listOf(
            e("Barbell Back Squat", 4, 10),
            e("Leg Press", 3, 12),
            e("Seated Leg Curl", 3, 12),
            e("Leg Extension", 3, 15),
            e("Standing Calf Raise", 4, 15)
        )),

        // ---------------- STRENGTH ----------------
        BuiltInTemplate("5x5 Workout A", "Strength", listOf(
            e("Barbell Back Squat", 5, 5),
            e("Barbell Bench Press", 5, 5),
            e("Barbell Bent Over Row", 5, 5)
        )),
        BuiltInTemplate("5x5 Workout B", "Strength", listOf(
            e("Barbell Back Squat", 5, 5),
            e("Barbell Overhead Press", 5, 5),
            e("Barbell Deadlift", 1, 5)
        )),
        BuiltInTemplate("Texas Method - Volume", "Strength", listOf(
            e("Barbell Back Squat", 5, 5),
            e("Barbell Bench Press", 5, 5),
            e("Barbell Bent Over Row", 5, 5)
        )),
        BuiltInTemplate("Texas Method - Intensity", "Strength", listOf(
            e("Barbell Back Squat", 1, 5),
            e("Barbell Overhead Press", 1, 5),
            e("Barbell Deadlift", 1, 5)
        )),
        BuiltInTemplate("5/3/1 Squat Day", "Strength", listOf(
            e("Barbell Back Squat", 3, 5),
            e("Leg Press", 5, 10),
            e("Seated Leg Curl", 5, 10),
            e("Hanging Leg Raise", 3, 12)
        )),
        BuiltInTemplate("5/3/1 Bench Day", "Strength", listOf(
            e("Barbell Bench Press", 3, 5),
            e("Incline Dumbbell Press", 5, 10),
            e("Cable Rope Pushdown", 5, 12),
            e("Dumbbell Bicep Curl", 5, 12)
        )),
        BuiltInTemplate("5/3/1 Deadlift Day", "Strength", listOf(
            e("Barbell Deadlift", 3, 5),
            e("Barbell Romanian Deadlift", 5, 10),
            e("Pull Ups", 5, 8),
            e("Hanging Leg Raise", 3, 12)
        )),
        BuiltInTemplate("5/3/1 Press Day", "Strength", listOf(
            e("Barbell Overhead Press", 3, 5),
            e("Dumbbell Shoulder Press", 5, 10),
            e("Cable Lateral Raise", 5, 15),
            e("Face Pulls", 5, 15)
        )),

        // ---------------- BODYWEIGHT ----------------
        BuiltInTemplate("Home Beginner (Bodyweight)", "Bodyweight", listOf(
            e("Bodyweight Squat", 3, 15),
            e("Push Ups", 3, 12),
            e("Inverted Row", 3, 10),
            e("Glute Bridge", 3, 15),
            e("Plank", 3, 30)
        )),
        BuiltInTemplate("Calisthenics", "Bodyweight", listOf(
            e("Pull Ups", 4, 8),
            e("Chest Dips", 4, 10),
            e("Pistol Squat", 3, 6),
            e("Pike Push Ups", 3, 10),
            e("Hanging Leg Raise", 3, 12)
        )),

        // ---------------- HOME GYM ----------------
        BuiltInTemplate("Dumbbells Only - Full Body", "Home Gym", listOf(
            e("Goblet Squat", 3, 12),
            e("Dumbbell Bench Press", 3, 10),
            e("Dumbbell Row", 3, 10),
            e("Dumbbell Shoulder Press", 3, 10),
            e("Dumbbell Romanian Deadlift", 3, 12),
            e("Hammer Curls", 3, 12)
        )),
        BuiltInTemplate("Adjustable Dumbbells - Upper", "Home Gym", listOf(
            e("Dumbbell Bench Press", 4, 10),
            e("Dumbbell Row", 4, 10),
            e("Arnold Press", 3, 10),
            e("Incline Dumbbell Curl", 3, 12),
            e("Dumbbell Overhead Tricep Extension", 3, 12)
        )),
        BuiltInTemplate("Adjustable Dumbbells - Lower", "Home Gym", listOf(
            e("Goblet Squat", 4, 12),
            e("Bulgarian Split Squat", 3, 10),
            e("Dumbbell Romanian Deadlift", 3, 12),
            e("Dumbbell Walking Lunge", 3, 12),
            e("Dumbbell Calf Raise", 4, 15)
        )),

        // ---------------- FAT LOSS ----------------
        BuiltInTemplate("Full Body Fat Loss", "Fat Loss", listOf(
            e("Goblet Squat", 3, 15),
            e("Dumbbell Bench Press", 3, 12),
            e("Dumbbell Row", 3, 12),
            e("Dumbbell Walking Lunge", 3, 12),
            e("Mountain Climbers", 3, 30),
            e("Plank", 3, 40)
        )),
        BuiltInTemplate("Conditioning Circuit", "Fat Loss", listOf(
            e("Kettlebell Swing", 4, 20),
            e("Burpees", 4, 12),
            e("Jump Rope", 4, 60),
            e("Mountain Climbers", 4, 30),
            e("Bicycle Crunches", 4, 20)
        ))
    )

    /** Names of the entry-level templates seeded for every install. */
    val DEFAULT_SEED_NAMES: Set<String> = setOf(
        "Full Body A", "Full Body B", "3 Day Strength"
    )
}
