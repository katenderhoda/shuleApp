package com.example.shulecard.activities.student

import android.graphics.Color
import android.os.Bundle
import android.widget.ProgressBar
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

class MyBalanceActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvCurrentBalance: TextView
    private lateinit var tvWeeklySpend: TextView
    private lateinit var tvMonthlySpend: TextView
    private lateinit var tvLastTopup: TextView
    private lateinit var tvDailyLimit: TextView
    private lateinit var tvLowBalanceAlert: TextView
    private lateinit var progressDailyLimit: ProgressBar

    private lateinit var prefsManager: PrefsManager

    companion object {
        private const val LOW_BALANCE_THRESHOLD = 10000.0
        private const val DAILY_LIMIT = 10000.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_balance)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarMyBalance)
        toolbar.setNavigationOnClickListener { finish() }

        tvCurrentBalance = findViewById(R.id.tvCurrentBalance)
        tvWeeklySpend = findViewById(R.id.tvWeeklySpend)
        tvMonthlySpend = findViewById(R.id.tvMonthlySpend)
        tvLastTopup = findViewById(R.id.tvLastTopup)
        tvDailyLimit = findViewById(R.id.tvDailyLimit)
        tvLowBalanceAlert = findViewById(R.id.tvLowBalanceAlert)
        progressDailyLimit = findViewById(R.id.progressDailyLimit)
    }

    override fun onResume() {
        super.onResume()
        loadBalanceData()
    }

    private fun loadBalanceData() {
        val studentId = prefsManager.getStudentId()
        val defaultBalance = StudentRegistryHelper.getDefaultBalanceForStudent(this, studentId)
        val currentBalance = prefsManager.getBalanceForStudent(studentId, defaultBalance).toDouble()

        val studentTransactions = prefsManager.getAllTransactions()
            .filter { it.studentId == studentId }

        val weeklySpend = studentTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .filter { isThisWeek(it.timestamp) }
            .sumOf { it.amount }

        val monthlySpend = studentTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .filter { isThisMonth(it.timestamp) }
            .sumOf { it.amount }

        val todaySpend = studentTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .filter { isToday(it.timestamp) }
            .sumOf { it.amount }

        val lastTopup = studentTransactions
            .filter { it.type.equals("topup", ignoreCase = true) }
            .maxByOrNull { it.timestamp }

        val progress = ((todaySpend / DAILY_LIMIT) * 100).toInt().coerceIn(0, 100)

        tvCurrentBalance.text = formatCurrency(currentBalance)
        tvWeeklySpend.text = "Spending this week: ${formatCurrency(weeklySpend)}"
        tvMonthlySpend.text = "Spending this month: ${formatCurrency(monthlySpend)}"
        tvLastTopup.text = "Last top-up: ${formatLastTopup(lastTopup)}"
        tvDailyLimit.text =
            "Daily limit: ${formatCurrency(DAILY_LIMIT)} (${formatCurrency(todaySpend)} used today)"

        progressDailyLimit.max = 100
        progressDailyLimit.progress = progress

        if (currentBalance < LOW_BALANCE_THRESHOLD) {
            tvLowBalanceAlert.text = "Warning: Low balance. Please ask your parent to load money."
            tvLowBalanceAlert.setTextColor(Color.parseColor("#E53935"))
        } else {
            tvLowBalanceAlert.text = "Balance status: Good"
            tvLowBalanceAlert.setTextColor(Color.parseColor("#43A047"))
        }
    }

    private fun formatCurrency(amount: Double): String {
        return "UGX ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
    }

    private fun formatLastTopup(transaction: Transaction?): String {
        if (transaction == null) return "No top-up yet"

        return try {
            val input = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val parsed = input.parse(transaction.dateTime)
            if (parsed != null) output.format(parsed) else transaction.dateTime
        } catch (_: Exception) {
            transaction.dateTime
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val tx = Calendar.getInstance().apply { timeInMillis = timestamp }

        return now.get(Calendar.YEAR) == tx.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == tx.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val tx = Calendar.getInstance().apply { timeInMillis = timestamp }

        return now.get(Calendar.YEAR) == tx.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == tx.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val tx = Calendar.getInstance().apply { timeInMillis = timestamp }

        return now.get(Calendar.YEAR) == tx.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == tx.get(Calendar.MONTH)
    }
}