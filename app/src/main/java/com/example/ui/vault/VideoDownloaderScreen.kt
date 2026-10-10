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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultRepository
import com.example.model.DownloadStatus
import com.example.model.VaultDownload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDownloaderScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val downloads by repository.downloads.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newUrlInput by remember { mutableStateOf("") }
    var newTitleInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_download_button")) {
                        Icon(Icons.Default.AddLink, contentDescription = "New Download", tint = Color(0xFF60A5FA))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151B))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                modifier = Modifier.testTag("floating_download_fab")
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Download")
            }
        },
        containerColor = Color(0xFF13151B)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("video_downloader_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Section Header matching Screenshot 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloading (${downloads.count { it.status == DownloadStatus.DOWNLOADING }})",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            items(downloads) { dl ->
                DownloadItemCard(
                    download = dl,
                    onToggleStatus = { repository.toggleDownloadStatus(dl.id) },
                    onSaveToVault = { repository.completeDownloadAndSaveToVault(dl.id) },
                    onDelete = { repository.deleteDownload(dl.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add Download Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Download Video to Vault", color = Color.White) },
            text = {
                Column {
                    Text("Paste any MP4, WebM, or HLS stream URL:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTitleInput,
                        onValueChange = { newTitleInput = it },
                        label = { Text("Video Name") },
                        placeholder = { Text("My Vault Video") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUrlInput,
                        onValueChange = { newUrlInput = it },
                        label = { Text("Video URL") },
                        placeholder = { Text("https://example.com/video.mp4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (newTitleInput.isNotBlank()) newTitleInput else "Downloaded_Video_${System.currentTimeMillis()}"
                        val url = if (newUrlInput.isNotBlank()) newUrlInput else "https://cdn.private/video.mp4"
                        repository.addDownload(title, url)
                        newTitleInput = ""
                        newUrlInput = ""
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Start Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color(0xFF9CA3AF))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }
}

@Composable
fun DownloadItemCard(
    download: VaultDownload,
    onToggleStatus: () -> Unit,
    onSaveToVault: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_card_${download.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.size(width = 64.dp, height = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${download.downloadedBytes / 1_000_000}MB/${download.totalBytes / 1_000_000}MB",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                        Text(
                            text = if (download.status == DownloadStatus.COMPLETED) "Completed" else download.speedText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (download.status == DownloadStatus.COMPLETED) Color(0xFF10B981) else Color(0xFF38BDF8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pause / Resume / Save Icon
                IconButton(
                    onClick = if (download.status == DownloadStatus.COMPLETED) onSaveToVault else onToggleStatus,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = when (download.status) {
                            DownloadStatus.DOWNLOADING -> Icons.Default.Pause
                            DownloadStatus.PAUSED -> Icons.Default.PlayArrow
                            DownloadStatus.COMPLETED -> Icons.Default.Lock
                            DownloadStatus.FAILED -> Icons.Default.Refresh
                        },
                        contentDescription = "Control",
                        tint = if (download.status == DownloadStatus.COMPLETED) Color(0xFF10B981) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar matching Screenshot 6
            LinearProgressIndicator(
                progress = { download.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF38BDF8),
                trackColor = Color(0xFF334155),
            )

            // One-click save to media vault button if complete
            if (download.status != DownloadStatus.COMPLETED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onSaveToVault,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Vault", color = Color(0xFF60A5FA), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
