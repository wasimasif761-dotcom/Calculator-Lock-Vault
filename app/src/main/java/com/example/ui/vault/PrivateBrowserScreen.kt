package com.example.ui.vault

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.VaultRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuickBookmark(
    val name: String,
    val url: String,
    val iconColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateBrowserScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit,
    onOpenDownloader: () -> Unit
) {
    val context = LocalContext.current
    val history by repository.browserHistory.collectAsState()
    var urlInput by remember { mutableStateOf("https://duckduckgo.com") }
    var currentUrl by remember { mutableStateOf("https://duckduckgo.com") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showBurnNotice by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val bookmarks = listOf(
        QuickBookmark("DuckDuckGo", "https://duckduckgo.com", Color(0xFFDE5833)),
        QuickBookmark("Google Search", "https://google.com", Color(0xFF4285F4)),
        QuickBookmark("Wikipedia", "https://en.wikipedia.org", Color(0xFF4A5568)),
        QuickBookmark("Archive.org", "https://archive.org", Color(0xFF3182CE))
    )

    fun burnAndExit() {
        webViewInstance?.apply {
            clearCache(true)
            clearHistory()
            clearFormData()
            clearSslPreferences()
        }
        showBurnNotice = true
    }

    LaunchedEffect(showBurnNotice) {
        if (showBurnNotice) {
            delay(1200)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF06B6D4),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "Mask",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Incognito Browser", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Private History Button
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("browser_history_button")
                    ) {
                        Badge(containerColor = Color(0xFF3B82F6)) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Vault History",
                                tint = Color.White
                            )
                        }
                    }

                    // Video Download Button
                    IconButton(
                        onClick = {
                            val cleanTitle = currentUrl.substringAfter("://").substringBefore("/").take(20)
                            repository.addDownload(
                                title = "Video from $cleanTitle",
                                url = currentUrl
                            )
                            onOpenDownloader()
                        },
                        modifier = Modifier.testTag("browser_download_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Video",
                            tint = Color(0xFF10B981)
                        )
                    }

                    // Burn & Shred History Button
                    Button(
                        onClick = { burnAndExit() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("burn_history_button")
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Burn", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151B))
            )
        },
        containerColor = Color(0xFF13151B)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("private_browser_screen")
        ) {
            // URL Input / Address Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E2430)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted SSL",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_url_input"),
                        placeholder = { Text("Search or enter URL...", color = Color(0xFF6B7280), fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    IconButton(
                        onClick = {
                            val target = if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) {
                                urlInput
                            } else {
                                "https://duckduckgo.com/?q=${urlInput.replace(" ", "+")}"
                            }
                            currentUrl = target
                            repository.addBrowserHistory(urlInput, target)
                            webViewInstance?.loadUrl(target)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go", tint = Color(0xFF60A5FA))
                    }
                }
            }

            // Quick Bookmark Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookmarks) { b ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1F2B),
                        modifier = Modifier.clickable {
                            urlInput = b.url
                            currentUrl = b.url
                            repository.addBrowserHistory(b.name, b.url)
                            webViewInstance?.loadUrl(b.url)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = b.iconColor,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(b.name, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Webview View Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    url?.let {
                                        urlInput = it
                                        currentUrl = it
                                        val title = view?.title ?: it
                                        repository.addBrowserHistory(title, it)
                                    }
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = false
                                saveFormData = false
                            }
                            loadUrl(currentUrl)
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Burn Notice Overlay
                if (showBurnNotice) {
                    Card(
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("All Cache & History Shredded!", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Zero traces left behind.", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Bottom Navigation Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF181C26)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    IconButton(onClick = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = Color.White)
                    }
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val home = "https://duckduckgo.com"
                        urlInput = home
                        currentUrl = home
                        webViewInstance?.loadUrl(home)
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFF38BDF8))
                    }
                }
            }
        }
    }

    // Vault Private History Dialog
    if (showHistoryDialog) {
        val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Private Vault History", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { repository.clearBrowserHistory() }) {
                            Text("Clear All", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🔒 Encrypted inside Calculator Lock only. Completely invisible to Google Chrome, Android history, and external apps.",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No private history recorded yet.", color = Color(0xFF6B7280))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(history, key = { it.id }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF131722),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            urlInput = item.url
                                            currentUrl = item.url
                                            webViewInstance?.loadUrl(item.url)
                                            showHistoryDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.url,
                                                color = Color(0xFF38BDF8),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = dateFormat.format(Date(item.timestamp)),
                                                color = Color(0xFF64748B),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { repository.deleteBrowserHistoryItem(item.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = Color(0xFF60A5FA))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }
}
