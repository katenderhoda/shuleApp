package com.example.shulecard.activities.vendor

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.adapters.TransactionAdapter
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.VendorBottomNav
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class VendorTransactionHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var prefsManager: PrefsManager

    private var vendorId: String = ""
    private var vendorName: String = "Vendor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_transaction_history)

        prefsManager = PrefsManager(this)
        vendorId = intent.getStringExtra("vendorId")?.ifBlank { null } ?: prefsManager.getVendorId()
        vendorName = intent.getStringExtra("vendorName")?.ifBlank { null }
            ?: prefsManager.getUserName().ifBlank { "Vendor" }

        toolbar = findViewById(R.id.toolbarVendorTransactions)
        rvTransactions = findViewById(R.id.rvVendorTransactions)
        tvEmptyState = findViewById(R.id.tvVendorEmptyState)

        toolbar.setNavigationOnClickListener { finish() }
        rvTransactions.layoutManager = LinearLayoutManager(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavVendorTransactions)
        VendorBottomNav.setup(this, bottomNav, R.id.navTransactions, vendorId, vendorName)
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun loadTransactions() {
        val transactions = prefsManager.getAllTransactions()
            .filter { it.vendor == vendorName }
            .sortedByDescending { it.timestamp }

        rvTransactions.adapter = TransactionAdapter(transactions)
        rvTransactions.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        tvEmptyState.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
    }
}
