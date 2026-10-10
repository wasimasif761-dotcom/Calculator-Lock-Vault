package com.example.ui.vault

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.model.AppDeveloperInfo
import com.example.model.IntruderAttempt
import com.example.ui.monetization.BannerAdPlaceholder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class VaultSection {
    DASHBOARD,
    MEDIA_VAULT,
    HIDE_APPS,
    APP_LOCK,
    BROWSER,
    DOWNLOADER,
    NOTES,
    SUBSCRIPTION,
    SETTINGS
}

data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val section: VaultSection,
    val countBadge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    repository: VaultRepository,
    onNavigateSection: (VaultSection) -> Unit,
    onLockApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isPremium by repository.isPremium.collectAsState()
    val mediaItems by repository.mediaItems.collectAsState()
    val notes by repository.notes.collectAsState()
    val lockedApps by repository.lockedApps.collectAsState()
    val downloads by repository.downloads.collectAsState()
    val userProfile by repository.userProfile.collectAsState()
    val intruderAttempts by repository.intruderAttempts.collectAsState()
    val currentPin by repository.passcode.collectAsState()

    val photosCount = mediaItems.count { !it.isDeleted && it.type == com.example.model.MediaType.PHOTO }
    val videosCount = mediaItems.count { !it.isDeleted && it.type == com.example.model.MediaType.VIDEO }
    val hiddenAppsCount = lockedApps.count { it.isHidden }
    val lockedAppsCount = lockedApps.count { it.isLocked }

    // Dialog state controllers
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showIntruderDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var pinChangeSuccess by remember { mutableStateOf(false) }

    // Intent helper functions
    fun openWhatsApp(number: String, message: String) {
        try {
            val cleanNumber = number.replace("+", "").replace(" ", "")
            val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openEmail(email: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareApp() {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Calculator Lock - Secret Vault")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Protect your photos, apps & notes with Calculator Lock! Developed by ${AppDeveloperInfo.DEVELOPER_NAME}. Contact: ${AppDeveloperInfo.WHATSAPP_NUMBER}"
                )
            }
            context.startActivity(Intent.createChooser(intent, "Share Calculator Lock via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val dashboardItems = listOf(
        DashboardItem(
            title = "Photos & Videos",
            subtitle = "$photosCount photos, $videosCount videos",
            icon = Icons.Default.PhotoLibrary,
            iconBgColor = Color(0xFF3B82F6),
            section = VaultSection.MEDIA_VAULT,
            countBadge = "${photosCount + videosCount}"
        ),
        DashboardItem(
            title = "Hide Apps",
            subtitle = "$hiddenAppsCount apps hidden",
            icon = Icons.Default.VisibilityOff,
            iconBgColor = Color(0xFF8B5CF6),
            section = VaultSection.HIDE_APPS,
            countBadge = if (hiddenAppsCount > 0) "$hiddenAppsCount" else null
        ),
        DashboardItem(
            title = "Smart App Lock",
            subtitle = "$lockedAppsCount apps protected",
            icon = Icons.Default.LockPerson,
            iconBgColor = Color(0xFF10B981),
            section = VaultSection.APP_LOCK,
            countBadge = "$lockedAppsCount"
        ),
        DashboardItem(
            title = "Private Browser",
            subtitle = "Zero history incognito",
            icon = Icons.Default.VpnLock,
            iconBgColor = Color(0xFF06B6D4),
            section = VaultSection.BROWSER
        ),
        DashboardItem(
            title = "Video Downloader",
            subtitle = "${downloads.size} in queue",
            icon = Icons.Default.DownloadForOffline,
            iconBgColor = Color(0xFFF59E0B),
            section = VaultSection.DOWNLOADER,
            countBadge = "${downloads.size}"
        ),
        DashboardItem(
            title = "Private Notes",
            subtitle = "${notes.size} secret notes",
            icon = Icons.Default.EditNote,
            iconBgColor = Color(0xFFEC4899),
            section = VaultSection.NOTES,
            countBadge = "${notes.size}"
        )
    )

    // 3-Line Menu / Navigation Drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF131722),
                drawerContentColor = Color.White,
                modifier = Modifier.width(320.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Profile Header Card (Google Account & Developer by Waseem Asif)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    showGoogleAccountDialog = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = userProfile.displayName.take(1),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPremium) Color(0xFFF59E0B) else Color(0xFF374151)
                                    ) {
                                        Text(
                                            text = if (isPremium) "VIP MEMBER" else "STANDARD",
                                            color = if (isPremium) Color.Black else Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = userProfile.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = userProfile.email,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF93C5FD))
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = Color(0xFF10B981), modifier = Modifier.size(6.dp)) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cloud Sync & Backup Active",
                                        color = Color(0xFF10B981),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF263042))
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Drawer Items
                    item {
                        DrawerNavigationItem(
                            icon = Icons.Default.Home,
                            title = "Vault Dashboard",
                            onClick = { scope.launch { drawerState.close() } }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.PhotoLibrary,
                            title = "Media Vault (Photos & Videos)",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateSection(VaultSection.MEDIA_VAULT)
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.VisibilityOff,
                            title = "Hide Apps",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateSection(VaultSection.HIDE_APPS)
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.LockPerson,
                            title = "Smart App Lock",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateSection(VaultSection.APP_LOCK)
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.VpnLock,
                            title = "Private Browser",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateSection(VaultSection.BROWSER)
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.Download,
                            title = "Video Downloader",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateSection(VaultSection.DOWNLOADER)
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.EditNote,
                            title = "Private Notes",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateSection(VaultSection.NOTES)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF263042))
                        Spacer(modifier = Modifier.height(8.dp))

                        DrawerNavigationItem(
                            icon = Icons.Default.CloudSync,
                            title = "Google Account & Backup",
                            onClick = {
                                scope.launch { drawerState.close() }
                                showGoogleAccountDialog = true
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.CameraAlt,
                            title = "Intruder Selfie Log",
                            badge = "${intruderAttempts.size}",
                            onClick = {
                                scope.launch { drawerState.close() }
                                showIntruderDialog = true
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.WorkspacePremium,
                            title = "VIP Membership",
                            badge = if (isPremium) "ACTIVE" else "UPGRADE",
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateSection(VaultSection.SUBSCRIPTION)
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.Security,
                            title = "Security & Change PIN",
                            onClick = {
                                scope.launch { drawerState.close() }
                                showSettingsDialog = true
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF263042))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Feedback & Contact Developer (Waseem Asif)
                        DrawerNavigationItem(
                            icon = Icons.Default.RateReview,
                            title = "Feedback & Rate Us",
                            onClick = {
                                scope.launch { drawerState.close() }
                                showFeedbackDialog = true
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.ContactSupport,
                            title = "Developer (Waseem Asif)",
                            onClick = {
                                scope.launch { drawerState.close() }
                                showDeveloperDialog = true
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.Share,
                            title = "Share App",
                            onClick = {
                                scope.launch { drawerState.close() }
                                shareApp()
                            }
                        )
                        DrawerNavigationItem(
                            icon = Icons.Default.Lock,
                            title = "Lock & Exit Vault",
                            tintColor = Color(0xFFEF4444),
                            onClick = {
                                scope.launch { drawerState.close() }
                                onLockApp()
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "${AppDeveloperInfo.APP_TITLE} • ${AppDeveloperInfo.APP_VERSION}\nDeveloper: ${AppDeveloperInfo.DEVELOPER_NAME}",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        // 3-Line Menu (Hamburger button)
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("three_line_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = Color.White
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Shield",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Secret Vault",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    },
                    actions = {
                        // VIP Badge / Upgrade button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onNavigateSection(VaultSection.SUBSCRIPTION) }
                                .testTag("vip_badge_button"),
                            color = if (isPremium) Color(0xFFF59E0B) else Color(0xFF252E3E)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "VIP",
                                    tint = if (isPremium) Color.Black else Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPremium) "VIP ACTIVE" else "GO VIP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPremium) Color.Black else Color(0xFFF59E0B)
                                    )
                                )
                            }
                        }

                        // Settings & Passcode button
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("vault_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF93C5FD)
                            )
                        }

                        // Lock & Exit Vault Button
                        IconButton(
                            onClick = onLockApp,
                            modifier = Modifier.testTag("lock_and_exit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock and Exit",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151B))
                )
            },
            containerColor = Color(0xFF13151B)
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .testTag("vault_dashboard_screen"),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Security & Storage Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C222E))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vault Encrypted & Protected",
                                        color = Color(0xFF10B981),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total Vault Items: ${mediaItems.size + notes.size}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = onLockApp,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF283142)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fast Lock", color = Color(0xFF60A5FA), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Vault Modules",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 2-Column Grid of Vault Features
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(dashboardItems.size) { index ->
                            val item = dashboardItems[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(132.dp)
                                    .clickable { onNavigateSection(item.section) }
                                    .testTag("dashboard_card_${item.title.replace(" ", "_")}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = item.iconBgColor.copy(alpha = 0.2f),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.title,
                                                    tint = item.iconBgColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        item.countBadge?.let { count ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF263042)
                                            ) {
                                                Text(
                                                    text = count,
                                                    color = Color(0xFF93C5FD),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Ad Banner for Free Users (matching monetization guidelines)
                if (!isPremium) {
                    BannerAdPlaceholder(
                        onRemoveAdsClicked = { onNavigateSection(VaultSection.SUBSCRIPTION) },
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // Vault Security & Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF60A5FA))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vault Security", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Current Secret Passcode", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                            Text(currentPin, color = Color(0xFF38BDF8), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Type this in calculator and tap = to unlock", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = {
                            showSettingsDialog = false
                            newPinInput = ""
                            pinChangeSuccess = false
                            showChangePinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Passcode")
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsDialog = false
                            showDeveloperDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Developer Info: Waseem Asif", color = Color(0xFF60A5FA))
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsDialog = false
                            repository.resetSetup()
                            onLockApp()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Onboarding Flow (Test Setup)", color = Color(0xFFF59E0B))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close", color = Color(0xFF93C5FD))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Passcode", color = Color.White) },
            text = {
                Column {
                    Text("Enter a new 4-digit secret PIN:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        placeholder = { Text("4 digits (e.g. 5678)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    if (pinChangeSuccess) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("✓ PIN successfully updated!", color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length == 4) {
                            repository.setPasscode(newPinInput)
                            pinChangeSuccess = true
                            showChangePinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }

    // Google Account & Cloud Backup Dialog
    if (showGoogleAccountDialog) {
        var isSyncing by remember { mutableStateOf(false) }
        val syncDateStr = remember(userProfile.lastSyncTimestamp) {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(userProfile.lastSyncTimestamp))
        }

        AlertDialog(
            onDismissRequest = { showGoogleAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF4285F4))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Google Account & Cloud", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = Color(0xFF4285F4), modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(userProfile.displayName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(userProfile.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(userProfile.email, color = Color(0xFF93C5FD), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF232D42))
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Last Synced:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                                Text(syncDateStr, color = Color(0xFF10B981), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Sync Vault to Google Drive", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = userProfile.cloudSyncEnabled,
                            onCheckedChange = { repository.toggleCloudSync() }
                        )
                    }

                    Button(
                        onClick = {
                            isSyncing = true
                            repository.performCloudSync()
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                isSyncing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSyncing) "Syncing..." else "Sync Cloud Vault Now")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleAccountDialog = false }) {
                    Text("Done", color = Color(0xFF93C5FD))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }

    // Feedback & Rating Dialog
    if (showFeedbackDialog) {
        var rating by remember { mutableIntStateOf(5) }
        var feedbackMessage by remember { mutableStateOf("") }
        var feedbackSubmitted by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RateReview, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("User Feedback & Rating", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How is your experience with Calculator Lock?", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)

                    // 5-Star Rating
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { rating = i }) {
                                Icon(
                                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star $i",
                                    tint = if (i <= rating) Color(0xFFF59E0B) else Color(0xFF4B5563),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { feedbackMessage = it },
                        placeholder = { Text("Write your thoughts or requested features...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (feedbackSubmitted) {
                        Text("✓ Thank you! Your feedback has been recorded.", color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Direct Support Options:", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openWhatsApp(
                                    AppDeveloperInfo.WHATSAPP_NUMBER,
                                    "Hello Waseem Asif, I am providing feedback on Calculator Lock App:\n$feedbackMessage (Rating: $rating/5)"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("WhatsApp", color = Color(0xFF25D366), style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                openEmail(
                                    AppDeveloperInfo.EMAIL_ADDRESS,
                                    "Calculator Lock Feedback ($rating Stars)",
                                    feedbackMessage
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Email", color = Color(0xFF60A5FA), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackMessage.isNotBlank()) {
                            repository.submitFeedback(rating, "General", feedbackMessage, userProfile.email)
                            feedbackSubmitted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Submit In-App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }

    // Developer & Contact Us Dialog (Waseem Asif, WhatsApp +966597447275, wasimasif761@gmail.com)
    if (showDeveloperDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Developer Information", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Developed by Waseem Asif",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )

                            Text(
                                text = "Senior Android Privacy & Vault Engineer",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF93C5FD))
                            )
                        }
                    }

                    // WhatsApp Contact Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132219)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                openWhatsApp(
                                    AppDeveloperInfo.WHATSAPP_NUMBER,
                                    "Hello Waseem Asif, I am contacting you regarding Calculator Lock App."
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = Color(0xFF25D366), modifier = Modifier.size(38.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("WhatsApp Contact", color = Color(0xFF86EFAC), style = MaterialTheme.typography.labelSmall)
                                Text(AppDeveloperInfo.WHATSAPP_NUMBER, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Email Contact Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2234)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                openEmail(
                                    AppDeveloperInfo.EMAIL_ADDRESS,
                                    "Inquiry regarding Calculator Lock App",
                                    "Hello Waseem Asif,\n\n"
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = Color(0xFF3B82F6), modifier = Modifier.size(38.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Email Address", color = Color(0xFF93C5FD), style = MaterialTheme.typography.labelSmall)
                                Text(AppDeveloperInfo.EMAIL_ADDRESS, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeveloperDialog = false }) {
                    Text("Close", color = Color(0xFF93C5FD))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }

    // Intruder Break-in Log Dialog
    if (showIntruderDialog) {
        AlertDialog(
            onDismissRequest = { showIntruderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SecurityUpdateWarning, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Intruder Break-in Log", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Snapshots of individuals who attempted incorrect passcodes on the calculator:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)

                    if (intruderAttempts.isEmpty()) {
                        Text("No unauthorized attempts detected. Your vault is 100% secure.", color = Color(0xFF10B981), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        intruderAttempts.forEach { attempt ->
                            val attemptTime = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(attempt.timestamp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF241518)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Face, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Wrong PIN: ${attempt.enteredPin}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text(attemptTime, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF991B1B)) {
                                        Text("Captured", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntruderDialog = false }) {
                    Text("Close", color = Color(0xFF93C5FD))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }
}

@Composable
fun DrawerNavigationItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    tintColor: Color = Color(0xFFE2E8F0),
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    color = tintColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            badge?.let { b ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF263248)
                ) {
                    Text(
                        text = b,
                        color = Color(0xFF93C5FD),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
