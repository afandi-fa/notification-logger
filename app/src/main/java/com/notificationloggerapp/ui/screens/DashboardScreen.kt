package com.notificationloggerapp.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import com.notificationloggerapp.utils.copyToClipboard
import com.notificationloggerapp.data.database.NotificationEntity
import com.notificationloggerapp.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: NotificationViewModel
) {
    val notifications by viewModel.notifications.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    var selectedNotification by remember { mutableStateOf<NotificationEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications (${notifications.size})") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Text("⚙️")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { sendTestNotification(context) }
            ) {
                Text("🧪", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search notifications...") },
                singleLine = true,
                leadingIcon = { Text("🔍") }
            )

            // Info Card
            if (notifications.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📱",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No notifications yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the 🧪 button to send a test notification",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Notification List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = notifications.size) { index ->
                    val notification = notifications[index]
                    NotificationCard(
                        notification = notification,
                        onClick = { selectedNotification = notification }
                    )
                }
            }
        }
    }

    // Detail Dialog
    selectedNotification?.let { notification ->
        NotificationDetailDialog(
            notification = notification,
            onDismiss = { selectedNotification = null }
        )
    }
}

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: App name + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = notification.appName.take(1).uppercase(),
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = notification.appName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = formatTime(notification.timestampReceived),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            if (!notification.title.isNullOrEmpty()) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Body Text
            if (!notification.text.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // OTP Badge
            if (notification.isOTP && notification.otpCode != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "🔐 OTP: ${notification.otpCode}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Tap to view indicator
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "👆 Tap to view details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailDialog(
    notification: NotificationEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatFullTime(notification.timestampReceived),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Text("✕", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // App Info
                    DetailSection(
                        icon = "📱",
                        title = "Application",
                        content = notification.appName,
                        onCopy = { copyToClipboard(context, "App Name", notification.appName) }
                    )

                    DetailSection(
                        icon = "📦",
                        title = "Package Name",
                        content = notification.packageName,
                        onCopy = { copyToClipboard(context, "Package", notification.packageName) }
                    )

                    // Title
                    if (!notification.title.isNullOrEmpty()) {
                        DetailSection(
                            icon = "📰",
                            title = "Title",
                            content = notification.title,
                            onCopy = { copyToClipboard(context, "Title", notification.title) }
                        )
                    }

                    // Text
                    if (!notification.text.isNullOrEmpty()) {
                        DetailSection(
                            icon = "💬",
                            title = "Message",
                            content = notification.text,
                            onCopy = { copyToClipboard(context, "Message", notification.text) }
                        )
                    }

                    // Big Text
                    if (!notification.bigText.isNullOrEmpty() && notification.bigText != notification.text) {
                        DetailSection(
                            icon = "📄",
                            title = "Full Message",
                            content = notification.bigText,
                            onCopy = { copyToClipboard(context, "Full Message", notification.bigText) }
                        )
                    }

                    // OTP
                    if (notification.isOTP && notification.otpCode != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🔐", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = "OTP Detected",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = notification.otpCode,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        copyToClipboard(context, "OTP", notification.otpCode)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📋 Copy OTP")
                                }
                            }
                        }
                    }

                    // SubText
                    if (!notification.subText.isNullOrEmpty()) {
                        DetailSection(
                            icon = "📌",
                            title = "Sub Text",
                            content = notification.subText,
                            onCopy = { copyToClipboard(context, "Sub Text", notification.subText) }
                        )
                    }

                    // Channel
                    if (!notification.channelId.isNullOrEmpty()) {
                        DetailSection(
                            icon = "📡",
                            title = "Channel ID",
                            content = notification.channelId,
                            onCopy = { copyToClipboard(context, "Channel ID", notification.channelId) }
                        )
                    }

                    // Priority
                    DetailSection(
                        icon = "⚡",
                        title = "Priority",
                        content = when (notification.priority) {
                            -2 -> "Minimum"
                            -1 -> "Low"
                            0 -> "Default"
                            1 -> "High"
                            2 -> "Maximum"
                            else -> "Unknown (${notification.priority})"
                        },
                        onCopy = null
                    )

                    // Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (notification.isOngoing) {
                            Chip(text = "Ongoing")
                        }
                        if (notification.isDismissible) {
                            Chip(text = "Dismissible")
                        }
                        if (!notification.category.isNullOrEmpty()) {
                            Chip(text = notification.category)
                        }
                    }
                }

                // Footer
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    icon: String,
    title: String,
    content: String,
    onCopy: (() -> Unit)?
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (onCopy != null) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("📋", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

//private fun copyToClipboard(context: Context, label: String, text: String) {
//    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//    val clip = ClipData.newPlainText(label, text)
//    clipboard.setPrimaryClip(clip)
//    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
//}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun formatFullTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun sendTestNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "test_channel",
            "Test Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for test notifications"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val testOTP = (1000..9999).random()

    val notification = NotificationCompat.Builder(context, "test_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Test Notification #${System.currentTimeMillis() % 1000}")
        .setContentText("Your OTP code is $testOTP. This is a test notification from Notification Logger app.")
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("Your OTP code is $testOTP. This is a test notification with a long message. This demonstrates how the app captures and displays notifications with extended content. The full text will be visible in the detail view when you tap on the notification card."))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
}