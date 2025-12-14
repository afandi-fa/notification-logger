package com.notificationloggerapp.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.google.gson.Gson
import com.notificationloggerapp.data.database.AppDatabase
import com.notificationloggerapp.data.database.NotificationEntity
import com.notificationloggerapp.utils.OTPDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private val otpDetector = OTPDetector()
    private val gson = Gson()

    private fun isAppBlocked(packageName: String): Boolean {
        val prefs = getSharedPreferences("filter_prefs", Context.MODE_PRIVATE)
        val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        return blockedApps.contains(packageName)
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(applicationContext)
        startForegroundServiceIfNeeded()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Check if app is blocked
        if (isAppBlocked(sbn.packageName)) {
            return // Skip blocked apps
        }

        serviceScope.launch {
            try {
                val notification = sbn.notification
                val extras = notification.extras

                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

                // OTP Detection
                val fullText = "$title $text $bigText"
                val otpResult = otpDetector.detectOTP(fullText)

                val entity = NotificationEntity(
                    packageName = sbn.packageName,
                    appName = getAppName(sbn.packageName),
                    notificationId = sbn.id,
                    channelId = notification.channelId,
                    title = title,
                    text = text,
                    subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                    bigText = bigText,
                    priority = notification.priority,
                    isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0,
                    isDismissible = (notification.flags and Notification.FLAG_NO_CLEAR) == 0,
                    timestampReceived = System.currentTimeMillis(),
                    timestampRemoved = null,
                    rawExtras = bundleToJson(extras),
                    isOTP = otpResult.first,
                    otpCode = otpResult.second,
                    category = notification.category
                )

                database.notificationDao().insert(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Track removal timestamp if needed
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun bundleToJson(bundle: Bundle): String {
        val map = mutableMapOf<String, Any?>()
        for (key in bundle.keySet()) {
            try {
                map[key] = bundle.get(key)?.toString()
            } catch (e: Exception) {
                // Skip unparseable values
            }
        }
        return gson.toJson(map)
    }

    private fun startForegroundServiceIfNeeded() {
        val intent = Intent(this, ForegroundService::class.java)
        startService(intent)
    }
}