package com.replog.domain.programs

enum class ProgramGoal {
    STRENGTH,
    HYPERTROPHY,
    POWERLIFTING,
    FAT_LOSS,
    GENERAL
}

data class ProgramBuilderRequest(
    val goal: ProgramGoal,
    val daysPerWeek: Int,
    val experienceLevel: String,
    val preferredEquipment: List<String> = emptyList(),
    val constraints: Map<String, String> = emptyMap()
)

data class ProgramWorkoutDraft(
    val name: String,
    val exerciseTargets: List<ProgramExerciseTarget>
)

data class ProgramExerciseTarget(
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val intensityNote: String? = null
)

data class ProgramDraft(
    val name: String,
    val goal: ProgramGoal,
    val workouts: List<ProgramWorkoutDraft>,
    val notes: List<String> = emptyList()
)

interface ProgramBuilderEngine {
    fun build(request: ProgramBuilderRequest): ProgramDraft
}
