package com.example.shulecard.utils

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.example.shulecard.activities.auth.LoginActivity
import com.example.shulecard.activities.shared.NotificationsActivity

/**
 * Re-checks the session every time a role screen is created, so a screen can't be reached by
 * back navigation after logout or by launching it directly (e.g. `adb shell am start`) while
 * logged in as a different role. Screens are mapped to roles by their Kotlin package.
 */
object RoleGuard : Application.ActivityLifecycleCallbacks {

    private const val PACKAGE = "com.example.shulecard.activities."

    /** The role allowed to open [activity], "any" for any logged-in user, or null if public. */
    private fun requiredRole(activity: Activity): String? {
        val name = activity.javaClass.name
        if (name == NotificationsActivity::class.java.name) return "any"
        if (!name.startsWith(PACKAGE)) return null
        return when (name.removePrefix(PACKAGE).substringBefore('.')) {
            "admin" -> "admin"
            "parent" -> "parent"
            "student" -> "student"
            "vendor" -> "vendor"
            else -> null // auth screens are public
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val required = requiredRole(activity) ?: return
        val prefs = PrefsManager(activity)
        val allowed = prefs.isLoggedIn() && (required == "any" || prefs.getUserRole() == required)
        if (allowed) return

        activity.startActivity(
            Intent(activity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        activity.finish()
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
