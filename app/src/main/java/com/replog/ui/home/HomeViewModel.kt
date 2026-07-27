package com.replog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.repository.GoalRepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.genome.TrainingGenomeEngine
import com.replog.domain.goals.GoalForecast
import com.replog.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeInsight(
    val title: String = "Start your training signal",
    val message: String = "Log a few workouts and RepLog will surface recovery, plateau and progression insights here."
)

/** A compact goal summary for the Home dashboard. */
data class HomeGoal(
    val title: String,
    val progressPercent: Int,
    val etaText: String,
    val summaryLine: String
)

/** Priority 2: a resumable in-progress workout shown as a large Home card. */
data class ContinueWorkout(
    val sessionId: Int,
    val templateName: String,
    val startTime: Long,
    val exerciseCount: Int,
    val setCount: Int
)

data class HomeUiState(
    val sessionCount: Int = 0,
    val totalVolume: Double = 0.0,
    val recentSessions: List<SessionWithExercises> = emptyList(),
    val recentPRs: List<SetLog> = emptyList(),
    val insight: HomeInsight = HomeInsight(),
    val sessionsThisWeek: Int = 0,
    val volumeThisWeek: Double = 0.0,
    val dayStreak: Int = 0,
    val topGoal: HomeGoal? = null,
    val genomeHeadline: String? = null,
    val favoriteTemplates: List<com.replog.data.model.TemplateWithExercises> = emptyList(),
    val isLoading: Boolean = true
)

