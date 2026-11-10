package com.example.shulecard.activities.admin

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.adapters.TransactionAdapter
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.PrefsManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import java.util.Calendar
import java.util.Locale

class TransactionLogsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvSummary: TextView
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: TransactionAdapter
    private lateinit var prefsManager: PrefsManager

    private var allTransactions: List<Transaction> = emptyList()
    private var currentQuery: String = ""
    private var currentFilter: FilterType = FilterType.ALL

    enum class FilterType {
        ALL, TODAY, WEEK, MONTH, TOPUP, PAYMENT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_logs)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbar)
        searchView = findViewById(R.id.searchView)
        chipGroup = findViewById(R.id.chipGroupFilters)
        rvTransactions = findViewById(R.id.rvTransactions)
        tvSummary = findViewById(R.id.tvSummary)
        tvEmpty = findViewById(R.id.tvEmpty)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        loadTransactions()
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(emptyList())
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter
    }

    private fun setupSearch() {
        searchView.clearFocus()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query?.trim().orEmpty()
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText?.trim().orEmpty()
                applyFilters()
                return true
            }
        })
    }

    private fun setupFilters() {
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipToday) -> FilterType.TODAY
                checkedIds.contains(R.id.chipWeek) -> FilterType.WEEK
                checkedIds.contains(R.id.chipMonth) -> FilterType.MONTH
                checkedIds.contains(R.id.chipTopup) -> FilterType.TOPUP
                checkedIds.contains(R.id.chipPayment) -> FilterType.PAYMENT
                else -> FilterType.ALL
            }
            applyFilters()
        }
    }

    private fun loadTransactions() {
        allTransactions = prefsManager.getAllTransactions()
            .sortedByDescending { it.timestamp }

        applyFilters()
    }

    private fun applyFilters() {
        val filtered = allTransactions.filter { transaction ->
            matchesSearch(transaction) && matchesFilter(transaction)
        }

        adapter.updateList(filtered)
        updateSummary(filtered)
        updateEmptyState(filtered)
    }

    private fun updateSummary(filtered: List<Transaction>) {
        val totalAmount = filtered.sumOf { it.amount }
        tvSummary.text = "Showing ${filtered.size} of ${allTransactions.size} transactions • Total UGX ${formatAmount(totalAmount)}"
    }

    private fun updateEmptyState(filtered: List<Transaction>) {
        if (filtered.isEmpty()) {
            rvTransactions.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvTransactions.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }

    private fun matchesSearch(transaction: Transaction): Boolean {
        if (currentQuery.isBlank()) return true

        val query = currentQuery.lowercase(Locale.getDefault())

        return transaction.studentName.lowercase(Locale.getDefault()).contains(query) ||
                transaction.studentId.lowercase(Locale.getDefault()).contains(query) ||
                transaction.vendor.lowercase(Locale.getDefault()).contains(query) ||
                transaction.transactionId.lowercase(Locale.getDefault()).contains(query) ||
                transaction.type.lowercase(Locale.getDefault()).contains(query) ||
                transaction.status.lowercase(Locale.getDefault()).contains(query)
    }

    private fun matchesFilter(transaction: Transaction): Boolean {
        return when (currentFilter) {
            FilterType.TODAY -> isToday(transaction.timestamp)
            FilterType.WEEK -> isThisWeek(transaction.timestamp)
            FilterType.MONTH -> isThisMonth(transaction.timestamp)
            FilterType.TOPUP -> transaction.type.equals("topup", ignoreCase = true)
            FilterType.PAYMENT -> transaction.type.equals("payment", ignoreCase = true)
            FilterType.ALL -> true
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

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.US, "%,.0f", amount)
    }
}