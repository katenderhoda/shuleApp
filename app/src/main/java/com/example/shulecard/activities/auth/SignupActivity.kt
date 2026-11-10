package com.example.shulecard.activities.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.ParentProfile
import com.example.shulecard.utils.ParentRegistryHelper
import com.example.shulecard.utils.PasswordHasher
import com.example.shulecard.utils.SecurityQuestions
import com.example.shulecard.utils.StudentRegistryHelper

class SignupActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var spRelationship: Spinner
    private lateinit var etStudentId: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spSecurityQuestion: Spinner
    private lateinit var etSecurityAnswer: EditText
    private lateinit var cbTerms: CheckBox
    private lateinit var cbReminders: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        spRelationship = findViewById(R.id.spRelationship)
        etStudentId = findViewById(R.id.etStudentId)
        etPassword = findViewById(R.id.etPasswordSignup)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        spSecurityQuestion = findViewById(R.id.spSecurityQuestion)
        etSecurityAnswer = findViewById(R.id.etSecurityAnswer)
        cbTerms = findViewById(R.id.cbTerms)
        cbReminders = findViewById(R.id.cbReminders)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLoginLink)

        val relationships = arrayOf("Father", "Mother", "Guardian", "Other")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            relationships
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRelationship.adapter = adapter

        val securityQuestionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            SecurityQuestions.QUESTIONS
        )
        securityQuestionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSecurityQuestion.adapter = securityQuestionAdapter

        btnRegister.setOnClickListener {
            registerParent()
        }

        tvLogin.setOnClickListener {
            // Login is always the screen underneath (it\'s the only way here), so just return to it.
            finish()
        }
    }

    private fun registerParent() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val relationship = spRelationship.selectedItem.toString()
        val studentId = etStudentId.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val securityAnswer = etSecurityAnswer.text.toString().trim()

        when {
            fullName.isEmpty() ||
                    email.isEmpty() ||
                    phone.isEmpty() ||
                    studentId.isEmpty() ||
                    password.isEmpty() ||
                    confirmPassword.isEmpty() ||
                    securityAnswer.isEmpty() -> {
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

            !cbTerms.isChecked -> {
                Toast.makeText(this, "You must accept the terms", Toast.LENGTH_SHORT).show()
            }

            ParentRegistryHelper.getParentByEmail(this, email) != null -> {
                Toast.makeText(
                    this,
                    "An account with this email already exists. Please log in instead.",
                    Toast.LENGTH_LONG
                ).show()
            }

            StudentRegistryHelper.getStudentById(this, studentId) == null -> {
                Toast.makeText(
                    this,
                    "No student found with that ID. Please check with the school administrator.",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                ParentRegistryHelper.upsertParent(
                    this,
                    ParentProfile(
                        email = email,
                        passwordHash = PasswordHasher.hash(password),
                        fullName = fullName,
                        phone = phone,
                        relationship = relationship,
                        studentIds = listOf(studentId),
                        receiveReminders = cbReminders.isChecked,
                        securityQuestion = spSecurityQuestion.selectedItem.toString(),
                        securityAnswerHash = PasswordHasher.hash(SecurityQuestions.normalize(securityAnswer))
                    )
                )

                Toast.makeText(this, "Parent registered successfully", Toast.LENGTH_LONG).show()
                // Login is always the screen underneath (it\'s the only way here), so just return to it.
                finish()
            }
        }
    }
}