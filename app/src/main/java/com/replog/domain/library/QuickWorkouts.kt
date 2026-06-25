package com.replog.domain.library

/**
 * Sprint 5 - Quick Workout Library (P2/P3).
 *
 * A curated, professionally structured catalogue of single-session workouts and
 * the days that make up famous programmes. Each entry references real exercises
 * from the bundled library by name (so it can be duplicated into the user's own
 * templates and resolve concrete exercises), and carries rich metadata used for
 * filtering: category, goal, experience level, estimated duration and equipment.
 *
 * This is pure data + filtering logic - no Android dependency - so it is fully
 * testable and offline. Built-in programmes are never modified by the user; the
 * UI duplicates a chosen workout into a new editable template.
 */

enum class WorkoutCategory(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    STRENGTH("Strength"),
    HYPERTROPHY("Hypertrophy"),
    POWERLIFTING("Powerlifting"),
    BODYBUILDING("Bodybuilding"),
    GENERAL_FITNESS("General Fitness"),
    HOME("Home Workouts"),
    DUMBBELL_ONLY("Dumbbell Only"),
    BARBELL_ONLY("Barbell Only"),
    MACHINE_ONLY("Machine Only"),
    UPPER_LOWER("Upper Lower"),
    PUSH_PULL_LEGS("Push Pull Legs"),
    FULL_BODY("Full Body"),
    FIVE_BY_FIVE("5x5"),
    MINIMAL_EQUIPMENT("Minimal Equipment"),
    TIME_LIMITED("Time Limited")
}

enum class WorkoutGoal(val label: String) {
    BUILD_MUSCLE("Build Muscle"),
    LOSE_FAT("Lose Fat"),
    INCREASE_STRENGTH("Increase Strength"),
    IMPROVE_CONDITIONING("Improve Conditioning"),
    ATHLETIC_PERFORMANCE("Athletic Performance"),
    GENERAL_HEALTH("General Health"),
    BEGINNER_CONFIDENCE("Beginner Confidence"),
    RETURN_AFTER_INJURY("Return After Injury"),
    BUSY_SCHEDULE("Busy Schedule")
}

enum class WorkoutEquipment(val label: String) {
    COMMERCIAL_GYM("Commercial Gym"),
    HOME_GYM("Home Gym"),
    DUMBBELLS("Dumbbells"),
    BARBELL("Barbell"),
    RESISTANCE_BANDS("Resistance Bands"),
    BODYWEIGHT("Bodyweight"),
    MACHINES("Machines")
}

/** One programmed exercise inside a quick workout. */
data class QuickWorkoutExercise(
    val exerciseName: String,
    val sets: Int,
    val reps: String,        // e.g. "5", "8-12", "AMRAP"
    val restSeconds: Int
)

/** A single session that can be started or duplicated into a template. */
data class QuickWorkout(
    val id: String,
    val name: String,
    val description: String,
    val level: WorkoutCategory,            // experience level (Beginner/Intermediate/Advanced)
    val categories: Set<WorkoutCategory>,  // every category chip this workout matches
    val goals: Set<WorkoutGoal>,
    val equipment: Set<WorkoutEquipment>,
    val estimatedMinutes: Int,
    val programmeName: String? = null,     // non-null when part of a multi-day programme
    val progressionNotes: String,
    val exercises: List<QuickWorkoutExercise>
) {
    val durationBucket: Int
        get() = when {
            estimatedMinutes <= 20 -> 20
            estimatedMinutes <= 30 -> 30
            estimatedMinutes <= 45 -> 45
            estimatedMinutes <= 60 -> 60
            else -> 90
        }
}

/** A named multi-day programme (e.g. StrongLifts 5x5) made of QuickWorkouts. */
data class WorkoutProgramme(
    val id: String,
    val name: String,
    val description: String,
    val goal: WorkoutGoal,
    val level: WorkoutCategory,
    val weeklySchedule: String,
    val progressionNotes: String,
    val workoutIds: List<String>
)

object QuickWorkouts {

