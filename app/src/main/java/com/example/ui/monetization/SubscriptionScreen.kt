package com.example.ui.monetization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.SubscriptionPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit,
    onOpenPaymentComparison: () -> Unit
) {
    val isPremium by repository.isPremium.collectAsState()
    val activePlan by repository.activePlan.collectAsState()
    val pushNotificationsEnabled by repository.pushNotificationsEnabled.collectAsState()

    var selectedPlanId by remember { mutableStateOf("yearly") }
    var showSuccessSnackbar by remember { mutableStateOf(false) }

    val plans = listOf(
        SubscriptionPlan(
            id = "monthly",
            title = "Monthly VIP",
            price = "$2.99",
            period = "/ month",
            features = listOf("Remove All Ads", "Unlimited Media Vault", "Private Notes Lock")
        ),
        SubscriptionPlan(
            id = "yearly",
            title = "Yearly VIP",
            price = "$19.99",
            period = "/ year",
            savingsBadge = "SAVE 45%",
            isPopular = true,
            features = listOf(
                "Remove All Ads Permanently",
                "Unlimited Media & Video Downloads",
                "Smart App Lock & Stealth Disguise",
                "Intruder Break-in Selfie Alert",
                "Priority VIP Customer Support"
            )
        ),
        SubscriptionPlan(
            id = "lifetime",
            title = "Lifetime License",
            price = "$39.99",
            period = "one-time",
            savingsBadge = "BEST VALUE",
            features = listOf(
                "All Current & Future Features",
                "No Recurring Payments",
                "Cloud Backup Multi-Device Sync",
                "Dedicated Developer Assistance"
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VIP Premium & Ad Removal", color = Color.White) },
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
                        onClick = onOpenPaymentComparison,
                        modifier = Modifier.testTag("open_payment_comparison_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Payment Comparison",
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Premium Status Dashboard Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subscription_status_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPremium) Color(0xFF1E3A8A) else Color(0xFF1E2430)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isPremium) "VIP Membership Active" else "Standard Free Plan",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (isPremium) "Plan: $activePlan" else "Ads enabled • Basic storage",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isPremium) Color(0xFF93C5FD) else Color(0xFF9CA3AF)
                                    )
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isPremium) Color(0xFFF59E0B) else Color(0xFF374151),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.Lock,
                                        contentDescription = "Status",
                                        tint = if (isPremium) Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle Live Premium Status for Instant Testing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulate Premium Status",
                                color = Color(0xFFD1D5DB),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = isPremium,
                                onCheckedChange = { checked ->
                                    val plan = if (checked) "VIP Annual Plan" else "Free Trial"
                                    repository.setPremium(checked, plan)
                                    showSuccessSnackbar = true
                                },
                                modifier = Modifier.testTag("toggle_premium_switch")
                            )
                        }

                        // Push Notifications for Renewal Alert
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Subscription Alerts & Expiry Notice",
                                    color = Color(0xFFD1D5DB),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Receive notifications before renewal and special offers",
                                    color = Color(0xFF9CA3AF),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Switch(
                                checked = pushNotificationsEnabled,
                                onCheckedChange = { repository.togglePushNotifications() },
                                modifier = Modifier.testTag("push_notifications_switch")
                            )
                        }
                    }
                }
            }

            // Gateway Specs Banner / Shortcut
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPaymentComparison)
                        .testTag("gateway_specs_shortcut_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A38))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = "Analysis",
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Payment Gateways Comparison",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Stripe, PayPal, Lemon Squeezy & Paddle specs",
                                    color = Color(0xFF93C5FD),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Select Subscription Tier",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Plan Selection Cards
            items(plans.size) { index ->
                val plan = plans[index]
                val isSelected = selectedPlanId == plan.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF2A2E39),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedPlanId = plan.id }
                        .testTag("plan_card_${plan.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E2638) else Color(0xFF181C26)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPlanId = plan.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = plan.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            plan.savingsBadge?.let { badge ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF059669)
                                ) {
                                    Text(
                                        text = badge,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(start = 48.dp)
                        ) {
                            Text(
                                text = plan.price,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF60A5FA)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = plan.period,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(modifier = Modifier.padding(start = 48.dp)) {
                            plan.features.forEach { feat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Feature",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = feat,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD1D5DB))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Purchase Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val chosen = plans.find { it.id == selectedPlanId }
                        repository.setPremium(true, chosen?.title ?: "VIP Annual")
                        showSuccessSnackbar = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("subscribe_now_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(
                        text = if (isPremium) "Update Current Subscription" else "Subscribe Now (Test Checkout)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (showSuccessSnackbar) {
                    Text(
                        text = "✓ Premium successfully activated! Ads removed & features unlocked.",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
