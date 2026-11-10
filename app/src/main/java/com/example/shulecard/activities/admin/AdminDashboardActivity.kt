package com.example.shulecard.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.activities.shared.NotificationsActivity
import com.example.shulecard.adapters.TransactionAdapter
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.AdminBottomNav
import com.example.shulecard.utils.AdminRegistryHelper
import com.example.shulecard.utils.LogoutHelper
import com.example.shulecard.utils.NotificationHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvTodayDate: TextView
    private lateinit var btnNotifications: FrameLayout
    private lateinit var tvNotificationBadge: TextView
    private lateinit var btnLogout: ImageView

    private lateinit var cardStatStudents: View
    private lateinit var tvStatStudents: TextView
    private lateinit var tvStatStudentsDelta: TextView
    private lateinit var tvStatActiveCards: TextView
    private lateinit var tvStatActiveCardsPct: TextView
    private lateinit var tvStatTodayTxnCount: TextView
    private lateinit var tvStatTodayTxnAmount: TextView
    private lateinit var tvStatAlerts: TextView
    private lateinit var tvStatAlertsStatus: TextView
    private lateinit var cardAlerts: View

    private lateinit var tileRegisterStudent: View
    private lateinit var tileRegisterVendor: View
    private lateinit var tileIssueCard: View
    private lateinit var tileViewVendors: View
    private lateinit var tileViewLogs: View
    private lateinit var tileReports: View
    private lateinit var tilePendingTopUps: View
    private lateinit var tvPendingTopUpsCount: TextView

    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var tvViewAllTransactions: TextView
    private lateinit var recentTransactionsAdapter: TransactionAdapter

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        prefsManager = PrefsManager(this)

        tvWelcome = findViewById(R.id.tvWelcomeAdmin)
        tvTodayDate = findViewById(R.id.tvTodayDate)
        btnNotifications = findViewById(R.id.btnNotifications)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)
        btnLogout = findViewById(R.id.btnLogout)

        cardStatStudents = findViewById(R.id.cardStatStudents)
        tvStatStudents = findViewById(R.id.tvStatStudents)
        tvStatStudentsDelta = findViewById(R.id.tvStatStudentsDelta)
        tvStatActiveCards = findViewById(R.id.tvStatActiveCards)
        tvStatActiveCardsPct = findViewById(R.id.tvStatActiveCardsPct)
        tvStatTodayTxnCount = findViewById(R.id.tvStatTodayTxnCount)
        tvStatTodayTxnAmount = findViewById(R.id.tvStatTodayTxnAmount)
        tvStatAlerts = findViewById(R.id.tvStatAlerts)
        tvStatAlertsStatus = findViewById(R.id.tvStatAlertsStatus)
        cardAlerts = findViewById(R.id.cardAlerts)

        tileRegisterStudent = findViewById(R.id.btnRegisterStudentDashboard)
        tileRegisterVendor = findViewById(R.id.btnRegisterVendorDashboard)
        tileIssueCard = findViewById(R.id.btnIssueCardDashboard)
        tileViewVendors = findViewById(R.id.btnViewVendorsDashboard)
        tileViewLogs = findViewById(R.id.btnViewLogsDashboard)
        tileReports = findViewById(R.id.btnReportsDashboard)
        tilePendingTopUps = findViewById(R.id.btnPendingTopUpsDashboard)
        tvPendingTopUpsCount = findViewById(R.id.tvPendingTopUpsCount)

        rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
        tvNoTransactions = findViewById(R.id.tvNoTransactions)
        tvViewAllTransactions = findViewById(R.id.tvViewAllTransactions)

        rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        recentTransactionsAdapter = TransactionAdapter(emptyList())
        rvRecentTransactions.adapter = recentTransactionsAdapter

        tvTodayDate.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

        btnNotifications.setOnClickListener {
            startActivity(openNotificationsIntent())
        }
        btnLogout.setOnClickListener {
            LogoutHelper.confirmAndLogout(this, prefsManager)
        }
        cardAlerts.setOnClickListener {
            startActivity(openNotificationsIntent())
        }

        tileRegisterStudent.setOnClickListener {
            startActivity(Intent(this, RegisterStudentActivity::class.java))
        }
        cardStatStudents.setOnClickListener {
            startActivity(Intent(this, ViewStudentsActivity::class.java))
        }
        tileRegisterVendor.setOnClickListener {
            startActivity(Intent(this, RegisterVendorActivity::class.java))
        }
        tileIssueCard.setOnClickListener {
            startActivity(Intent(this, IssueCardActivity::class.java))
        }
        tileViewVendors.setOnClickListener {
            startActivity(Intent(this, ViewVendorsActivity::class.java))
        }
        tileViewLogs.setOnClickListener {
            startActivity(Intent(this, TransactionLogsActivity::class.java))
        }
        tileReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }
        tilePendingTopUps.setOnClickListener {
            startActivity(Intent(this, PendingTopUpsActivity::class.java))
        }
        tvViewAllTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionLogsActivity::class.java))
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavAdmin)
        AdminBottomNav.setup(this, bottomNav, R.id.navHome)
    }

    override fun onResume() {
        super.onResume()
        loadWelcome()
        loadStats()
        loadRecentTransactions()
        loadNotificationBadge()
    }

    private fun loadWelcome() {
        val adminName = prefsManager.getUserName().ifBlank {
            AdminRegistryHelper.getAdminName(this)
        }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        tvWelcome.text = "$greeting, $adminName"
    }

    private fun openNotificationsIntent(): Intent {
        return Intent(this, NotificationsActivity::class.java).apply {
            putExtra(NotificationsActivity.EXTRA_ROLE, NotificationsActivity.Role.ADMIN.name)
        }
    }

    private fun loadNotificationBadge() {
        val unread = NotificationHelper.getAdminUnreadCount(this)
        if (unread > 0) {
            tvNotificationBadge.visibility = View.VISIBLE
            tvNotificationBadge.text = if (unread > 9) "9+" else unread.toString()
        } else {
            tvNotificationBadge.visibility = View.GONE
        }
    }

    private fun loadStats() {
        val students = StudentRegistryHelper.getAllStudents(this)
        val allTransactions = prefsManager.getAllTransactions()
        val vendorCount = getVendorCount()

        val pendingTopUps = allTransactions.count {
            it.type.equals("topup", ignoreCase = true) && it.status.equals("pending", ignoreCase = true)
        }
        tvPendingTopUpsCount.text = pendingTopUps.toString()

        tvStatStudents.text = students.size.toString()
        tvStatStudentsDelta.text = "+${countRegisteredThisMonth(students)} this month"

        if (students.isEmpty()) {
            tvStatActiveCards.text = "0"
            tvStatActiveCardsPct.text = "No students yet"
            tvStatTodayTxnCount.text = "0"
            tvStatTodayTxnAmount.text = "UGX 0"
            updateAlerts(pendingTopUps)
            return
        }

        val cardPrefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)

        var activeCards = 0
        var frozenOrBlockedCards = 0
        var lowBalanceCount = 0
        var pendingCardRequests = 0

        for (student in students) {
            val studentId = student.studentId
            val hasIssuedCard = StudentRegistryHelper.hasIssuedCardForStudent(this, studentId)
            val cardStatus = if (hasIssuedCard) {
                cardPrefs.getString("cardStatus_$studentId", "Active") ?: "Active"
            } else {
                "Not issued"
            }

            if (hasIssuedCard && cardStatus.equals("Active", ignoreCase = true)) {
                activeCards++
            }
            if (cardStatus.equals("Frozen", ignoreCase = true) || cardStatus.equals("Blocked", ignoreCase = true)) {
                frozenOrBlockedCards++
            }
            if (cardPrefs.getBoolean("newCardRequested_$studentId", false)) {
                pendingCardRequests++
            }

            val balance = prefsManager.getBalanceForStudent(studentId, student.initialBalance)
            if (balance < LOW_BALANCE_THRESHOLD) {
                lowBalanceCount++
            }
        }

        tvStatActiveCards.text = activeCards.toString()
        val activePct = activeCards * 100 / students.size
        tvStatActiveCardsPct.text = "$activePct% of students"

        val studentIds = students.map { it.studentId }.toSet()
        val relevantTransactions = allTransactions.filter { it.studentId in studentIds }
        val todaysTransactions = relevantTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .filter { isTodayTransaction(it) }

        tvStatTodayTxnCount.text = todaysTransactions.size.toString()
        tvStatTodayTxnAmount.text = "UGX ${formatNumber(todaysTransactions.sumOf { it.amount }.toInt())}"

        updateAlerts(frozenOrBlockedCards + lowBalanceCount + pendingCardRequests + pendingTopUps)
    }

    private fun updateAlerts(count: Int) {
        tvStatAlerts.text = count.toString()
        if (count > 0) {
            tvStatAlertsStatus.text = "Needs attention"
            tvStatAlertsStatus.setTextColor(getColor(R.color.status_warning))
        } else {
            tvStatAlertsStatus.text = "You're all caught up."
            tvStatAlertsStatus.setTextColor(getColor(R.color.status_success))
        }
    }

    private fun countRegisteredThisMonth(students: List<com.example.shulecard.utils.StudentProfile>): Int {
        val now = Calendar.getInstance()
        return students.count { student ->
            if (student.registeredAt <= 0L) return@count false
            val registered = Calendar.getInstance().apply { timeInMillis = student.registeredAt }
            registered.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                registered.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        }
    }

    private fun loadRecentTransactions() {
        val recent = prefsManager.getAllTransactions()
            .sortedByDescending { it.timestamp }
            .take(5)

        recentTransactionsAdapter.updateList(recent)
        rvRecentTransactions.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
        tvNoTransactions.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun getVendorCount(): Int {
        val prefs = getSharedPreferences("VendorRegistry", MODE_PRIVATE)
        val vendorIds = prefs.getString("vendorIds", "") ?: ""
        return if (vendorIds.isEmpty()) 0 else vendorIds.split(",").size
    }

    private fun isTodayTransaction(transaction: Transaction): Boolean {
        val value = transaction.dateTime.trim()

        if (value.contains("Today", ignoreCase = true)) {
            return true
        }

        val formats = listOf(
            "yyyy-MM-dd HH:mm",
            "dd MMM yyyy HH:mm",
            "dd MMM yyyy, HH:mm",
            "dd/MM/yyyy HH:mm"
        )

        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val parsedDate = sdf.parse(value) ?: continue

                val txCal = Calendar.getInstance().apply { time = parsedDate }
                val todayCal = Calendar.getInstance()

                if (txCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    txCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                ) {
                    return true
                }
            } catch (_: Exception) {
            }
        }

        val todayText = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        return value.contains(todayText, ignoreCase = true)
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    companion object {
        private const val LOW_BALANCE_THRESHOLD = 10000.0
    }
}
