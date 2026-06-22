package com.replog.ui.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.replog.MainActivity
import com.replog.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RestTimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD_30 -> adjustEnd(30)
            ACTION_ADD_60 -> adjustEnd(60)
            ACTION_SKIP -> { stopTimer(true); return START_NOT_STICKY }
            ACTION_RESTART -> { /* handled by manager */ }
            else -> { /* START */ }
        }
        val endAt = intent?.getLongExtra(EXTRA_END_AT, 0L) ?: 0L
        if (endAt > 0) startTimer(endAt)
        return START_STICKY
    }

    private var currentEndAt = 0L
    private fun startTimer(endAt: Long) {
        currentEndAt = endAt
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Rest timer", timeLeft(endAt), remaining = 60, total = 60, running = true))
        tickJob?.cancel()
        tickJob = scope.launch {
            var wasReady = false
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((endAt - now) / 1000).coerceAtLeast(0).toInt()
                val total = ((endAt - (endAt - remaining * 1000L - 60000)) / 1000).toInt() // fallback
                if (remaining <= 0) {
                    if (!wasReady) {
                        wasReady = true
                        notifyComplete()
                        updateNotification("READY FOR NEXT SET", "Time to lift!", 0, 1, false)
                    }
                    delay(1000)
                    continue
                }
                updateNotification("Rest timer", timeLeft(endAt), remaining, remaining + 30, true)
                delay(1000)
            }
        }
    }

    private fun adjustEnd(deltaSec: Int) {
        if (currentEndAt > 0) startTimer(currentEndAt + deltaSec * 1000L)
    }

    private fun stopTimer(skipped: Boolean) {
        tickJob?.cancel()
        stopForeground(true)
        stopSelf()
    }

    private fun notifyComplete() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Vibration
        try {
            val vibrate = if (Build.VERSION.SDK_INT >= 33) {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.ringerMode == AudioManager.RINGER_MODE_NORMAL
            } else true
            if (vibrate) {
                val vib = if (Build.VERSION.SDK_INT >= 31) {
                    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26) {
                    vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1))
                } else @Suppress("DEPRECATION") vib.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
            }
        } catch (_: Exception) {}

        // Sound
        try {
            RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
        } catch (_: Exception) {}
    }

    private fun timeLeft(endAt: Long): String {
        val s = ((endAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0).toInt()
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun updateNotification(title: String, text: String, remaining: Int, total: Int, ongoing: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(title, text, remaining, total, ongoing))
    }

    private fun buildNotification(title: String, text: String, remaining: Int, total: Int, ongoing: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val add30 = PendingIntent.getService(this, 1, Intent(this, RestTimerService::class.java).apply { action = ACTION_ADD_30 }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val add60 = PendingIntent.getService(this, 2, Intent(this, RestTimerService::class.java).apply { action = ACTION_ADD_60 }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val skip = PendingIntent.getService(this, 3, Intent(this, RestTimerService::class.java).apply { action = ACTION_SKIP }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "+30s", add30)
            .addAction(0, "+60s", add60)
            .addAction(0, "Skip", skip)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Workout rest timer"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            })
        }
    }

    override fun onDestroy() { tickJob?.cancel(); super.onDestroy() }

    companion object {
        const val ACTION_START = "com.replog.REST_START"
        const val ACTION_ADD_30 = "com.replog.REST_ADD_30"
        const val ACTION_ADD_60 = "com.replog.REST_ADD_60"
        const val ACTION_SKIP = "com.replog.REST_SKIP"
        const val ACTION_RESTART = "com.replog.REST_RESTART"
        const val EXTRA_END_AT = "end_at"
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 1001
    }
}
