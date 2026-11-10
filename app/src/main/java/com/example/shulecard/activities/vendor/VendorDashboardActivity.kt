package com.example.shulecard.activities.vendor

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
import com.example.shulecard.utils.LogoutHelper
import com.example.shulecard.utils.NotificationHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.VendorBottomNav
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class VendorDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvTodayDate: TextView
    private lateinit var btnNotifications: FrameLayout
    private lateinit var tvNotificationBadge: TextView
    private lateinit var btnLogout: ImageView

    private lateinit var tvStatTodaySales: TextView
    private lateinit var tvStatTodayTxnCount: TextView
    private lateinit var tvStatMonthSales: TextView
    private lateinit var tvStatAlerts: TextView
    private lateinit var tvStatAlertsStatus: TextView
    private lateinit var cardAlerts: View

    private lateinit var tileScan: View
    private lateinit var tileTransactionHistory: View
    private lateinit var tileMyItems: View
    private lateinit var tileRedeemOrder: View

    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var tvViewAllTransactions: TextView
    private lateinit var recentTransactionsAdapter: TransactionAdapter

    private lateinit var prefsManager: PrefsManager
    private var vendorId: String = ""
    private var vendorName: String = "Vendor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_dashboard)

        prefsManager = PrefsManager(this)

        vendorId = intent.getStringExtra("vendorId")?.ifBlank { null } ?: prefsManager.getVendorId()
        vendorName = intent.getStringExtra("vendorName")?.ifBlank { null }
            ?: prefsManager.getUserName().ifBlank { "Vendor" }
        prefsManager.saveVendorId(vendorId)

        tvWelcome = findViewById(R.id.tvWelcomeVendor)
        tvTodayDate = findViewById(R.id.tvTodayDate)
        btnNotifications = findViewById(R.id.btnNotifications)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)
        btnLogout = findViewById(R.id.btnLogout)

        tvStatTodaySales = findViewById(R.id.tvStatTodaySales)
        tvStatTodayTxnCount = findViewById(R.id.tvStatTodayTxnCount)
        tvStatMonthSales = findViewById(R.id.tvStatMonthSales)
        tvStatAlerts = findViewById(R.id.tvStatAlerts)
        tvStatAlertsStatus = findViewById(R.id.tvStatAlertsStatus)
        cardAlerts = findViewById(R.id.cardAlerts)

        tileScan = findViewById(R.id.tileScan)
        tileTransactionHistory = findViewById(R.id.tileTransactionHistory)
        tileMyItems = findViewById(R.id.tileMyItems)
        tileRedeemOrder = findViewById(R.id.tileRedeemOrder)

        rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
        tvNoTransactions = findViewById(R.id.tvNoTransactions)
        tvViewAllTransactions = findViewById(R.id.tvViewAllTransactions)

        rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        recentTransactionsAdapter = TransactionAdapter(emptyList())
        rvRecentTransactions.adapter = recentTransactionsAdapter

        tvTodayDate.text =
            java.text.SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(java.util.Date())

        btnNotifications.setOnClickListener { startActivity(openNotificationsIntent()) }
        cardAlerts.setOnClickListener { startActivity(openNotificationsIntent()) }
        btnLogout.setOnClickListener { LogoutHelper.confirmAndLogout(this, prefsManager) }

        tileScan.setOnClickListener { startActivity(vendorIntent(ScanPaymentActivity::class.java)) }
        tileTransactionHistory.setOnClickListener { startActivity(vendorIntent(VendorTransactionHistoryActivity::class.java)) }
        tileMyItems.setOnClickListener { startActivity(vendorIntent(VendorItemsActivity::class.java)) }
        tileRedeemOrder.setOnClickListener { startActivity(vendorIntent(RedeemOrderActivity::class.java)) }
        tvViewAllTransactions.setOnClickListener { startActivity(vendorIntent(VendorTransactionHistoryActivity::class.java)) }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavVendor)
        VendorBottomNav.setup(this, bottomNav, R.id.navHome, vendorId, vendorName)
    }

    override fun onResume() {
        super.onResume()
        loadWelcome()
        loadStats()
        loadRecentTransactions()
        loadNotificationBadge()
    }

    private fun vendorIntent(target: Class<*>): Intent {
        return Intent(this, target).apply {
            putExtra("vendorId", vendorId)
            putExtra("vendorName", vendorName)
        }
    }

    private fun openNotificationsIntent(): Intent {
        return Intent(this, NotificationsActivity::class.java).apply {
            putExtra(NotificationsActivity.EXTRA_ROLE, NotificationsActivity.Role.VENDOR.name)
            putExtra(NotificationsActivity.EXTRA_VENDOR_ID, vendorId)
            putExtra(NotificationsActivity.EXTRA_VENDOR_NAME, vendorName)
        }
    }

    private fun loadWelcome() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        tvWelcome.text = "$greeting, $vendorName"
    }

    private fun loadNotificationBadge() {
        val unread = NotificationHelper.getVendorUnreadCount(this, vendorId, vendorName)
        if (unread > 0) {
            tvNotificationBadge.visibility = View.VISIBLE
            tvNotificationBadge.text = if (unread > 9) "9+" else unread.toString()
        } else {
            tvNotificationBadge.visibility = View.GONE
        }
    }

    private fun loadStats() {
        val myTransactions = prefsManager.getAllTransactions()
            .filter { it.vendor == vendorName }
            .filter { it.type.equals("payment", ignoreCase = true) }

        val todaysTransactions = myTransactions.filter { isToday(it.timestamp) }
        val monthsTransactions = myTransactions.filter { isThisMonth(it.timestamp) }

        tvStatTodaySales.text = "UGX ${formatNumber(todaysTransactions.sumOf { it.amount }.toInt())}"
        tvStatTodayTxnCount.text = todaysTransactions.size.toString()
        tvStatMonthSales.text = "UGX ${formatNumber(monthsTransactions.sumOf { it.amount }.toInt())}"

        val alerts = NotificationHelper.getVendorNotifications(this, vendorId, vendorName)
        tvStatAlerts.text = alerts.size.toString()
        if (alerts.isNotEmpty()) {
            tvStatAlertsStatus.text = "Needs attention"
            tvStatAlertsStatus.setTextColor(getColor(R.color.status_warning))
        } else {
            tvStatAlertsStatus.text = "You're all caught up."
            tvStatAlertsStatus.setTextColor(getColor(R.color.status_success))
        }
    }

    private fun loadRecentTransactions() {
        val recent = prefsManager.getAllTransactions()
            .filter { it.vendor == vendorName }
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

    private fun isThisMonth(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val tx = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.YEAR) == tx.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == tx.get(Calendar.MONTH)
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }
}
