package com.example.shulecard.activities.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.activities.admin.AdminDashboardActivity
import com.example.shulecard.activities.parent.ParentDashboardActivity
import com.example.shulecard.activities.student.StudentDashboardActivity
import com.example.shulecard.activities.vendor.ScanPaymentActivity
import com.example.shulecard.utils.AdminRegistryHelper
import com.example.shulecard.utils.PrefsManager

class SplashScreenActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val prefsManager = PrefsManager(this)

        if (prefsManager.isLoggedIn()) {
            openNextScreenImmediately(prefsManager)
        } else {
            handler.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    val destination = if (AdminRegistryHelper.hasAdminAccount(this)) {
                        LoginActivity::class.java
                    } else {
                        AdminSetupActivity::class.java
                    }
                    startActivity(Intent(this, destination))
                    finish()
                }
            }, 1000)
        }
    }

    private fun openNextScreenImmediately(prefsManager: PrefsManager) {
        val intent = when (prefsManager.getUserRole()) {
            "student" -> Intent(this, StudentDashboardActivity::class.java)
            "parent" -> Intent(this, ParentDashboardActivity::class.java)
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            "vendor" -> Intent(this, ScanPaymentActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}