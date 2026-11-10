package com.example.shulecard.activities.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.AdminBottomNav
import com.example.shulecard.utils.StudentProfile
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.random.Random

class IssueCardActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var spinnerStudent: Spinner
    private lateinit var tvStudentInfo: TextView
    private lateinit var rgCardType: RadioGroup
    private lateinit var rbWristband: RadioButton
    private lateinit var rbPlasticCard: RadioButton
    private lateinit var tvGeneratedCardNumber: TextView
    private lateinit var tvGeneratedPin: TextView
    private lateinit var btnGenerateCard: Button
    private lateinit var btnIssueCard: Button

    private var generatedCardNumber = ""
    private var generatedPin = ""
    private var students: List<StudentProfile> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_issue_card)

        toolbar = findViewById(R.id.toolbarIssueCard)
        spinnerStudent = findViewById(R.id.spinnerIssueCardStudent)
        tvStudentInfo = findViewById(R.id.tvStudentInfo)
        rgCardType = findViewById(R.id.rgCardType)
        rbWristband = findViewById(R.id.rbWristband)
        rbPlasticCard = findViewById(R.id.rbPlasticCard)
        tvGeneratedCardNumber = findViewById(R.id.tvGeneratedCardNumber)
        tvGeneratedPin = findViewById(R.id.tvGeneratedPin)
        btnGenerateCard = findViewById(R.id.btnGenerateCard)
        btnIssueCard = findViewById(R.id.btnIssueCard)

        toolbar.setNavigationOnClickListener { finish() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavIssueCard)
        AdminBottomNav.setup(this, bottomNav, R.id.navCards)

        loadStudentInfo()

        spinnerStudent.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                showSelectedStudentInfo(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnGenerateCard.setOnClickListener {
            generateCardDetails()
        }

        btnIssueCard.setOnClickListener {
            issueCard()
        }
    }

    private fun loadStudentInfo() {
        students = StudentRegistryHelper.getAllStudents(this)

        if (students.isEmpty()) {
            tvStudentInfo.text = "No students registered yet.\nRegister a student first."
            spinnerStudent.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("No students available")
            )
            btnGenerateCard.isEnabled = false
            btnIssueCard.isEnabled = false
            return
        }

        val options = students.map { "${it.studentName} (${it.studentId})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStudent.adapter = adapter

        showSelectedStudentInfo(0)
    }

    private fun showSelectedStudentInfo(position: Int) {
        val student = students.getOrNull(position) ?: return
        tvStudentInfo.text = "Student: ${student.studentName}\nStudent ID: ${student.studentId}"
    }

    private fun selectedStudent(): StudentProfile? =
        students.getOrNull(spinnerStudent.selectedItemPosition)

    private fun generateCardDetails() {
        generatedCardNumber = generateCardNumber()
        generatedPin = generatePin()

        tvGeneratedCardNumber.text = "Card Number: $generatedCardNumber"
        tvGeneratedPin.text = "PIN: $generatedPin"
    }

    private fun issueCard() {
        val student = selectedStudent()
        if (student == null) {
            Toast.makeText(this, "Select a student first", Toast.LENGTH_SHORT).show()
            return
        }

        if (generatedCardNumber.isEmpty() || generatedPin.isEmpty()) {
            Toast.makeText(this, "Generate card details first", Toast.LENGTH_SHORT).show()
            return
        }

        val studentId = student.studentId

        val cardType = when (rgCardType.checkedRadioButtonId) {
            R.id.rbWristband -> "Wristband"
            R.id.rbPlasticCard -> "Plastic Card"
            else -> "Student Card"
        }

        val studentRegistryPrefs = getSharedPreferences("StudentRegistry", MODE_PRIVATE)
        studentRegistryPrefs.edit()
            .putString("issuedCardType_$studentId", cardType)
            .putString("issuedCardNumber_$studentId", generatedCardNumber)
            .putString("issuedCardPin_$studentId", generatedPin)
            .apply()

        getSharedPreferences("CardPrefs", MODE_PRIVATE)
            .edit()
            .putString("cardStatus_$studentId", "Active")
            .putBoolean("cardFrozen_$studentId", false)
            .putInt("dailyLimit_$studentId", 10000)
            .putInt("weeklyLimit_$studentId", 50000)
            .putInt("monthlyLimit_$studentId", 200000)
            .putBoolean("purchaseAlert_$studentId", true)
            .putBoolean("lowBalanceAlert_$studentId", true)
            .putBoolean("newCardRequested_$studentId", false)
            .apply()

        Toast.makeText(
            this,
            "Card issued successfully for ${student.studentName}",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun generateCardNumber(): String {
        return "SC-${Random.nextInt(1000, 9999)}-${Random.nextInt(1000, 9999)}"
    }

    private fun generatePin(): String {
        return Random.nextInt(1000, 9999).toString()
    }
}