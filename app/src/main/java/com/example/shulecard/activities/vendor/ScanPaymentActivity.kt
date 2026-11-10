package com.example.shulecard.activities.vendor

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import com.example.shulecard.utils.VendorBottomNav
import com.example.shulecard.utils.VendorItem
import com.example.shulecard.utils.VendorItemRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScanPaymentActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvVendorName: TextView
    private lateinit var etStudentId: TextInputEditText
    private lateinit var btnLookupStudent: MaterialButton
    private lateinit var btnScanStudentQr: MaterialButton
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentBalance: TextView
    private lateinit var tvCardStatus: TextView
    private lateinit var spinnerItem: Spinner
    private lateinit var etAmount: TextInputEditText
    private lateinit var btnProcessPayment: MaterialButton

    private lateinit var prefsManager: PrefsManager

    private var vendorId: String = ""
    private var vendorName: String = "School Cafeteria"
    private var vendorItems: List<VendorItem> = emptyList()
    private var lookedUpStudentId: String? = null
    private var lookedUpStudentName: String? = null
    private var lookedUpInitialBalance: Float = 0f

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val scannedText = result.contents
        if (!scannedText.isNullOrBlank()) {
            etStudentId.setText(scannedText.trim())
            lookupStudent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_payment)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarScanPayment)
        tvVendorName = findViewById(R.id.tvVendorName)
        etStudentId = findViewById(R.id.etStudentId)
        btnLookupStudent = findViewById(R.id.btnLookupStudent)
        btnScanStudentQr = findViewById(R.id.btnScanStudentQr)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentBalance = findViewById(R.id.tvStudentBalance)
        tvCardStatus = findViewById(R.id.tvCardStatus)
        spinnerItem = findViewById(R.id.spinnerItem)
        etAmount = findViewById(R.id.etAmountVendor)
        btnProcessPayment = findViewById(R.id.btnProcessPayment)

        vendorId = intent.getStringExtra("vendorId")?.ifBlank { null } ?: prefsManager.getVendorId()
        vendorName = intent.getStringExtra("vendorName")?.ifBlank { null }
            ?: prefsManager.getUserName().ifBlank { "School Cafeteria" }
        prefsManager.saveVendorId(vendorId)

        tvVendorName.text = "Vendor: $vendorName"

        setupItemSpinner()
        toolbar.setNavigationOnClickListener { finish() }

        btnLookupStudent.setOnClickListener {
            lookupStudent()
        }

        btnScanStudentQr.setOnClickListener {
            scanLauncher.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt("Scan the student's QR code")
                    .setBeepEnabled(true)
                    .setOrientationLocked(true)
            )
        }

        btnProcessPayment.setOnClickListener {
            processPayment()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavScan)
        VendorBottomNav.setup(this, bottomNav, R.id.navScan, vendorId, vendorName)
    }

    /**
     * Populates the item picker from this vendor's own catalog (see VendorItemsActivity) so the
     * amount can be auto-filled from a real price instead of typed by hand. Falls back to a
     * plain manual-amount mode if the vendor hasn't added any items yet.
     */
    private fun setupItemSpinner() {
        vendorItems = VendorItemRegistryHelper.getAvailableItemsForVendor(this, vendorId)

        if (vendorItems.isEmpty()) {
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("No items added — enter amount manually")
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerItem.adapter = adapter
            spinnerItem.isEnabled = false
            return
        }

        spinnerItem.isEnabled = true
        val itemLabels = vendorItems.map {
            "${it.name} — UGX ${NumberFormat.getNumberInstance(Locale.US).format(it.price)}"
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, itemLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerItem.adapter = adapter

        spinnerItem.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = vendorItems.getOrNull(position) ?: return
                etAmount.setText(item.price.toInt().toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etAmount.setText(vendorItems.first().price.toInt().toString())
    }

    private fun lookupStudent() {
        val enteredStudentId = etStudentId.text?.toString()?.trim().orEmpty()

        if (enteredStudentId.isEmpty()) {
            etStudentId.error = "Enter student ID"
            etStudentId.requestFocus()
            return
        }

        val matchedStudent = StudentRegistryHelper.getStudentById(this, enteredStudentId)

        if (matchedStudent == null) {
            showMessage("Student Not Found", "No student matched the entered student ID.")
            clearStudentPreview()
            return
        }

        lookedUpStudentId = matchedStudent.studentId
        lookedUpStudentName = matchedStudent.studentName
        lookedUpInitialBalance = matchedStudent.initialBalance

        refreshStudentPreview()
    }

    private fun refreshStudentPreview() {
        val studentId = lookedUpStudentId ?: return
        val studentName = lookedUpStudentName ?: return

        val currentBalance =
            prefsManager.getBalanceForStudent(studentId, lookedUpInitialBalance).toDouble()

        val cardPrefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)
        val cardStatus = cardPrefs.getString("cardStatus_$studentId", "Active") ?: "Active"

        tvStudentName.visibility = View.VISIBLE
        tvStudentBalance.visibility = View.VISIBLE
        tvCardStatus.visibility = View.VISIBLE

        tvStudentName.text = "Student: $studentName ($studentId)"
        tvStudentBalance.text = "Balance: ${formatCurrency(currentBalance)}"
        tvCardStatus.text = "Card Status: $cardStatus"
    }

    private fun clearStudentPreview() {
        lookedUpStudentId = null
        lookedUpStudentName = null
        lookedUpInitialBalance = 0f

        tvStudentName.visibility = View.GONE
        tvStudentBalance.visibility = View.GONE
        tvCardStatus.visibility = View.GONE
    }

    private fun processPayment() {
        val studentId = lookedUpStudentId
        val studentName = lookedUpStudentName
        val amountText = etAmount.text?.toString()?.trim().orEmpty()
        val selectedItem = vendorItems.getOrNull(spinnerItem.selectedItemPosition)?.name
            ?: spinnerItem.selectedItem?.toString().orEmpty()

        if (studentId.isNullOrEmpty() || studentName.isNullOrEmpty()) {
            Toast.makeText(this, "Look up the student first", Toast.LENGTH_SHORT).show()
            return
        }

        if (amountText.isEmpty()) {
            etAmount.error = "Enter amount"
            etAmount.requestFocus()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            etAmount.error = "Enter a valid amount"
            etAmount.requestFocus()
            return
        }

        val cardPrefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)
        val cardStatus = cardPrefs.getString("cardStatus_$studentId", "Active") ?: "Active"

        if (cardStatus.equals("Frozen", ignoreCase = true)) {
            showMessage("Payment Blocked", "This card is frozen and cannot be used.")
            return
        }

        if (cardStatus.equals("Blocked", ignoreCase = true)) {
            showMessage("Payment Blocked", "This card has been blocked and cannot be used.")
            return
        }

        val currentBalance =
            prefsManager.getBalanceForStudent(studentId, lookedUpInitialBalance).toDouble()

        if (currentBalance < amount) {
            showMessage(
                "Insufficient Balance",
                "Current balance is ${formatCurrency(currentBalance)}.\nYou need ${formatCurrency(amount - currentBalance)} more."
            )
            return
        }

        val dailyLimit = cardPrefs.getInt("dailyLimit_$studentId", 10000)
        val todaySpend = prefsManager.getAllTransactions()
            .filter { it.studentId == studentId }
            .filter { it.type.equals("payment", ignoreCase = true) }
            .filter { isToday(it.timestamp) }
            .sumOf { it.amount }

        if ((todaySpend + amount) > dailyLimit) {
            showMessage(
                "Daily Limit Exceeded",
                "This payment exceeds the daily spending limit.\n\nDaily limit: ${formatCurrency(dailyLimit.toDouble())}\nUsed today: ${formatCurrency(todaySpend)}"
            )
            return
        }

        val newBalance = currentBalance - amount
        prefsManager.saveBalanceForStudent(studentId, newBalance.toFloat())

        val now = System.currentTimeMillis()
        val transaction = Transaction(
            transactionId = "VND-$now",
            studentId = studentId,
            studentName = studentName,
            type = "payment",
            amount = amount,
            vendor = vendorName,
            dateTime = getCurrentDateTime(),
            status = "completed",
            timestamp = now
        )

        prefsManager.addTransaction(transaction)

        refreshStudentPreview()
        etAmount.text?.clear()

        AlertDialog.Builder(this)
            .setTitle("Payment Successful")
            .setMessage(
                "Payment processed successfully.\n\n" +
                        "Student: $studentName\n" +
                        "Item: $selectedItem\n" +
                        "Amount: ${formatCurrency(amount)}\n" +
                        "Reference: ${transaction.transactionId}\n" +
                        "New Balance: ${formatCurrency(newBalance)}"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val tx = Calendar.getInstance().apply { timeInMillis = timestamp }

        return now.get(Calendar.YEAR) == tx.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == tx.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatCurrency(amount: Double): String {
        return "UGX ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun showMessage(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}