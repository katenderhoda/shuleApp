package com.example.shulecard.activities.student

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.LogoutHelper
import com.example.shulecard.utils.NotificationHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentBottomNav
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvName: TextView
    private lateinit var tvStudentId: TextView
    private lateinit var tvClass: TextView
    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarProfile)
        tvName = findViewById(R.id.tvProfileName)
        tvStudentId = findViewById(R.id.tvProfileStudentId)
        tvClass = findViewById(R.id.tvProfileClass)

        toolbar.setNavigationOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.rowChangePassword).setOnClickListener { showChangePasswordDialog() }
        findViewById<LinearLayout>(R.id.rowNotificationSettings).setOnClickListener { showNotificationSettingsDialog() }
        findViewById<LinearLayout>(R.id.rowSecurity).setOnClickListener { showSecurityInfoDialog() }
        findViewById<LinearLayout>(R.id.rowHelpSupport).setOnClickListener { showHelpDialog() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnProfileLogout)
            .setOnClickListener { showLogoutConfirmation() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavProfile)
        StudentBottomNav.setup(this, bottomNav, R.id.navProfile)
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val studentId = prefsManager.getStudentId()
        val student = StudentRegistryHelper.getStudentById(this, studentId)

        tvName.text = student?.studentName ?: prefsManager.getUserName().ifBlank { "Student" }
        tvStudentId.text = "Student ID: $studentId"
        tvClass.text = student?.studentClass?.ifBlank { "Class not set" } ?: "Class not set"
    }

    private fun dialogPadding() = (20 * resources.displayMetrics.density).toInt()

    private fun showChangePasswordDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dialogPadding()
            setPadding(pad, pad / 2, pad, 0)
        }

        val currentField = EditText(this).apply {
            hint = "Current Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val newField = EditText(this).apply {
            hint = "New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmField = EditText(this).apply {
            hint = "Confirm New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        container.addView(currentField)
        container.addView(newField)
        container.addView(confirmField)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val current = currentField.text.toString()
                val newPassword = newField.text.toString()
                val confirm = confirmField.text.toString()

                if (current.isEmpty() || newPassword.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPassword.length < 4) {
                    Toast.makeText(this, "New password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPassword != confirm) {
                    Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val changed = StudentRegistryHelper.changePassword(this, prefsManager.getStudentId(), current, newPassword)
                if (changed) {
                    Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotificationSettingsDialog() {
        val studentId = prefsManager.getStudentId()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = dialogPadding()
            setPadding(pad, pad / 2, pad, 0)
        }

        val label = TextView(this).apply {
            text = "Show alert notifications"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val toggle = Switch(this).apply {
            isChecked = NotificationHelper.isStudentNotificationsEnabled(this@StudentProfileActivity, studentId)
        }

        container.addView(label)
        container.addView(toggle)

        AlertDialog.Builder(this)
            .setTitle("Notification Settings")
            .setView(container)
            .setPositiveButton("Done") { _, _ ->
                NotificationHelper.setStudentNotificationsEnabled(this, studentId, toggle.isChecked)
            }
            .show()
    }

    private fun showSecurityInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("Security")
            .setMessage(
                "Your password is never stored in plain text — it's protected with salted " +
                    "PBKDF2 hashing.\n\nIf you forget your password, you can recover access " +
                    "from the login screen's \"Forgot Password?\" link using the security " +
                    "question set when your account was created."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage(
                "ShuleCard is your school's cashless payment card. Use My Balance to check " +
                    "your wallet, Show Card to present your QR code at a vendor, and " +
                    "Transactions to review your payments.\n\nFor account recovery, use " +
                    "\"Forgot Password?\" on the login screen. For anything else, ask your " +
                    "school administrator."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        LogoutHelper.confirmAndLogout(this, prefsManager)
    }
}
