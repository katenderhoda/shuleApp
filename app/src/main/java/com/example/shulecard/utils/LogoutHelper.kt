package com.example.shulecard.utils

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.example.shulecard.activities.auth.LoginActivity

/** Shared confirm-then-clear-session-then-return-to-login flow, used by every role's logout entry point. */
object LogoutHelper {

    fun confirmAndLogout(activity: Activity, prefsManager: PrefsManager) {
        AlertDialog.Builder(activity)
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("LOG OUT") { _, _ ->
                prefsManager.logout()
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
}
