package com.hackerankit.ai

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AssistantForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "hacker_ankit_active"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_LISTENING = "START_LISTENING"
        const val ACTION_STOP = "STOP"
    }

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hacker Ankit AI",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val openAppIntent = Intent(this, MainActivity::class.java)

        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            10,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Hacker Ankit AI")
            .setContentText("Assistant active")
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
