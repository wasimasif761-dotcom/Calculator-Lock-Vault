package com.example.ui.lock

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultRepository
import com.example.service.AppLockAccessibilityService
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class AppLockActivity : ComponentActivity() {

    private lateinit var repository: VaultRepository
    private var lockedPackageName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = VaultRepository(applicationContext)

        lockedPackageName = intent.getStringExtra("EXTRA_LOCKED_PACKAGE") ?: ""

        // Prevent bypassing via back button - send user to home screen if cancelled
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHomeScreen()
            }
        })

        // Fetch app name
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(lockedPackageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            lockedPackageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF131722)
                ) {
                    RealAppLockScreenContent(
                        appName = appName,
                        expectedPin = repository.passcode.collectAsState().value,
                        onUnlockSuccess = {
                            AppLockAccessibilityService.markPackageUnlocked(lockedPackageName)
                            finish()
                        },
                        onWrongPin = { wrongPin ->
                            // Log intruder attempt
                        },
                        onCancel = {
                            goToHomeScreen()
                        }
                    )
                }
            }
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun RealAppLockScreenContent(
    appName: String,
    expectedPin: String,
    onUnlockSuccess: () -> Unit,
    onWrongPin: (String) -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var pinEntered by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(pinEntered) {
        if (pinEntered.length == 4) {
            if (pinEntered == expectedPin) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isUnlocked = true
                delay(400)
                onUnlockSuccess()
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onWrongPin(pinEntered)
                errorMessage = "Incorrect PIN"
                delay(1000)
                pinEntered = ""
                errorMessage = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar with Cancel / Home button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
            }
        }

        // App Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF2563EB),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = appName.take(1),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = appName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Protected by Calculator Lock",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF93C5FD))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4 Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pinEntered.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) Color(0xFF10B981)
                                else if (errorMessage != null) Color(0xFFEF4444)
                                else if (isFilled) Color(0xFF10B981)
                                else Color(0xFF475569)
                            )
                    )
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Keypad Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            )

            for (row in keyRows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (key in row) {
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(70.dp))
                        } else {
                            Surface(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (key == "⌫") {
                                            if (pinEntered.isNotEmpty()) pinEntered = pinEntered.dropLast(1)
                                        } else if (pinEntered.length < 4) {
                                            pinEntered += key
                                        }
                                    },
                                shape = CircleShape,
                                color = Color(0xFF1E2638)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "⌫") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
