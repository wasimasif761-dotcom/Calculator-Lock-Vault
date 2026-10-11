package com.example.ui.vault

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultRepository
import com.example.model.DownloadStatus
import com.example.model.MediaType
import com.example.model.VaultDownload
import com.example.model.VaultMedia

data class SampleVideoPreset(
    val title: String,
    val url: String,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDownloaderScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val downloads by repository.downloads.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var viewingCompletedMedia by remember { mutableStateOf<VaultMedia?>(null) }

    val presets = listOf(
        SampleVideoPreset("Nature HD 1080p", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "MP4"),
        SampleVideoPreset("Animation HD", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "HD"),
        SampleVideoPreset("Space Universe", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "4K")
    )

    fun startDownload(url: String, title: String) {
        if (url.isBlank()) {
            Toast.makeText(context, "Please paste or enter a video URL", Toast.LENGTH_SHORT).show()
            return
        }
        val safeTitle = if (title.isNotBlank()) title else "Downloaded_${System.currentTimeMillis()}"
        repository.startRealVideoDownload(safeTitle, url)
        Toast.makeText(context, "Downloading video into secret Vault...", Toast.LENGTH_SHORT).show()
        urlInput = ""
        titleInput = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Downloader", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clipText.isNullOrBlank()) {
                                urlInput = clipText
                                Toast.makeText(context, "Link pasted from clipboard!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("paste_clipboard_icon")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFF60A5FA))
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
                .padding(horizontal = 16.dp)
                .testTag("video_downloader_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Fast Paste & Instant Download Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2433))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Easy Video Downloader",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Saves directly into your hidden Vault Videos",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // URL Input with 1-Tap Paste Button
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("Paste any video link (MP4, Stream, Web)...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clipText.isNullOrBlank()) {
                                            urlInput = clipText
                                        }
                                    }
                                ) {
                                    Text("Paste", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Optional Title
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            placeholder = { Text("Video Name (Optional)", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Download Button
                        Button(
                            onClick = { startDownload(urlInput, titleInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_video_download_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Video to Vault", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Test Presets
                        Text("Quick Test Downloads (1-Tap):", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(presets) { p ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF131722),
                                    modifier = Modifier.clickable {
                                        urlInput = p.url
                                        titleInput = p.title
                                        startDownload(p.url, p.title)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(p.badge, color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(p.title, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vault Downloads (${downloads.size})",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            items(downloads, key = { it.id }) { dl ->
                DownloadItemCard(
                    download = dl,
                    onToggleStatus = { repository.toggleDownloadStatus(dl.id) },
                    onPlay = {
                        val media = VaultMedia(
                            title = "${dl.title}.mp4",
                            type = MediaType.VIDEO,
                            localUri = dl.sourceUrl
                        )
                        viewingCompletedMedia = media
                    },
                    onDelete = { repository.deleteDownload(dl.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Video Player Viewer for completed download preview
    viewingCompletedMedia?.let { media ->
        VaultMediaViewer(
            media = media,
            onDismiss = { viewingCompletedMedia = null },
            onUnhide = {
                Toast.makeText(context, "Saved in Vault Videos", Toast.LENGTH_SHORT).show()
                viewingCompletedMedia = null
            },
            onDelete = { viewingCompletedMedia = null }
        )
    }
}

@Composable
fun DownloadItemCard(
    download: VaultDownload,
    onToggleStatus: () -> Unit,
    onPlay: () -> Unit,
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
                    modifier = Modifier
                        .size(width = 64.dp, height = 48.dp)
                        .clickable(enabled = download.status == DownloadStatus.COMPLETED, onClick = onPlay)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (download.status) {
                                DownloadStatus.COMPLETED -> Icons.Default.PlayArrow
                                else -> Icons.Default.PlayCircle
                            },
                            contentDescription = null,
                            tint = if (download.status == DownloadStatus.COMPLETED) Color(0xFF10B981) else Color(0xFF60A5FA),
                            modifier = Modifier.size(28.dp)
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
                            text = if (download.status == DownloadStatus.COMPLETED) "Saved to Vault" else download.speedText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (download.status == DownloadStatus.COMPLETED) Color(0xFF10B981) else Color(0xFF38BDF8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play or Action Icon
                if (download.status == DownloadStatus.COMPLETED) {
                    IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = Color(0xFF10B981))
                    }
                } else {
                    IconButton(onClick = onToggleStatus, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = when (download.status) {
                                DownloadStatus.DOWNLOADING -> Icons.Default.Pause
                                DownloadStatus.PAUSED -> Icons.Default.PlayArrow
                                else -> Icons.Default.Refresh
                            },
                            contentDescription = "Control",
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { download.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (download.status == DownloadStatus.COMPLETED) Color(0xFF10B981) else Color(0xFF38BDF8),
                trackColor = Color(0xFF334155),
            )
        }
    }
}
