package com.example.ui.monetization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentGatewayInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentComparisonScreen(
    onNavigateBack: () -> Unit
) {
    val gateways = listOf(
        PaymentGatewayInfo(
            name = "Lemon Squeezy",
            badge = "RECOMMENDED FOR GLOBAL TAX",
            transactionFees = "5% + $0.50 per transaction",
            internationalTaxMoR = "Full MoR (Handles VAT, GST, Sales Tax worldwide automatically)",
            apiEase = "Very High (Clean REST API, Webhooks, Pre-built Hosted Checkouts)",
            pros = listOf(
                "Acts as Merchant of Record (MoR) — eliminates tax filings in 100+ countries",
                "Built-in subscription management & customer portal",
                "Native PayPal & Apple Pay / Google Pay support out-of-the-box",
                "Instant payout to Pakistani & international bank accounts via Wise/Payoneer"
            ),
            cons = listOf(
                "Slightly higher percentage fee than raw Stripe (5% vs 2.9%)",
                "External checkout flow when used outside Google Play store"
            ),
            bestFor = "Fastest international compliance with lowest administrative overhead",
            devDocsUrl = "https://docs.lemonsqueezy.com"
        ),
        PaymentGatewayInfo(
            name = "Paddle",
            badge = "POPULAR MOR FOR SAAS & APPS",
            transactionFees = "5% + $0.50 per transaction",
            internationalTaxMoR = "Full MoR (Manages 100% of global tax collection, remittance & invoicing)",
            apiEase = "High (V2 API, Sandbox environment, Webhooks, Kotlin SDK/Webview)",
            pros = listOf(
                "Complete liability offloading for EU VAT, US State Tax & UK VAT",
                "Fraud prevention and automated dunning (recovering failed renewals)",
                "Handles chargebacks and dispute defense automatically"
            ),
            cons = listOf(
                "Strict merchant approval process for certain app categories",
                "Minimum payout thresholds apply"
            ),
            bestFor = "Growing apps seeking zero tax liability and automated global invoicing",
            devDocsUrl = "https://developer.paddle.com"
        ),
        PaymentGatewayInfo(
            name = "Stripe",
            badge = "INDUSTRY STANDARD API",
            transactionFees = "2.9% + $0.30 (Domestic) / +1.5% International",
            internationalTaxMoR = "Not an MoR (You must register & remit sales tax via Stripe Tax for extra 0.5%)",
            apiEase = "Highest (Gold standard developer documentation & native Android SDK)",
            pros = listOf(
                "Lowest base transaction fees for high-volume transactions",
                "Native Android PaymentSheet SDK (`stripe-android`) for in-app card forms",
                "Extensive documentation, SDK samples in Kotlin, and webhook reliability"
            ),
            cons = listOf(
                "Developer remains legally responsible for global tax compliance & filing",
                "Requires a registered business entity in supported Stripe countries"
            ),
            bestFor = "Teams with existing legal entities in US/UK/EU wanting complete checkout UI control",
            devDocsUrl = "https://stripe.com/docs"
        ),
        PaymentGatewayInfo(
            name = "PayPal",
            badge = "HIGHEST CONSUMER TRUST",
            transactionFees = "3.49% + $0.49 per transaction",
            internationalTaxMoR = "Not an MoR (Seller responsible for taxes)",
            apiEase = "Moderate (PayPal Braintree SDK or REST v2 Subscriptions)",
            pros = listOf(
                "Massive global user trust and 400M+ active digital wallets",
                "One-click login for customers without typing card numbers"
            ),
            cons = listOf(
                "Higher international cross-border fees (often 4.5%+)",
                "Frequent account reserves or fund hold policies on new merchant accounts"
            ),
            bestFor = "Providing an alternative payment method alongside credit cards",
            devDocsUrl = "https://developer.paypal.com"
        ),
        PaymentGatewayInfo(
            name = "Google Play Billing",
            badge = "MANDATORY FOR PLAY STORE DISTRIBUTION",
            transactionFees = "15% on first $1M/year (Play Service Fee)",
            internationalTaxMoR = "Full MoR (Google handles local tax collection & Play account billing)",
            apiEase = "High (Play Billing Library v7 Kotlin coroutines API)",
            pros = listOf(
                "Zero checkout friction: Users pay with one biometric fingerprint or Google Pay",
                "Mandatory requirement if distributed via Google Play Store for digital features",
                "Highest conversion rate on Android devices"
            ),
            cons = listOf(
                "15% fee tier is higher than independent gateways"
            ),
            bestFor = "Standard Google Play Store distribution compliance",
            devDocsUrl = "https://developer.android.com/google/play/billing"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gateway Architecture Report", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
                // Executive Summary for Waseem Asif
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B3E)),
                    modifier = Modifier.testTag("executive_summary_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Summary",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Technical Assessment for Waseem Asif",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "For global digital sales, traditional processors like Stripe & PayPal force you to register for VAT in Europe, GST in Australia/India, and sales tax across 40+ US states. " +
                                   "\n\nBy contrast, Merchant of Record (MoR) providers like Lemon Squeezy or Paddle take full legal responsibility for collecting and remitting international sales tax, slashing accounting overhead to zero.\n\n" +
                                   "• For direct web/APK sales: Lemon Squeezy is the top recommendation.\n" +
                                   "• For official Google Play publishing: Google Play Billing (15% tier) is required.\n\n" +
                                   "Project Lead: Waseem Asif\nWhatsApp: +966597447275 • wasimasif761@gmail.com",
                            color = Color(0xFFE2E8F0),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
                        )
                    }
                }
            }

            items(gateways) { gateway ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gateway_card_${gateway.name.replace(" ", "_")}")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = gateway.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2563EB).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = gateway.badge,
                                    color = Color(0xFF93C5FD),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fee & MoR details
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Transaction Fees", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                                Text(gateway.transactionFees, color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column {
                            Text("Tax & MoR Status", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                            Text(gateway.internationalTaxMoR, color = Color(0xFF34D399), style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column {
                            Text("API Accessibility & Dev Ease", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                            Text(gateway.apiEase, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Key Advantages:", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        gateway.pros.forEach { pro ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Pro",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(pro, color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Trade-offs:", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        gateway.cons.forEach { con ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Con",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(con, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Verdict: ${gateway.bestFor}",
                                color = Color(0xFF60A5FA),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
