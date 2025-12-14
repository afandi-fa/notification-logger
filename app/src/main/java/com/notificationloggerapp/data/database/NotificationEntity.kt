package com.notificationloggerapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val notificationId: Int,
    val channelId: String?,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val priority: Int,
    val isOngoing: Boolean,
    val isDismissible: Boolean,
    val timestampReceived: Long,
    val timestampRemoved: Long?,
    val rawExtras: String?,
    val isOTP: Boolean = false,
    val otpCode: String? = null,
    val category: String?
)