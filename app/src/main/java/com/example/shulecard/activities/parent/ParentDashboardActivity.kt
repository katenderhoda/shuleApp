package com.example.shulecard.activities.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.activities.shared.NotificationsActivity
import com.example.shulecard.adapters.TransactionAdapter
import com.example.shulecard.utils.LinkChildResult
import com.example.shulecard.utils.LogoutHelper
import com.example.shulecard.utils.NotificationHelper
import com.example.shulecard.utils.ParentBottomNav
import com.example.shulecard.utils.ParentProfile
import com.example.shulecard.utils.ParentRegistryHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentProfile
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ParentDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvWelcomeSubtitle: TextView
    private lateinit var tvTodayDate: TextView
    private lateinit var btnNotifications: FrameLayout
    private lateinit var tvNotificationBadge: TextView
    private lateinit var btnLogout: ImageView
    private lateinit var spinnerActiveChild: Spinner

    private lateinit var tvStatBalance: TextView
    private lateinit var tvStatMonthlySpend: TextView
    private lateinit var ivCardStatusIcon: ImageView
    private lateinit var tvStatCardStatus: TextView
    private lateinit var tvStatAlerts: TextView
    private lateinit var tvStatAlertsStatus: TextView
    private lateinit var cardAlerts: View

    private lateinit var tileLoadMoney: View
    private lateinit var tileCardManagement: View
    private lateinit var tileTransactions: View
    private lateinit var tileLinkChild: View

    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var tvViewAllTransactions: TextView
    private lateinit var recentTransactionsAdapter: TransactionAdapter

    private lateinit var prefsManager: PrefsManager
    private var linkedChildren: List<StudentProfile> = emptyList()
    private var isInitializingSpinner = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        prefsManager = PrefsManager(this)

        tvWelcome = findViewById(R.id.tvWelcomeParent)
        tvWelcomeSubtitle = findViewById(R.id.tvWelcomeSubtitle)
        tvTodayDate = findViewById(R.id.tvTodayDate)
        btnNotifications = findViewById(R.id.btnNotifications)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)
        btnLogout = findViewById(R.id.btnLogout)
        spinnerActiveChild = findViewById(R.id.spinnerActiveChild)

        tvStatBalance = findViewById(R.id.tvStatBalance)
        tvStatMonthlySpend = findViewById(R.id.tvStatMonthlySpend)
        ivCardStatusIcon = findViewById(R.id.ivCardStatusIcon)
        tvStatCardStatus = findViewById(R.id.tvStatCardStatus)
        tvStatAlerts = findViewById(R.id.tvStatAlerts)
        tvStatAlertsStatus = findViewById(R.id.tvStatAlertsStatus)
        cardAlerts = findViewById(R.id.cardAlerts)

        tileLoadMoney = findViewById(R.id.btnTestLoadMoney)
        tileCardManagement = findViewById(R.id.btnCardManagement)
        tileTransactions = findViewById(R.id.btnParentTransactions)
        tileLinkChild = findViewById(R.id.btnLinkChild)

        rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
        tvNoTransactions = findViewById(R.id.tvNoTransactions)
        tvViewAllTransactions = findViewById(R.id.tvViewAllTransactions)

        rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        recentTransactionsAdapter = TransactionAdapter(emptyList())
        rvRecentTransactions.adapter = recentTransactionsAdapter

        tvTodayDate.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

        setupClicks()
        setupChildSpinner()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavParent)
        ParentBottomNav.setup(this, bottomNav, R.id.navHome)
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun setupClicks() {
        tileLoadMoney.setOnClickListener {
            startActivity(Intent(this, LoadMoneyActivity::class.java))
        }

        tileTransactions.setOnClickListener {
            startActivity(Intent(this, ParentTransactionHistoryActivity::class.java))
        }

        tileCardManagement.setOnClickListener {
            startActivity(Intent(this, CardManagementActivity::class.java))
        }

        tileLinkChild.setOnClickListener {
            promptLinkChild()
        }

        tvViewAllTransactions.setOnClickListener {
            startActivity(Intent(this, ParentTransactionHistoryActivity::class.java))
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
    }

    private fun openNotificationsIntent(): Intent {
        val email = prefsManager.getUserEmail()
        val studentIds = linkedChildren.joinToString(",") { it.studentId }
        return Intent(this, NotificationsActivity::class.java).apply {
            putExtra(NotificationsActivity.EXTRA_ROLE, NotificationsActivity.Role.PARENT.name)
            putExtra(NotificationsActivity.EXTRA_PARENT_EMAIL, email)
            putExtra(NotificationsActivity.EXTRA_STUDENT_IDS, studentIds)
        }
    }

    private fun setupChildSpinner() {
        spinnerActiveChild.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializingSpinner) return
                val selected = linkedChildren.getOrNull(position) ?: return

                val email = prefsManager.getUserEmail()
                if (email.isNotBlank()) {
                    ParentRegistryHelper.setActiveStudentId(this@ParentDashboardActivity, email, selected.studentId)
                }
                prefsManager.saveStudentId(selected.studentId)
                if (!prefsManager.hasBalanceForStudent(selected.studentId)) {
                    prefsManager.saveBalanceForStudent(selected.studentId, selected.initialBalance)
                }
                renderChildInfo(selected)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun promptLinkChild() {
        val email = prefsManager.getUserEmail()
        if (email.isBlank()) {
            Toast.makeText(this, "Could not identify your account. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        val input = EditText(this).apply {
            hint = "Student ID"
        }

        AlertDialog.Builder(this)
            .setTitle("Link Another Child")
            .setMessage("Enter the Student ID the school gave your other child.")
            .setView(input)
            .setPositiveButton("Link") { _, _ ->
                val studentId = input.text.toString().trim()
                if (studentId.isEmpty()) {
                    Toast.makeText(this, "Enter a student ID", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                when (ParentRegistryHelper.linkStudent(this, email, studentId)) {
                    LinkChildResult.SUCCESS -> {
                        ParentRegistryHelper.setActiveStudentId(this, email, studentId)
                        Toast.makeText(this, "Child linked successfully", Toast.LENGTH_SHORT).show()
                        loadDashboardData()
                    }
                    LinkChildResult.ALREADY_LINKED ->
                        Toast.makeText(this, "That child is already linked to your account", Toast.LENGTH_LONG).show()
                    LinkChildResult.STUDENT_NOT_FOUND ->
                        Toast.makeText(this, "No student found with that ID. Please check with the school administrator.", Toast.LENGTH_LONG).show()
                    LinkChildResult.PARENT_NOT_FOUND ->
                        Toast.makeText(this, "Could not identify your account. Please log in again.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadDashboardData() {
        val parentName = prefsManager.getUserName().ifBlank { "Parent" }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        tvWelcome.text = "$greeting, $parentName"

        val email = prefsManager.getUserEmail()
        val parent: ParentProfile? = if (email.isNotBlank()) {
            ParentRegistryHelper.getParentByEmail(this, email)
        } else null

        linkedChildren = parent?.studentIds
            ?.mapNotNull { StudentRegistryHelper.getStudentById(this, it) }
            ?: emptyList()

        loadNotificationBadge()

        if (linkedChildren.isEmpty()) {
            spinnerActiveChild.visibility = View.GONE
            val target = StudentRegistryHelper.getTargetStudent(this)
            tvWelcomeSubtitle.text = "Here's how ${target.studentName}'s wallet is doing"
            renderChildInfo(target)
            return
        }

        spinnerActiveChild.visibility = if (linkedChildren.size > 1) View.VISIBLE else View.GONE

        val options = linkedChildren.map { "${it.studentName} (${it.studentId})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val activeStudentId = parent?.let { ParentRegistryHelper.getActiveStudentId(this, it) }
        val activeIndex = linkedChildren.indexOfFirst { it.studentId.equals(activeStudentId, ignoreCase = true) }
            .let { if (it >= 0) it else 0 }

        isInitializingSpinner = true
        spinnerActiveChild.adapter = adapter
        spinnerActiveChild.setSelection(activeIndex)
        isInitializingSpinner = false

        val activeChild = linkedChildren[activeIndex]
        prefsManager.saveStudentId(activeChild.studentId)
        if (!prefsManager.hasBalanceForStudent(activeChild.studentId)) {
            prefsManager.saveBalanceForStudent(activeChild.studentId, activeChild.initialBalance)
        }
        tvWelcomeSubtitle.text = "Here's how ${activeChild.studentName}'s wallet is doing"
        renderChildInfo(activeChild)
    }

    private fun loadNotificationBadge() {
        val email = prefsManager.getUserEmail()
        val studentIds = linkedChildren.map { it.studentId }
        val unread = NotificationHelper.getParentUnreadCount(this, email, studentIds)
        if (unread > 0) {
            tvNotificationBadge.visibility = View.VISIBLE
            tvNotificationBadge.text = if (unread > 9) "9+" else unread.toString()
        } else {
            tvNotificationBadge.visibility = View.GONE
        }
    }

    private fun renderChildInfo(targetStudent: StudentProfile) {
        val balance = prefsManager.getBalanceForStudent(
            targetStudent.studentId,
            targetStudent.initialBalance
        ).toDouble()

        val studentTransactions = prefsManager.getAllTransactions()
            .filter { it.studentId == targetStudent.studentId }

        val monthlySpend = studentTransactions
            .filter { it.type.equals("payment", ignoreCase = true) }
            .filter { isThisMonth(it.timestamp) }
            .sumOf { it.amount }

        val cardStatus = getSharedPreferences("CardPrefs", MODE_PRIVATE)
            .getString("cardStatus_${targetStudent.studentId}", "Active") ?: "Active"

        tvStatBalance.text = formatCurrency(balance)
        tvStatMonthlySpend.text = formatCurrency(monthlySpend)
        tvStatCardStatus.text = cardStatus
        val cardColor = if (cardStatus.equals("Active", ignoreCase = true)) {
            getColor(R.color.status_success)
        } else {
            getColor(R.color.status_danger)
        }
        ivCardStatusIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(cardColor)

        val alerts = NotificationHelper.getParentNotifications(
            this,
            prefsManager.getUserEmail(),
            listOf(targetStudent.studentId)
        )
        tvStatAlerts.text = alerts.size.toString()
        if (alerts.isNotEmpty()) {
            tvStatAlertsStatus.text = "Needs attention"
            tvStatAlertsStatus.setTextColor(getColor(R.color.status_warning))
        } else {
            tvStatAlertsStatus.text = "You're all caught up."
            tvStatAlertsStatus.setTextColor(getColor(R.color.status_success))
        }

        val recent = studentTransactions.sortedByDescending { it.timestamp }.take(5)
        recentTransactionsAdapter.updateList(recent)
        rvRecentTransactions.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
        tvNoTransactions.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun formatCurrency(amount: Double): String {
        return "UGX ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
    }

    private fun isThisMonth(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val tx = Calendar.getInstance().apply { timeInMillis = timestamp }

        return now.get(Calendar.YEAR) == tx.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == tx.get(Calendar.MONTH)
    }
}
