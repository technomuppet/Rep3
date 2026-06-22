package com.replog.domain.recommendation

import com.replog.data.model.Exercise
import com.replog.data.model.SessionWithExercises
import com.replog.domain.trainingdna.TrainingDnaConfig

class RecommendationEngine(
    private val config: TrainingDnaConfig = TrainingDnaConfig()
) {

    fun generate(context: RecommendationContext): Recommendation {
        val completed = context.sessions.filter { it.session.endTime != null }
        val now = context.nowMillis

        if (completed.isEmpty()) {
            return fallbackRecommendation(
                "Start with Full Body",
                "No completed workouts yet. A full-body session is the best way to begin building your training signal.",
                emptyList(),
                emptyList(),
                "Log your first workout and RepLog will start learning your training patterns."
            )
        }

        val overallRecovery = RecoveryAnalyzer.overallRecovery(completed, context.bodyweights, now)
        val muscleRecovery = RecoveryAnalyzer.muscleRecovery(completed, now)
        val recentSessions = completed.sortedByDescending { it.session.startTime }
        val lastSession = recentSessions.firstOrNull()
        val hoursSinceLastSession = lastSession?.let { (now - it.session.startTime) / (3_600_000.0) } ?: Double.MAX_VALUE

        val stalledEvents = context.plateauEvents
            .filter { it.detectedAt >= now - 60L * DAY }
            .distinctBy { it.exerciseId }

        val stalledLiftNames = stalledEvents.map { it.exerciseName }.takeIf { it.isNotEmpty() } ?: emptyList()

        val undertrainedMuscles = context.dnaSnapshot?.weakestMuscles
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val dnaFrequency = context.dnaSnapshot?.preferredFrequency ?: "Not enough data"
        val dnaRepRange = context.dnaSnapshot?.preferredRepRange ?: "7–10"
        val dnaVolume = context.dnaSnapshot?.preferredVolumeRange ?: "Moderate"

        val dataUsed = mutableListOf<String>()
        dataUsed += "Recovery score: ${overallRecovery.score.toInt()}/100 (${overallRecovery.label})"
        if (muscleRecovery.isNotEmpty()) dataUsed += "Muscle recovery: ${muscleRecovery.count { it.status == MuscleRecoveryStatus.FRESH || it.status == MuscleRecoveryStatus.RECOVERED }} fresh, ${muscleRecovery.count { it.status == MuscleRecoveryStatus.FATIGUED || it.status == MuscleRecoveryStatus.VERY_FATIGUED }} fatigued"
        if (stalledLiftNames.isNotEmpty()) dataUsed += "Stalled lifts: ${stalledLiftNames.joinToString(", ")}"
        if (undertrainedMuscles.isNotEmpty()) dataUsed += "Undertrained muscles: ${undertrainedMuscles.joinToString(", ")}"
        dataUsed += "Training DNA: ${dnaFrequency} frequency, ${dnaRepRange} rep range, ${dnaVolume} volume"

        val reasoning = mutableListOf<String>()
        reasoning += overallRecovery.reasons

        // Decision: REST
        if (overallRecovery.score < 35 || hoursSinceLastSession < 12) {
            reasoning += if (hoursSinceLastSession < 12) "Last session was very recent; prioritize recovery." else "Recovery score is very low; take a rest day."
            return Recommendation(
                type = RecommendationType.REST,
                title = "Take a Rest Day",
                explanation = "Your recovery signal is low (${overallRecovery.label}). Rest today and come back stronger.",
                dataUsed = dataUsed,
                reasoning = reasoning,
                expectedOutcome = "Better recovery, lower injury risk, and improved performance in your next session.",
                confidenceScore = (95.0 - overallRecovery.score).coerceIn(70.0, 99.0),
                estimatedDurationMinutes = 0
            )
        }

        // Decision: DELOAD
        if (overallRecovery.score < 55 && (stalledLiftNames.isNotEmpty() || overallRecovery.reasons.any { it.contains("high") || it.contains("jumped") })) {
            reasoning += "Recovery is low and there are signs of fatigue or stalled progress; a deload is appropriate."
            return Recommendation(
                type = RecommendationType.DELOAD,
                title = "Deload Workout",
                explanation = "Reduce load and volume today to dissipate fatigue while keeping movement patterns fresh.",
                dataUsed = dataUsed,
                reasoning = reasoning,
                expectedOutcome = "Recovery improves, plateaus break, and you can resume hard training in the next session.",
                confidenceScore = 80.0,
                estimatedDurationMinutes = 30,
                workoutPlan = generateDeloadPlan(context.exercises, muscleRecovery, stalledLiftNames, recentSessions, dnaRepRange)
            )
        }

        // Decision: REPEAT or PROGRESS
        if (lastSession != null && hoursSinceLastSession < 48) {
            val lastSplit = inferSplit(lastSession)
            val lastFatigue = lastSession.exercises.flatMap { it.sets }.mapNotNull { it.rpe }.averageOrNull()
            val type = if (lastFatigue != null && lastFatigue < 8.5 && overallRecovery.score >= 65) RecommendationType.PROGRESS else RecommendationType.REPEAT
            val title = if (type == RecommendationType.PROGRESS) "Progress Last Workout" else "Repeat Last Workout"
            val explanation = if (type == RecommendationType.PROGRESS) {
                "Your last session was manageable and recovery is good. Push the same lifts slightly harder today."
            } else {
                "Repeat your last workout to build consistency and technique before increasing load."
            }
            reasoning += "Last session was $lastSplit ${hoursSinceLastSession.toInt()} hours ago."
            reasoning += if (type == RecommendationType.PROGRESS) "Recovery is good and RPE was manageable, so progress the same exercises." else "Repeat the same stimulus to build mastery and volume."

            return Recommendation(
                type = type,
                title = title,
                explanation = explanation,
                dataUsed = dataUsed,
                reasoning = reasoning,
                expectedOutcome = if (type == RecommendationType.PROGRESS) "Continued progressive overload on familiar lifts." else "Technique and work capacity improve with repeated exposure.",
                confidenceScore = 75.0,
                estimatedDurationMinutes = estimateDuration(context.dnaSnapshot),
                workoutPlan = generateRepeatPlan(lastSession, type, context.exercises, muscleRecovery, dnaRepRange)
            )
        }

        // Decision: TRAIN with specific split
        val split = chooseSplit(
            recentSessions = recentSessions,
            muscleRecovery = muscleRecovery,
            undertrainedMuscles = undertrainedMuscles,
            stalledLiftNames = stalledLiftNames,
            dnaFrequency = dnaFrequency
        )

        reasoning += "Recovery is ${overallRecovery.label.lowercase()}."
        reasoning += "Selected ${WorkoutPlanGenerator.splitName(split)} based on muscle recovery and recent training balance."
        if (stalledLiftNames.isNotEmpty()) reasoning += "Prioritizing exercises that can address stalled lifts."
        if (undertrainedMuscles.isNotEmpty()) reasoning += "Undertrained muscles get priority in exercise selection."

        val plan = WorkoutPlanGenerator.generate(
            split = split,
            exercises = context.exercises,
            muscleRecovery = muscleRecovery,
            stalledExercises = stalledEvents,
            undertrainedMuscles = undertrainedMuscles,
            recentSessions = recentSessions,
            dnaRepRange = dnaRepRange
        )

        val planWithProgression = applyProgression(plan, context.exercises, recentSessions, muscleRecovery, stalledEvents, overallRecovery.score)

        return Recommendation(
            type = RecommendationType.TRAIN,
            title = "Train ${WorkoutPlanGenerator.splitName(split)}",
            explanation = buildTrainExplanation(planWithProgression, overallRecovery, stalledLiftNames, undertrainedMuscles),
            dataUsed = dataUsed,
            reasoning = reasoning,
            expectedOutcome = "Targeted stimulus for ${planWithProgression.exercises.size} exercises, balancing recovery and progression priorities.",
            confidenceScore = calculateConfidence(overallRecovery.score, muscleRecovery, stalledLiftNames, planWithProgression),
            estimatedDurationMinutes = estimateDuration(context.dnaSnapshot),
            workoutPlan = planWithProgression
        )
    }

    private fun applyProgression(
        plan: WorkoutPlan,
        exercises: List<Exercise>,
        sessions: List<SessionWithExercises>,
        muscleRecovery: List<MuscleRecovery>,
        stalledEvents: List<com.replog.data.model.PlateauEvent>,
        overallRecoveryScore: Double
    ): WorkoutPlan {
        val stalledIds = stalledEvents.map { it.exerciseId }.toSet()
        val progressed = plan.exercises.map { planned ->
            val exercise = exercises.firstOrNull { it.id == planned.exerciseId }
            val muscle = (exercise?.primaryMuscles?.split(",")?.firstOrNull()?.trim()?.lowercase() ?: "")
            val recovery = muscleRecovery.firstOrNull { it.muscle.lowercase() == muscle }
            val decision = if (exercise != null) {
                ProgressionDecider.decide(exercise, sessions, recovery, exercise.id in stalledIds, overallRecoveryScore)
            } else ProgressionDecision.MAINTAIN

            val reason = when (decision) {
                ProgressionDecision.INCREASE_LOAD -> "Recent performance suggests you can handle more load."
                ProgressionDecision.INCREASE_REPS -> "Add reps to keep progressive overload moving."
                ProgressionDecision.INCREASE_SETS -> "Recovery is good; add a set to build volume."
                ProgressionDecision.MAINTAIN -> "Maintain to consolidate technique and recovery."
                ProgressionDecision.DELOAD -> "This lift is stalled or fatigued; reduce load today."
            }

            planned.copy(progression = decision, reason = reason)
        }
        return plan.copy(exercises = progressed)
    }

    private fun generateDeloadPlan(
        exercises: List<Exercise>,
        muscleRecovery: List<MuscleRecovery>,
        stalledLiftNames: List<String>,
        recentSessions: List<SessionWithExercises>,
        dnaRepRange: String
    ): WorkoutPlan {
        val freshMuscles = muscleRecovery.filter { it.status == MuscleRecoveryStatus.FRESH || it.status == MuscleRecoveryStatus.RECOVERED }.map { it.muscle.lowercase() }.toSet()
        val candidates = exercises.filter { ex ->
            val muscles = (ex.primaryMuscles + "," + ex.secondaryMuscles + "," + ex.muscles).split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
            muscles.any { it in freshMuscles }
        }

        val selected = candidates.take(5).ifEmpty { exercises.take(5) }
        return WorkoutPlan(
            split = WorkoutSplit.FULL_BODY,
            exercises = selected.map { exercise ->
                PlannedExercise(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    targetSets = 2,
                    targetReps = WorkoutPlanGenerator.repTargetFromDna(dnaRepRange),
                    targetWeight = null,
                    progression = ProgressionDecision.DELOAD,
                    reason = "Deload: reduce sets and use lighter loads to recover."
                )
            }
        )
    }

    private fun generateRepeatPlan(
        lastSession: SessionWithExercises,
        type: RecommendationType,
        exercises: List<Exercise>,
        muscleRecovery: List<MuscleRecovery>,
        dnaRepRange: String
    ): WorkoutPlan {
        val planned = lastSession.exercises.map { entry ->
            val exercise = entry.exercise
            val muscle = exercise.primaryMuscles.split(",").firstOrNull()?.trim()?.lowercase() ?: ""
            val recovery = muscleRecovery.firstOrNull { it.muscle.lowercase() == muscle }
            val decision = if (type == RecommendationType.PROGRESS) {
                ProgressionDecider.decide(exercise, listOf(lastSession), recovery, false, 75.0)
            } else ProgressionDecision.MAINTAIN

            PlannedExercise(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                targetSets = entry.sets.filter { it.setType != com.replog.data.model.SetType.WARMUP }.size.coerceAtLeast(3),
                targetReps = entry.sets.filter { it.setType != com.replog.data.model.SetType.WARMUP }.maxOfOrNull { it.reps } ?: WorkoutPlanGenerator.repTargetFromDna(dnaRepRange),
                targetWeight = entry.sets.filter { it.setType != com.replog.data.model.SetType.WARMUP }.maxOfOrNull { it.weight },
                progression = decision,
                reason = if (type == RecommendationType.PROGRESS) "Progress from last session based on performance." else "Repeat the same stimulus."
            )
        }

        return WorkoutPlan(
            split = inferSplit(lastSession),
            exercises = planned
        )
    }

    private fun chooseSplit(
        recentSessions: List<SessionWithExercises>,
        muscleRecovery: List<MuscleRecovery>,
        undertrainedMuscles: List<String>,
        stalledLiftNames: List<String>,
        dnaFrequency: String
    ): WorkoutSplit {
        val fatigued = muscleRecovery.filter { it.status == MuscleRecoveryStatus.FATIGUED || it.status == MuscleRecoveryStatus.VERY_FATIGUED }.map { it.muscle.lowercase() }.toSet()
        val undertrainedLower = undertrainedMuscles.any { it.lowercase() in legMuscles }
        val undertrainedUpper = undertrainedMuscles.any { it.lowercase() in upperMuscles }
        val stalledLower = stalledLiftNames.any { it.lowercase() in legExerciseHints }
        val stalledUpper = stalledLiftNames.any { it.lowercase() in upperExerciseHints }

        val lastSplit = recentSessions.firstOrNull()?.let { inferSplit(it) }
        val lastSplits = recentSessions.take(4).map { inferSplit(it) }
        val splitCounts = lastSplits.groupingBy { it }.eachCount()

        val candidates = mutableListOf<WorkoutSplit>()

        when {
            dnaFrequency.contains("1x") || recentSessions.size < 5 -> candidates += WorkoutSplit.FULL_BODY
            dnaFrequency.contains("4x") || dnaFrequency.contains("3x") -> {
                candidates += WorkoutSplit.PUSH
                candidates += WorkoutSplit.PULL
                candidates += WorkoutSplit.LEGS
            }
            else -> {
                candidates += WorkoutSplit.UPPER
                candidates += WorkoutSplit.LOWER
            }
        }

        // Prefer splits that avoid fatigued muscles and hit undertrained/stalled areas
        val scored = candidates.map { split ->
            val targetMuscles = splitMuscleGroups[split] ?: emptySet()
            var score = 50.0
            if (split == lastSplit) score -= 20.0
            score -= (splitCounts[split] ?: 0) * 10.0
            if (targetMuscles.any { it in fatigued }) score -= 25.0
            if (undertrainedMuscles.any { it.lowercase() in targetMuscles }) score += 20.0
            if (stalledLiftNames.any { name -> targetMuscles.any { it in name.lowercase() } }) score += 15.0
            if (undertrainedLower && split == WorkoutSplit.LEGS) score += 20.0
            if (undertrainedUpper && split in setOf(WorkoutSplit.UPPER, WorkoutSplit.PUSH, WorkoutSplit.PULL)) score += 15.0
            if (stalledLower && split == WorkoutSplit.LEGS) score += 20.0
            if (stalledUpper && split in setOf(WorkoutSplit.UPPER, WorkoutSplit.PUSH, WorkoutSplit.PULL)) score += 15.0
            split to score
        }.sortedByDescending { it.second }

        return scored.firstOrNull()?.first ?: WorkoutSplit.FULL_BODY
    }

    private fun inferSplit(session: SessionWithExercises): WorkoutSplit {
        val muscles = session.exercises.flatMap { entry ->
            (entry.exercise.primaryMuscles + "," + entry.exercise.secondaryMuscles + "," + entry.exercise.muscles)
                .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        }.toSet()

        val legCount = muscles.count { it in legMuscles }
        val upperCount = muscles.count { it in upperMuscles }
        val pushCount = muscles.count { it in pushMuscles }
        val pullCount = muscles.count { it in pullMuscles }

        return when {
            legCount == 0 && upperCount > 0 -> WorkoutSplit.UPPER
            upperCount == 0 && legCount > 0 -> WorkoutSplit.LOWER
            pushCount > 0 && pullCount == 0 && legCount == 0 -> WorkoutSplit.PUSH
            pullCount > 0 && pushCount == 0 && legCount == 0 -> WorkoutSplit.PULL
            legCount > 0 && upperCount == 0 -> WorkoutSplit.LEGS
            else -> WorkoutSplit.FULL_BODY
        }
    }

    private fun buildTrainExplanation(
        plan: WorkoutPlan,
        recovery: OverallRecovery,
        stalledLiftNames: List<String>,
        undertrainedMuscles: List<String>
    ): String {
        val parts = mutableListOf<String>()
        parts += "Recovery is ${recovery.label.lowercase()}."
        if (stalledLiftNames.isNotEmpty()) {
            parts += "${stalledLiftNames.first()} has stalled; today's plan targets this."
        }
        if (undertrainedMuscles.isNotEmpty()) {
            parts += "${undertrainedMuscles.first()} is undertrained and gets priority."
        }
        parts += "${plan.exercises.size} exercises selected for ${WorkoutPlanGenerator.splitName(plan.split)}."
        return parts.joinToString(" ")
    }

    private fun calculateConfidence(
        recoveryScore: Double,
        muscleRecovery: List<MuscleRecovery>,
        stalledLiftNames: List<String>,
        plan: WorkoutPlan
    ): Double {
        val base = 70.0
        val recoveryBonus = (recoveryScore - 60.0) * 0.15
        val musclePenalty = muscleRecovery.count { it.status == MuscleRecoveryStatus.VERY_FATIGUED } * 5.0
        val dataBonus = if (stalledLiftNames.isNotEmpty()) 5.0 else 0.0
        val exerciseBonus = (plan.exercises.size.coerceIn(3, 6) - 3) * 1.5
        return (base + recoveryBonus - musclePenalty + dataBonus + exerciseBonus).coerceIn(55.0, 95.0)
    }

    private fun estimateDuration(dnaSnapshot: com.replog.data.model.TrainingDnaSnapshot?): Int {
        val avg = dnaSnapshot?.averageWorkoutDuration ?: 0.0
        return if (avg > 0) avg.toInt() else 45
    }

    private fun fallbackRecommendation(
        title: String,
        explanation: String,
        dataUsed: List<String>,
        reasoning: List<String>,
        expectedOutcome: String
    ): Recommendation = Recommendation(
        type = RecommendationType.TRAIN,
        title = title,
        explanation = explanation,
        dataUsed = dataUsed,
        reasoning = reasoning,
        expectedOutcome = expectedOutcome,
        confidenceScore = 60.0,
        estimatedDurationMinutes = 45
    )

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private val legMuscles = setOf("quads", "quadriceps", "hamstrings", "glutes", "calves", "adductors", "legs")
    private val upperMuscles = setOf("chest", "pectorals", "shoulders", "deltoids", "triceps", "back", "lats", "latissimus", "biceps", "rear delt")
    private val pushMuscles = setOf("chest", "pectorals", "shoulders", "deltoids", "triceps")
    private val pullMuscles = setOf("back", "lats", "latissimus", "biceps", "rear delt")
    private val legExerciseHints = setOf("squat", "deadlift", "leg press", "lunge", "leg curl", "leg extension", "calf")
    private val upperExerciseHints = setOf("bench", "press", "row", "pull", "curl", "fly", "delt")

    private val splitMuscleGroups = mapOf(
        WorkoutSplit.PUSH to setOf("chest", "pectorals", "shoulders", "deltoids", "triceps", "front delt"),
        WorkoutSplit.PULL to setOf("back", "lats", "latissimus", "traps", "rhomboids", "biceps", "rear delt"),
        WorkoutSplit.LEGS to setOf("quads", "quadriceps", "hamstrings", "glutes", "calves", "adductors"),
        WorkoutSplit.UPPER to setOf("chest", "pectorals", "shoulders", "deltoids", "triceps", "back", "lats", "latissimus", "biceps", "rear delt"),
        WorkoutSplit.LOWER to setOf("quads", "quadriceps", "hamstrings", "glutes", "calves", "adductors"),
        WorkoutSplit.FULL_BODY to setOf("chest", "pectorals", "back", "lats", "quads", "quadriceps", "hamstrings", "glutes")
    )

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
    }
}
