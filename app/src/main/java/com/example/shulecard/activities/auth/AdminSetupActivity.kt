package com.example.shulecard.activities.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.AdminRegistryHelper
import com.example.shulecard.utils.SecurityQuestions

/**
 * One-time setup screen shown when the app has no admin account yet. There is no hardcoded
 * fallback admin credential — this screen is the only way to create one.
 */
class AdminSetupActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spSecurityQuestion: Spinner
    private lateinit var etSecurityAnswer: EditText
    private lateinit var btnCreateAdmin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AdminRegistryHelper.hasAdminAccount(this)) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_admin_setup)

        etFullName = findViewById(R.id.etAdminFullName)
        etEmail = findViewById(R.id.etAdminEmail)
        etPassword = findViewById(R.id.etAdminPassword)
        etConfirmPassword = findViewById(R.id.etAdminConfirmPassword)
        spSecurityQuestion = findViewById(R.id.spAdminSecurityQuestion)
        etSecurityAnswer = findViewById(R.id.etAdminSecurityAnswer)
        btnCreateAdmin = findViewById(R.id.btnCreateAdmin)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, SecurityQuestions.QUESTIONS)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSecurityQuestion.adapter = adapter

        btnCreateAdmin.setOnClickListener {
            createAdminAccount()
        }
    }

    private fun createAdminAccount() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val securityAnswer = etSecurityAnswer.text.toString().trim()

        when {
            fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            }

            password.length < 4 -> {
                Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
            }

            password != confirmPassword -> {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }

            securityAnswer.isEmpty() -> {
                Toast.makeText(this, "Answer the security question — it's the only way to recover this account", Toast.LENGTH_LONG).show()
            }

            else -> {
                val securityQuestion = spSecurityQuestion.selectedItem.toString()
                AdminRegistryHelper.createAdminAccount(
                    this, fullName, email, password, securityQuestion, securityAnswer
                )
                Toast.makeText(this, "Admin account created. Please log in.", Toast.LENGTH_LONG).show()
                goToLogin()
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
