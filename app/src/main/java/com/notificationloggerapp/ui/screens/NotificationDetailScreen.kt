package com.notificationloggerapp.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.notificationloggerapp.data.database.NotificationEntity
import com.notificationloggerapp.utils.copyToClipboard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notification: NotificationEntity,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = notification.appName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column {
                            Text(
                                text = notification.appName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = formatFullTimestamp(notification.timestampReceived),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider()

                    DetailRow("Package", notification.packageName)
                    DetailRow("Notification ID", notification.notificationId.toString())
                    notification.channelId?.let {
                        DetailRow("Channel ID", it)
                    }
                }
            }

            // Content Section
            if (!notification.title.isNullOrEmpty() || !notification.text.isNullOrEmpty()) {
                DetailSection(title = "Content") {
                    notification.title?.let {
                        DetailField(
                            label = "Title",
                            value = it,
                            onCopy = { copyToClipboard(context, "Title", it) }
                        )
                    }
                    notification.text?.let {
                        DetailField(
                            label = "Text",
                            value = it,
                            onCopy = { copyToClipboard(context, "Text", it) }
                        )
                    }
                    notification.bigText?.let {
                        if (it != notification.text) {
                            DetailField(
                                label = "Expanded Text",
                                value = it,
                                onCopy = { copyToClipboard(context, "Expanded Text", it) }
                            )
                        }
                    }
                    notification.subText?.let {
                        DetailField(
                            label = "Sub Text",
                            value = it,
                            onCopy = { copyToClipboard(context, "Sub Text", it) }
                        )
                    }
                }
            }

            // OTP Section
            if (notification.isOTP && notification.otpCode != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OTP Detected",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = notification.otpCode,
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        FilledTonalButton(
                            onClick = {
                                copyToClipboard(context, "OTP", notification.otpCode)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy OTP")
                        }
                    }
                }
            }

            // Properties Section
            DetailSection(title = "Properties") {
                DetailRow("Priority", getPriorityText(notification.priority))
                DetailRow("Category", notification.category ?: "None")
                DetailRow("Ongoing", if (notification.isOngoing) "Yes" else "No")
                DetailRow("Dismissible", if (notification.isDismissible) "Yes" else "No")
            }

            // Raw Data (Collapsed by default)
            notification.rawExtras?.let {
                DetailSection(title = "Raw Extras") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DetailField(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun formatFullTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getPriorityText(priority: Int): String {
    return when (priority) {
        -2 -> "Minimum"
        -1 -> "Low"
        0 -> "Default"
        1 -> "High"
        2 -> "Maximum"
        else -> "Unknown ($priority)"
    }
}

//fun copyToClipboard(context: Context, label: String, text: String) {
//    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
//    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
//}
