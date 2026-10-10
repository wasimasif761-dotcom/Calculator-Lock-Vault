package com.example.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultRepository
import com.example.model.LockableApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideAppsScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val apps by repository.lockedApps.collectAsState()
    var selectedAppToLaunch by remember { mutableStateOf<LockableApp?>(null) }
    var showDisguiseGuide by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hide Apps", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDisguiseGuide = true },
                        modifier = Modifier.testTag("disguise_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Disguise Guide",
                            tint = Color(0xFF60A5FA)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151B))
            )
        },
        containerColor = Color(0xFF13151B)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .testTag("hide_apps_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                // Hero Banner matching Screenshot 5
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2435))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hide Apps",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Hide apps so no one can find them",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Selected apps will be removed from external view and kept safely inside this encrypted vault space.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Stealth Management",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "${apps.count { it.isHidden }} hidden",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF818CF8))
                    )
                }
            }

            items(apps) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hide_app_card_${app.packageName}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181C26))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAppToLaunch = app }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(app.iconColor),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = app.appName.take(1),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (app.isHidden) "Invisible on phone • In Vault" else app.category,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (app.isHidden) Color(0xFF818CF8) else Color(0xFF9CA3AF)
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = app.isHidden,
                            onCheckedChange = { repository.toggleAppHide(app.packageName) },
                            modifier = Modifier.testTag("toggle_hide_${app.packageName}")
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Launch App Simulator Dialog
    selectedAppToLaunch?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppToLaunch = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(app.iconColor),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(app.appName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Launch ${app.appName}", color = Color.White)
                }
            },
            text = {
                Text(
                    "Launching ${app.appName} securely inside the isolated hidden vault container without leaving traces on the device system launcher.",
                    color = Color(0xFFD1D5DB)
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedAppToLaunch = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Open App")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppToLaunch = null }) {
                    Text("Cancel", color = Color(0xFF9CA3AF))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }

    // Disguise Guide Dialog
    if (showDisguiseGuide) {
        AlertDialog(
            onDismissRequest = { showDisguiseGuide = false },
            title = { Text("App Icon Disguise & Stealth Guide", color = Color.White) },
            text = {
                Text(
                    "How Hide Apps works:\n\n" +
                    "1. Stealth Container: Hidden apps are moved into the vault's private space.\n" +
                    "2. Camouflage: The public launcher only shows the Calculator disguise icon.\n" +
                    "3. Zero Trace: Notifications and app icons are routed exclusively through Calculator Lock.",
                    color = Color(0xFFD1D5DB)
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisguiseGuide = false }) {
                    Text("Close", color = Color(0xFF60A5FA))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }
}
