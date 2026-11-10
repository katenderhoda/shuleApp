package com.example.shulecard.activities.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var spinnerStudent: Spinner
    private lateinit var spinnerVendor: Spinner
    private lateinit var btnDailyReport: Button
    private lateinit var btnStudentReport: Button
    private lateinit var btnVendorReport: Button
    private lateinit var btnLowBalance: Button
    private lateinit var tvReportPreview: TextView

    private lateinit var prefsManager: PrefsManager
    private var allTransactions: List<Transaction> = emptyList()

    // Index-aligned with spinnerStudent's options; null represents "All Students".
    private var studentIdsForSpinner: List<String?> = listOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbar)
        spinnerStudent = findViewById(R.id.spinnerStudent)
        spinnerVendor = findViewById(R.id.spinnerVendor)
        btnDailyReport = findViewById(R.id.btnGenerateDailyReport)
        btnStudentReport = findViewById(R.id.btnGenerateStudentReport)
        btnVendorReport = findViewById(R.id.btnGenerateVendorReport)
        btnLowBalance = findViewById(R.id.btnLowBalanceReport)
        tvReportPreview = findViewById(R.id.tvReportPreview)

        toolbar.setNavigationOnClickListener { finish() }

        setupSpinners()
        loadTransactions()
        setupClicks()
        showWelcomePreview()
    }

    override fun onResume() {
        super.onResume()
        setupSpinners()
        loadTransactions()
    }

    private fun setupSpinners() {
        val students = StudentRegistryHelper.getAllStudents(this)

        val studentOptions = mutableListOf("All Students")
        studentIdsForSpinner = mutableListOf<String?>(null).also { ids ->
            students.forEach { student ->
                studentOptions.add("${student.studentName} (${student.studentId})")
                ids.add(student.studentId)
            }
        }

        val studentAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            studentOptions
        )
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStudent.adapter = studentAdapter

        val vendorOptions = listOf(
            "All Vendors",
            "Parent Top-up",
            "School Cafeteria",
            "School Store",
            "Library",
            "Water"
        )

        val vendorAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            vendorOptions
        )
        vendorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVendor.adapter = vendorAdapter
    }

    private fun loadTransactions() {
        allTransactions = prefsManager.getAllTransactions()
    }

    private fun setupClicks() {
        btnDailyReport.setOnClickListener {
            generateDailyReport()
        }

        btnStudentReport.setOnClickListener {
            generateStudentReport()
        }

        btnVendorReport.setOnClickListener {
            generateVendorReport()
        }

        btnLowBalance.setOnClickListener {
            generateLowBalanceReport()
        }
    }

    private fun showWelcomePreview() {
        tvReportPreview.text =
            "Reports Preview\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "Use the buttons above to generate summaries for transactions, students, vendors, and low balances."
    }

    private fun generateDailyReport() {
        val todayTransactions = allTransactions.filter { isToday(it.timestamp) }
        val totalAmount = todayTransactions.sumOf { it.amount }
        val totalPayments = todayTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .sumOf { it.amount }
        val totalTopups = todayTransactions
            .filter { it.type.equals("topup", ignoreCase = true) }
            .sumOf { it.amount }

        val topSpender = todayTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .groupBy { it.studentName }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .maxByOrNull { it.value }

        val mostActiveVendor = todayTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .groupingBy { it.vendor }
            .eachCount()
            .maxByOrNull { it.value }

        val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        tvReportPreview.text =
            "Daily Summary Report - $today\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "Total Transactions: ${todayTransactions.size}\n" +
                    "Total Value: UGX ${formatAmount(totalAmount)}\n" +
                    "Total Top-ups: UGX ${formatAmount(totalTopups)}\n" +
                    "Total Spending: UGX ${formatAmount(totalPayments)}\n" +
                    "Top Spender: ${topSpender?.key ?: "N/A"}\n" +
                    "Most Active Vendor: ${mostActiveVendor?.key ?: "N/A"}"
    }

    private fun generateStudentReport() {
        val selectedStudentId = studentIdsForSpinner.getOrNull(spinnerStudent.selectedItemPosition)

        val student = selectedStudentId?.let { StudentRegistryHelper.getStudentById(this, it) }
            ?: StudentRegistryHelper.getAllStudents(this).firstOrNull()

        if (student == null) {
            tvReportPreview.text =
                "Student Spending Report\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "No registered student found."
            return
        }

        val studentId = student.studentId
        val studentName = student.studentName

        val studentTransactions = allTransactions.filter { it.studentId == studentId }
        val totalTopups = studentTransactions
            .filter { it.type.equals("topup", ignoreCase = true) }
            .sumOf { it.amount }
        val totalPayments = studentTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .sumOf { it.amount }

        val currentBalance = prefsManager.getBalanceForStudent(studentId, 0f)

        val favoriteVendor = studentTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .groupingBy { it.vendor }
            .eachCount()
            .maxByOrNull { it.value }

        tvReportPreview.text =
            "Student Spending Report\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "Student: $studentName ($studentId)\n" +
                    "Transactions: ${studentTransactions.size}\n" +
                    "Total Top-ups: UGX ${formatAmount(totalTopups)}\n" +
                    "Total Spending: UGX ${formatAmount(totalPayments)}\n" +
                    "Current Balance: UGX ${formatAmount(currentBalance.toDouble())}\n" +
                    "Most Used Vendor: ${favoriteVendor?.key ?: "N/A"}"
    }

    private fun generateVendorReport() {
        val selectedVendor = spinnerVendor.selectedItem.toString()

        val vendorTransactions = if (selectedVendor == "All Vendors") {
            allTransactions.filter { it.type.equals("payment", ignoreCase = true) }
        } else {
            allTransactions.filter {
                it.type.equals("payment", ignoreCase = true) &&
                        it.vendor.equals(selectedVendor, ignoreCase = true)
            }
        }

        val totalValue = vendorTransactions.sumOf { it.amount }
        val uniqueStudents = vendorTransactions.map { it.studentId }.distinct().size

        tvReportPreview.text =
            "Vendor Report\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "Vendor: $selectedVendor\n" +
                    "Transactions: ${vendorTransactions.size}\n" +
                    "Total Sales: UGX ${formatAmount(totalValue)}\n" +
                    "Unique Students Served: $uniqueStudents"
    }

    private fun generateLowBalanceReport() {
        val threshold = 10000f
        val selectedStudentId = studentIdsForSpinner.getOrNull(spinnerStudent.selectedItemPosition)

        if (selectedStudentId != null) {
            val student = StudentRegistryHelper.getStudentById(this, selectedStudentId)
            if (student == null) {
                tvReportPreview.text =
                    "Low Balance Report\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "No registered student found."
                return
            }

            val balance = prefsManager.getBalanceForStudent(student.studentId, student.initialBalance)
            val status = if (balance < threshold) "LOW BALANCE ALERT" else "Balance OK"

            tvReportPreview.text =
                "Low Balance Report\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "Student: ${student.studentName} (${student.studentId})\n" +
                        "Current Balance: UGX ${formatAmount(balance.toDouble())}\n" +
                        "Threshold: UGX ${formatAmount(threshold.toDouble())}\n" +
                        "Status: $status"
            return
        }

        val students = StudentRegistryHelper.getAllStudents(this)
        if (students.isEmpty()) {
            tvReportPreview.text =
                "Low Balance Report\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "No registered student found."
            return
        }

        val lowBalanceStudents = students.filter {
            prefsManager.getBalanceForStudent(it.studentId, it.initialBalance) < threshold
        }

        val body = if (lowBalanceStudents.isEmpty()) {
            "All students are above the threshold."
        } else {
            lowBalanceStudents.joinToString("\n") {
                val balance = prefsManager.getBalanceForStudent(it.studentId, it.initialBalance)
                "${it.studentName} (${it.studentId}): UGX ${formatAmount(balance.toDouble())}"
            }
        }

        tvReportPreview.text =
            "Low Balance Report\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "Threshold: UGX ${formatAmount(threshold.toDouble())}\n" +
                    "Students below threshold: ${lowBalanceStudents.size}\n\n" +
                    body
    }

    private fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val tx = Calendar.getInstance().apply { timeInMillis = timestamp }

        return now.get(Calendar.YEAR) == tx.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == tx.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatAmount(amount: Double): String {
        return NumberFormat.getNumberInstance(Locale.US).format(amount)
    }
}