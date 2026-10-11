package com.example.ui.vault

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.VaultRepository
import com.example.model.MediaType
import com.example.model.VaultMedia
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaVaultScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val mediaItems by repository.mediaItems.collectAsState()
    var selectedTab by remember { mutableStateOf(MediaType.PHOTO) }
    var viewingMedia by remember { mutableStateOf<VaultMedia?>(null) }
    var showImportMenu by remember { mutableStateOf(false) }
    var recentlyImportedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showDeleteOriginalDialog by remember { mutableStateOf(false) }

    // System IntentSender launcher for Android 11+ MediaStore deletion
    val deleteIntentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Original files deleted from phone Gallery!", Toast.LENGTH_LONG).show()
        }
    }

    // Modern Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_media_${System.currentTimeMillis()}"
                repository.addMedia(
                    title = fileName,
                    type = selectedTab,
                    uri = uri
                )
            }
            recentlyImportedUris = uris
            showDeleteOriginalDialog = true
        }
    }

    // Generic file picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "document_${System.currentTimeMillis()}"
                repository.addMedia(
                    title = fileName,
                    type = MediaType.FILE,
                    uri = uri
                )
            }
            recentlyImportedUris = uris
            showDeleteOriginalDialog = true
        }
    }

    // Secret Camera Capture directly into Vault
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val fileName = "Secret_Camera_${System.currentTimeMillis()}"
            repository.addMediaFromBitmap(fileName, bitmap)
            Toast.makeText(context, "Secret photo saved directly in Vault! (Not visible in phone gallery)", Toast.LENGTH_LONG).show()
        }
    }

    val filteredList = mediaItems.filter { !it.isDeleted && it.type == selectedTab }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Media Vault", color = Color.White) },
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
                    IconButton(onClick = { cameraLauncher.launch(null) }) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Secret Camera Snap",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151B))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == MediaType.FILE) {
                        documentPickerLauncher.launch("*/*")
                    } else {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                if (selectedTab == MediaType.PHOTO)
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                else
                                    ActivityResultContracts.PickVisualMedia.VideoOnly
                            )
                        )
                    }
                },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                modifier = Modifier.testTag("import_media_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Import Media"
                )
            }
        },
        containerColor = Color(0xFF13151B)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("media_vault_screen")
        ) {
            // Vault Privacy Indicator Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted Vault Active • Files stay 100% private inside app",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF181C26),
                contentColor = Color(0xFF3B82F6),
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == MediaType.PHOTO,
                    onClick = { selectedTab = MediaType.PHOTO },
                    text = { Text("Photos (${mediaItems.count { !it.isDeleted && it.type == MediaType.PHOTO }})") }
                )
                Tab(
                    selected = selectedTab == MediaType.VIDEO,
                    onClick = { selectedTab = MediaType.VIDEO },
                    text = { Text("Videos (${mediaItems.count { !it.isDeleted && it.type == MediaType.VIDEO }})") }
                )
                Tab(
                    selected = selectedTab == MediaType.FILE,
                    onClick = { selectedTab = MediaType.FILE },
                    text = { Text("Files (${mediaItems.count { !it.isDeleted && it.type == MediaType.FILE }})") }
                )
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E2533),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (selectedTab) {
                                        MediaType.PHOTO -> Icons.Default.PhotoLibrary
                                        MediaType.VIDEO -> Icons.Default.VideoLibrary
                                        MediaType.FILE -> Icons.Default.FolderZip
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hidden ${selectedTab.name.lowercase()}s yet",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to import and hide private items from your device gallery.",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { item ->
                        MediaVaultItemTile(
                            item = item,
                            onClick = { viewingMedia = item }
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Media Viewer Dialog
    viewingMedia?.let { media ->
        Dialog(onDismissRequest = { viewingMedia = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("media_viewer_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181E29))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = media.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewingMedia = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Media preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (media.localUri != null) {
                            AsyncImage(
                                model = if (media.localUri.startsWith("/")) File(media.localUri) else Uri.parse(media.localUri),
                                contentDescription = media.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = when (media.type) {
                                        MediaType.PHOTO -> Icons.Default.Image
                                        MediaType.VIDEO -> Icons.Default.PlayCircle
                                        MediaType.FILE -> Icons.Default.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Encrypted Vault Content",
                                    color = Color(0xFF93C5FD),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Unhide / Restore button
                        OutlinedButton(
                            onClick = {
                                val ok = repository.unhideMediaToGallery(media.id)
                                if (ok) {
                                    Toast.makeText(context, "Restored to Phone Gallery!", Toast.LENGTH_SHORT).show()
                                } else {
                                    repository.restoreMedia(media.id)
                                    Toast.makeText(context, "Item restored.", Toast.LENGTH_SHORT).show()
                                }
                                viewingMedia = null
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unhide to Gallery", color = Color(0xFF60A5FA))
                        }

                        // Delete button
                        Button(
                            onClick = {
                                repository.permanentlyDeleteMedia(media.id)
                                Toast.makeText(context, "Permanently deleted from Vault", Toast.LENGTH_SHORT).show()
                                viewingMedia = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Dialog asking to delete original from phone gallery so it is 100% hidden
    if (showDeleteOriginalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteOriginalDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete from Phone Gallery?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${recentlyImportedUris.size} item(s) are encrypted inside Calculator Lock.\n\nTo make them completely disappear from your phone's Gallery so only you can see them, delete the original copies now.",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteOriginalDialog = false
                        var anyPending = false
                        for (uri in recentlyImportedUris) {
                            val pendingIntent = repository.deleteOriginalFromPhone(uri)
                            if (pendingIntent != null && !anyPending) {
                                anyPending = true
                                deleteIntentSenderLauncher.launch(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            }
                        }
                        if (!anyPending) {
                            Toast.makeText(context, "Original deleted from phone! It is now hidden only in Vault.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete from Gallery (Hide)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteOriginalDialog = false }) {
                    Text("Keep in Phone Gallery", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E2533)
        )
    }
}

@Composable
fun MediaVaultItemTile(
    item: VaultMedia,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .testTag("media_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2432))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.localUri != null) {
                AsyncImage(
                    model = if (item.localUri.startsWith("/")) File(item.localUri) else Uri.parse(item.localUri),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (item.type) {
                                MediaType.PHOTO -> Color(0xFF1E293B)
                                MediaType.VIDEO -> Color(0xFF1E1B4B)
                                MediaType.FILE -> Color(0xFF0F2E2B)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.type) {
                            MediaType.PHOTO -> Icons.Default.Image
                            MediaType.VIDEO -> Icons.Default.PlayArrow
                            MediaType.FILE -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Duration badge for videos
            item.durationText?.let { duration ->
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = duration,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Lock indicator
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Hidden",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
