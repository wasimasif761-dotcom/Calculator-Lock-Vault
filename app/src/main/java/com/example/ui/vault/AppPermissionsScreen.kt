package com.example.ui.vault

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultRepository
import com.example.model.SystemPermissionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionsScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val permissionList = remember(refreshTrigger) {
        repository.checkPermissionsStatus(context)
    }

    val grantedCount = permissionList.count { it.isGranted }
    val totalCount = permissionList.size

    // Master multi-permission runtime launcher
    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        refreshTrigger++
        val allGranted = results.values.all { it }
        Toast.makeText(
            context,
            if (allGranted) "All device permissions granted!" else "Permissions updated.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Single permission launcher
    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTrigger++
    }

    fun requestAllPermissions() {
        val permissionsToAsk = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToAsk.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToAsk.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsToAsk.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissionsToAsk.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionsToAsk.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissionsToAsk.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissionsToAsk.add(Manifest.permission.READ_CONTACTS)
        permissionsToAsk.add(Manifest.permission.WRITE_CONTACTS)
        permissionsToAsk.add(Manifest.permission.CAMERA)
        multiPermissionLauncher.launch(permissionsToAsk.toTypedArray())
    }

    fun openSystemSettings(action: String?) {
        if (action == null) {
            // General App Info settings
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            val intent = when (action) {
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    } else {
                        Intent(Settings.ACTION_SETTINGS)
                    }
                }
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION -> {
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                }
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS -> {
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                }
                else -> Intent(action)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback to app details
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open setting: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Device Permissions & Protection",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$grantedCount of $totalCount protections active",
                            color = if (grantedCount == totalCount) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontSize = 12.sp
                        )
                    }
                },
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
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151B))
            )
        },
        containerColor = Color(0xFF13151B)
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (grantedCount == totalCount) Color(0xFF0F3E2E) else Color(0xFF261D15)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (grantedCount == totalCount) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (grantedCount == totalCount) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (grantedCount == totalCount) Icons.Default.Security else Icons.Default.GppMaybe,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (grantedCount == totalCount) "Maximum Protection Active" else "Setup Real Mobile Access",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (grantedCount == totalCount)
                                        "Calculator Lock has all necessary system access for full vault security."
                                    else
                                        "Grant all permissions so the app stays active & protects your photos, contacts & apps.",
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        if (grantedCount < totalCount) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { requestAllPermissions() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("grant_all_permissions_button")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant All Essential Permissions", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "SYSTEM PERMISSIONS STATUS",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            // Permissions list items
            items(permissionList, key = { it.id }) { item ->
                PermissionItemCard(
                    item = item,
                    onRequestRuntime = {
                        item.permissionManifestName?.let { manifestName ->
                            singlePermissionLauncher.launch(manifestName)
                        } ?: run {
                            openSystemSettings(item.settingsAction)
                        }
                    },
                    onOpenSettings = {
                        openSystemSettings(item.settingsAction)
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionItemCard(
    item: SystemPermissionItem,
    onRequestRuntime: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("perm_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Surface(
                    shape = CircleShape,
                    color = if (item.isGranted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when (item.iconName) {
                            "PhotoLibrary" -> Icons.Default.PhotoLibrary
                            "FolderSpecial" -> Icons.Default.FolderSpecial
                            "Contacts" -> Icons.Default.Contacts
                            "CameraAlt" -> Icons.Default.CameraAlt
                            "NotificationsActive" -> Icons.Default.NotificationsActive
                            "Layers" -> Icons.Default.Layers
                            "QueryStats" -> Icons.Default.QueryStats
                            "BatterySaver" -> Icons.Default.BatterySaver
                            else -> Icons.Default.Security
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (item.isGranted) Color(0xFF34D399) else Color(0xFFF87171),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isGranted) Color(0xFF065F46) else Color(0xFF7F1D1D)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isGranted) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (item.isGranted) Color(0xFF34D399) else Color(0xFFFCA5A5),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isGranted) "ACTIVE" else "DENIED",
                            color = if (item.isGranted) Color(0xFF34D399) else Color(0xFFFCA5A5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!item.isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2E3445))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isDangerousRuntime && item.permissionManifestName != null) {
                        Button(
                            onClick = onRequestRuntime,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
