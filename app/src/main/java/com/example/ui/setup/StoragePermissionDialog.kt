package com.example.ui.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun StoragePermissionDialog(
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onAllow()
    }

    Dialog(onDismissRequest = onDeny) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("storage_permission_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Folder icon
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folder",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Allow Calculator to access photos, media, contacts and files on your device?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827),
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "This allows the vault to hide your private photos, encrypt secret contacts, and run stealth background protection.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                TextButton(
                    onClick = {
                        val permissions = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        permissions.add(Manifest.permission.READ_CONTACTS)
                        permissions.add(Manifest.permission.WRITE_CONTACTS)
                        permissions.add(Manifest.permission.CAMERA)
                        permissionLauncher.launch(permissions.toTypedArray())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("allow_permission_button")
                ) {
                    Text(
                        text = "Allow",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                TextButton(
                    onClick = onDeny,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("deny_permission_button")
                ) {
                    Text(
                        text = "Deny",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF4B5563)
                        )
                    )
                }
            }
        }
    }
}