    private fun ex(name: String, sets: Int, reps: String, rest: Int) = QuickWorkoutExercise(name, sets, reps, rest)

    // ---------------------------------------------------------------------
    // Standalone / programme-day workouts
    // ---------------------------------------------------------------------
    val ALL: List<QuickWorkout> = listOf(

        // ---- StrongLifts 5x5 ----
        QuickWorkout(
            id = "sl5x5_a", name = "StrongLifts 5x5 - Workout A",
            description = "Squat-focused full-body strength session: the A day of StrongLifts 5x5.",
            level = WorkoutCategory.BEGINNER,
            categories = setOf(WorkoutCategory.BEGINNER, WorkoutCategory.STRENGTH, WorkoutCategory.FIVE_BY_FIVE, WorkoutCategory.FULL_BODY, WorkoutCategory.BARBELL_ONLY),
            goals = setOf(WorkoutGoal.INCREASE_STRENGTH, WorkoutGoal.BUILD_MUSCLE, WorkoutGoal.BEGINNER_CONFIDENCE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 45, programmeName = "StrongLifts 5x5",
            progressionNotes = "Add 2.5 kg to each lift every session you complete all 5x5. Deload 10% after three failed sessions.",
            exercises = listOf(
                ex("Barbell Back Squat", 5, "5", 180),
                ex("Barbell Bench Press", 5, "5", 180),
                ex("Barbell Bent Over Row", 5, "5", 180)
            )
        ),
        QuickWorkout(
            id = "sl5x5_b", name = "StrongLifts 5x5 - Workout B",
            description = "The B day of StrongLifts 5x5: squat, overhead press and deadlift.",
            level = WorkoutCategory.BEGINNER,
            categories = setOf(WorkoutCategory.BEGINNER, WorkoutCategory.STRENGTH, WorkoutCategory.FIVE_BY_FIVE, WorkoutCategory.FULL_BODY, WorkoutCategory.BARBELL_ONLY),
            goals = setOf(WorkoutGoal.INCREASE_STRENGTH, WorkoutGoal.BUILD_MUSCLE, WorkoutGoal.BEGINNER_CONFIDENCE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 45, programmeName = "StrongLifts 5x5",
            progressionNotes = "Deadlift is 1x5. Add 2.5 kg per session on squat/press, 5 kg on deadlift while reps are clean.",
            exercises = listOf(
                ex("Barbell Back Squat", 5, "5", 180),
                ex("Barbell Overhead Press", 5, "5", 180),
                ex("Barbell Deadlift", 1, "5", 180)
            )
        ),

        // ---- Starting Strength ----
        QuickWorkout(
            id = "ss_a", name = "Starting Strength - Workout A",
            description = "Squat, press, deadlift. The classic novice linear-progression A day.",
            level = WorkoutCategory.BEGINNER,
            categories = setOf(WorkoutCategory.BEGINNER, WorkoutCategory.STRENGTH, WorkoutCategory.FULL_BODY, WorkoutCategory.BARBELL_ONLY),
            goals = setOf(WorkoutGoal.INCREASE_STRENGTH, WorkoutGoal.BEGINNER_CONFIDENCE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 45, programmeName = "Starting Strength",
            progressionNotes = "3x5 on squat/press. Add weight every session; deadlift 1x5. Reset 10% on stall.",
            exercises = listOf(
                ex("Barbell Back Squat", 3, "5", 180),
                ex("Barbell Overhead Press", 3, "5", 180),
                ex("Barbell Deadlift", 1, "5", 180)
            )
        ),
        QuickWorkout(
            id = "ss_b", name = "Starting Strength - Workout B",
            description = "Squat, bench, deadlift. The novice linear-progression B day.",
            level = WorkoutCategory.BEGINNER,
            categories = setOf(WorkoutCategory.BEGINNER, WorkoutCategory.STRENGTH, WorkoutCategory.FULL_BODY, WorkoutCategory.BARBELL_ONLY),
            goals = setOf(WorkoutGoal.INCREASE_STRENGTH, WorkoutGoal.BEGINNER_CONFIDENCE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 45, programmeName = "Starting Strength",
            progressionNotes = "3x5 squat/bench. Alternate A and B each session, three sessions per week.",
            exercises = listOf(
                ex("Barbell Back Squat", 3, "5", 180),
                ex("Barbell Bench Press", 3, "5", 180),
                ex("Barbell Deadlift", 1, "5", 180)
            )
        ),

        // ---- Upper / Lower 4 Day ----
        QuickWorkout(
            id = "ul_upper_a", name = "Upper / Lower - Upper A",
            description = "Strength-leaning upper body day for a 4-day upper/lower split.",
            level = WorkoutCategory.INTERMEDIATE,
            categories = setOf(WorkoutCategory.INTERMEDIATE, WorkoutCategory.UPPER_LOWER, WorkoutCategory.HYPERTROPHY, WorkoutCategory.STRENGTH),
            goals = setOf(WorkoutGoal.BUILD_MUSCLE, WorkoutGoal.INCREASE_STRENGTH),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.DUMBBELLS, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 60, programmeName = "Upper Lower 4 Day",
            progressionNotes = "Add reps within range, then add load when you hit the top of the range on all sets.",
            exercises = listOf(
                ex("Barbell Bench Press", 4, "5-8", 150),
                ex("Barbell Bent Over Row", 4, "6-8", 150),
                ex("Barbell Overhead Press", 3, "8-10", 120),
                ex("Lat Pulldown", 3, "10-12", 90),
                ex("Dumbbell Bicep Curl", 3, "10-12", 60),
                ex("Tricep Pushdown", 3, "10-12", 60)
            )
        ),
        QuickWorkout(
            id = "ul_lower_a", name = "Upper / Lower - Lower A",
            description = "Strength-leaning lower body day for a 4-day upper/lower split.",
            level = WorkoutCategory.INTERMEDIATE,
            categories = setOf(WorkoutCategory.INTERMEDIATE, WorkoutCategory.UPPER_LOWER, WorkoutCategory.HYPERTROPHY, WorkoutCategory.STRENGTH),
            goals = setOf(WorkoutGoal.BUILD_MUSCLE, WorkoutGoal.INCREASE_STRENGTH),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.MACHINES, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 60, programmeName = "Upper Lower 4 Day",
            progressionNotes = "Squat heavy first; accessories in the 10-15 range to drive hypertrophy.",
            exercises = listOf(
                ex("Barbell Back Squat", 4, "5-8", 180),
                ex("Barbell Romanian Deadlift", 3, "8-10", 150),
                ex("Leg Press", 3, "10-12", 90),
                ex("Lying Leg Curl", 3, "10-12", 75),
                ex("Standing Calf Raise", 4, "12-15", 60)
            )
        ),

        // ---- Push / Pull / Legs 6 Day ----
        QuickWorkout(
            id = "ppl_push", name = "Push / Pull / Legs - Push",
            description = "Chest, shoulders and triceps. The push day of a PPL split.",
            level = WorkoutCategory.INTERMEDIATE,
            categories = setOf(WorkoutCategory.INTERMEDIATE, WorkoutCategory.PUSH_PULL_LEGS, WorkoutCategory.HYPERTROPHY, WorkoutCategory.BODYBUILDING),
            goals = setOf(WorkoutGoal.BUILD_MUSCLE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.DUMBBELLS, WorkoutEquipment.MACHINES, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 60, programmeName = "Push Pull Legs 6 Day",
            progressionNotes = "Double progression: build to the top of each rep range, then add load.",
            exercises = listOf(
                ex("Barbell Bench Press", 4, "6-8", 150),
                ex("Dumbbell Shoulder Press", 3, "8-12", 120),
                ex("Incline Dumbbell Press", 3, "8-12", 90),
                ex("Dumbbell Lateral Raise", 3, "12-15", 60),
                ex("Tricep Pushdown", 3, "10-15", 60)
            )
        ),
        QuickWorkout(
            id = "ppl_pull", name = "Push / Pull / Legs - Pull",
            description = "Back and biceps. The pull day of a PPL split.",
            level = WorkoutCategory.INTERMEDIATE,
            categories = setOf(WorkoutCategory.INTERMEDIATE, WorkoutCategory.PUSH_PULL_LEGS, WorkoutCategory.HYPERTROPHY, WorkoutCategory.BODYBUILDING),
            goals = setOf(WorkoutGoal.BUILD_MUSCLE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.DUMBBELLS, WorkoutEquipment.MACHINES, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 60, programmeName = "Push Pull Legs 6 Day",
            progressionNotes = "Lead with a heavy compound pull; finish with curls and rear delts.",
            exercises = listOf(
                ex("Barbell Bent Over Row", 4, "6-8", 150),
                ex("Lat Pulldown", 3, "8-12", 90),
                ex("Seated Cable Row", 3, "10-12", 90),
                ex("Face Pulls", 3, "12-15", 60),
                ex("Barbell Curl", 3, "8-12", 60)
            )
        ),
        QuickWorkout(
            id = "ppl_legs", name = "Push / Pull / Legs - Legs",
            description = "Quads, hamstrings, glutes and calves. The leg day of a PPL split.",
            level = WorkoutCategory.INTERMEDIATE,
            categories = setOf(WorkoutCategory.INTERMEDIATE, WorkoutCategory.PUSH_PULL_LEGS, WorkoutCategory.HYPERTROPHY, WorkoutCategory.BODYBUILDING),
            goals = setOf(WorkoutGoal.BUILD_MUSCLE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.MACHINES, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 60, programmeName = "Push Pull Legs 6 Day",
            progressionNotes = "Squat heavy, then build hypertrophy volume on the machines.",
            exercises = listOf(
                ex("Barbell Back Squat", 4, "6-8", 180),
                ex("Barbell Romanian Deadlift", 3, "8-10", 120),
                ex("Leg Press", 3, "10-12", 90),
                ex("Lying Leg Curl", 3, "10-12", 75),
                ex("Standing Calf Raise", 4, "12-15", 60)
            )
        ),

        // ---- 3 Day Full Body ----
        QuickWorkout(
            id = "fb3_a", name = "3 Day Full Body - Day 1",
            description = "Balanced full-body session built around the main compound lifts.",
            level = WorkoutCategory.BEGINNER,
            categories = setOf(WorkoutCategory.BEGINNER, WorkoutCategory.FULL_BODY, WorkoutCategory.GENERAL_FITNESS),
            goals = setOf(WorkoutGoal.BUILD_MUSCLE, WorkoutGoal.GENERAL_HEALTH, WorkoutGoal.BEGINNER_CONFIDENCE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.DUMBBELLS, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 45, programmeName = "3 Day Full Body",
            progressionNotes = "Add reps to the top of the range, then 2.5 kg. Run three non-consecutive days a week.",
            exercises = listOf(
                ex("Barbell Back Squat", 3, "8-10", 120),
                ex("Barbell Bench Press", 3, "8-10", 120),
                ex("Barbell Bent Over Row", 3, "8-10", 120),
                ex("Dumbbell Shoulder Press", 2, "10-12", 90)
            )
        ),

        // ---- 5 Day Bodybuilding Split ----
        QuickWorkout(
            id = "bb5_chest", name = "5 Day Bodybuilding - Chest",
            description = "High-volume chest day from a classic 5-day body-part split.",
            level = WorkoutCategory.ADVANCED,
            categories = setOf(WorkoutCategory.ADVANCED, WorkoutCategory.BODYBUILDING, WorkoutCategory.HYPERTROPHY),
            goals = setOf(WorkoutGoal.BUILD_MUSCLE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.DUMBBELLS, WorkoutEquipment.MACHINES, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 60, programmeName = "5 Day Bodybuilding Split",
            progressionNotes = "Chase a quality pump with controlled tempo; progress reps before load on isolations.",
            exercises = listOf(
                ex("Barbell Bench Press", 4, "8-10", 120),
                ex("Incline Dumbbell Press", 4, "10-12", 90),
                ex("Cable Chest Fly", 3, "12-15", 60),
                ex("Tricep Dips", 3, "10-12", 75)
            )
        ),

        // ---- Beginner Full Body ----
        QuickWorkout(
            id = "beginner_fb", name = "Beginner Full Body",
            description = "A confidence-building first programme using simple, safe movements.",
            level = WorkoutCategory.BEGINNER,
            categories = setOf(WorkoutCategory.BEGINNER, WorkoutCategory.FULL_BODY, WorkoutCategory.GENERAL_FITNESS, WorkoutCategory.MINIMAL_EQUIPMENT),
            goals = setOf(WorkoutGoal.BEGINNER_CONFIDENCE, WorkoutGoal.GENERAL_HEALTH, WorkoutGoal.RETURN_AFTER_INJURY),
            equipment = setOf(WorkoutEquipment.DUMBBELLS, WorkoutEquipment.MACHINES, WorkoutEquipment.HOME_GYM),
            estimatedMinutes = 30,
            progressionNotes = "Master form first. Add a rep or a small amount of load each week.",
            exercises = listOf(
                ex("Goblet Squat", 3, "10-12", 90),
                ex("Dumbbell Bench Press", 3, "10-12", 90),
                ex("Dumbbell Row", 3, "10-12", 90),
                ex("Plank", 3, "30s", 45)
            )
        ),

        // ---- Dumbbell Muscle Builder ----
        QuickWorkout(
            id = "db_muscle", name = "Dumbbell Muscle Builder",
            description = "A complete hypertrophy session needing only a pair of dumbbells.",
            level = WorkoutCategory.INTERMEDIATE,
            categories = setOf(WorkoutCategory.INTERMEDIATE, WorkoutCategory.HYPERTROPHY, WorkoutCategory.DUMBBELL_ONLY, WorkoutCategory.HOME, WorkoutCategory.FULL_BODY, WorkoutCategory.MINIMAL_EQUIPMENT),
            goals = setOf(WorkoutGoal.BUILD_MUSCLE, WorkoutGoal.GENERAL_HEALTH),
            equipment = setOf(WorkoutEquipment.DUMBBELLS, WorkoutEquipment.HOME_GYM),
            estimatedMinutes = 45,
            progressionNotes = "Double progression: top of range on every set, then grab the next dumbbell up.",
            exercises = listOf(
                ex("Goblet Squat", 4, "10-12", 90),
                ex("Dumbbell Bench Press", 4, "10-12", 90),
                ex("Dumbbell Row", 4, "10-12", 90),
                ex("Dumbbell Shoulder Press", 3, "10-12", 75),
                ex("Dumbbell Bicep Curl", 3, "12-15", 60)
            )
        ),

        // ---- Fat Loss Circuit ----
        QuickWorkout(
            id = "fatloss_circuit", name = "Fat Loss Circuit",
            description = "A fast, full-body circuit that keeps the heart rate up to burn calories.",
            level = WorkoutCategory.INTERMEDIATE,
            categories = setOf(WorkoutCategory.INTERMEDIATE, WorkoutCategory.GENERAL_FITNESS, WorkoutCategory.TIME_LIMITED, WorkoutCategory.MINIMAL_EQUIPMENT, WorkoutCategory.FULL_BODY),
            goals = setOf(WorkoutGoal.LOSE_FAT, WorkoutGoal.IMPROVE_CONDITIONING, WorkoutGoal.BUSY_SCHEDULE),
            equipment = setOf(WorkoutEquipment.DUMBBELLS, WorkoutEquipment.BODYWEIGHT, WorkoutEquipment.HOME_GYM),
            estimatedMinutes = 30,
            progressionNotes = "Rest only between rounds. Add a round or shave rest each week.",
            exercises = listOf(
                ex("Goblet Squat", 3, "15", 45),
                ex("Push Ups", 3, "15", 45),
                ex("Dumbbell Row", 3, "12", 45),
                ex("Walking Lunge", 3, "20", 45),
                ex("Plank", 3, "45s", 45)
            )
        ),

        // ---- Powerbuilding ----
        QuickWorkout(
            id = "powerbuilding_upper", name = "Powerbuilding - Upper",
            description = "Heavy compound work followed by hypertrophy accessories.",
            level = WorkoutCategory.ADVANCED,
            categories = setOf(WorkoutCategory.ADVANCED, WorkoutCategory.POWERLIFTING, WorkoutCategory.HYPERTROPHY, WorkoutCategory.STRENGTH, WorkoutCategory.UPPER_LOWER),
            goals = setOf(WorkoutGoal.INCREASE_STRENGTH, WorkoutGoal.BUILD_MUSCLE, WorkoutGoal.ATHLETIC_PERFORMANCE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.DUMBBELLS, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 75,
            progressionNotes = "Top single or triple on bench, then back-off volume. Accessories by double progression.",
            exercises = listOf(
                ex("Barbell Bench Press", 5, "3-5", 210),
                ex("Barbell Overhead Press", 3, "6-8", 150),
                ex("Barbell Bent Over Row", 4, "6-8", 120),
                ex("Incline Dumbbell Press", 3, "8-12", 90),
                ex("Barbell Curl", 3, "8-12", 60)
            )
        ),

        // ---- General Strength ----
        QuickWorkout(
            id = "general_strength", name = "General Strength",
            description = "A simple, balanced strength session for staying strong and healthy.",
            level = WorkoutCategory.INTERMEDIATE,
            categories = setOf(WorkoutCategory.INTERMEDIATE, WorkoutCategory.GENERAL_FITNESS, WorkoutCategory.STRENGTH, WorkoutCategory.FULL_BODY),
            goals = setOf(WorkoutGoal.INCREASE_STRENGTH, WorkoutGoal.GENERAL_HEALTH, WorkoutGoal.ATHLETIC_PERFORMANCE),
            equipment = setOf(WorkoutEquipment.BARBELL, WorkoutEquipment.DUMBBELLS, WorkoutEquipment.COMMERCIAL_GYM),
            estimatedMinutes = 45,
            progressionNotes = "Keep one rep in reserve. Add load slowly and consistently.",
            exercises = listOf(
                ex("Barbell Front Squat", 3, "5-6", 150),
                ex("Barbell Overhead Press", 3, "5-6", 150),
                ex("Barbell Bent Over Row", 3, "6-8", 120),
                ex("Barbell Romanian Deadlift", 3, "8-10", 120)
            )
        ),

        // ---- Time Limited / Busy ----
        QuickWorkout(
            id = "express_full_body", name = "20-Minute Express Full Body",
            description = "A tight, efficient full-body session for a busy day.",
            level = WorkoutCategory.BEGINNER,
            categories = setOf(WorkoutCategory.BEGINNER, WorkoutCategory.TIME_LIMITED, WorkoutCategory.FULL_BODY, WorkoutCategory.MINIMAL_EQUIPMENT),
            goals = setOf(WorkoutGoal.BUSY_SCHEDULE, WorkoutGoal.GENERAL_HEALTH),
            equipment = setOf(WorkoutEquipment.DUMBBELLS, WorkoutEquipment.HOME_GYM),
            estimatedMinutes = 20,
            progressionNotes = "Superset the pairs and keep rest short. Add a round if time allows.",
            exercises = listOf(
                ex("Goblet Squat", 3, "12", 60),
                ex("Dumbbell Bench Press", 3, "12", 60),
                ex("Dumbbell Row", 3, "12", 60)
            )
        )
    )

    // ---------------------------------------------------------------------
    // Named multi-day programmes (P3)
    // ---------------------------------------------------------------------
    val PROGRAMMES: List<WorkoutProgramme> = listOf(
        WorkoutProgramme(
            id = "stronglifts_5x5", name = "StrongLifts 5x5",
            description = "The most popular beginner barbell programme. Two alternating full-body workouts, three days a week, adding weight every session.",
            goal = WorkoutGoal.INCREASE_STRENGTH, level = WorkoutCategory.BEGINNER,
            weeklySchedule = "3 days/week, alternating A / B (e.g. Mon A, Wed B, Fri A)",
            progressionNotes = "Add 2.5 kg per lift each session you complete all reps. Deload 10% after three stalls.",
            workoutIds = listOf("sl5x5_a", "sl5x5_b")
        ),
        WorkoutProgramme(
            id = "starting_strength", name = "Starting Strength",
            description = "Mark Rippetoe's classic novice linear progression on the main barbell lifts.",
            goal = WorkoutGoal.INCREASE_STRENGTH, level = WorkoutCategory.BEGINNER,
            weeklySchedule = "3 days/week, alternating A / B",
            progressionNotes = "3x5 on squat/press/bench, 1x5 deadlift. Add weight every session; reset 10% on stall.",
            workoutIds = listOf("ss_a", "ss_b")
        ),
        WorkoutProgramme(
            id = "upper_lower_4", name = "Upper Lower 4 Day",
            description = "A balanced four-day upper/lower split for intermediates wanting size and strength.",
            goal = WorkoutGoal.BUILD_MUSCLE, level = WorkoutCategory.INTERMEDIATE,
            weeklySchedule = "4 days/week (Upper, Lower, rest, Upper, Lower)",
            progressionNotes = "Double progression on every lift. Heavy compounds first, then hypertrophy accessories.",
            workoutIds = listOf("ul_upper_a", "ul_lower_a")
        ),
        WorkoutProgramme(
            id = "ppl_6", name = "Push Pull Legs 6 Day",
            description = "High-frequency six-day push/pull/legs for advanced trainees chasing maximum muscle.",
            goal = WorkoutGoal.BUILD_MUSCLE, level = WorkoutCategory.ADVANCED,
            weeklySchedule = "6 days/week (Push, Pull, Legs, repeat)",
            progressionNotes = "Run each day twice a week. Manage fatigue; deload every 5-6 weeks.",
            workoutIds = listOf("ppl_push", "ppl_pull", "ppl_legs")
        ),
        WorkoutProgramme(
            id = "full_body_3", name = "3 Day Full Body",
            description = "An efficient three-day full-body plan - ideal frequency for most lifters.",
            goal = WorkoutGoal.BUILD_MUSCLE, level = WorkoutCategory.BEGINNER,
            weeklySchedule = "3 non-consecutive days/week",
            progressionNotes = "Add reps to the top of the range, then load. Great for beginners and busy intermediates.",
            workoutIds = listOf("fb3_a")
        ),
        WorkoutProgramme(
            id = "bodybuilding_5", name = "5 Day Bodybuilding Split",
            description = "A classic body-part split with high volume per muscle group.",
            goal = WorkoutGoal.BUILD_MUSCLE, level = WorkoutCategory.ADVANCED,
            weeklySchedule = "5 days/week (e.g. Chest, Back, Legs, Shoulders, Arms)",
            progressionNotes = "Train each muscle once or twice weekly with high volume; progress reps before load.",
            workoutIds = listOf("bb5_chest")
        ),
        WorkoutProgramme(
            id = "powerbuilding", name = "Powerbuilding",
            description = "Blends powerlifting strength work with bodybuilding hypertrophy accessories.",
            goal = WorkoutGoal.INCREASE_STRENGTH, level = WorkoutCategory.ADVANCED,
            weeklySchedule = "4 days/week (heavy compound + accessories)",
            progressionNotes = "Top set then back-off volume on the main lift; double progression on accessories.",
            workoutIds = listOf("powerbuilding_upper")
        )
    )

    fun byId(id: String): QuickWorkout? = ALL.firstOrNull { it.id == id }

    /** Apply combinable filters. Empty filters return everything. */
    fun filter(
        category: WorkoutCategory? = null,
        goal: WorkoutGoal? = null,
        equipment: WorkoutEquipment? = null,
        maxMinutes: Int? = null
    ): List<QuickWorkout> = ALL.filter { w ->
        (category == null || category in w.categories || category == w.level) &&
            (goal == null || goal in w.goals) &&
            (equipment == null || equipment in w.equipment) &&
            (maxMinutes == null || w.estimatedMinutes <= maxMinutes)
    }
}
