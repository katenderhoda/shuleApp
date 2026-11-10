package com.example.shulecard.activities.auth

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.AdminRegistryHelper
import com.example.shulecard.utils.ParentRegistryHelper
import com.example.shulecard.utils.PasswordHasher
import com.example.shulecard.utils.SecurityQuestions
import com.example.shulecard.utils.StudentRegistryHelper

/**
 * Self-service password recovery for every role. The app has no backend to email/SMS a reset
 * link through, so this relies on the security question captured at account creation instead.
 * Since there's no higher-privilege account above admin, this is also the *only* way an admin
 * can recover their own password.
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private enum class AccountType { STUDENT, PARENT, ADMIN, VENDOR }

    private lateinit var etIdentifier: EditText
    private lateinit var btnFindAccount: Button
    private lateinit var groupReset: View
    private lateinit var tvSecurityQuestion: TextView
    private lateinit var etSecurityAnswer: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmNewPassword: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var tvBackToLogin: TextView

    private var matchedAccountType: AccountType? = null
    private var matchedIdentifier: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        etIdentifier = findViewById(R.id.etIdentifier)
        btnFindAccount = findViewById(R.id.btnFindAccount)
        groupReset = findViewById(R.id.groupReset)
        tvSecurityQuestion = findViewById(R.id.tvSecurityQuestion)
        etSecurityAnswer = findViewById(R.id.etSecurityAnswer)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnFindAccount.setOnClickListener { findAccount() }
        btnResetPassword.setOnClickListener { resetPassword() }
        tvBackToLogin.setOnClickListener {
            // Login is always the screen underneath (it\'s the only way here), so just return to it.
            finish()
        }
    }

    private fun findAccount() {
        val identifier = etIdentifier.text.toString().trim()
        if (identifier.isEmpty()) {
            etIdentifier.error = "Enter your Email / Student ID / Vendor ID"
            return
        }

        val question = lookUpSecurityQuestion(identifier)
        if (question == null) {
            groupReset.visibility = View.GONE
            matchedAccountType = null
            matchedIdentifier = null
            Toast.makeText(this, "No account found with that identifier.", Toast.LENGTH_LONG).show()
            return
        }

        if (question.isBlank()) {
            groupReset.visibility = View.GONE
            matchedAccountType = null
            matchedIdentifier = null
            Toast.makeText(
                this,
                "This account has no recovery info set. Please contact the school administrator.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        tvSecurityQuestion.text = question
        groupReset.visibility = View.VISIBLE
    }

    /** Searches every account registry for [identifier]; sets [matchedAccountType] on a hit. */
    private fun lookUpSecurityQuestion(identifier: String): String? {
        StudentRegistryHelper.getStudentById(this, identifier)?.let {
            matchedAccountType = AccountType.STUDENT
            matchedIdentifier = it.studentId
            return it.securityQuestion
        }

        ParentRegistryHelper.getParentByEmail(this, identifier)?.let {
            matchedAccountType = AccountType.PARENT
            matchedIdentifier = it.email
            return it.securityQuestion
        }

        AdminRegistryHelper.getSecurityQuestion(this, identifier)?.let {
            matchedAccountType = AccountType.ADMIN
            matchedIdentifier = identifier
            return it
        }

        findVendorId(identifier)?.let { vendorId ->
            val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
            matchedAccountType = AccountType.VENDOR
            matchedIdentifier = vendorId
            return vendorPrefs.getString("vendorSecurityQuestion_$vendorId", "") ?: ""
        }

        return null
    }

    private fun findVendorId(identifier: String): String? {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val vendorIds = vendorPrefs.getString("vendorIds", "") ?: ""
        if (vendorIds.isEmpty()) return null

        return vendorIds.split(",").find { it.equals(identifier, ignoreCase = true) }
    }

    private fun resetPassword() {
        val accountType = matchedAccountType
        val identifier = matchedIdentifier
        if (accountType == null || identifier == null) {
            Toast.makeText(this, "Find your account first", Toast.LENGTH_SHORT).show()
            return
        }

        val securityAnswer = etSecurityAnswer.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmNewPassword = etConfirmNewPassword.text.toString().trim()

        if (securityAnswer.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmNewPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val success = when (accountType) {
            AccountType.STUDENT -> StudentRegistryHelper.resetPassword(this, identifier, securityAnswer, newPassword)
            AccountType.PARENT -> ParentRegistryHelper.resetPassword(this, identifier, securityAnswer, newPassword)
            AccountType.ADMIN -> AdminRegistryHelper.resetPassword(this, identifier, securityAnswer, newPassword)
            AccountType.VENDOR -> resetVendorPassword(identifier, securityAnswer, newPassword)
        }

        if (!success) {
            Toast.makeText(this, "That answer doesn't match. Please try again.", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Password reset successfully. Please log in.", Toast.LENGTH_LONG).show()
        // Login is always the screen underneath (it\'s the only way here), so just return to it.
        finish()
    }

    private fun resetVendorPassword(vendorId: String, securityAnswer: String, newPassword: String): Boolean {
        val vendorPrefs: SharedPreferences = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val savedAnswerHash = vendorPrefs.getString("vendorSecurityAnswerHash_$vendorId", "") ?: ""
        if (savedAnswerHash.isBlank()) return false
        if (!PasswordHasher.verify(SecurityQuestions.normalize(securityAnswer), savedAnswerHash)) return false

        vendorPrefs.edit()
            .putString("vendorPasswordHash_$vendorId", PasswordHasher.hash(newPassword))
            .apply()
        return true
    }
}
