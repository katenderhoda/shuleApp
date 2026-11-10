package com.example.shulecard.activities.student

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
import com.example.shulecard.utils.LogoutHelper
import com.example.shulecard.utils.NotificationHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentBottomNav
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvTodayDate: TextView
    private lateinit var btnNotifications: FrameLayout
    private lateinit var tvNotificationBadge: TextView
    private lateinit var btnLogout: ImageView

    private lateinit var tvStatBalance: TextView
    private lateinit var tvStatTodaySpend: TextView
    private lateinit var ivStatCardStatus: ImageView
    private lateinit var tvStatCardStatus: TextView
    private lateinit var tvStatAlerts: TextView
    private lateinit var cardAlerts: View

    private lateinit var tileMyBalance: View
    private lateinit var tileShowCard: View
    private lateinit var tileBrowseVendors: View
    private lateinit var tileViewTransactions: View

    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var tvViewAllTransactions: TextView
    private lateinit var recentTransactionsAdapter: TransactionAdapter

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        prefsManager = PrefsManager(this)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvTodayDate = findViewById(R.id.tvTodayDate)
        btnNotifications = findViewById(R.id.btnNotifications)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)
        btnLogout = findViewById(R.id.btnLogout)

        tvStatBalance = findViewById(R.id.tvStatBalance)
        tvStatTodaySpend = findViewById(R.id.tvStatTodaySpend)
        ivStatCardStatus = findViewById(R.id.ivStatCardStatus)
        tvStatCardStatus = findViewById(R.id.tvStatCardStatus)
        tvStatAlerts = findViewById(R.id.tvStatAlerts)
        cardAlerts = findViewById(R.id.cardAlerts)

        tileMyBalance = findViewById(R.id.btnMyBalance)
        tileShowCard = findViewById(R.id.btnShowCard)
        tileBrowseVendors = findViewById(R.id.btnBrowseVendors)
        tileViewTransactions = findViewById(R.id.btnViewTransactions)

        rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
        tvNoTransactions = findViewById(R.id.tvNoTransactions)
        tvViewAllTransactions = findViewById(R.id.tvViewAllTransactions)

        rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        recentTransactionsAdapter = TransactionAdapter(emptyList())
        rvRecentTransactions.adapter = recentTransactionsAdapter

        tvTodayDate.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
        loadNotificationBadge()
    }

    private fun setupClicks() {
        tileMyBalance.setOnClickListener {
            startActivity(Intent(this, MyBalanceActivity::class.java))
        }

        tileShowCard.setOnClickListener {
            startActivity(Intent(this, QRCodeDisplayActivity::class.java))
        }

        tileBrowseVendors.setOnClickListener {
            startActivity(Intent(this, BrowseVendorsActivity::class.java))
        }

        tileViewTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        tvViewAllTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        btnNotifications.setOnClickListener {
            startActivity(openNotificationsIntent())
        }
        cardAlerts.setOnClickListener {
            startActivity(openNotificationsIntent())
        }
        btnLogout.setOnClickListener {
            LogoutHelper.confirmAndLogout(this, prefsManager)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavStudent)
        StudentBottomNav.setup(this, bottomNav, R.id.navHome)
    }

    private fun openNotificationsIntent(): Intent {
        return Intent(this, NotificationsActivity::class.java).apply {
            putExtra(NotificationsActivity.EXTRA_ROLE, NotificationsActivity.Role.STUDENT.name)
            putExtra(NotificationsActivity.EXTRA_STUDENT_ID, prefsManager.getStudentId())
        }
    }

    private fun loadNotificationBadge() {
        val unread = NotificationHelper.getStudentUnreadCount(this, prefsManager.getStudentId())
        if (unread > 0) {
            tvNotificationBadge.visibility = View.VISIBLE
            tvNotificationBadge.text = if (unread > 9) "9+" else unread.toString()
        } else {
            tvNotificationBadge.visibility = View.GONE
        }
    }

    private fun loadDashboardData() {
        val studentId = prefsManager.getStudentId()
        val savedName = prefsManager.getUserName()
        val defaultBalance = StudentRegistryHelper.getDefaultBalanceForStudent(this, studentId)
        val balance = prefsManager.getBalanceForStudent(studentId, defaultBalance).toDouble()

        val displayName = if (savedName.isBlank() || savedName == "User") {
            StudentRegistryHelper.getStudentById(this, studentId)?.studentName ?: "Student"
        } else {
            savedName
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        tvWelcome.text = "$greeting, $displayName"
        tvStatBalance.text = formatCurrency(balance)

        val studentTransactions = prefsManager.getAllTransactions().filter { it.studentId == studentId }

        val todaySpend = studentTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .filter { isToday(it.timestamp) }
            .sumOf { it.amount }
        tvStatTodaySpend.text = formatCurrency(todaySpend)

        val cardStatus = getSharedPreferences("CardPrefs", MODE_PRIVATE)
            .getString("cardStatus_$studentId", "Active") ?: "Active"
        tvStatCardStatus.text = cardStatus
        ivStatCardStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
            getColor(if (cardStatus.equals("Active", ignoreCase = true)) R.color.status_success else R.color.status_danger)
        )

        val alertCount = NotificationHelper.getStudentNotifications(this, studentId).size
        tvStatAlerts.text = alertCount.toString()

        loadRecentTransactions(studentId)
    }

    private fun loadRecentTransactions(studentId: String) {
        val recent = prefsManager.getAllTransactions()
            .filter { it.studentId == studentId }
            .sortedByDescending { it.timestamp }
            .take(5)

        recentTransactionsAdapter.updateList(recent)
        rvRecentTransactions.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
        tvNoTransactions.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
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
}
