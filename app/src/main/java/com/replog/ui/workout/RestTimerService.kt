package com.replog.ui.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.replog.R

class RestTimerService : Service() {
    private var timer: CountDownTimer? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra(EXTRA_SECONDS, 90) ?: 90
        ensureChannel()
        startForeground(1001, notification("Rest timer", "$seconds seconds remaining"))
        timer?.cancel()
        timer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) { val left = (ms / 1000L).toInt(); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1001, notification("Rest timer", "$left seconds remaining")) }
            override fun onFinish() { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1001, notification("Rest complete", "Time for your next set.")); stopSelf() }
        }.start()
        return START_NOT_STICKY
    }
    override fun onDestroy() { timer?.cancel(); super.onDestroy() }
    private fun notification(title: String, text: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle(title).setContentText(text).setOnlyAlertOnce(true).setOngoing(true).build()
    private fun ensureChannel() { if (Build.VERSION.SDK_INT >= 26) (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_LOW)) }
    companion object { const val EXTRA_SECONDS = "seconds"; private const val CHANNEL_ID = "rest_timer" }
}
