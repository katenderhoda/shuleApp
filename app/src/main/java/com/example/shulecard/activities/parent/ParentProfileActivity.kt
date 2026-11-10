package com.example.shulecard.activities.parent

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
import com.example.shulecard.utils.ParentBottomNav
import com.example.shulecard.utils.ParentProfile
import com.example.shulecard.utils.ParentRegistryHelper
import com.example.shulecard.utils.PrefsManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class ParentProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvBadge: TextView
    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_profile)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarProfile)
        tvName = findViewById(R.id.tvProfileName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        tvBadge = findViewById(R.id.tvProfileBadge)

        toolbar.setNavigationOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.rowEditProfile).setOnClickListener { showEditProfileDialog() }
        findViewById<LinearLayout>(R.id.rowChangePassword).setOnClickListener { showChangePasswordDialog() }
        findViewById<LinearLayout>(R.id.rowNotificationSettings).setOnClickListener { showNotificationSettingsDialog() }
        findViewById<LinearLayout>(R.id.rowSecurity).setOnClickListener { showSecurityInfoDialog() }
        findViewById<LinearLayout>(R.id.rowHelpSupport).setOnClickListener { showHelpDialog() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnProfileLogout)
            .setOnClickListener { showLogoutConfirmation() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavProfile)
        ParentBottomNav.setup(this, bottomNav, R.id.navProfile)
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun currentParent(): ParentProfile? {
        val email = prefsManager.getUserEmail()
        if (email.isBlank()) return null
        return ParentRegistryHelper.getParentByEmail(this, email)
    }

    private fun loadProfile() {
        val parent = currentParent()
        tvName.text = parent?.fullName?.ifBlank { "Parent" } ?: prefsManager.getUserName().ifBlank { "Parent" }
        tvEmail.text = parent?.email ?: prefsManager.getUserEmail()

        val childCount = parent?.studentIds?.size ?: 0
        tvBadge.text = when {
            childCount == 0 -> "No linked children"
            childCount == 1 -> "1 linked child"
            else -> "$childCount linked children"
        }
    }

    private fun dialogPadding() = (20 * resources.displayMetrics.density).toInt()

    private fun showEditProfileDialog() {
        val parent = currentParent()
        if (parent == null) {
            Toast.makeText(this, "Could not identify your account. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dialogPadding()
            setPadding(pad, pad / 2, pad, 0)
        }

        val nameField = EditText(this).apply {
            hint = "Full Name"
            setText(parent.fullName)
        }
        val phoneField = EditText(this).apply {
            hint = "Phone"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(parent.phone)
        }

        container.addView(nameField)
        container.addView(phoneField)

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val name = nameField.text.toString().trim()
                val phone = phoneField.text.toString().trim()

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Name and phone can't be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                ParentRegistryHelper.updateProfile(this, parent.email, name, phone)
                prefsManager.saveUserName(name)
                loadProfile()
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val parent = currentParent()
        if (parent == null) {
            Toast.makeText(this, "Could not identify your account. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

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

                val changed = ParentRegistryHelper.changePassword(this, parent.email, current, newPassword)
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
        val email = prefsManager.getUserEmail()

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
            isChecked = NotificationHelper.isParentNotificationsEnabled(this@ParentProfileActivity, email)
        }

        container.addView(label)
        container.addView(toggle)

        AlertDialog.Builder(this)
            .setTitle("Notification Settings")
            .setView(container)
            .setPositiveButton("Done") { _, _ ->
                NotificationHelper.setParentNotificationsEnabled(this, email, toggle.isChecked)
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
                        "question you set when this account was created."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage(
                "ShuleCard lets you top up your child's school wallet, manage their card, and " +
                        "track their spending from this dashboard.\n\nFor account recovery, use " +
                        "\"Forgot Password?\" on the login screen."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        LogoutHelper.confirmAndLogout(this, prefsManager)
    }
}
