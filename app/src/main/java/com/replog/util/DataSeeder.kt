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
