package com.example.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class SetupPinStep {
    NONE,
    CREATE_PIN,
    CONFIRM_PIN,
    SUCCESS
}

@Composable
fun CalculatorScreen(
    isInitialSetup: Boolean = false,
    savedPasscode: String = "1234",
    onPasscodeConfigured: (String) -> Unit = {},
    onVaultUnlocked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var showTooltip by remember { mutableStateOf(isInitialSetup) }
    var setupStep by remember {
        mutableStateOf(if (isInitialSetup) SetupPinStep.CREATE_PIN else SetupPinStep.NONE)
    }
    var firstPinAttempt by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessBadge by remember { mutableStateOf(false) }
    var showHintDialog by remember { mutableStateOf(false) }

    // Clear error message after delay
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000)
            errorMessage = null
        }
    }

    // Handles button press
    fun onButtonPress(char: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when (char) {
            "C" -> {
                expression = ""
                resultText = ""
            }
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                    if (setupStep == SetupPinStep.NONE && expression.isNotEmpty()) {
                        resultText = CalculatorEngine.evaluate(expression)
                    } else {
                        resultText = ""
                    }
                }
            }
            "=" -> {
                if (setupStep == SetupPinStep.CREATE_PIN) {
                    if (expression.length == 4 && expression.all { it.isDigit() }) {
                        firstPinAttempt = expression
                        expression = ""
                        setupStep = SetupPinStep.CONFIRM_PIN
                    } else {
                        errorMessage = "Please enter exactly 4 digits, then press ="
                    }
                } else if (setupStep == SetupPinStep.CONFIRM_PIN) {
                    if (expression == firstPinAttempt) {
                        setupStep = SetupPinStep.SUCCESS
                        showSuccessBadge = true
                        onPasscodeConfigured(firstPinAttempt)
                    } else {
                        errorMessage = "PINs do not match. Please re-enter."
                        expression = ""
                    }
                } else {
                    // Regular Calculator mode
                    if (expression == savedPasscode) {
                        // Secret Vault Triggered!
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onVaultUnlocked()
                    } else {
                        // Standard math evaluation
                        val eval = CalculatorEngine.evaluate(expression)
                        resultText = eval
                        if (eval != "Error") {
                            expression = eval
                        }
                    }
                }
            }
            "()" -> {
                val openCount = expression.count { it == '(' }
                val closeCount = expression.count { it == ')' }
                expression += if (openCount > closeCount && expression.lastOrNull()?.isDigit() == true) ")" else "("
            }
            else -> {
                expression += char
                if (setupStep == SetupPinStep.NONE) {
                    // Live preview evaluation if contains operators
                    if (expression.any { it in "+-×÷" }) {
                        resultText = CalculatorEngine.evaluate(expression)
                    }
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("calculator_screen"),
        color = Color(0xFF13151B)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (setupStep != SetupPinStep.NONE) {
                        Text(
                            text = if (setupStep == SetupPinStep.CREATE_PIN)
                                "Enter a 4-digit password and press the = button"
                            else
                                "Re-enter password and press = to confirm",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFD1D5DB),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy Shield",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Calculator",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { showHintDialog = true },
                            modifier = Modifier.testTag("help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help",
                                tint = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                // Error Message Notice
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // PIN / Expression Display Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // PIN dots if in setup mode
                    if (setupStep != SetupPinStep.NONE) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 4) {
                                val isFilled = i < expression.length
                                Text(
                                    text = if (isFilled) "✱ " else "— ",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = if (isFilled) Color(0xFF60A5FA) else Color(0xFF4B5563),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // Main display
                    Text(
                        text = if (expression.isEmpty()) "0" else expression,
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.sp
                        ),
                        maxLines = 2,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("calculator_display")
                    )

                    // Secondary evaluation result
                    if (resultText.isNotEmpty() && setupStep == SetupPinStep.NONE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "= $resultText",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color(0xFF9CA3AF)
                            ),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Calculator Buttons Grid (5 rows x 4 columns)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val rows = listOf(
                        listOf("%", "()", "⌫", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("C", "0", ".", "=")
                    )

                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (btn in row) {
                                CalculatorKey(
                                    label = btn,
                                    isEqual = btn == "=",
                                    isOperator = btn in listOf("÷", "×", "-", "+"),
                                    isFunction = btn in listOf("%", "()", "⌫", "C"),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onButtonPress(btn) }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Setup Tooltip (matching Screenshot 01:22)
            if (showTooltip && setupStep == SetupPinStep.CREATE_PIN) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .testTag("pin_setup_tooltip"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E232E)
                    ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Create Your PIN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set Your 4 Digit Passcode, and Tap the = Button",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF9CA3AF)
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showTooltip = false },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB)
                            )
                        ) {
                            Text("Got it")
                        }
                    }
                }
            }

            // Setup Success Toast Badge (matching Screenshot 01:23)
            AnimatedVisibility(
                visible = showSuccessBadge,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1F2937)
                    ),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Text(
                        text = "Setup Success",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }
            }

            // Help Dialog for PIN Hints
            if (showHintDialog) {
                AlertDialog(
                    onDismissRequest = { showHintDialog = false },
                    title = {
                        Text("Secret Vault Guide", color = Color.White)
                    },
                    text = {
                        Text(
                            "This calculator protects your private files.\n\n" +
                            "• To open your hidden vault: Enter your 4-digit passcode ($savedPasscode) and tap '='.\n" +
                            "• Standard math calculations will work normally.\n" +
                            "• Keep this passcode confidential.",
                            color = Color(0xFFD1D5DB)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showHintDialog = false }) {
                            Text("Understood", color = Color(0xFF60A5FA))
                        }
                    },
                    containerColor = Color(0xFF1E232E)
                )
            }
        }
    }
}

@Composable
fun CalculatorKey(
    label: String,
    isEqual: Boolean = false,
    isOperator: Boolean = false,
    isFunction: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isEqual -> Color(0xFF2563EB) // Bright Blue equal key
        isOperator -> Color(0xFF202634)
        isFunction -> Color(0xFF232938)
        else -> Color(0xFF1B1E26)
    }

    val contentColor = when {
        isEqual -> Color.White
        isOperator -> Color(0xFF60A5FA)
        isFunction -> Color(0xFF93C5FD)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .aspectRatio(1.15f)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .testTag("calc_key_$label"),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (isEqual || label in listOf("+", "-", "×", "÷")) 26.sp else 22.sp,
                    fontWeight = if (isEqual) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            )
        }
    }
}
