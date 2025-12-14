package com.notificationloggerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.notificationloggerapp.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by remember { mutableStateOf(0) }

    // Refresh apps list when screen appears
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDistinctApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filters & Rules") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Bar
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Apps") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Keywords") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Rules") }
                )
            }

            when (selectedTab) {
                0 -> AppFilterTab(viewModel)
                1 -> KeywordFilterTab(viewModel)
                2 -> RulesTab(viewModel)
            }
        }
    }
}

@Composable
fun AppFilterTab(viewModel: NotificationViewModel) {
    val apps by viewModel.distinctApps.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Block notifications from specific apps",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Blocked apps: ${blockedApps.size} • Total apps: ${apps.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (apps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No apps have sent notifications yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Generate a test notification to see apps here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(apps.size) { index ->
                        val app = apps[index]
                        val isBlocked = blockedApps.contains(app.packageName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isBlocked)
                                        MaterialTheme.colorScheme.errorContainer
                                    else
                                        MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isBlocked) "✕" else app.appName.take(1).uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isBlocked)
                                                MaterialTheme.colorScheme.onErrorContainer
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = !isBlocked,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        viewModel.unblockApp(app.packageName)
                                    } else {
                                        viewModel.blockApp(app.packageName)
                                    }
                                }
                            )
                        }
                        if (index < apps.size - 1) {
                            Divider(modifier = Modifier.padding(start = 44.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeywordFilterTab(viewModel: NotificationViewModel) {
    val keywords by viewModel.keywords.collectAsState()
    var newKeyword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Highlight notifications containing these keywords",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Active keywords: ${keywords.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newKeyword,
                onValueChange = { newKeyword = it },
                modifier = Modifier.weight(1f),
                label = { Text("Add keyword") },
                placeholder = { Text("e.g., OTP, urgent, payment") },
                singleLine = true
            )
            FilledTonalButton(
                onClick = {
                    if (newKeyword.isNotBlank()) {
                        viewModel.addKeyword(newKeyword.trim())
                        newKeyword = ""
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }

        if (keywords.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No keywords added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Highlighted notifications will have a colored border",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tap to remove:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        keywords.forEach { keyword ->
                            KeywordChip(
                                text = keyword,
                                onRemove = { viewModel.removeKeyword(keyword) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeywordChip(text: String, onRemove: () -> Unit) {
    AssistChip(
        onClick = onRemove,
        label = { Text(text) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
fun RulesTab(viewModel: NotificationViewModel) {
    val autoDeleteEnabled by viewModel.autoDeleteEnabled.collectAsState()
    val autoDeleteDays by viewModel.autoDeleteDays.collectAsState()
    val autoExportEnabled by viewModel.autoExportEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Automation rules",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RuleItem(
                    title = "Auto-delete old notifications",
                    description = "Delete notifications older than $autoDeleteDays days",
                    checked = autoDeleteEnabled,
                    onCheckedChange = { viewModel.setAutoDelete(it) }
                )

                if (autoDeleteEnabled) {
                    Slider(
                        value = autoDeleteDays.toFloat(),
                        onValueChange = { viewModel.setAutoDeleteDays(it.toInt()) },
                        valueRange = 7f..90f,
                        steps = 0,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "Delete after $autoDeleteDays days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Divider()

                RuleItem(
                    title = "Auto-export daily",
                    description = "Export logs to CSV every 24 hours",
                    checked = autoExportEnabled,
                    onCheckedChange = { viewModel.setAutoExport(it) }
                )
            }
        }
    }
}

@Composable
fun RuleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}