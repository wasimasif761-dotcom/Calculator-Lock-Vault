package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.VaultRepository
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.monetization.InterstitialAdDialog
import com.example.ui.monetization.PaymentComparisonScreen
import com.example.ui.monetization.SubscriptionScreen
import com.example.ui.setup.StoragePermissionDialog
import com.example.ui.setup.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.vault.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = VaultRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF13151B)
                ) {
                    VaultAppContent(repository)
                }
            }
        }
    }
}

@Composable
fun VaultAppContent(repository: VaultRepository) {
    val isSetupCompleted by repository.isSetupCompleted.collectAsState()
    val hasAgreedPrivacyNotice by repository.hasAgreedPrivacyNotice.collectAsState()
    val passcode by repository.passcode.collectAsState()
    val isPremium by repository.isPremium.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSetupAd by remember { mutableStateOf(false) }
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var currentVaultSection by remember { mutableStateOf(VaultSection.DASHBOARD) }
    var showPaymentComparison by remember { mutableStateOf(false) }

    // Navigation back handlers
    if (isVaultUnlocked) {
        BackHandler {
            if (showPaymentComparison) {
                showPaymentComparison = false
            } else if (currentVaultSection != VaultSection.DASHBOARD) {
                currentVaultSection = VaultSection.DASHBOARD
            } else {
                // Exit & Relock Vault
                isVaultUnlocked = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // First time setup - Step 1: Welcome & Privacy screen
            !hasAgreedPrivacyNotice -> {
                WelcomeScreen(
                    onAgreeAndContinue = {
                        showPermissionDialog = true
                    },
                    onDisagree = {
                        // User declined terms, can still proceed to standard calculator
                        repository.setAgreedPrivacyNotice(true)
                    }
                )

                if (showPermissionDialog) {
                    StoragePermissionDialog(
                        onAllow = {
                            repository.setStoragePermission(true)
                            repository.setAgreedPrivacyNotice(true)
                            showPermissionDialog = false
                        },
                        onDeny = {
                            repository.setStoragePermission(false)
                            repository.setAgreedPrivacyNotice(true)
                            showPermissionDialog = false
                        }
                    )
                }
            }

            // First time setup - Step 2: PIN Creation & Confirmation on Calculator
            !isSetupCompleted -> {
                CalculatorScreen(
                    isInitialSetup = true,
                    savedPasscode = passcode,
                    onPasscodeConfigured = { newPin ->
                        repository.setPasscode(newPin)
                        // Trigger Interstitial Ad transition (matching video screenshot)
                        showSetupAd = true
                    },
                    onVaultUnlocked = {
                        isVaultUnlocked = true
                    }
                )

                if (showSetupAd) {
                    InterstitialAdDialog(
                        onDismiss = {
                            showSetupAd = false
                            isVaultUnlocked = true
                        }
                    )
                }
            }

            // Unlocked Hidden Vault Backend
            isVaultUnlocked -> {
                if (showPaymentComparison) {
                    PaymentComparisonScreen(
                        onNavigateBack = { showPaymentComparison = false }
                    )
                } else {
                    when (currentVaultSection) {
                        VaultSection.DASHBOARD -> {
                            VaultDashboardScreen(
                                repository = repository,
                                onNavigateSection = { section ->
                                    if (section == VaultSection.SETTINGS) {
                                        showPaymentComparison = true
                                    } else {
                                        currentVaultSection = section
                                    }
                                },
                                onLockApp = {
                                    isVaultUnlocked = false
                                }
                            )
                        }
                        VaultSection.MEDIA_VAULT -> {
                            MediaVaultScreen(
                                repository = repository,
                                onNavigateBack = { currentVaultSection = VaultSection.DASHBOARD }
                            )
                        }
                        VaultSection.HIDE_APPS -> {
                            HideAppsScreen(
                                repository = repository,
                                onNavigateBack = { currentVaultSection = VaultSection.DASHBOARD }
                            )
                        }
                        VaultSection.APP_LOCK -> {
                            AppLockScreen(
                                repository = repository,
                                onNavigateBack = { currentVaultSection = VaultSection.DASHBOARD }
                            )
                        }
                        VaultSection.BROWSER -> {
                            PrivateBrowserScreen(
                                repository = repository,
                                onNavigateBack = { currentVaultSection = VaultSection.DASHBOARD },
                                onOpenDownloader = { currentVaultSection = VaultSection.DOWNLOADER }
                            )
                        }
                        VaultSection.DOWNLOADER -> {
                            VideoDownloaderScreen(
                                repository = repository,
                                onNavigateBack = { currentVaultSection = VaultSection.DASHBOARD }
                            )
                        }
                        VaultSection.NOTES -> {
                            PrivateNotesScreen(
                                repository = repository,
                                onNavigateBack = { currentVaultSection = VaultSection.DASHBOARD }
                            )
                        }
                        VaultSection.SUBSCRIPTION -> {
                            SubscriptionScreen(
                                repository = repository,
                                onNavigateBack = { currentVaultSection = VaultSection.DASHBOARD },
                                onOpenPaymentComparison = { showPaymentComparison = true }
                            )
                        }
                        VaultSection.SETTINGS -> {
                            PaymentComparisonScreen(
                                onNavigateBack = { currentVaultSection = VaultSection.DASHBOARD }
                            )
                        }
                    }
                }
            }

            // Normal Locked Mode: Fully operational fake calculator frontend
            else -> {
                CalculatorScreen(
                    isInitialSetup = false,
                    savedPasscode = passcode,
                    onVaultUnlocked = {
                        // Secret Trigger: Valid PIN + '=' tapped
                        isVaultUnlocked = true
                        currentVaultSection = VaultSection.DASHBOARD
                    }
                )
            }
        }
    }
}
