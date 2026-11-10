package com.example.shulecard.activities.student

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.QrCodeGenerator
import com.example.shulecard.utils.StudentBottomNav
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class QRCodeDisplayActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentId: TextView
    private lateinit var tvCardNumber: TextView
    private lateinit var tvCardStatus: TextView
    private lateinit var ivStudentQr: ImageView
    private lateinit var tvQrStudentId: TextView
    private lateinit var btnShowPin: Button
    private lateinit var btnReportLost: Button

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode_display)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarQrDisplay)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentId = findViewById(R.id.tvStudentId)
        tvCardNumber = findViewById(R.id.tvCardNumber)
        tvCardStatus = findViewById(R.id.tvCardStatus)
        ivStudentQr = findViewById(R.id.ivStudentQr)
        tvQrStudentId = findViewById(R.id.tvQrStudentId)
        btnShowPin = findViewById(R.id.btnShowPin)
        btnReportLost = findViewById(R.id.btnReportLost)

        toolbar.setNavigationOnClickListener { finish() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavQrDisplay)
        StudentBottomNav.setup(this, bottomNav, R.id.navCard)

        btnShowPin.setOnClickListener {
            showPin()
        }

        btnReportLost.setOnClickListener {
            reportLostCard()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCardDetails()
    }

    private fun loadCardDetails() {
        val studentId = prefsManager.getStudentId()

        val studentName = prefsManager.getUserName().ifBlank {
            StudentRegistryHelper.getStudentById(this, studentId)?.studentName ?: "Student"
        }

        val cardType = StudentRegistryHelper.getCardTypeForStudent(this, studentId)
        val cardNumber = StudentRegistryHelper.getCardNumberForStudent(this, studentId)

        val cardPrefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)
        val cardStatus = cardPrefs.getString("cardStatus_$studentId", "Active") ?: "Active"

        tvStudentName.text = studentName
        tvStudentId.text = "Student ID: $studentId"
        tvCardNumber.text = "$cardType Number: $cardNumber"

        tvQrStudentId.text = studentId
        val qrBitmap = QrCodeGenerator.generate(studentId)
        if (qrBitmap != null) {
            ivStudentQr.setImageBitmap(qrBitmap)
        }

        updateStatusBadge(cardStatus)

        val isBlocked = cardStatus.equals("Blocked", ignoreCase = true)
        btnReportLost.isEnabled = !isBlocked
        if (isBlocked) {
            btnReportLost.text = "Card Already Blocked"
        } else {
            btnReportLost.text = "Report Lost Card"
        }
    }

    private fun showPin() {
        val studentId = prefsManager.getStudentId()
        val cardPin = StudentRegistryHelper.getCardPinForStudent(this, studentId)

        AlertDialog.Builder(this)
            .setTitle("Card PIN")
            .setMessage("Your card PIN is: $cardPin")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun reportLostCard() {
        val studentId = prefsManager.getStudentId()
        val cardPrefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)
        val currentStatus = cardPrefs.getString("cardStatus_$studentId", "Active") ?: "Active"

        if (currentStatus.equals("Blocked", ignoreCase = true)) {
            Toast.makeText(this, "This card is already blocked", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Report Lost Card")
            .setMessage("Do you want to report this card as lost and block it?")
            .setPositiveButton("Yes") { _, _ ->
                cardPrefs.edit()
                    .putString("cardStatus_$studentId", "Blocked")
                    .putBoolean("cardFrozen_$studentId", true)
                    .apply()

                updateStatusBadge("Blocked")
                btnReportLost.isEnabled = false
                btnReportLost.text = "Card Already Blocked"

                Toast.makeText(this, "Card reported as lost", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStatusBadge(cardStatus: String) {
        tvCardStatus.text = cardStatus

        val (textColor, bgColor) = when (cardStatus.lowercase()) {
            "active" -> Pair("#2E7D32", "#E8F5E9")
            "frozen" -> Pair("#EF6C00", "#FFF3E0")
            "blocked" -> Pair("#C62828", "#FFEBEE")
            else -> Pair("#212121", "#EEEEEE")
        }

        tvCardStatus.setTextColor(Color.parseColor(textColor))

        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(Color.parseColor(bgColor))
        }
        tvCardStatus.background = shape
    }
}