/** Typed holders so the Home dashboard combine() has no positional casts. */
private data class HomeGroupA(
    val stats: Pair<Int, Double>,
    val sessions: List<SessionWithExercises>,
    val prs: List<SetLog>,
    val summaries: List<com.replog.data.model.SessionSummaryRow>,
    val recentCompleted: List<SessionWithExercises>
)
private data class HomeGroupB(
    val activeGoals: List<com.replog.data.model.Goal>,
    val favorites: List<com.replog.data.model.TemplateWithExercises>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: WorkoutRepository,
    private val goalRepository: GoalRepository,
    private val prefs: PreferencesManager,
    private val workoutStarter: com.replog.util.WorkoutStarter,
    private val intelligenceRepository: com.replog.data.repository.IntelligenceRepository
) : ViewModel() {
    private val stats = MutableStateFlow(0 to 0.0)

    // Priority 1: the unified Today's Briefing. Computed on demand and cached so
    // the expensive multi-engine pass does NOT run on every set/DB change.
    private val _briefing = MutableStateFlow<com.replog.domain.intelligence.TodaysBriefing?>(null)
    val briefing: StateFlow<com.replog.domain.intelligence.TodaysBriefing?> = _briefing
    private var briefingLoadedForSessionCount = -1
    // Phase 2 Gap 2: day-of-year cache key — LocalDate.toEpochDay() is a SINGLE
    // monotonically-increasing day counter since 1970-01-01 (unlike Calendar
    // .DAY_OF_YEAR which wraps annually and would falsely hit its prior-year
    // value on January 1st each year, suppressing the post-midnight refresh).
    private var briefingLoadedForEpochDay: Int = -1
    // Phase 2 Gap 2: cancel any in-flight recompute before launching a new one,
    // so concurrent triggers (e.g. ON_RESUME firing while the minute tick also
    // fires) cannot both run the expensive `buildBriefing()` / `buildRepLogScore()`
    // chain. Cheap cancellation points (`ensureActive()`) are implicit at every
    // suspend inside the load block because `Job.cancel()` is cooperative.
    private var intelligenceJob: kotlinx.coroutines.Job? = null

    // Priority 2: an in-progress workout to resume, if any.
    private val _continueWorkout = MutableStateFlow<ContinueWorkout?>(null)
    val continueWorkout: StateFlow<ContinueWorkout?> = _continueWorkout

    // Sprint 8 P5: RepLog Score, cached alongside the briefing.
    private val _repLogScore = MutableStateFlow<com.replog.domain.intelligence.RepLogScoreResult?>(null)
    val repLogScore: StateFlow<com.replog.domain.intelligence.RepLogScoreResult?> = _repLogScore

    // Phase 2 Gap 5: weekly volume landmarks card on Home. Same freshness
    // predicate as the briefing so the two cards refresh together (a single
    // recompute + a single DB read covers both rather than re-entering the
    // bounded `getRecentCompletedSessions(60)` flow twice within a frame).
    private val _weeklyLandmarks = MutableStateFlow<List<com.replog.domain.volume.VolumeLandmark>?>(null)
    val weeklyLandmarks: StateFlow<List<com.replog.domain.volume.VolumeLandmark>?> = _weeklyLandmarks
    private var weeklyLandmarksLoadedForSessionCount = -1
    private var weeklyLandmarksLoadedForEpochDay: Int = -1

    // Phase 2 Gap 4: muscle-gap card on Home. Same freshness predicate as the
    // briefing + weekly volumes so all three intelligence cards refresh in
    // one frame rather than re-fetching the DNA snapshot + exercise library
    // three times within a single ON_RESUME tick.
    private val _muscleGapSuggestions = MutableStateFlow<List<com.replog.domain.musclegap.MuscleGapSuggestion>?>(null)
    val muscleGapSuggestions: StateFlow<List<com.replog.domain.musclegap.MuscleGapSuggestion>?> = _muscleGapSuggestions
    private var muscleGapLoadedForSessionCount = -1
    private var muscleGapLoadedForEpochDay: Int = -1

    /** The user's chosen display name for personalised greetings (null before onboarding). */
    val displayName: StateFlow<String?> = prefs.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Load (or refresh) the briefing + continue-workout card. Cheap to call repeatedly.
     *
     * Phase 2 Gap 2 reactive refresh:
     *   - Cached by (sessionCount, epochDay) so the briefing refreshes after a
     *     workout (session count climbed) OR after the local date rolls past
     *     midnight (epoch day), without leaving Home.
     *   - Cancels any in-flight recompute before launching a new one so two
     *     triggers firing back-to-back (e.g. ON_RESUME racing with the minute
     *     tick) cannot both run the expensive engine chain.
     */
    fun loadIntelligence() {
        intelligenceJob?.cancel()
        intelligenceJob = viewModelScope.launch {
            val activeId = prefs.activeSessionId.first()
            _continueWorkout.value = activeId?.let { id ->
                repo.getSessionById(id)?.takeIf { it.session.endTime == null }?.let { s ->
                    ContinueWorkout(
                        sessionId = id,
                        templateName = s.session.templateName ?: "Workout",
                        startTime = s.session.startTime,
                        exerciseCount = s.exercises.size,
                        setCount = s.exercises.sumOf { it.sets.size }
                    )
                }
            }
            val count = repo.getCompletedSessionCount()
            val today = currentEpochDay()
            if (count != briefingLoadedForSessionCount
                || today != briefingLoadedForEpochDay
                || _briefing.value == null
            ) {
                _briefing.value = runCatching { intelligenceRepository.buildBriefing() }.getOrNull()
                _repLogScore.value = runCatching { intelligenceRepository.buildRepLogScore() }.getOrNull()
                briefingLoadedForSessionCount = count
                briefingLoadedForEpochDay = today
            }
            // Phase 2 Gap 5: reuse the same (sessionCount, epochDay) guard so the
            // weekly volume card refreshes in lockstep with the briefing. Empty
            // list is a legitimate state (low-data fallback in the repository).
            if (count != weeklyLandmarksLoadedForSessionCount
                || today != weeklyLandmarksLoadedForEpochDay
                || _weeklyLandmarks.value == null
            ) {
                _weeklyLandmarks.value =
                    runCatching { intelligenceRepository.buildWeeklyLandmarks() }.getOrNull()
                weeklyLandmarksLoadedForSessionCount = count
                weeklyLandmarksLoadedForEpochDay = today
            }
            // Phase 2 Gap 4: muscle-gap card refreshes with the same predicate so
            // the briefing, weekly volumes, and muscle-gap trio are all consistent
            // after a workout or across a midnight boundary.
            if (count != muscleGapLoadedForSessionCount
                || today != muscleGapLoadedForEpochDay
                || _muscleGapSuggestions.value == null
            ) {
                _muscleGapSuggestions.value =
                    runCatching { intelligenceRepository.buildMuscleGapSuggestions() }.getOrNull()
                muscleGapLoadedForSessionCount = count
                muscleGapLoadedForEpochDay = today
            }
        }
    }

    /** Phase 2 Gap 2: thin refresh triggers wired from HomeScreen. */
    fun onResumed() = loadIntelligence()
    fun onMinuteTick() = loadIntelligence()

    /** Days since 1970-01-01 — monotonically increasing, no annual wrap. */
    private fun currentEpochDay(): Int = LocalDate.now().toEpochDay().toInt()

    /** P5: launch a favourite template (sets it active; Training tab resumes it). */
    fun startTemplate(template: com.replog.data.model.TemplateWithExercises) = viewModelScope.launch {
        workoutStarter.startTemplate(template)
    }

    /**
     * Phase 2 Gap 4 — start a focus workout for an under-trained muscle.
     *
     * Delegates to `IntelligenceRepository.startMuscleGapWorkout(muscle)` which
     * already creates a session with 4 ranked exercises and sets the active
     * session id in DataStore. The Home screen's `onStartWorkout` callback is
     * fired by the caller immediately after this returns (typically inside the
     * same chained click handler) so the user lands on the workout screen.
     */
    fun startMuscleGapFocus(muscle: String) = viewModelScope.launch {
        runCatching { intelligenceRepository.startMuscleGapWorkout(muscle) }
            .getOrNull() // success/failure is observed by Navigation moving to the active session
    }

    /** P6: one-tap repeat of a recent session. */
    fun repeatSession(session: SessionWithExercises) = viewModelScope.launch {
        workoutStarter.repeatSession(session)
    }

    /** P5: star/unstar a template from the Home quick-launch row. */
    fun toggleFavorite(template: com.replog.data.model.TemplateWithExercises) = viewModelScope.launch {
        repo.setTemplateFavorite(template.template.id, !template.template.isFavorite)
    }

    // Two TYPED combine groups (kotlinx provides typed combine up to 5 flows),
    // joined into a Pair. This replaces the previous combine(listOf(...)) with
    // positional `values[N] as Type` unchecked casts, which silently broke at
    // runtime (ClassCastException) if a flow was reordered. Now every value is
    // statically typed and a reorder is a compile error.
    private val homeGroupA: kotlinx.coroutines.flow.Flow<HomeGroupA> = combine(
        stats,
        repo.getRecentSessions(5),
        repo.getRecentPRs(),
        repo.getCompletedSessionSummaries(),
        repo.getRecentCompletedSessions(30)
    ) { s, sessions, prs, summaries, recentCompleted ->
        HomeGroupA(s, sessions, prs, summaries, recentCompleted)
    }
    private val homeGroupB: kotlinx.coroutines.flow.Flow<HomeGroupB> = combine(
        goalRepository.getActive(),
        repo.getFavoriteTemplates()
    ) { activeGoals, favorites -> HomeGroupB(activeGoals, favorites) }

    val uiState: StateFlow<HomeUiState> = combine(homeGroupA, homeGroupB) { a, b ->
        val s = a.stats
        val sessions = a.sessions
        val prs = a.prs
        val summaries = a.summaries
        val recentCompleted = a.recentCompleted
        val activeGoals = b.activeGoals
        val favorites = b.favorites
        val now = System.currentTimeMillis()
        // Phase 2: cheap stats from SQL-aggregated summaries (no full graph load).
        val startTimes = summaries.map { it.startTime }
        val startTimesToVolume = summaries.map { it.startTime to it.volume }
        val weekly = com.replog.domain.home.HomeDashboardStats.weeklyProgress(startTimesToVolume, now)

        // Top active goal with a live forecast (best-effort).
        val useKg = prefs.useKg.first()
        val topGoal = activeGoals.firstOrNull()?.let { goal ->
            runCatching {
                val f: GoalForecast = goalRepository.forecastFor(goal, useKg)
                HomeGoal(goal.title, f.progressPercent, f.etaText, f.summaryLine)
            }.getOrNull()
        }

        // Training Genome headline (only when it has learned something).
        // Uses a bounded recent window of full sessions (Phase 2).
        val genome = TrainingGenomeEngine.analyze(recentCompleted, now)
        val genomeHeadline = genome.takeIf { it.hasEnoughData }?.traits?.firstOrNull()
            ?.let { "${it.dimension}: ${it.bestValue}" }

        HomeUiState(
            sessionCount = s.first,
            totalVolume = s.second,
            recentSessions = sessions,
            recentPRs = prs,
            insight = buildHomeInsight(recentCompleted),
            sessionsThisWeek = weekly.sessionsThisWeek,
            volumeThisWeek = weekly.volumeThisWeek,
            dayStreak = com.replog.domain.home.HomeDashboardStats.dayStreak(startTimes, now),
            topGoal = topGoal,
            genomeHeadline = genomeHeadline,
            favoriteTemplates = favorites,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        stats.value = repo.getCompletedSessionCount() to repo.getTotalVolume()
    }

    private fun buildHomeInsight(sessions: List<SessionWithExercises>): HomeInsight {
        if (sessions.size < 3) return HomeInsight()
        val latest = sessions.maxByOrNull { it.session.startTime } ?: return HomeInsight()
        val latestVolume = latest.exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } }
        val avgRecentVolume = sessions.sortedByDescending { it.session.startTime }
            .take(4)
            .map { session -> session.exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } } }
            .average()
        val prCount = latest.exercises.sumOf { entry -> entry.sets.count { it.isPR } }
        return when {
            prCount > 0 -> HomeInsight("PB momentum", "Your last workout produced $prCount personal best${if (prCount == 1) "" else "s"}. Keep the next session focused and repeatable.")
            latestVolume > avgRecentVolume * 1.25 -> HomeInsight("Volume jump detected", "Your last session was much higher volume than recent average. Watch recovery before adding more work.")
            latestVolume < avgRecentVolume * 0.7 -> HomeInsight("Lower-volume session", "Your last session was lighter than usual. Good if intentional; if not, check fatigue or schedule pressure.")
            else -> HomeInsight("Training looks steady", "Recent workload is consistent. Keep progressing one variable at a time: reps, load, sets or technique quality.")
        }
    }
}
