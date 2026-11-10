package com.example.shulecard.activities.parent

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.adapters.TransactionAdapter
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.ParentBottomNav
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class ParentTransactionHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnAll: Button
    private lateinit var btnPayments: Button
    private lateinit var btnTopups: Button

    private lateinit var prefsManager: PrefsManager
    private lateinit var adapter: TransactionAdapter

    private var allTransactions: List<Transaction> = emptyList()

    private enum class FilterType {
        ALL, PAYMENTS, TOPUPS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_transaction_history)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarParentTransactionHistory)
        toolbar.setNavigationOnClickListener { finish() }

        rvTransactions = findViewById(R.id.rvParentTransactions)
        tvEmptyState = findViewById(R.id.tvParentEmptyState)
        btnAll = findViewById(R.id.btnParentAll)
        btnPayments = findViewById(R.id.btnParentPayments)
        btnTopups = findViewById(R.id.btnParentTopups)

        rvTransactions.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(emptyList())
        rvTransactions.adapter = adapter

        btnAll.setOnClickListener {
            applyFilter(FilterType.ALL)
        }

        btnPayments.setOnClickListener {
            applyFilter(FilterType.PAYMENTS)
        }

        btnTopups.setOnClickListener {
            applyFilter(FilterType.TOPUPS)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavParentTransactions)
        ParentBottomNav.setup(this, bottomNav, R.id.navTransactions)
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
        applyFilter(FilterType.ALL)
    }

    private fun loadTransactions() {
        val targetStudent = StudentRegistryHelper.getTargetStudent(this)

        allTransactions = prefsManager.getAllTransactions()
            .filter { it.studentId == targetStudent.studentId }
            .sortedByDescending { it.timestamp }
    }

    private fun applyFilter(filterType: FilterType) {
        val filteredList = when (filterType) {
            FilterType.ALL -> allTransactions
            FilterType.PAYMENTS -> allTransactions.filter {
                it.type.equals("payment", ignoreCase = true)
            }
            FilterType.TOPUPS -> allTransactions.filter {
                it.type.equals("topup", ignoreCase = true)
            }
        }

        adapter.updateList(filteredList)
        updateEmptyState(filteredList)
        updateFilterButtons(filterType)
    }

    private fun updateEmptyState(list: List<Transaction>) {
        if (list.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvTransactions.visibility = View.GONE
            tvEmptyState.text = "No transactions found"
        } else {
            tvEmptyState.visibility = View.GONE
            rvTransactions.visibility = View.VISIBLE
        }
    }

    private fun updateFilterButtons(selected: FilterType) {
        resetButtonStyle(btnAll)
        resetButtonStyle(btnPayments)
        resetButtonStyle(btnTopups)

        when (selected) {
            FilterType.ALL -> setSelectedButtonStyle(btnAll)
            FilterType.PAYMENTS -> setSelectedButtonStyle(btnPayments)
            FilterType.TOPUPS -> setSelectedButtonStyle(btnTopups)
        }
    }

    private fun setSelectedButtonStyle(button: Button) {
        button.setBackgroundColor(Color.parseColor("#1E88E5"))
        button.setTextColor(Color.WHITE)
    }

    private fun resetButtonStyle(button: Button) {
        button.setBackgroundColor(Color.parseColor("#E0E0E0"))
        button.setTextColor(Color.parseColor("#212121"))
    }
}