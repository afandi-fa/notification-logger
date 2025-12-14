package com.notificationloggerapp.data.repository

import com.notificationloggerapp.data.database.NotificationDao
import com.notificationloggerapp.data.database.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {

    fun getAllNotifications(): Flow<List<NotificationEntity>> {
        return dao.getAllNotifications()
    }

    fun searchNotifications(query: String): Flow<List<NotificationEntity>> {
        return dao.searchNotifications(query)
    }

    suspend fun insert(notification: NotificationEntity) {
        dao.insert(notification)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
