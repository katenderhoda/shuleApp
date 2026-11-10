package com.example.shulecard.activities.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.AdminBottomNav
import com.example.shulecard.utils.PasswordHasher
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.SecurityQuestions
import com.example.shulecard.utils.StudentProfile
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout
import kotlin.random.Random

class RegisterStudentActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvFormIntro: TextView
    private lateinit var etFullName: EditText
    private lateinit var etStudentId: EditText
    private lateinit var tilStudentPassword: TextInputLayout
    private lateinit var etStudentPassword: EditText
    private lateinit var spClassName: Spinner
    private lateinit var spSecurityQuestion: Spinner
    private lateinit var tilSecurityAnswer: TextInputLayout
    private lateinit var etSecurityAnswer: EditText
    private lateinit var etParentName: EditText
    private lateinit var etParentPhone: EditText
    private lateinit var etParentEmail: EditText
    private lateinit var etInitialBalance: EditText
    private lateinit var btnRegisterStudent: Button

    private lateinit var prefsManager: PrefsManager

    /** Non-null when this screen was opened to edit an existing student rather than register a new one. */
    private var editingStudent: StudentProfile? = null

    private val classList = arrayOf(
        "Primary 1", "Primary 2", "Primary 3", "Primary 4",
        "Primary 5", "Primary 6", "Primary 7",
        "Senior 1", "Senior 2", "Senior 3", "Senior 4", "Senior 5", "Senior 6"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_student)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarRegisterStudent)
        tvFormIntro = findViewById(R.id.tvFormIntro)
        etFullName = findViewById(R.id.etStudentFullName)
        etStudentId = findViewById(R.id.etStudentId)
        tilStudentPassword = findViewById(R.id.tilStudentPassword)
        etStudentPassword = findViewById(R.id.etStudentPassword)
        spClassName = findViewById(R.id.spClassName)
        spSecurityQuestion = findViewById(R.id.spStudentSecurityQuestion)
        tilSecurityAnswer = findViewById(R.id.tilStudentSecurityAnswer)
        etSecurityAnswer = findViewById(R.id.etStudentSecurityAnswer)
        etParentName = findViewById(R.id.etParentName)
        etParentPhone = findViewById(R.id.etParentPhone)
        etParentEmail = findViewById(R.id.etParentEmail)
        etInitialBalance = findViewById(R.id.etInitialBalance)
        btnRegisterStudent = findViewById(R.id.btnRegisterStudent)

        toolbar.setNavigationOnClickListener { finish() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavRegisterStudent)
        AdminBottomNav.setup(this, bottomNav, R.id.navStudents)

        setupClassSpinner()
        setupSecurityQuestionSpinner()

        val editStudentId = intent.getStringExtra("editStudentId")
        val existing = editStudentId?.let { StudentRegistryHelper.getStudentById(this, it) }

        if (existing != null) {
            applyEditMode(existing)
        } else {
            prefillStudentId()
        }

        btnRegisterStudent.setOnClickListener {
            registerStudent()
        }
    }

    private fun setupClassSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spClassName.adapter = adapter
    }

    private fun setupSecurityQuestionSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, SecurityQuestions.QUESTIONS)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSecurityQuestion.adapter = adapter
    }

    private fun prefillStudentId() {
        if (etStudentId.text.toString().trim().isEmpty()) {
            etStudentId.setText(generateStudentId())
        }
    }

    /**
     * Prefills every field this screen can show for an existing student. The password and
     * security-answer hashes can't be reversed to show the original values, so those two fields
     * are left blank and made optional — [registerStudent] keeps the existing hash when they're
     * left empty rather than requiring the admin to know/reset them just to fix a typo elsewhere.
     */
    private fun applyEditMode(existing: StudentProfile) {
        editingStudent = existing

        toolbar.title = "Edit Student"
        tvFormIntro.text = "Update this student's profile"
        btnRegisterStudent.text = "Save Changes"

        etFullName.setText(existing.studentName)
        etStudentId.setText(existing.studentId)
        etStudentId.isEnabled = false

        val classIndex = classList.indexOf(existing.studentClass)
        if (classIndex >= 0) spClassName.setSelection(classIndex)

        val questionIndex = SecurityQuestions.QUESTIONS.indexOf(existing.securityQuestion)
        if (questionIndex >= 0) spSecurityQuestion.setSelection(questionIndex)

        etParentName.setText(existing.parentName)
        etParentPhone.setText(existing.parentPhone)
        etParentEmail.setText(existing.parentEmail)
        if (existing.initialBalance > 0f) {
            etInitialBalance.setText(existing.initialBalance.toInt().toString())
        }

        tilStudentPassword.hint = "New Password (leave blank to keep current)"
        tilSecurityAnswer.hint = "New Answer (leave blank to keep current)"
    }

    private fun registerStudent() {
        val editing = editingStudent

        val fullName = etFullName.text.toString().trim()
        val studentId = editing?.studentId ?: run {
            val studentIdInput = etStudentId.text.toString().trim()
            if (studentIdInput.isBlank()) generateStudentId() else studentIdInput.uppercase()
        }
        val studentPasswordInput = etStudentPassword.text.toString().trim()
        val className = spClassName.selectedItem?.toString()?.trim().orEmpty()
        val parentName = etParentName.text.toString().trim()
        val parentPhone = etParentPhone.text.toString().trim()
        val parentEmail = etParentEmail.text.toString().trim()
        val initialBalanceText = etInitialBalance.text.toString().trim()
        val securityAnswerInput = etSecurityAnswer.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Enter full name"
            etFullName.requestFocus()
            return
        }

        if (studentId.isEmpty()) {
            etStudentId.error = "Enter student ID"
            etStudentId.requestFocus()
            return
        }

        if (studentPasswordInput.isEmpty() && editing == null) {
            etStudentPassword.error = "Enter student password"
            etStudentPassword.requestFocus()
            return
        }

        if (studentPasswordInput.isNotEmpty() && studentPasswordInput.length < 4) {
            etStudentPassword.error = "Password must be at least 4 characters"
            etStudentPassword.requestFocus()
            return
        }

        if (parentName.isEmpty()) {
            etParentName.error = "Enter parent/guardian name"
            etParentName.requestFocus()
            return
        }

        if (parentPhone.isEmpty()) {
            etParentPhone.error = "Enter parent phone"
            etParentPhone.requestFocus()
            return
        }

        if (parentEmail.isEmpty()) {
            etParentEmail.error = "Enter parent email"
            etParentEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(parentEmail).matches()) {
            etParentEmail.error = "Enter a valid email address"
            etParentEmail.requestFocus()
            return
        }

        if (securityAnswerInput.isEmpty() && editing == null) {
            etSecurityAnswer.error = "Answer the security question so the student can recover their password"
            etSecurityAnswer.requestFocus()
            return
        }

        val initialBalance = initialBalanceText.toFloatOrNull() ?: 0f
        if (initialBalance < 0f) {
            etInitialBalance.error = "Initial balance cannot be negative"
            etInitialBalance.requestFocus()
            return
        }

        val studentPasswordHash = if (studentPasswordInput.isNotEmpty()) {
            PasswordHasher.hash(studentPasswordInput)
        } else {
            editing!!.studentPasswordHash
        }

        val securityQuestion: String
        val securityAnswerHash: String
        if (securityAnswerInput.isNotEmpty()) {
            securityQuestion = spSecurityQuestion.selectedItem.toString()
            securityAnswerHash = PasswordHasher.hash(SecurityQuestions.normalize(securityAnswerInput))
        } else {
            securityQuestion = editing!!.securityQuestion
            securityAnswerHash = editing.securityAnswerHash
        }

        val existingStudent = editing ?: StudentRegistryHelper.getStudentById(this, studentId)
        val isExistingStudent = existingStudent != null

        StudentRegistryHelper.upsertStudent(
            this,
            StudentProfile(
                studentId = studentId,
                studentName = fullName,
                studentPasswordHash = studentPasswordHash,
                studentClass = className,
                parentName = parentName,
                parentPhone = parentPhone,
                parentEmail = parentEmail,
                initialBalance = initialBalance,
                securityQuestion = securityQuestion,
                securityAnswerHash = securityAnswerHash,
                // Preserve the original registration date when correcting an existing student;
                // only a brand-new record gets "now".
                registeredAt = existingStudent?.registeredAt?.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
        )

        // Only seed a fresh balance/card slate for a brand-new student; re-saving an
        // existing student (e.g. correcting a typo) must not wipe their wallet or card.
        if (!isExistingStudent) {
            prefsManager.saveBalanceForStudent(studentId, initialBalance)

            getSharedPreferences("CardPrefs", MODE_PRIVATE)
                .edit()
                .remove("cardStatus_$studentId")
                .remove("cardFrozen_$studentId")
                .remove("dailyLimit_$studentId")
                .remove("weeklyLimit_$studentId")
                .remove("monthlyLimit_$studentId")
                .remove("purchaseAlert_$studentId")
                .remove("lowBalanceAlert_$studentId")
                .remove("newCardRequested_$studentId")
                .apply()
        }

        Toast.makeText(
            this,
            if (editing != null) "Student updated successfully." else "Student registered successfully.\nLogin with ID: $studentId",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun generateStudentId(): String {
        return "STU${Random.nextInt(100, 999)}"
    }
}
