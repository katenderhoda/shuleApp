package com.example.shulecard.activities.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.activities.admin.AdminDashboardActivity
import com.example.shulecard.activities.parent.ParentDashboardActivity
import com.example.shulecard.activities.student.StudentDashboardActivity
import com.example.shulecard.activities.vendor.VendorDashboardActivity
import com.example.shulecard.utils.AdminRegistryHelper
import com.example.shulecard.utils.ParentRegistryHelper
import com.example.shulecard.utils.PasswordHasher
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: Button
    private lateinit var tvSignup: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var prefsManager: PrefsManager
    private var loginAttempts = 0

    data class VendorAccount(
        val vendorId: String,
        val password: String,
        val vendorName: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefsManager = PrefsManager(this)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignup = findViewById(R.id.tvSignup)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Clear previous errors
        tilUsername.error = null
        tilPassword.error = null

        if (!validateInputs(username, password)) {
            return
        }

        showLoading(true)

        // Simulate network delay for better UX
        CoroutineScope(Dispatchers.Main).launch {
            delay(800) // Simulate API call

            val loginResult = authenticateUser(username, password)

            if (loginResult != null) {
                loginAttempts = 0 // Reset attempts on successful login
                handleSuccessfulLogin(loginResult)
            } else {
                loginAttempts++
                handleFailedLogin()
            }

            showLoading(false)
        }
    }

    private fun validateInputs(username: String, password: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            tilUsername.error = "Username is required"
            etUsername.requestFocus()
            isValid = false
        }

        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            if (username.isNotEmpty()) {
                etPassword.requestFocus()
            }
            isValid = false
        }

        return isValid
    }

    private fun authenticateUser(username: String, password: String): LoginResult? {
        // Try to authenticate as a registered student
        if (validateRegisteredStudent(username, password)) {
            val matchedStudent = StudentRegistryHelper.getStudentById(this, username)
            return LoginResult(
                role = "student",
                displayName = matchedStudent?.studentName ?: username,
                studentId = matchedStudent?.studentId ?: username,
                initialBalance = matchedStudent?.initialBalance
                    ?: StudentRegistryHelper.getDefaultBalanceForStudent(this, username)
            )
        }

        // Try to authenticate as a registered parent
        if (validateRegisteredParent(username, password)) {
            val matchedParent = ParentRegistryHelper.getParentByEmail(this, username)
            val activeStudentId = matchedParent?.let { ParentRegistryHelper.getActiveStudentId(this, it) }
            val linkedStudent = activeStudentId?.let { StudentRegistryHelper.getStudentById(this, it) }
                ?: StudentRegistryHelper.getTargetStudent(this)

            return LoginResult(
                role = "parent",
                displayName = matchedParent?.fullName ?: "Parent",
                studentId = linkedStudent.studentId,
                initialBalance = linkedStudent.initialBalance,
                email = matchedParent?.email ?: username
            )
        }

        // Try to authenticate as the school admin
        if (AdminRegistryHelper.verifyPassword(this, username, password)) {
            return LoginResult(
                role = "admin",
                displayName = AdminRegistryHelper.getAdminName(this)
            )
        }

        // Try to authenticate as a registered vendor
        val vendorAccount = validateVendorAccount(username, password)
        if (vendorAccount != null) {
            return LoginResult(
                role = "vendor",
                displayName = vendorAccount.vendorName,
                vendorName = vendorAccount.vendorName,
                vendorId = vendorAccount.vendorId
            )
        }

        return null
    }

    private fun handleSuccessfulLogin(result: LoginResult) {
        prefsManager.setLoggedIn(true)
        prefsManager.saveUserRole(result.role)
        prefsManager.saveUserName(result.displayName)
        prefsManager.saveUserEmail(result.email ?: "")

        if (result.studentId != null && result.initialBalance != null) {
            prefsManager.saveStudentId(result.studentId)
            if (!prefsManager.hasBalanceForStudent(result.studentId)) {
                prefsManager.saveBalanceForStudent(result.studentId, result.initialBalance)
            }
        }

        if (result.role == "vendor" && !result.vendorId.isNullOrBlank()) {
            prefsManager.saveVendorId(result.vendorId)
        }

        val intent = when (result.role) {
            "student" -> Intent(this, StudentDashboardActivity::class.java)
            "parent" -> Intent(this, ParentDashboardActivity::class.java)
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            "vendor" -> Intent(this, VendorDashboardActivity::class.java).apply {
                putExtra("vendorId", result.vendorId ?: "")
                putExtra("vendorName", result.vendorName ?: "Vendor")
            }
            else -> return
        }

        startActivity(intent)
        Toast.makeText(this, "Welcome back, ${result.displayName}!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun handleFailedLogin() {
        // Show error on password field
        tilPassword.error = "Invalid username or password"

        // Show toast with attempt information
        val message = when {
            loginAttempts >= 5 -> "Too many failed attempts. Please try again later or contact administrator."
            loginAttempts >= 3 -> "Invalid credentials. ${5 - loginAttempts} attempts remaining."
            else -> "Invalid credentials. Please try again."
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // Keep the username but clear password for security
        // etPassword.text?.clear()  // Uncomment this line if you want to clear password

        // Focus on password field so user can retry
        etPassword.requestFocus()

        // If too many attempts, disable login button temporarily
        if (loginAttempts >= 5) {
            btnLogin.isEnabled = false
            btnLogin.text = "LOGIN DISABLED"

            // Re-enable after 30 seconds
            CoroutineScope(Dispatchers.Main).launch {
                delay(30000) // 30 seconds timeout
                btnLogin.isEnabled = true
                btnLogin.text = "LOGIN"
                loginAttempts = 0
                tilPassword.error = null
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnLogin.text = if (show) "" else "LOGIN"
        etUsername.isEnabled = !show
        etPassword.isEnabled = !show
        tvSignup.isEnabled = !show
        tvForgotPassword.isEnabled = !show
    }

    private fun validateRegisteredStudent(studentId: String, password: String): Boolean {
        return StudentRegistryHelper.verifyPassword(this, studentId, password)
    }

    private fun validateRegisteredParent(email: String, password: String): Boolean {
        return ParentRegistryHelper.verifyPassword(this, email, password)
    }

    private fun validateVendorAccount(vendorId: String, password: String): VendorAccount? {
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val vendorIds = vendorPrefs.getString("vendorIds", "") ?: ""

        if (vendorIds.isEmpty()) return null

        val idList = vendorIds.split(",")
        for (id in idList) {
            if (id.equals(vendorId, ignoreCase = true)) {
                val savedPasswordHash = vendorPrefs.getString("vendorPasswordHash_$id", "") ?: ""
                val status = vendorPrefs.getString("vendorStatus_$id", "Active")

                if (PasswordHasher.verify(password, savedPasswordHash) && status == "Active") {
                    if (!PasswordHasher.isHashed(savedPasswordHash)) {
                        vendorPrefs.edit()
                            .putString("vendorPasswordHash_$id", PasswordHasher.hash(password))
                            .apply()
                    }
                    val vendorName = vendorPrefs.getString("vendorName_$id", "Vendor") ?: "Vendor"
                    return VendorAccount(id, password, vendorName)
                }
            }
        }

        return null
    }

    data class LoginResult(
        val role: String,
        val displayName: String,
        val studentId: String? = null,
        val initialBalance: Float? = null,
        val vendorName: String? = null,
        val vendorId: String? = null,
        val email: String? = null
    )
}