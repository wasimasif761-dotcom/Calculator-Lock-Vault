package com.example.model

import java.util.UUID

enum class MediaType {
    PHOTO, VIDEO, FILE
}

data class VaultMedia(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: MediaType,
    val localUri: String? = null,
    val resourceName: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 1024 * 1024 * 2, // 2MB default
    val durationText: String? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false
)

enum class NoteCategory(val displayName: String) {
    DIARY("Diary"),
    PASSWORD("Passwords"),
    SECRET("Secrets"),
    CONTACT("Contacts"),
    FINANCE("Bank & Finance"),
    GENERAL("General")
}

data class VaultNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: NoteCategory = NoteCategory.GENERAL,
    val dateModified: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val colorHex: Long = 0xFF2A2E39
)

data class LockableApp(
    val packageName: String,
    val appName: String,
    val category: String,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val iconColor: Long = 0xFF3B82F6
)

enum class DownloadStatus {
    DOWNLOADING, PAUSED, COMPLETED, FAILED
}

data class VaultDownload(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sourceUrl: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speedText: String,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val durationText: String? = "03:45",
    val dateStarted: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class SubscriptionPlan(
    val id: String,
    val title: String,
    val price: String,
    val period: String,
    val savingsBadge: String? = null,
    val isPopular: Boolean = false,
    val features: List<String>
)

data class PaymentGatewayInfo(
    val name: String,
    val badge: String,
    val transactionFees: String,
    val internationalTaxMoR: String,
    val apiEase: String,
    val pros: List<String>,
    val cons: List<String>,
    val bestFor: String,
    val devDocsUrl: String
)

data class UserProfile(
    val isLoggedIn: Boolean = false,
    val displayName: String = "Waseem Asif",
    val email: String = "wasimasif761@gmail.com",
    val photoUrl: String? = null,
    val isGoogleLinked: Boolean = false,
    val cloudSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

data class FeedbackItem(
    val id: String = UUID.randomUUID().toString(),
    val rating: Int = 5,
    val category: String = "Feature Suggestion",
    val message: String,
    val userEmail: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class IntruderAttempt(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val enteredPin: String,
    val snapshotRes: String = "ic_intruder_mock"
)

data class VaultContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val notes: String = "",
    val isHidden: Boolean = true,
    val avatarColorHex: Long = 0xFF2563EB,
    val dateAdded: Long = System.currentTimeMillis()
)

data class SystemPermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isGranted: Boolean,
    val isDangerousRuntime: Boolean = true,
    val permissionManifestName: String? = null,
    val settingsAction: String? = null
)

object AppDeveloperInfo {
    const val DEVELOPER_NAME = "Waseem Asif"
    const val WHATSAPP_NUMBER = "+966597447275"
    const val WHATSAPP_LINK = "https://wa.me/966597447275"
    const val EMAIL_ADDRESS = "wasimasif761@gmail.com"
    const val APP_VERSION = "v1.0.0 Pro"
    const val APP_TITLE = "Calculator Lock & Stealth Vault"
}
