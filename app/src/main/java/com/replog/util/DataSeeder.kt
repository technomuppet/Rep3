package com.replog.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.replog.data.model.Exercise
import com.replog.data.model.TemplateExercise
import com.replog.data.model.WorkoutTemplate
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.templates.BuiltInTemplate
import com.replog.domain.templates.BuiltInTemplates
import com.replog.domain.templates.GeneratedProgram
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exercises: ExerciseRepository,
    private val workouts: WorkoutRepository,
    private val prefs: PreferencesManager
) {
    suspend fun seedDataIfFirstLaunch() {
        val count = exercises.countExercises()
        if (!prefs.isFirstLaunch.first() && count > 0) {
            upgradeExerciseMetadataIfNeeded()
            return
        }
        seedExercises()
        seedTemplates()
        prefs.setFirstLaunchComplete()
    }

    private suspend fun seedExercises() {
        if (exercises.countExercises() > 0) return
        exercises.insertExercises(loadExerciseData().map { it.toExercise() })
    }

    private suspend fun upgradeExerciseMetadataIfNeeded() {
        val toInsert = mutableListOf<Exercise>()
        loadExerciseData().forEach { data ->
            val existing = exercises.getExerciseByName(data.name)
            if (existing == null) {
                // Bundled exercise not yet in the user's library (e.g. an expanded
                // library shipped in an app update). Add it without touching the
                // user's custom exercises or existing data.
                toInsert += data.toExercise()
            } else if (existing.movementPattern.isBlank() || existing.primaryMuscles.isBlank() || existing.mediaAsset.isBlank()) {
                val enriched = data.toExercise().copy(
                    id = existing.id,
                    isCustom = existing.isCustom
                )
                exercises.updateExercise(enriched)
            }
        }
        if (toInsert.isNotEmpty()) exercises.insertExercises(toInsert)
    }

    private fun loadExerciseData(): List<ExerciseData> {
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<ExerciseData>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun ExerciseData.toExercise(): Exercise = Exercise(
        name = name,
        category = category,
        equipment = equipment,
        type = type,
        muscles = muscles.joinToString(", "),
        primaryMuscles = primaryMuscles.ifEmpty { muscles.take(1) }.joinToString(", "),
        secondaryMuscles = secondaryMuscles.ifEmpty { muscles.drop(1) }.joinToString(", "),
        movementPattern = movementPattern,
        difficulty = difficulty,
        mediaAsset = mediaAsset
    )

    private suspend fun seedTemplates() {
        // Seed only the entry-level templates by default; the full catalog is
        // installed either via onboarding personalization or "Browse all".
        val defaults = BuiltInTemplates.ALL.filter { it.name in BuiltInTemplates.DEFAULT_SEED_NAMES }
        installTemplates(defaults)
    }

    /**
     * Idempotently install a list of built-in templates. Existing built-in
     * templates with the same name are skipped (so re-running on app update or
     * "install all" never creates duplicates and never touches user templates).
     */
    suspend fun installTemplates(templates: List<BuiltInTemplate>) {
        val existingNames = workouts.getAllTemplates().first().map { it.template.name }.toSet()
        templates.forEach { spec ->
            if (spec.name in existingNames) return@forEach
            val templateId = workouts.insertTemplate(WorkoutTemplate(name = spec.name, isBuiltIn = true)).toInt()
            spec.exercises.forEachIndexed { index, exSpec ->
                exercises.getExerciseByName(exSpec.exerciseName)?.let { ex ->
                    workouts.insertTemplateExercise(
                        TemplateExercise(
                            templateId = templateId,
                            exerciseId = ex.id,
                            defaultSets = exSpec.sets,
                            orderIndex = index,
                            targetReps = exSpec.reps
                        )
                    )
                }
            }
        }
    }

    /** Install every built-in template in the catalog (e.g. "Browse all templates"). */
    suspend fun installAllBuiltInTemplates() = installTemplates(BuiltInTemplates.ALL)

    /**
     * Apply a personalized program produced by ProgramGenerator: install the
     * concrete templates it references (deduplicated, preserving day order).
     */
    suspend fun installGeneratedProgram(program: GeneratedProgram) {
        val byName = BuiltInTemplates.ALL.associateBy { it.name }
        val toInstall = program.templateNames.distinct().mapNotNull { byName[it] }
        installTemplates(toInstall)
    }

    /**
     * Import a shared template (.replogtemplate). Exercises are resolved by name;
     * any the user does not have are created as custom exercises from the
     * metadata in the file. If the template name already exists, a numeric
     * suffix is added so the import never silently overwrites or fails.
     * Returns the final template name on success.
     */
    suspend fun importSharedTemplate(shared: SharedTemplate): String {
        val existingNames = workouts.getAllTemplates().first().map { it.template.name }.toSet()
        var name = shared.name.trim().ifBlank { "Imported template" }
        if (name in existingNames) {
            var n = 2
            while ("$name ($n)" in existingNames) n++
            name = "$name ($n)"
        }
        val templateId = workouts.insertTemplate(WorkoutTemplate(name = name, isBuiltIn = false)).toInt()
        shared.exercises.sortedBy { it.orderIndex }.forEachIndexed { index, spec ->
            val exercise = exercises.getExerciseByName(spec.exerciseName)
                ?: exercises.insertExercise(
                    Exercise(
                        name = spec.exerciseName,
                        category = spec.category.ifBlank { "Imported" },
                        equipment = spec.equipment.ifBlank { "Other" },
                        muscles = spec.muscles,
                        primaryMuscles = spec.primaryMuscles,
                        secondaryMuscles = spec.secondaryMuscles,
                        movementPattern = spec.movementPattern,
                        difficulty = spec.difficulty.ifBlank { "Intermediate" },
                        isCustom = true
                    )
                ).let { id -> exercises.getExerciseById(id.toInt()) }
                ?: return@forEachIndexed
            workouts.insertTemplateExercise(
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = exercise.id,
                    defaultSets = spec.sets.coerceAtLeast(1),
                    orderIndex = index,
                    targetReps = spec.reps.coerceAtLeast(1),
                    targetWeight = spec.targetWeight
                )
            )
        }
        return name
    }

    /**
     * Sprint 5 P3: duplicate a curated Quick Workout into the user's own editable
     * templates without touching the original catalogue. Exercises resolve by
     * name against the library; the rep range is parsed to a representative
     * target rep. Returns the new template name (deduped if it already exists),
     * or null if no exercises could be resolved.
     */
    suspend fun duplicateQuickWorkout(workout: com.replog.domain.library.QuickWorkout): String? {
        val existingNames = workouts.getAllTemplates().first().map { it.template.name }.toSet()
        var name = workout.name.trim().ifBlank { "Quick workout" }
        if (name in existingNames) {
            var n = 2
            while ("$name ($n)" in existingNames) n++
            name = "$name ($n)"
        }
        val templateId = workouts.insertTemplate(WorkoutTemplate(name = name, isBuiltIn = false)).toInt()
        var added = 0
        workout.exercises.forEachIndexed { index, spec ->
            val exercise = exercises.getExerciseByName(spec.exerciseName) ?: return@forEachIndexed
            workouts.insertTemplateExercise(
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = exercise.id,
                    defaultSets = spec.sets.coerceAtLeast(1),
                    orderIndex = index,
                    targetReps = parseTargetReps(spec.reps)
                )
            )
            added++
        }
        if (added == 0) {
            workouts.deleteTemplate(WorkoutTemplate(id = templateId, name = name, isBuiltIn = false))
            return null
        }
        return name
    }

    /** Parse "5", "8-12", "AMRAP", "30s" into a representative integer rep target. */
    private fun parseTargetReps(reps: String): Int {
        val nums = Regex("\\d+").findAll(reps).map { it.value.toInt() }.toList()
        return when {
            nums.isEmpty() -> 8
            nums.size >= 2 -> ((nums[0] + nums[1]) / 2).coerceAtLeast(1)
            else -> nums[0].coerceAtLeast(1)
        }
    }

    data class ExerciseData(
        val name: String,
        val category: String,
        val equipment: String,
        val type: String = "Strength",
        val muscles: List<String> = emptyList(),
        val primaryMuscles: List<String> = emptyList(),
        val secondaryMuscles: List<String> = emptyList(),
        val movementPattern: String = "",
        val difficulty: String = "Intermediate",
        val mediaAsset: String = ""
    )
}
