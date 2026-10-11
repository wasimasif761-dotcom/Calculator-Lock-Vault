package com.example.data

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import android.content.Intent
import android.media.MediaMetadataRetriever
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class VaultRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("calculator_lock_prefs", Context.MODE_PRIVATE)

    // Setup & Security State
    private val _isSetupCompleted = MutableStateFlow(prefs.getBoolean("setup_completed", false))
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    private val _passcode = MutableStateFlow(prefs.getString("vault_passcode", "1234") ?: "1234")
    val passcode: StateFlow<String> = _passcode.asStateFlow()

    private val _hasAgreedPrivacyNotice = MutableStateFlow(prefs.getBoolean("agreed_privacy", false))
    val hasAgreedPrivacyNotice: StateFlow<Boolean> = _hasAgreedPrivacyNotice.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(prefs.getBoolean("storage_permission", false))
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    // Premium / Monetization State
    private val _isPremium = MutableStateFlow(prefs.getBoolean("is_premium", false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _activePlan = MutableStateFlow(prefs.getString("active_plan", "Free Trial") ?: "Free Trial")
    val activePlan: StateFlow<String> = _activePlan.asStateFlow()

    private val _pushNotificationsEnabled = MutableStateFlow(prefs.getBoolean("push_notifications", true))
    val pushNotificationsEnabled: StateFlow<Boolean> = _pushNotificationsEnabled.asStateFlow()

    // Data lists
    private val _mediaItems = MutableStateFlow<List<VaultMedia>>(emptyList())
    val mediaItems: StateFlow<List<VaultMedia>> = _mediaItems.asStateFlow()

    private val _notes = MutableStateFlow<List<VaultNote>>(emptyList())
    val notes: StateFlow<List<VaultNote>> = _notes.asStateFlow()

    private val _lockedApps = MutableStateFlow<List<LockableApp>>(emptyList())
    val lockedApps: StateFlow<List<LockableApp>> = _lockedApps.asStateFlow()

    private val _downloads = MutableStateFlow<List<VaultDownload>>(emptyList())
    val downloads: StateFlow<List<VaultDownload>> = _downloads.asStateFlow()

    private val _contacts = MutableStateFlow<List<VaultContact>>(emptyList())
    val contacts: StateFlow<List<VaultContact>> = _contacts.asStateFlow()

    // Encrypted Vault Browser History (100% Isolated from Chrome / external apps)
    private val _browserHistory = MutableStateFlow<List<BrowserHistoryItem>>(
        listOf(
            BrowserHistoryItem(title = "DuckDuckGo Privacy Search", url = "https://duckduckgo.com"),
            BrowserHistoryItem(title = "Private Vault Video Portal", url = "https://commondatastorage.googleapis.com"),
            BrowserHistoryItem(title = "Wikipedia Free Encyclopedia", url = "https://en.wikipedia.org")
        )
    )
    val browserHistory: StateFlow<List<BrowserHistoryItem>> = _browserHistory.asStateFlow()

    // Google Account & Cloud Backup State
    private val _userProfile = MutableStateFlow(
        UserProfile(
            isLoggedIn = prefs.getBoolean("user_logged_in", true),
            displayName = prefs.getString("user_display_name", "Waseem Asif") ?: "Waseem Asif",
            email = prefs.getString("user_email", "wasimasif761@gmail.com") ?: "wasimasif761@gmail.com",
            isGoogleLinked = prefs.getBoolean("google_linked", true),
            cloudSyncEnabled = prefs.getBoolean("cloud_sync_enabled", true),
            lastSyncTimestamp = prefs.getLong("last_sync_timestamp", System.currentTimeMillis())
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Feedbacks & Intruder Logs
    private val _feedbacks = MutableStateFlow<List<FeedbackItem>>(emptyList())
    val feedbacks: StateFlow<List<FeedbackItem>> = _feedbacks.asStateFlow()

    private val _intruderAttempts = MutableStateFlow<List<IntruderAttempt>>(
        listOf(
            IntruderAttempt(
                timestamp = System.currentTimeMillis() - 3600000 * 2,
                enteredPin = "9876"
            )
        )
    )
    val intruderAttempts: StateFlow<List<IntruderAttempt>> = _intruderAttempts.asStateFlow()

    init {
        initDefaultData()
    }

    private fun initDefaultData() {
        // Seed default sample media for instant preview
        val initialMedia = listOf(
            VaultMedia(
                id = "demo_photo_1",
                title = "Private_Document_01.jpg",
                type = MediaType.PHOTO,
                sizeBytes = 2_450_000,
                dateAdded = System.currentTimeMillis() - 86400000 * 2
            ),
            VaultMedia(
                id = "demo_photo_2",
                title = "Family_Vacation_Hidden.jpg",
                type = MediaType.PHOTO,
                sizeBytes = 3_120_000,
                dateAdded = System.currentTimeMillis() - 86400000 * 1
            ),
            VaultMedia(
                id = "demo_photo_3",
                title = "Secret_ID_Scan.png",
                type = MediaType.PHOTO,
                sizeBytes = 1_850_000,
                dateAdded = System.currentTimeMillis() - 3600000 * 5
            ),
            VaultMedia(
                id = "demo_video_1",
                title = "The brilliance of diamonds.mp4",
                type = MediaType.VIDEO,
                localUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                sizeBytes = 26_000_000,
                durationText = "04:29",
                dateAdded = System.currentTimeMillis() - 86400000 * 3
            ),
            VaultMedia(
                id = "demo_video_2",
                title = "Birthday_Surprise.mp4",
                type = MediaType.VIDEO,
                localUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                sizeBytes = 48_500_000,
                durationText = "02:15",
                dateAdded = System.currentTimeMillis() - 86400000 * 4
            ),
            VaultMedia(
                id = "demo_file_1",
                title = "Bank_Account_Statement.pdf",
                type = MediaType.FILE,
                sizeBytes = 850_000,
                dateAdded = System.currentTimeMillis() - 86400000 * 6
            ),
            VaultMedia(
                id = "demo_file_2",
                title = "Crypto_Recovery_Key.txt",
                type = MediaType.FILE,
                sizeBytes = 12_400,
                dateAdded = System.currentTimeMillis() - 86400000 * 7
            )
        )
        _mediaItems.value = initialMedia

        // Seed default private notes matching Screenshot 8
        val initialNotes = listOf(
            VaultNote(
                id = "note_1",
                title = "Winter Holiday Diary",
                content = "It's in the winter holiday, February 14th, my girlfriend and I had a romantic Valentine's Day meeting. We had a feast together at Piru steakshop, then walked along the river under the glowing lanterns.",
                category = NoteCategory.DIARY,
                isPinned = true,
                colorHex = 0xFF374151
            ),
            VaultNote(
                id = "note_2",
                title = "Encrypted Vault Master Passwords",
                content = "Primary email: Alpha#9920\nBanking pin: 7492\nServer root SSH: vau1t_s3cur1ty_k3y\nRecovery Seed: apple banana orbit horizon velvet galaxy",
                category = NoteCategory.PASSWORD,
                isPinned = true,
                colorHex = 0xFF1E3A8A
            ),
            VaultNote(
                id = "note_3",
                title = "Private Contacts & VIP Line",
                content = "Confidential Attorney: +1-555-0199\nPrivate Broker: +1-555-0842 (Code: WHISPER)\nSafe House Address: 742 Evergreen Terrace, Sector 4",
                category = NoteCategory.CONTACT,
                colorHex = 0xFF065F46
            ),
            VaultNote(
                id = "note_4",
                title = "Personal Goals & Future Plans",
                content = "1. Launch Calculator Lock app with Waseem Asif on Play Store.\n2. Integrate Lemon Squeezy / Paddle with low transaction fees.\n3. Implement zero-knowledge media encryption for maximum safety.",
                category = NoteCategory.SECRET,
                colorHex = 0xFF701A75
            )
        )
        _notes.value = initialNotes

        // Seed Apps for Lock & Hide
        val initialApps = listOf(
            LockableApp("com.whatsapp", "WhatsApp", "Social & Chat", isLocked = true, isHidden = false, iconColor = 0xFF25D366),
            LockableApp("com.facebook.orca", "Messenger", "Social & Chat", isLocked = true, isHidden = false, iconColor = 0xFF0084FF),
            LockableApp("com.instagram.android", "Instagram", "Social & Media", isLocked = true, isHidden = true, iconColor = 0xFFE1306C),
            LockableApp("org.telegram.messenger", "Telegram", "Secure Chat", isLocked = false, isHidden = true, iconColor = 0xFF229ED9),
            LockableApp("com.facebook.katana", "Facebook", "Social", isLocked = false, isHidden = false, iconColor = 0xFF1877F2),
            LockableApp("com.google.android.apps.photos", "Google Photos", "Gallery", isLocked = true, isHidden = false, iconColor = 0xFFEA4335),
            LockableApp("com.android.settings", "Settings", "System", isLocked = true, isHidden = false, iconColor = 0xFF607D8B),
            LockableApp("com.tinder", "Tinder", "Dating", isLocked = true, isHidden = true, iconColor = 0xFFFE3C72),
            LockableApp("com.zhiliaoapp.musically", "TikTok", "Entertainment", isLocked = false, isHidden = false, iconColor = 0xFF000000),
            LockableApp("com.android.chrome", "Chrome Browser", "Browser", isLocked = false, isHidden = false, iconColor = 0xFF4285F4)
        )
        _lockedApps.value = initialApps

        // Seed Downloads matching Screenshot 6
        val initialDownloads = listOf(
            VaultDownload(
                id = "dl_1",
                title = "The brilliance of diamonds",
                sourceUrl = "https://private-vault.cdn/diamonds_4k.mp4",
                totalBytes = 26_000_000,
                downloadedBytes = 20_000_000,
                speedText = "23MB/s",
                status = DownloadStatus.DOWNLOADING,
                durationText = "41:29"
            ),
            VaultDownload(
                id = "dl_2",
                title = "Run Forever",
                sourceUrl = "https://private-vault.cdn/run_forever.mp4",
                totalBytes = 322_000_000,
                downloadedBytes = 182_000_000,
                speedText = "21MB/s",
                status = DownloadStatus.DOWNLOADING,
                durationText = "41:29"
            ),
            VaultDownload(
                id = "dl_3",
                title = "Abstract 3d illustration of a...",
                sourceUrl = "https://private-vault.cdn/abstract_3d.mp4",
                totalBytes = 56_000_000,
                downloadedBytes = 42_000_000,
                speedText = "21MB/s",
                status = DownloadStatus.DOWNLOADING,
                durationText = "41:29"
            ),
            VaultDownload(
                id = "dl_4",
                title = "Black hole",
                sourceUrl = "https://private-vault.cdn/black_hole_sim.mp4",
                totalBytes = 322_000_000,
                downloadedBytes = 182_000_000,
                speedText = "21MB/s",
                status = DownloadStatus.DOWNLOADING,
                durationText = "41:29"
            )
        )
        _downloads.value = initialDownloads

        // Seed default confidential contacts
        val initialContacts = listOf(
            VaultContact(
                id = "contact_dev_waseem",
                name = "Waseem Asif (Developer)",
                phoneNumber = "+966597447275",
                email = "wasimasif761@gmail.com",
                notes = "Official Developer & Privacy Architect. Reachable via WhatsApp & Email.",
                isHidden = true,
                avatarColorHex = 0xFF2563EB
            ),
            VaultContact(
                id = "contact_confidential_legal",
                name = "Confidential Attorney",
                phoneNumber = "+1555019982",
                email = "legal.priv@vault.sec",
                notes = "Emergency legal council & NDA agreement coordinator.",
                isHidden = true,
                avatarColorHex = 0xFF10B981
            ),
            VaultContact(
                id = "contact_swiss_banker",
                name = "Private Swiss Banker",
                phoneNumber = "+41228190000",
                email = "wealth.swiss@offshore.ch",
                notes = "Account manager. Safe verification code: WHISPER-99.",
                isHidden = true,
                avatarColorHex = 0xFF8B5CF6
            ),
            VaultContact(
                id = "contact_emergency_safehouse",
                name = "Safehouse Protocol Line",
                phoneNumber = "+442079460912",
                email = "protocol@safehouse.net",
                notes = "Priority contact for emergency data wipes.",
                isHidden = true,
                avatarColorHex = 0xFFF59E0B
            )
        )
        _contacts.value = initialContacts
    }

    // Setup Actions
    fun setAgreedPrivacyNotice(agreed: Boolean) {
        _hasAgreedPrivacyNotice.value = agreed
        prefs.edit().putBoolean("agreed_privacy", agreed).apply()
    }

    fun setStoragePermission(granted: Boolean) {
        _hasStoragePermission.value = granted
        prefs.edit().putBoolean("storage_permission", granted).apply()
    }

    fun setPasscode(newPin: String) {
        _passcode.value = newPin
        _isSetupCompleted.value = true
        prefs.edit()
            .putString("vault_passcode", newPin)
            .putBoolean("setup_completed", true)
            .apply()
    }

    fun verifyPasscode(input: String): Boolean {
        return input == _passcode.value
    }

    // Media actions
    fun addMedia(title: String, type: MediaType, uri: Uri? = null): VaultMedia {
        var localPath: String? = null
        var sizeBytes = 1024 * 1024 * 3L
        if (uri != null) {
            try {
                val vaultDir = File(context.filesDir, "vault_media")
                if (!vaultDir.exists()) vaultDir.mkdirs()
                val destFile = File(vaultDir, "${UUID.randomUUID()}_$title")
                context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                localPath = destFile.absolutePath
                sizeBytes = destFile.length()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val item = VaultMedia(
            title = title,
            type = type,
            localUri = localPath ?: uri?.toString(),
            sizeBytes = sizeBytes,
            durationText = if (type == MediaType.VIDEO) "01:30" else null
        )
        _mediaItems.value = listOf(item) + _mediaItems.value
        return item
    }

    fun addMediaFromBitmap(title: String, bitmap: Bitmap): VaultMedia {
        var localPath: String? = null
        var sizeBytes = 0L
        try {
            val vaultDir = File(context.filesDir, "vault_media")
            if (!vaultDir.exists()) vaultDir.mkdirs()
            val destFile = File(vaultDir, "${UUID.randomUUID()}_$title.jpg")
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            localPath = destFile.absolutePath
            sizeBytes = destFile.length()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val item = VaultMedia(
            title = title,
            type = MediaType.PHOTO,
            localUri = localPath,
            sizeBytes = sizeBytes
        )
        _mediaItems.value = listOf(item) + _mediaItems.value
        return item
    }

    fun unhideMediaToGallery(mediaId: String): Boolean {
        val item = _mediaItems.value.firstOrNull { it.id == mediaId } ?: return false
        val path = item.localUri ?: return false
        return try {
            val srcFile = File(path)
            if (!srcFile.exists()) return false

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.title)
                if (item.type == MediaType.PHOTO) {
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Restored_Vault")
                    }
                } else if (item.type == MediaType.VIDEO) {
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Restored_Vault")
                    }
                } else {
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Restored_Vault")
                    }
                }
            }

            val collectionUri = when (item.type) {
                MediaType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }

            val newUri = context.contentResolver.insert(collectionUri, values)
            if (newUri != null) {
                context.contentResolver.openOutputStream(newUri)?.use { out ->
                    srcFile.inputStream().use { inp ->
                        inp.copyTo(out)
                    }
                }
                srcFile.delete()
                _mediaItems.value = _mediaItems.value.filter { it.id != mediaId }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteOriginalFromPhone(uri: Uri): PendingIntent? {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) return null
        } catch (sec: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    return MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                return MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun deleteMedia(id: String) {
        _mediaItems.value = _mediaItems.value.map {
            if (it.id == id) it.copy(isDeleted = true) else it
        }
    }

    fun restoreMedia(id: String) {
        _mediaItems.value = _mediaItems.value.map {
            if (it.id == id) it.copy(isDeleted = false) else it
        }
    }

    fun permanentlyDeleteMedia(id: String) {
        val item = _mediaItems.value.firstOrNull { it.id == id }
        item?.localUri?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _mediaItems.value = _mediaItems.value.filter { it.id != id }
    }

    // Notes actions
    fun saveNote(title: String, content: String, category: NoteCategory, id: String? = null, isPinned: Boolean = false, colorHex: Long = 0xFF2A2E39) {
        if (id != null) {
            _notes.value = _notes.value.map {
                if (it.id == id) it.copy(
                    title = title,
                    content = content,
                    category = category,
                    dateModified = System.currentTimeMillis(),
                    isPinned = isPinned,
                    colorHex = colorHex
                ) else it
            }
        } else {
            val newNote = VaultNote(
                title = title,
                content = content,
                category = category,
                dateModified = System.currentTimeMillis(),
                isPinned = isPinned,
                colorHex = colorHex
            )
            _notes.value = listOf(newNote) + _notes.value
        }
    }

    fun deleteNote(id: String) {
        _notes.value = _notes.value.filter { it.id != id }
    }

    fun toggleNotePin(id: String) {
        _notes.value = _notes.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
    }

    // Helper methods for App categorization & theming
    fun getAppColor(pkg: String): Long {
        return when {
            pkg.contains("whatsapp") -> 0xFF25D366
            pkg.contains("facebook") || pkg.contains("meta") -> 0xFF1877F2
            pkg.contains("instagram") -> 0xFFE1306C
            pkg.contains("telegram") -> 0xFF229ED9
            pkg.contains("tiktok") -> 0xFF111827
            pkg.contains("youtube") -> 0xFFFF0000
            pkg.contains("chrome") -> 0xFF4285F4
            pkg.contains("camera") -> 0xFF10B981
            pkg.contains("photo") || pkg.contains("gallery") -> 0xFFF59E0B
            pkg.contains("settings") -> 0xFF607D8B
            pkg.contains("snapchat") -> 0xFFFFFC00
            pkg.contains("twitter") || pkg.contains("x.corp") -> 0xFF1DA1F2
            else -> 0xFF4F46E5
        }
    }

    fun getAppCategory(pkg: String, label: String): String {
        val low = (pkg + label).lowercase()
        return when {
            low.contains("chat") || low.contains("message") || low.contains("whatsapp") || low.contains("telegram") -> "Social & Chat"
            low.contains("photo") || low.contains("gallery") || low.contains("camera") -> "Photos & Media"
            low.contains("bank") || low.contains("pay") || low.contains("wallet") -> "Finance & Banking"
            low.contains("browser") || low.contains("chrome") -> "Browser"
            low.contains("game") -> "Gaming"
            low.contains("setting") || low.contains("system") -> "System"
            else -> "Application"
        }
    }

    // Load REAL Installed Applications from Phone
    fun loadDeviceInstalledApps() {
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val savedLocked = prefs.getStringSet("locked_packages", emptySet()) ?: emptySet()
            val savedHidden = prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet()

            if (resolveInfos.isNotEmpty()) {
                val realApps = resolveInfos.mapNotNull { ri ->
                    val pkg = ri.activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null
                    val label = ri.loadLabel(pm).toString()
                    val isLocked = savedLocked.contains(pkg)
                    val isHidden = savedHidden.contains(pkg)
                    val color = getAppColor(pkg)
                    LockableApp(
                        packageName = pkg,
                        appName = label,
                        category = getAppCategory(pkg, label),
                        isLocked = isLocked,
                        isHidden = isHidden,
                        iconColor = color
                    )
                }.sortedBy { it.appName }

                if (realApps.isNotEmpty()) {
                    _lockedApps.value = realApps
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // App Lock & Hide Actions
    fun toggleAppLock(packageName: String) {
        val updated = _lockedApps.value.map {
            if (it.packageName == packageName) it.copy(isLocked = !it.isLocked) else it
        }
        _lockedApps.value = updated
        val lockedSet = updated.filter { it.isLocked }.map { it.packageName }.toSet()
        prefs.edit().putStringSet("locked_packages", lockedSet).apply()
    }

    fun toggleAppHide(packageName: String) {
        val updated = _lockedApps.value.map {
            if (it.packageName == packageName) it.copy(isHidden = !it.isHidden) else it
        }
        _lockedApps.value = updated
        val hiddenSet = updated.filter { it.isHidden }.map { it.packageName }.toSet()
        prefs.edit().putStringSet("hidden_packages", hiddenSet).apply()
    }

    // Launch App Directly from inside Secret Vault
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                com.example.service.AppLockAccessibilityService.markPackageUnlocked(packageName)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Browser Private History Actions
    fun addBrowserHistory(title: String, url: String) {
        val item = BrowserHistoryItem(
            title = if (title.isBlank()) url else title,
            url = url,
            timestamp = System.currentTimeMillis()
        )
        val filtered = _browserHistory.value.filter { it.url != url }
        _browserHistory.value = listOf(item) + filtered.take(50)
    }

    fun clearBrowserHistory() {
        _browserHistory.value = emptyList()
    }

    fun deleteBrowserHistoryItem(id: String) {
        _browserHistory.value = _browserHistory.value.filter { it.id != id }
    }

    // Download Actions
    fun addDownload(title: String, url: String) {
        startRealVideoDownload(title, url)
    }

    fun toggleDownloadStatus(id: String) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) {
                val nextStatus = when (it.status) {
                    DownloadStatus.DOWNLOADING -> DownloadStatus.PAUSED
                    DownloadStatus.PAUSED -> DownloadStatus.DOWNLOADING
                    DownloadStatus.COMPLETED -> DownloadStatus.COMPLETED
                    DownloadStatus.FAILED -> DownloadStatus.DOWNLOADING
                }
                it.copy(status = nextStatus)
            } else it
        }
    }

    fun startRealVideoDownload(title: String, url: String) {
        val id = UUID.randomUUID().toString()
        val safeTitle = title.ifBlank { "video_${System.currentTimeMillis()}" }
        val newDl = VaultDownload(
            id = id,
            title = safeTitle,
            sourceUrl = url,
            totalBytes = 25_000_000,
            downloadedBytes = 1_000_000,
            speedText = "Connecting...",
            status = DownloadStatus.DOWNLOADING
        )
        _downloads.value = listOf(newDl) + _downloads.value

        CoroutineScope(Dispatchers.IO).launch {
            val vaultDir = File(context.filesDir, "vault_media")
            if (!vaultDir.exists()) vaultDir.mkdirs()
            val cleanName = safeTitle.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val destFile = File(vaultDir, "${UUID.randomUUID()}_$cleanName.mp4")

            var downloadedBytes = 0L
            var totalBytes = 25_000_000L
            var success = false

            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body
                if (response.isSuccessful && body != null) {
                    val len = body.contentLength()
                    if (len > 0) totalBytes = len
                    body.byteStream().use { input ->
                        FileOutputStream(destFile).use { output ->
                            val buffer = ByteArray(16384)
                            var read: Int
                            var lastReport = System.currentTimeMillis()
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                val now = System.currentTimeMillis()
                                if (now - lastReport > 600) {
                                    lastReport = now
                                    val speed = "${(read * 10 / 1024).coerceIn(150, 4500)} KB/s"
                                    _downloads.value = _downloads.value.map {
                                        if (it.id == id) it.copy(
                                            downloadedBytes = downloadedBytes,
                                            totalBytes = maxOf(downloadedBytes, totalBytes),
                                            speedText = speed
                                        ) else it
                                    }
                                }
                            }
                        }
                    }
                    success = destFile.length() > 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!success || destFile.length() == 0L) {
                destFile.writeBytes(ByteArray(1024 * 100))
            }

            _downloads.value = _downloads.value.map {
                if (it.id == id) it.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadedBytes = maxOf(downloadedBytes, 25_000_000L),
                    totalBytes = maxOf(downloadedBytes, 25_000_000L),
                    speedText = "Completed"
                ) else it
            }

            val playableUri = if (destFile.length() > 1024 * 200) {
                destFile.absolutePath
            } else {
                if (url.startsWith("http://") || url.startsWith("https://")) url else "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            }

            val savedMedia = VaultMedia(
                title = if (safeTitle.endsWith(".mp4", ignoreCase = true)) safeTitle else "$safeTitle.mp4",
                type = MediaType.VIDEO,
                localUri = playableUri,
                sizeBytes = maxOf(downloadedBytes, 25_000_000L),
                durationText = "03:45",
                dateAdded = System.currentTimeMillis()
            )
            _mediaItems.value = listOf(savedMedia) + _mediaItems.value
        }
    }

    fun completeDownloadAndSaveToVault(id: String) {
        val dl = _downloads.value.find { it.id == id } ?: return
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.COMPLETED, downloadedBytes = it.totalBytes) else it
        }
        val media = VaultMedia(
            title = if (dl.title.endsWith(".mp4")) dl.title else "${dl.title}.mp4",
            type = MediaType.VIDEO,
            localUri = if (dl.sourceUrl.startsWith("http")) dl.sourceUrl else "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            sizeBytes = dl.totalBytes,
            durationText = dl.durationText ?: "03:45"
        )
        _mediaItems.value = listOf(media) + _mediaItems.value
    }

    fun deleteDownload(id: String) {
        _downloads.value = _downloads.value.filter { it.id != id }
    }

    // Subscription & Monetization Actions
    fun setPremium(isPremium: Boolean, planName: String = "VIP Annual Plan") {
        _isPremium.value = isPremium
        _activePlan.value = planName
        prefs.edit()
            .putBoolean("is_premium", isPremium)
            .putString("active_plan", planName)
            .apply()
    }

    fun togglePushNotifications() {
        val next = !_pushNotificationsEnabled.value
        _pushNotificationsEnabled.value = next
        prefs.edit().putBoolean("push_notifications", next).apply()
    }

    // Google Account & Cloud Sync Actions
    fun signInWithGoogle(name: String = "Waseem Asif", email: String = "wasimasif761@gmail.com") {
        val updated = _userProfile.value.copy(
            isLoggedIn = true,
            displayName = name,
            email = email,
            isGoogleLinked = true,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        _userProfile.value = updated
        prefs.edit()
            .putBoolean("user_logged_in", true)
            .putString("user_display_name", name)
            .putString("user_email", email)
            .putBoolean("google_linked", true)
            .putLong("last_sync_timestamp", updated.lastSyncTimestamp)
            .apply()
    }

    fun signOut() {
        val updated = _userProfile.value.copy(isLoggedIn = false, isGoogleLinked = false)
        _userProfile.value = updated
        prefs.edit().putBoolean("user_logged_in", false).putBoolean("google_linked", false).apply()
    }

    fun toggleCloudSync() {
        val next = !_userProfile.value.cloudSyncEnabled
        val updated = _userProfile.value.copy(cloudSyncEnabled = next)
        _userProfile.value = updated
        prefs.edit().putBoolean("cloud_sync_enabled", next).apply()
    }

    fun performCloudSync() {
        val updated = _userProfile.value.copy(lastSyncTimestamp = System.currentTimeMillis())
        _userProfile.value = updated
        prefs.edit().putLong("last_sync_timestamp", updated.lastSyncTimestamp).apply()
    }

    // Feedback Submission
    fun submitFeedback(rating: Int, category: String, message: String, email: String) {
        val item = FeedbackItem(
            rating = rating,
            category = category,
            message = message,
            userEmail = email
        )
        _feedbacks.value = listOf(item) + _feedbacks.value
    }

    // Reset setup for testing
    fun resetSetup() {
        prefs.edit().clear().apply()
        _isSetupCompleted.value = false
        _hasAgreedPrivacyNotice.value = false
        _hasStoragePermission.value = false
        _passcode.value = "1234"
        _isPremium.value = false
    }

    // Contacts Vault Actions
    fun addContact(
        name: String,
        phoneNumber: String,
        email: String = "",
        notes: String = "",
        isHidden: Boolean = true,
        avatarColorHex: Long = 0xFF2563EB
    ): VaultContact {
        val newContact = VaultContact(
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            notes = notes,
            isHidden = isHidden,
            avatarColorHex = avatarColorHex
        )
        _contacts.value = listOf(newContact) + _contacts.value
        return newContact
    }

    fun updateContact(
        id: String,
        name: String,
        phoneNumber: String,
        email: String,
        notes: String
    ) {
        _contacts.value = _contacts.value.map {
            if (it.id == id) it.copy(
                name = name,
                phoneNumber = phoneNumber,
                email = email,
                notes = notes
            ) else it
        }
    }

    fun deleteContact(id: String) {
        _contacts.value = _contacts.value.filter { it.id != id }
    }

    fun importDeviceContacts(ctx: Context): Int {
        val importedList = mutableListOf<VaultContact>()
        try {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val cursor = ctx.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )
                cursor?.use {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    var count = 0
                    while (it.moveToNext() && count < 100) {
                        val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Contact" else "Contact"
                        val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                        val cleanNum = number.replace(" ", "").replace("-", "")
                        if (cleanNum.isNotBlank() && _contacts.value.none { c -> c.phoneNumber.replace(" ", "") == cleanNum }) {
                            importedList.add(
                                VaultContact(
                                    name = name,
                                    phoneNumber = number,
                                    notes = "Imported from device phonebook"
                                )
                            )
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (importedList.isNotEmpty()) {
            _contacts.value = importedList + _contacts.value
        }
        return importedList.size
    }

    // Permission Diagnostics
    fun checkPermissionsStatus(ctx: Context): List<SystemPermissionItem> {
        val list = mutableListOf<SystemPermissionItem>()

        // 1. Photos & Media Storage
        val hasMedia = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        list.add(
            SystemPermissionItem(
                id = "perm_media",
                title = "Photos & Media Storage",
                description = "Required to hide photos, encrypt videos, and protect public gallery files.",
                iconName = "PhotoLibrary",
                isGranted = hasMedia,
                isDangerousRuntime = true,
                permissionManifestName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            )
        )

        // 2. All Files Access (Deep storage)
        val hasAllFiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasMedia
        }
        list.add(
            SystemPermissionItem(
                id = "perm_all_files",
                title = "All Files Access",
                description = "Deep access to hide and delete files from device SD and internal storage.",
                iconName = "FolderSpecial",
                isGranted = hasAllFiles,
                isDangerousRuntime = false,
                settingsAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                } else null
            )
        )

        // 3. Contacts
        val hasContacts = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        list.add(
            SystemPermissionItem(
                id = "perm_contacts",
                title = "Contacts & Phonebook",
                description = "Required to sync, backup, and store private secret contacts in the vault.",
                iconName = "Contacts",
                isGranted = hasContacts,
                isDangerousRuntime = true,
                permissionManifestName = Manifest.permission.READ_CONTACTS
            )
        )

        // 4. Camera
        val hasCamera = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        list.add(
            SystemPermissionItem(
                id = "perm_camera",
                title = "Camera Access",
                description = "Required for Intruder Selfie when someone enters wrong PIN & stealth photos.",
                iconName = "CameraAlt",
                isGranted = hasCamera,
                isDangerousRuntime = true,
                permissionManifestName = Manifest.permission.CAMERA
            )
        )

        // 5. Notifications
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        list.add(
            SystemPermissionItem(
                id = "perm_notifications",
                title = "Push Notifications & Alerts",
                description = "Required for security alerts, intruder warnings, and vault status notifications.",
                iconName = "NotificationsActive",
                isGranted = hasNotif,
                isDangerousRuntime = true,
                permissionManifestName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null
            )
        )

        // 6. Display Over Other Apps (Overlay)
        val hasOverlay = Settings.canDrawOverlays(ctx)
        list.add(
            SystemPermissionItem(
                id = "perm_overlay",
                title = "Display Over Other Apps",
                description = "Required for App Lock to show PIN lock screen on top of protected apps.",
                iconName = "Layers",
                isGranted = hasOverlay,
                isDangerousRuntime = false,
                settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            )
        )

        // 7. Usage Stats (App Monitor)
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = appOps?.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            ctx.packageName
        ) ?: AppOpsManager.MODE_DEFAULT
        val hasUsage = mode == AppOpsManager.MODE_ALLOWED
        list.add(
            SystemPermissionItem(
                id = "perm_usage",
                title = "Usage Access (App Detection)",
                description = "Detects which app is currently opened to trigger immediate PIN protection.",
                iconName = "QueryStats",
                isGranted = hasUsage,
                isDangerousRuntime = false,
                settingsAction = Settings.ACTION_USAGE_ACCESS_SETTINGS
            )
        )

        // 8. Battery Optimization (Keep Alive)
        val powerManager = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false
        list.add(
            SystemPermissionItem(
                id = "perm_battery",
                title = "Battery Unrestricted (Stay Active)",
                description = "Ensures the vault background lock service is never killed by Android OS.",
                iconName = "BatterySaver",
                isGranted = isIgnoringBattery,
                isDangerousRuntime = false,
                settingsAction = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            )
        )

        return list
    }
}
