package com.hackerankit.ai

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AssistantForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "hacker_ankit_active"
        const val NOTIFICATION_ID = 1001
    }
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Hacker Ankit AI", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        val stopIntent = Intent(this, AssistantForegroundService::class.java).setAction("STOP")
        val pi = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Hacker Ankit AI — Active")
            .setContentText("Assistant active. Listening only when the user starts voice recognition.")
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this, 3, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", pi)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
