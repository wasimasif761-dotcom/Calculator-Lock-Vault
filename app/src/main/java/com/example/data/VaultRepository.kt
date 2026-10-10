package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                sizeBytes = 26_000_000,
                durationText = "04:29",
                dateAdded = System.currentTimeMillis() - 86400000 * 3
            ),
            VaultMedia(
                id = "demo_video_2",
                title = "Birthday_Surprise.mp4",
                type = MediaType.VIDEO,
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

    // App Lock & Hide Actions
    fun toggleAppLock(packageName: String) {
        _lockedApps.value = _lockedApps.value.map {
            if (it.packageName == packageName) it.copy(isLocked = !it.isLocked) else it
        }
    }

    fun toggleAppHide(packageName: String) {
        _lockedApps.value = _lockedApps.value.map {
            if (it.packageName == packageName) it.copy(isHidden = !it.isHidden) else it
        }
    }

    // Download Actions
    fun addDownload(title: String, url: String) {
        val newDl = VaultDownload(
            title = title,
            sourceUrl = url,
            totalBytes = 35_000_000,
            downloadedBytes = 12_000_000,
            speedText = "18MB/s",
            status = DownloadStatus.DOWNLOADING
        )
        _downloads.value = listOf(newDl) + _downloads.value
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

    fun completeDownloadAndSaveToVault(id: String) {
        val dl = _downloads.value.find { it.id == id } ?: return
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.COMPLETED, downloadedBytes = it.totalBytes) else it
        }
        // Save into media vault
        addMedia(title = "${dl.title}.mp4", type = MediaType.VIDEO)
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
}
