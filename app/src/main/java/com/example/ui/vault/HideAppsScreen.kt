package com.example.ui.vault

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultRepository
import com.example.model.LockableApp

enum class HideAppsTab(val label: String) {
    HIDDEN("Hidden in Vault"),
    ALL_APPS("All Phone Apps")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideAppsScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val apps by repository.lockedApps.collectAsState()
    var selectedTab by remember { mutableStateOf(HideAppsTab.HIDDEN) }
    var searchQuery by remember { mutableStateOf("") }
    var showDisguiseGuide by remember { mutableStateOf(false) }

    val hiddenApps = remember(apps) { apps.filter { it.isHidden } }
    val displayedApps = remember(apps, selectedTab, searchQuery) {
        val baseList = if (selectedTab == HideAppsTab.HIDDEN) hiddenApps else apps
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hide Apps & Stealth Vault", color = Color.White) },
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
                            contentDescription = "Stealth Guide",
                            tint = Color(0xFF60A5FA)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151B))
            )
        },
        containerColor = Color(0xFF13151B)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("hide_apps_screen")
        ) {
            // Hero Status Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2435))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Hide Apps",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${hiddenApps.size} Apps Shielded in Vault",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Launch & use hidden apps securely inside this Vault.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }
            }

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("hide_apps_search"),
                placeholder = { Text("Search any app on phone (WhatsApp, YouTube...)", color = Color(0xFF6B7280), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF2E384D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF181C26),
                contentColor = Color(0xFF818CF8),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                HideAppsTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = if (tab == HideAppsTab.HIDDEN) "${tab.label} (${hiddenApps.size})" else "${tab.label} (${apps.size})",
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) Color(0xFF818CF8) else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }

            // Apps List
            if (displayedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTab == HideAppsTab.HIDDEN) Icons.Default.VisibilityOff else Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == HideAppsTab.HIDDEN)
                                "No apps hidden yet\nSwitch to 'All Phone Apps' to hide apps!"
                            else
                                "No matching apps found",
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(displayedApps, key = { it.packageName }) { app ->
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
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val ok = repository.launchApp(app.packageName)
                                            if (ok) {
                                                Toast.makeText(context, "Opening ${app.appName} inside Vault...", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "App not accessible directly", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(app.iconColor),
                                        modifier = Modifier.size(46.dp)
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

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (app.isHidden) "Protected in Vault • Tap to Open" else app.category,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (app.isHidden) Color(0xFF818CF8) else Color(0xFF94A3B8)
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (app.isHidden) {
                                        Button(
                                            onClick = {
                                                val ok = repository.launchApp(app.packageName)
                                                if (ok) {
                                                    Toast.makeText(context, "Launching ${app.appName}...", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Cannot launch ${app.appName}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Open", fontSize = 12.sp)
                                        }
                                    }

                                    Switch(
                                        checked = app.isHidden,
                                        onCheckedChange = {
                                            repository.toggleAppHide(app.packageName)
                                            val status = if (!app.isHidden) "Hidden in Vault" else "Unhidden"
                                            Toast.makeText(context, "${app.appName}: $status", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("toggle_hide_${app.packageName}")
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Disguise Guide Dialog
    if (showDisguiseGuide) {
        AlertDialog(
            onDismissRequest = { showDisguiseGuide = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF818CF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("How App Stealth Works", color = Color.White)
                }
            },
            text = {
                Text(
                    "1. Secret Vault Access:\nWhen you mark an app as Hidden, you can launch and use it directly from the 'Hidden in Vault' tab.\n\n" +
                    "2. External Interception:\nActivate 'Smart App Lock' so if anyone tries to open the app outside this vault, the Calculator PIN is immediately requested!\n\n" +
                    "3. Stealth Launcher:\nOnly Calculator Lock appears on your home screen, keeping your apps private.",
                    color = Color(0xFFD1D5DB),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisguiseGuide = false }) {
                    Text("Got It", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }
}
