package com.replog.util.timer

import android.content.Context
import android.content.Intent
import android.os.Build
import com.replog.data.db.RestLogDao
import com.replog.data.model.Exercise
import com.replog.data.model.RestLog
import com.replog.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class RestTimerState(
    val active: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val endAtMillis: Long = 0L,
    val startedAtMillis: Long = 0L,
    val ready: Boolean = false,
    val sessionExerciseId: Int? = null,
    val setId: Int? = null
)

@Singleton
class RestTimerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val restLogDao: RestLogDao
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var currentRestLogId: Int? = null
    private var onCompleteCallback: (() -> Unit)? = null

    init {
        scope.launch {
            // Restore persisted timer
            prefs.restTimerPersist.first()?.let { p ->
                val now = System.currentTimeMillis()
                if (p.endAt > now) {
                    resumeTimer(p.endAt, p.startedAt, p.plannedSeconds, p.sessionExerciseId, p.setId)
                } else if (p.endAt > 0) {
                    _state.value = RestTimerState(active = false, ready = true, totalSeconds = p.plannedSeconds, remainingSeconds = 0, endAtMillis = p.endAt, startedAtMillis = p.startedAt, sessionExerciseId = p.sessionExerciseId, setId = p.setId)
                    prefs.persistRestTimer(0,0,0,null,null)
                }
            }
        }
    }

    fun setOnComplete(cb: (() -> Unit)?) { onCompleteCallback = cb }

    suspend fun resolveRestSeconds(exercise: Exercise): Int {
        val presets = prefs.restPresets.first()
        return RestPresetResolver.resolve(exercise, presets)
    }

    suspend fun start(seconds: Int, sessionExerciseId: Int? = null, setId: Int? = null) {
        cancelInternal(save = true, skipped = false)
        val s = seconds.coerceIn(15, 600)
        val now = System.currentTimeMillis()
        val endAt = now + s * 1000L

        // Log rest start
        if (sessionExerciseId != null) {
            currentRestLogId = restLogDao.insert(
                RestLog(
                    sessionExerciseId = sessionExerciseId,
                    setId = setId,
                    plannedSeconds = s,
                    actualSeconds = null,
                    skipped = false,
                    startedAt = now,
                    completedAt = null
                )
            ).toInt()
        }

        prefs.persistRestTimer(endAt, now, s, sessionExerciseId, setId)
        resumeTimer(endAt, now, s, sessionExerciseId, setId)

        // Start foreground service
        val intent = Intent(context, com.replog.ui.workout.RestTimerService::class.java).apply {
            action = com.replog.ui.workout.RestTimerService.ACTION_START
            putExtra(com.replog.ui.workout.RestTimerService.EXTRA_END_AT, endAt)
        }
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
    }

    private fun resumeTimer(endAt: Long, startedAt: Long, totalSeconds: Int, sessionExerciseId: Int?, setId: Int?) {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((endAt - now) / 1000).toInt()
                if (remaining <= 0) {
                    _state.value = RestTimerState(active = false, ready = true, remainingSeconds = 0, totalSeconds = totalSeconds, endAtMillis = endAt, startedAtMillis = startedAt, sessionExerciseId = sessionExerciseId, setId = setId)
                    completeRestLog(totalSeconds, false)
                    prefs.persistRestTimer(0,0,0,null,null)
                    onCompleteCallback?.invoke()
                    stopService()
                    break
                } else {
                    _state.value = RestTimerState(active = true, ready = false, remainingSeconds = remaining, totalSeconds = totalSeconds, endAtMillis = endAt, startedAtMillis = startedAt, sessionExerciseId = sessionExerciseId, setId = setId)
                    delay(250)
                }
            }
        }
    }

    suspend fun addSeconds(delta: Int) {
        val cur = _state.value
        if (!cur.active) return
        val newEnd = cur.endAtMillis + delta * 1000L
        val newTotal = cur.totalSeconds + delta
        prefs.persistRestTimer(newEnd, cur.startedAtMillis, newTotal.coerceAtLeast(15), cur.sessionExerciseId, cur.setId)
        resumeTimer(newEnd, cur.startedAtMillis, newTotal.coerceAtLeast(15), cur.sessionExerciseId, cur.setId)
        // update service
        val intent = Intent(context, com.replog.ui.workout.RestTimerService::class.java).apply {
            action = com.replog.ui.workout.RestTimerService.ACTION_START
            putExtra(com.replog.ui.workout.RestTimerService.EXTRA_END_AT, newEnd)
        }
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
    }

    suspend fun skip() { cancelInternal(save = true, skipped = true) }
    suspend fun restart() {
        val cur = _state.value
        val total = if (cur.totalSeconds > 0) cur.totalSeconds else 90
        start(total, cur.sessionExerciseId, cur.setId)
    }

    suspend fun cancel() { cancelInternal(save = true, skipped = true) }

    private suspend fun cancelInternal(save: Boolean, skipped: Boolean) {
        tickJob?.cancel()
        val cur = _state.value
        if (save && currentRestLogId != null && cur.startedAtMillis > 0) {
            val actual = ((System.currentTimeMillis() - cur.startedAtMillis) / 1000).toInt().coerceAtLeast(0)
            runCatching { 
                restLogDao.getById(currentRestLogId!!)?.let { log ->
                    restLogDao.update(log.copy(actualSeconds = actual, skipped = skipped, completedAt = System.currentTimeMillis()))
                }
            }
        }
        currentRestLogId = null
        _state.value = RestTimerState()
        prefs.persistRestTimer(0,0,0,null,null)
        stopService()
    }

    private suspend fun completeRestLog(actualSeconds: Int, skipped: Boolean) {
        currentRestLogId?.let { id ->
            runCatching {
                restLogDao.getById(id)?.let { log ->
                    restLogDao.update(log.copy(actualSeconds = actualSeconds, skipped = skipped, completedAt = System.currentTimeMillis()))
                }
            }
        }
        currentRestLogId = null
    }

    private fun stopService() {
        context.stopService(Intent(context, com.replog.ui.workout.RestTimerService::class.java))
    }
}
