package com.notificationloggerapp.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.gson.GsonBuilder
import com.notificationloggerapp.data.database.AppDatabase
import com.notificationloggerapp.data.database.AppInfo
import com.notificationloggerapp.data.database.NotificationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.notificationDao()
    private val prefs = application.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedNotification = MutableStateFlow<NotificationEntity?>(null)
    val selectedNotification: StateFlow<NotificationEntity?> = _selectedNotification.asStateFlow()

    // Filter: Blocked Apps
    private val _blockedApps = MutableStateFlow<Set<String>>(loadBlockedApps())
    val blockedApps: StateFlow<Set<String>> = _blockedApps.asStateFlow()

    // Filter: Keywords
    private val _keywords = MutableStateFlow<List<String>>(loadKeywords())
    val keywords: StateFlow<List<String>> = _keywords.asStateFlow()

    // Rules: Auto-delete
    private val _autoDeleteEnabled = MutableStateFlow(prefs.getBoolean("auto_delete_enabled", false))
    val autoDeleteEnabled: StateFlow<Boolean> = _autoDeleteEnabled.asStateFlow()

    private val _autoDeleteDays = MutableStateFlow(prefs.getInt("auto_delete_days", 30))
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays.asStateFlow()

    // Rules: Auto-export
    private val _autoExportEnabled = MutableStateFlow(prefs.getBoolean("auto_export_enabled", false))
    val autoExportEnabled: StateFlow<Boolean> = _autoExportEnabled.asStateFlow()

    // Distinct apps
    private val _distinctApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val distinctApps: StateFlow<List<AppInfo>> = _distinctApps.asStateFlow()

    // Export status
    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    init {
        loadDistinctApps()
        checkAutoDelete()
    }

    val notifications: StateFlow<List<NotificationEntity>> = combine(
        _searchQuery,
        _blockedApps
    ) { query, blocked ->
        Pair(query, blocked)
    }.flatMapLatest { (query, blocked) ->
        if (query.isEmpty()) {
            dao.getAllNotifications()
        } else {
            dao.searchNotifications(query)
        }
    }.map { list ->
        // Filter out blocked apps
        list.filter { !_blockedApps.value.contains(it.packageName) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectNotification(notification: NotificationEntity) {
        _selectedNotification.value = notification
    }

    // Filter: Apps
    fun blockApp(packageName: String) {
        val updated = _blockedApps.value.toMutableSet()
        updated.add(packageName)
        _blockedApps.value = updated
        saveBlockedApps(updated)
    }

    fun unblockApp(packageName: String) {
        val updated = _blockedApps.value.toMutableSet()
        updated.remove(packageName)
        _blockedApps.value = updated
        saveBlockedApps(updated)
    }

    private fun loadBlockedApps(): Set<String> {
        val saved = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        return saved.toSet()
    }

    private fun saveBlockedApps(apps: Set<String>) {
        prefs.edit().putStringSet("blocked_apps", apps).apply()
    }

    // Filter: Keywords
    fun addKeyword(keyword: String) {
        val updated = _keywords.value.toMutableList()
        if (!updated.contains(keyword)) {
            updated.add(keyword)
            _keywords.value = updated
            saveKeywords(updated)
        }
    }

    fun removeKeyword(keyword: String) {
        val updated = _keywords.value.toMutableList()
        updated.remove(keyword)
        _keywords.value = updated
        saveKeywords(updated)
    }

    private fun loadKeywords(): List<String> {
        val saved = prefs.getStringSet("keywords", emptySet()) ?: emptySet()
        return saved.toList()
    }

    private fun saveKeywords(keywords: List<String>) {
        prefs.edit().putStringSet("keywords", keywords.toSet()).apply()
    }

    // Rules: Auto-delete
    fun setAutoDelete(enabled: Boolean) {
        _autoDeleteEnabled.value = enabled
        prefs.edit().putBoolean("auto_delete_enabled", enabled).apply()
        if (enabled) {
            checkAutoDelete()
        }
    }

    fun setAutoDeleteDays(days: Int) {
        _autoDeleteDays.value = days
        prefs.edit().putInt("auto_delete_days", days).apply()
    }

    private fun checkAutoDelete() {
        if (_autoDeleteEnabled.value) {
            viewModelScope.launch {
                val cutoffTime = System.currentTimeMillis() - (_autoDeleteDays.value * 86400000L)
                dao.deleteOlderThan(cutoffTime)
            }
        }
    }

    // Rules: Auto-export
    fun setAutoExport(enabled: Boolean) {
        _autoExportEnabled.value = enabled
        prefs.edit().putBoolean("auto_export_enabled", enabled).apply()
    }

    // Distinct Apps
    private fun loadDistinctApps() {
        viewModelScope.launch {
            val apps = dao.getDistinctApps()
            _distinctApps.value = apps
        }
    }

    fun refreshDistinctApps() {
        loadDistinctApps()
    }

    // Data Management
    fun clearAllData() {
        viewModelScope.launch {
            dao.deleteAll()
            _distinctApps.value = emptyList()
        }
    }

    fun exportToCSV(): String? {
        var filePath: String? = null
        viewModelScope.launch {
            try {
                _exportStatus.value = ExportStatus.Loading

                val allNotifications = dao.getAllNotifications().first()

                if (allNotifications.isEmpty()) {
                    _exportStatus.value = ExportStatus.Error("No notifications to export")
                    return@launch
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "notifications_$timestamp.csv"

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                val csvContent = buildString {
                    // Header
                    appendLine("ID,App Name,Package Name,Title,Text,Big Text,Sub Text,Channel ID,Priority,Category,Is OTP,OTP Code,Is Ongoing,Is Dismissible,Timestamp Received,Timestamp Removed")

                    // Data rows
                    allNotifications.forEach { notification ->
                        append("${notification.id},")
                        append("\"${escapeCSV(notification.appName)}\",")
                        append("\"${escapeCSV(notification.packageName)}\",")
                        append("\"${escapeCSV(notification.title)}\",")
                        append("\"${escapeCSV(notification.text)}\",")
                        append("\"${escapeCSV(notification.bigText)}\",")
                        append("\"${escapeCSV(notification.subText)}\",")
                        append("\"${escapeCSV(notification.channelId)}\",")
                        append("${notification.priority},")
                        append("\"${escapeCSV(notification.category)}\",")
                        append("${notification.isOTP},")
                        append("\"${escapeCSV(notification.otpCode)}\",")
                        append("${notification.isOngoing},")
                        append("${notification.isDismissible},")
                        append("${notification.timestampReceived},")
                        appendLine("${notification.timestampRemoved ?: ""}")
                    }
                }

                file.writeText(csvContent)
                filePath = file.absolutePath

                _exportStatus.value = ExportStatus.Success(file.absolutePath, allNotifications.size)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
            }
        }
        return filePath
    }

    fun exportToJSON(): String? {
        var filePath: String? = null
        viewModelScope.launch {
            try {
                _exportStatus.value = ExportStatus.Loading

                val allNotifications = dao.getAllNotifications().first()

                if (allNotifications.isEmpty()) {
                    _exportStatus.value = ExportStatus.Error("No notifications to export")
                    return@launch
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "notifications_$timestamp.json"

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                val gson = GsonBuilder()
                    .setPrettyPrinting()
                    .create()

                val jsonContent = gson.toJson(allNotifications)

                file.writeText(jsonContent)
                filePath = file.absolutePath

                _exportStatus.value = ExportStatus.Success(file.absolutePath, allNotifications.size)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
            }
        }
        return filePath
    }

    private fun escapeCSV(value: String?): String {
        if (value == null) return ""
        return value.replace("\"", "\"\"")
    }

    fun resetExportStatus() {
        _exportStatus.value = ExportStatus.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return NotificationViewModel(application) as T
            }
        }
    }
}

sealed class ExportStatus {
    object Idle : ExportStatus()
    object Loading : ExportStatus()
    data class Success(val filePath: String, val count: Int) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}