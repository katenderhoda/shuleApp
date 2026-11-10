package com.example.shulecard.activities.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.adapters.PendingTopUpAdapter
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar

class PendingTopUpsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvNoPendingTopUps: View
    private lateinit var rvPendingTopUps: RecyclerView
    private lateinit var topUpAdapter: PendingTopUpAdapter

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_topups)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarPendingTopUps)
        tvNoPendingTopUps = findViewById(R.id.tvNoPendingTopUps)
        rvPendingTopUps = findViewById(R.id.rvPendingTopUps)

        toolbar.setNavigationOnClickListener { finish() }

        rvPendingTopUps.layoutManager = LinearLayoutManager(this)
        topUpAdapter = PendingTopUpAdapter(
            emptyList(),
            onApprove = { approveTopUp(it) },
            onReject = { confirmReject(it) }
        )
        rvPendingTopUps.adapter = topUpAdapter

        loadPendingTopUps()
    }

    override fun onResume() {
        super.onResume()
        loadPendingTopUps()
    }

    private fun loadPendingTopUps() {
        val pending = prefsManager.getAllTransactions()
            .filter { it.type.equals("topup", ignoreCase = true) && it.status.equals("pending", ignoreCase = true) }
            .sortedByDescending { it.timestamp }

        topUpAdapter.updateList(pending)
        tvNoPendingTopUps.visibility = if (pending.isEmpty()) View.VISIBLE else View.GONE
        rvPendingTopUps.visibility = if (pending.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * Re-reads the stored status, so a top-up that was already approved or rejected (a double
     * tap, or a second admin screen left open) is never credited or rejected a second time.
     */
    private fun isStillPending(transaction: Transaction): Boolean {
        val stored = prefsManager.getAllTransactions().firstOrNull { it.transactionId == transaction.transactionId }
        if (stored?.status.equals("pending", ignoreCase = true)) return true

        Toast.makeText(this, "This top-up has already been processed", Toast.LENGTH_SHORT).show()
        loadPendingTopUps()
        return false
    }

    private fun approveTopUp(transaction: Transaction) {
        if (!isStillPending(transaction)) return

        val defaultBalance = StudentRegistryHelper.getDefaultBalanceForStudent(this, transaction.studentId)
        val currentBalance = prefsManager.getBalanceForStudent(transaction.studentId, defaultBalance).toDouble()
        val newBalance = currentBalance + transaction.amount

        prefsManager.saveBalanceForStudent(transaction.studentId, newBalance.toFloat())
        prefsManager.updateTransactionStatus(transaction.transactionId, "completed")

        Toast.makeText(
            this,
            "Approved ${formatCurrency(transaction.amount)} for ${transaction.studentName}",
            Toast.LENGTH_SHORT
        ).show()

        loadPendingTopUps()
    }

    private fun confirmReject(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Reject Top-up")
            .setMessage(
                "Reject ${formatCurrency(transaction.amount)} requested for ${transaction.studentName}? " +
                    "The amount will not be added to their balance."
            )
            .setPositiveButton("Reject") { _, _ ->
                if (!isStillPending(transaction)) return@setPositiveButton
                prefsManager.updateTransactionStatus(transaction.transactionId, "rejected")
                Toast.makeText(this, "Top-up rejected", Toast.LENGTH_SHORT).show()
                loadPendingTopUps()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatCurrency(amount: Double): String {
        return "UGX ${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amount)}"
    }
}
