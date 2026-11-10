package com.example.shulecard.activities.vendor

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
import com.example.shulecard.utils.VendorBottomNav
import com.example.shulecard.utils.VendorRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class VendorProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvCategory: TextView
    private lateinit var prefsManager: PrefsManager

    private var vendorId: String = ""
    private var vendorName: String = "Vendor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_profile)

        prefsManager = PrefsManager(this)
        vendorId = intent.getStringExtra("vendorId")?.ifBlank { null } ?: prefsManager.getVendorId()
        vendorName = intent.getStringExtra("vendorName")?.ifBlank { null }
            ?: prefsManager.getUserName().ifBlank { "Vendor" }

        toolbar = findViewById(R.id.toolbarVendorProfile)
        tvName = findViewById(R.id.tvProfileName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        tvCategory = findViewById(R.id.tvProfileCategory)

        toolbar.setNavigationOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.rowEditProfile).setOnClickListener { showEditProfileDialog() }
        findViewById<LinearLayout>(R.id.rowChangePassword).setOnClickListener { showChangePasswordDialog() }
        findViewById<LinearLayout>(R.id.rowNotificationSettings).setOnClickListener { showNotificationSettingsDialog() }
        findViewById<LinearLayout>(R.id.rowSecurity).setOnClickListener { showSecurityInfoDialog() }
        findViewById<LinearLayout>(R.id.rowHelpSupport).setOnClickListener { showHelpDialog() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnProfileLogout)
            .setOnClickListener { showLogoutConfirmation() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavVendorProfile)
        VendorBottomNav.setup(this, bottomNav, R.id.navProfile, vendorId, vendorName)
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        tvName.text = VendorRegistryHelper.getVendorName(this, vendorId).ifBlank { vendorName }
        tvEmail.text = VendorRegistryHelper.getVendorEmail(this, vendorId)
        tvCategory.text = VendorRegistryHelper.getVendorCategory(this, vendorId).ifBlank { "Vendor" }
    }

    private fun dialogPadding() = (20 * resources.displayMetrics.density).toInt()

    private fun showEditProfileDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dialogPadding()
            setPadding(pad, pad / 2, pad, 0)
        }

        val contactPersonField = EditText(this).apply {
            hint = "Contact Person"
            setText(VendorRegistryHelper.getContactPerson(this@VendorProfileActivity, vendorId))
        }
        val contactPhoneField = EditText(this).apply {
            hint = "Contact Phone"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(VendorRegistryHelper.getContactPhone(this@VendorProfileActivity, vendorId))
        }
        val contactEmailField = EditText(this).apply {
            hint = "Contact Email"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(VendorRegistryHelper.getVendorEmail(this@VendorProfileActivity, vendorId))
        }

        container.addView(contactPersonField)
        container.addView(contactPhoneField)
        container.addView(contactEmailField)

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val contactPerson = contactPersonField.text.toString().trim()
                val contactPhone = contactPhoneField.text.toString().trim()
                val contactEmail = contactEmailField.text.toString().trim()

                if (contactPerson.isEmpty() || contactPhone.isEmpty() || contactEmail.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()) {
                    Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                VendorRegistryHelper.updateProfile(this, vendorId, contactPerson, contactPhone, contactEmail)
                loadProfile()
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

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

                val changed = VendorRegistryHelper.changePassword(this, vendorId, current, newPassword)
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
            isChecked = NotificationHelper.isVendorNotificationsEnabled(this@VendorProfileActivity, vendorId)
        }

        container.addView(label)
        container.addView(toggle)

        AlertDialog.Builder(this)
            .setTitle("Notification Settings")
            .setView(container)
            .setPositiveButton("Done") { _, _ ->
                NotificationHelper.setVendorNotificationsEnabled(this, vendorId, toggle.isChecked)
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
                "ShuleCard lets your school's students pay you straight from their wallet — " +
                        "look them up by student ID on the Scan screen to accept a payment.\n\n" +
                        "For account recovery, use \"Forgot Password?\" on the login screen."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        LogoutHelper.confirmAndLogout(this, prefsManager)
    }
}
