package com.notificationloggerapp.data.database



import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestampReceived DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY timestampReceived DESC")
    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE appName LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%' OR bigText LIKE '%' || :query || '%' ORDER BY timestampReceived DESC")
    fun searchNotifications(query: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isOTP = 1 ORDER BY timestampReceived DESC")
    fun getOTPNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT DISTINCT packageName, appName FROM notifications")
    suspend fun getDistinctApps(): List<AppInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Update
    suspend fun update(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE timestampReceived < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getCount(): Int
}

data class AppInfo(
    val packageName: String,
    val appName: String
)