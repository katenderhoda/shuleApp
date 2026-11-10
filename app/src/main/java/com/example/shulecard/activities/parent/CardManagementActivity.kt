package com.example.shulecard.activities.parent

import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.ParentBottomNav
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class CardManagementActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvCardDetails: TextView
    private lateinit var switchFreezeCard: Switch
    private lateinit var etDailyLimit: EditText
    private lateinit var etWeeklyLimit: EditText
    private lateinit var etMonthlyLimit: EditText
    private lateinit var switchPurchaseAlert: Switch
    private lateinit var switchLowBalanceAlert: Switch
    private lateinit var btnSaveLimits: Button
    private lateinit var btnReportLost: Button
    private lateinit var btnRequestNewCard: Button

    private var isInitializing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_management)

        toolbar = findViewById(R.id.toolbarCardManagement)
        toolbar.setNavigationOnClickListener { finish() }

        tvCardDetails = findViewById(R.id.tvCardDetails)
        switchFreezeCard = findViewById(R.id.switchFreezeCard)
        etDailyLimit = findViewById(R.id.etDailyLimit)
        etWeeklyLimit = findViewById(R.id.etWeeklyLimit)
        etMonthlyLimit = findViewById(R.id.etMonthlyLimit)
        switchPurchaseAlert = findViewById(R.id.switchPurchaseAlert)
        switchLowBalanceAlert = findViewById(R.id.switchLowBalanceAlert)
        btnSaveLimits = findViewById(R.id.btnSaveLimits)
        btnReportLost = findViewById(R.id.btnReportLost)
        btnRequestNewCard = findViewById(R.id.btnRequestNewCard)

        loadCardSettings()
        setupListeners()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavCardManagement)
        ParentBottomNav.setup(this, bottomNav, R.id.navCard)
    }

    override fun onResume() {
        super.onResume()
        loadCardSettings()
    }

    private fun loadCardSettings() {
        val targetStudent = StudentRegistryHelper.getTargetStudent(this)
        val studentId = targetStudent.studentId
        val cardType = StudentRegistryHelper.getCardTypeForStudent(this, studentId)
        val cardNumber = StudentRegistryHelper.getCardNumberForStudent(this, studentId)

        val prefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)

        val cardStatus = prefs.getString("cardStatus_$studentId", "Active") ?: "Active"
        val cardFrozen = prefs.getBoolean("cardFrozen_$studentId", false)
        val dailyLimit = prefs.getInt("dailyLimit_$studentId", 10000)
        val weeklyLimit = prefs.getInt("weeklyLimit_$studentId", 50000)
        val monthlyLimit = prefs.getInt("monthlyLimit_$studentId", 200000)
        val purchaseAlert = prefs.getBoolean("purchaseAlert_$studentId", true)
        val lowBalanceAlert = prefs.getBoolean("lowBalanceAlert_$studentId", true)

        isInitializing = true
        switchFreezeCard.isChecked = cardFrozen || cardStatus.equals("Blocked", ignoreCase = true)
        etDailyLimit.setText(dailyLimit.toString())
        etWeeklyLimit.setText(weeklyLimit.toString())
        etMonthlyLimit.setText(monthlyLimit.toString())
        switchPurchaseAlert.isChecked = purchaseAlert
        switchLowBalanceAlert.isChecked = lowBalanceAlert
        isInitializing = false

        updateCardDetails(
            studentName = targetStudent.studentName,
            studentId = studentId,
            cardType = cardType,
            cardNumber = cardNumber,
            cardStatus = cardStatus
        )

        switchFreezeCard.isEnabled = !cardStatus.equals("Blocked", ignoreCase = true)
    }

    private fun setupListeners() {
        switchFreezeCard.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (isInitializing) return@setOnCheckedChangeListener

            val targetStudent = StudentRegistryHelper.getTargetStudent(this)
            val studentId = targetStudent.studentId
            val cardType = StudentRegistryHelper.getCardTypeForStudent(this, studentId)
            val cardNumber = StudentRegistryHelper.getCardNumberForStudent(this, studentId)

            val prefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)
            val currentStatus = prefs.getString("cardStatus_$studentId", "Active") ?: "Active"

            if (currentStatus.equals("Blocked", ignoreCase = true)) {
                switchFreezeCard.isChecked = true
                Toast.makeText(this, "Blocked cards cannot be unfrozen", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            val newStatus = if (isChecked) "Frozen" else "Active"

            prefs.edit()
                .putBoolean("cardFrozen_$studentId", isChecked)
                .putString("cardStatus_$studentId", newStatus)
                .apply()

            updateCardDetails(
                studentName = targetStudent.studentName,
                studentId = studentId,
                cardType = cardType,
                cardNumber = cardNumber,
                cardStatus = newStatus
            )

            Toast.makeText(
                this,
                if (isChecked) "Card frozen successfully" else "Card reactivated successfully",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnSaveLimits.setOnClickListener {
            saveCardSettings()
        }

        btnReportLost.setOnClickListener {
            reportLostCard()
        }

        btnRequestNewCard.setOnClickListener {
            val targetStudent = StudentRegistryHelper.getTargetStudent(this)
            val prefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)

            prefs.edit()
                .putBoolean("newCardRequested_${targetStudent.studentId}", true)
                .apply()

            Toast.makeText(this, "New card request submitted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCardSettings() {
        val daily = etDailyLimit.text.toString().trim()
        val weekly = etWeeklyLimit.text.toString().trim()
        val monthly = etMonthlyLimit.text.toString().trim()

        if (daily.isEmpty() || weekly.isEmpty() || monthly.isEmpty()) {
            Toast.makeText(this, "Fill in all spending limits", Toast.LENGTH_SHORT).show()
            return
        }

        val dailyValue = daily.toIntOrNull()
        val weeklyValue = weekly.toIntOrNull()
        val monthlyValue = monthly.toIntOrNull()

        if (dailyValue == null || weeklyValue == null || monthlyValue == null) {
            Toast.makeText(this, "Enter valid numeric limits", Toast.LENGTH_SHORT).show()
            return
        }

        if (dailyValue <= 0 || weeklyValue <= 0 || monthlyValue <= 0) {
            Toast.makeText(this, "Limits must be greater than zero", Toast.LENGTH_SHORT).show()
            return
        }

        val targetStudent = StudentRegistryHelper.getTargetStudent(this)
        val studentId = targetStudent.studentId

        val prefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)
        prefs.edit()
            .putInt("dailyLimit_$studentId", dailyValue)
            .putInt("weeklyLimit_$studentId", weeklyValue)
            .putInt("monthlyLimit_$studentId", monthlyValue)
            .putBoolean("purchaseAlert_$studentId", switchPurchaseAlert.isChecked)
            .putBoolean("lowBalanceAlert_$studentId", switchLowBalanceAlert.isChecked)
            .apply()

        Toast.makeText(this, "Card settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun reportLostCard() {
        val targetStudent = StudentRegistryHelper.getTargetStudent(this)
        val studentId = targetStudent.studentId
        val cardType = StudentRegistryHelper.getCardTypeForStudent(this, studentId)
        val cardNumber = StudentRegistryHelper.getCardNumberForStudent(this, studentId)

        val prefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)

        AlertDialog.Builder(this)
            .setTitle("Report Lost Card")
            .setMessage("Do you want to block this card permanently?")
            .setPositiveButton("Yes") { _, _ ->
                prefs.edit()
                    .putString("cardStatus_$studentId", "Blocked")
                    .putBoolean("cardFrozen_$studentId", true)
                    .apply()

                isInitializing = true
                switchFreezeCard.isChecked = true
                switchFreezeCard.isEnabled = false
                isInitializing = false

                updateCardDetails(
                    studentName = targetStudent.studentName,
                    studentId = studentId,
                    cardType = cardType,
                    cardNumber = cardNumber,
                    cardStatus = "Blocked"
                )

                Toast.makeText(this, "Card blocked successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCardDetails(
        studentName: String,
        studentId: String,
        cardType: String,
        cardNumber: String,
        cardStatus: String
    ) {
        tvCardDetails.text =
            "Child: $studentName ($studentId)\n" +
                    "Card Type: $cardType\n" +
                    "Card Number: $cardNumber\n" +
                    "Status: $cardStatus"
    }
}