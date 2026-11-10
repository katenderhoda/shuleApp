package com.example.shulecard.activities.admin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.PasswordHasher
import com.example.shulecard.utils.SecurityQuestions
import com.google.android.material.appbar.MaterialToolbar
import kotlin.random.Random

class RegisterVendorActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etVendorName: EditText
    private lateinit var etVendorId: EditText
    private lateinit var etVendorPassword: EditText
    private lateinit var spVendorCategory: Spinner
    private lateinit var spSecurityQuestion: Spinner
    private lateinit var etSecurityAnswer: EditText
    private lateinit var etContactPerson: EditText
    private lateinit var etContactPhone: EditText
    private lateinit var etContactEmail: EditText
    private lateinit var etLocation: EditText
    private lateinit var btnRegisterVendor: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_vendor)

        toolbar = findViewById(R.id.toolbarRegisterVendor)
        etVendorName = findViewById(R.id.etVendorName)
        etVendorId = findViewById(R.id.etVendorId)
        etVendorPassword = findViewById(R.id.etVendorPassword)
        spVendorCategory = findViewById(R.id.spVendorCategory)
        spSecurityQuestion = findViewById(R.id.spVendorSecurityQuestion)
        etSecurityAnswer = findViewById(R.id.etVendorSecurityAnswer)
        etContactPerson = findViewById(R.id.etContactPerson)
        etContactPhone = findViewById(R.id.etContactPhone)
        etContactEmail = findViewById(R.id.etContactEmail)
        etLocation = findViewById(R.id.etLocation)
        btnRegisterVendor = findViewById(R.id.btnRegisterVendor)

        toolbar.setNavigationOnClickListener { finish() }

        setupCategorySpinner()
        setupSecurityQuestionSpinner()
        prefillVendorId()

        btnRegisterVendor.setOnClickListener {
            registerVendor()
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf(
            "Cafeteria",
            "School Store",
            "Library",
            "Water Point",
            "Stationery Shop",
            "Uniform Shop",
            "Bookstore",
            "Sports Shop",
            "IT Services",
            "Other"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spVendorCategory.adapter = adapter
    }

    private fun setupSecurityQuestionSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, SecurityQuestions.QUESTIONS)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSecurityQuestion.adapter = adapter
    }

    private fun prefillVendorId() {
        if (etVendorId.text.toString().trim().isEmpty()) {
            etVendorId.setText(generateVendorId())
        }
    }

    private fun registerVendor() {
        val vendorName = etVendorName.text.toString().trim()
        val vendorIdInput = etVendorId.text.toString().trim()
        val vendorId = if (vendorIdInput.isBlank()) generateVendorId() else vendorIdInput.uppercase()
        val vendorPassword = etVendorPassword.text.toString().trim()
        val category = spVendorCategory.selectedItem?.toString()?.trim().orEmpty()
        val contactPerson = etContactPerson.text.toString().trim()
        val contactPhone = etContactPhone.text.toString().trim()
        val contactEmail = etContactEmail.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val securityAnswer = etSecurityAnswer.text.toString().trim()

        // Validation
        if (vendorName.isEmpty()) {
            etVendorName.error = "Enter vendor name"
            etVendorName.requestFocus()
            return
        }

        if (vendorId.isEmpty()) {
            etVendorId.error = "Enter vendor ID"
            etVendorId.requestFocus()
            return
        }

        if (vendorPassword.isEmpty()) {
            etVendorPassword.error = "Enter vendor password"
            etVendorPassword.requestFocus()
            return
        }

        if (vendorPassword.length < 4) {
            etVendorPassword.error = "Password must be at least 4 characters"
            etVendorPassword.requestFocus()
            return
        }

        if (contactPerson.isEmpty()) {
            etContactPerson.error = "Enter contact person name"
            etContactPerson.requestFocus()
            return
        }

        if (contactPhone.isEmpty()) {
            etContactPhone.error = "Enter contact phone"
            etContactPhone.requestFocus()
            return
        }

        if (contactEmail.isEmpty()) {
            etContactEmail.error = "Enter contact email"
            etContactEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()) {
            etContactEmail.error = "Enter a valid email address"
            etContactEmail.requestFocus()
            return
        }

        if (securityAnswer.isEmpty()) {
            etSecurityAnswer.error = "Answer the security question so this account can be recovered"
            etSecurityAnswer.requestFocus()
            return
        }

        // Check if vendor ID already exists
        val vendorPrefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val existingIds = vendorPrefs.getString("vendorIds", "") ?: ""
        val idList = if (existingIds.isEmpty()) emptyList() else existingIds.split(",")

        if (idList.contains(vendorId)) {
            etVendorId.error = "Vendor ID already exists"
            etVendorId.requestFocus()
            return
        }

        // Save vendor data
        val editor = vendorPrefs.edit()

        // Store vendor details
        editor.putString("vendorName_$vendorId", vendorName)
        editor.putString("vendorPasswordHash_$vendorId", PasswordHasher.hash(vendorPassword))
        editor.putString("vendorSecurityQuestion_$vendorId", spSecurityQuestion.selectedItem.toString())
        editor.putString(
            "vendorSecurityAnswerHash_$vendorId",
            PasswordHasher.hash(SecurityQuestions.normalize(securityAnswer))
        )
        editor.putString("vendorCategory_$vendorId", category)
        editor.putString("vendorContactPerson_$vendorId", contactPerson)
        editor.putString("vendorContactPhone_$vendorId", contactPhone)
        editor.putString("vendorContactEmail_$vendorId", contactEmail)
        editor.putString("vendorLocation_$vendorId", location)
        editor.putString("vendorStatus_$vendorId", "Active")
        editor.putLong("vendorRegistrationDate_$vendorId", System.currentTimeMillis())

        // Update vendor IDs list
        val updatedIds = if (existingIds.isEmpty()) vendorId else "$existingIds,$vendorId"
        editor.putString("vendorIds", updatedIds)
        editor.apply()

        Toast.makeText(
            this,
            "Vendor registered successfully!\nVendor ID: $vendorId\nVendor: $vendorName",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun generateVendorId(): String {
        val prefix = when (spVendorCategory.selectedItem?.toString()) {
            "Cafeteria" -> "CAF"
            "School Store" -> "STR"
            "Library" -> "LIB"
            "Water Point" -> "WAT"
            "Stationery Shop" -> "STA"
            "Uniform Shop" -> "UNI"
            "Bookstore" -> "BOK"
            "Sports Shop" -> "SPT"
            "IT Services" -> "ITS"
            else -> "VEN"
        }
        return "$prefix${Random.nextInt(100, 999)}"
    }
}