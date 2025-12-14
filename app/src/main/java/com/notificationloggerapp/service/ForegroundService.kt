package com.notificationloggerapp.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class ForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notification Logger Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the notification logger running"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Notification Logger")
                .setContentText("Logging notifications in background")
                .setSmallIcon(R.drawable.ic_dialog_info)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Notification Logger")
                .setContentText("Logging notifications in background")
                .setSmallIcon(R.drawable.ic_dialog_info)
                .build()
        }
    }

    companion object {
        private const val CHANNEL_ID = "notification_logger_service"
    }
}