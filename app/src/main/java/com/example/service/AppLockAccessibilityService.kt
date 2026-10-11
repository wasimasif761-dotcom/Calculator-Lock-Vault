package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.data.VaultRepository
import com.example.ui.lock.AppLockActivity

class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceRunning = false
            private set

        private val unlockedPackagesInSession = mutableSetOf<String>()

        fun markPackageUnlocked(pkg: String) {
            unlockedPackagesInSession.add(pkg)
        }

        fun lockPackageAgain(pkg: String) {
            unlockedPackagesInSession.remove(pkg)
        }

        fun clearSession() {
            unlockedPackagesInSession.clear()
        }

        fun isAccessibilityEnabled(context: Context): Boolean {
            val prefString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return prefString.contains(context.packageName)
        }
    }

    private var repository: VaultRepository? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        repository = VaultRepository(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Ignore our own app, system UI, and home launchers
        if (pkgName == packageName ||
            pkgName == "com.android.systemui" ||
            pkgName.contains("launcher") ||
            pkgName.contains("quickstep")
        ) {
            return
        }

        // Check if package is temporarily unlocked in this session
        if (unlockedPackagesInSession.contains(pkgName)) {
            return
        }

        val repo = repository ?: VaultRepository(applicationContext).also { repository = it }
        val lockedApps = repo.lockedApps.value

        val isTargetLocked = lockedApps.any { it.packageName == pkgName && it.isLocked }

        if (isTargetLocked) {
            // Launch the real AppLock screen on top!
            val lockIntent = Intent(this, AppLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("EXTRA_LOCKED_PACKAGE", pkgName)
            }
            startActivity(lockIntent)
        }
    }

    override fun onInterrupt() {
        isServiceRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }
}
