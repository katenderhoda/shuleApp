package com.example.shulecard.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.models.Transaction
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private var transactionList: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvVendor: TextView = itemView.findViewById(R.id.tvVendorName)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvTransactionDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvStatus: TextView = itemView.findViewById(R.id.tvTransactionStatus)
        val tvReference: TextView = itemView.findViewById(R.id.tvTransactionReference)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]

        holder.tvVendor.text = "${transaction.studentName} • ${transaction.vendor}"
        holder.tvDateTime.text = transaction.dateTime
        holder.tvStatus.text = transaction.status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        holder.tvReference.text = "Ref: ${transaction.transactionId} • ${transaction.type.uppercase(Locale.getDefault())}"

        val formattedAmount = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(transaction.amount)}"
        val isCompleted = transaction.status.equals("completed", ignoreCase = true)
        val isRejected = transaction.status.equals("rejected", ignoreCase = true)

        if (transaction.type.equals("topup", ignoreCase = true)) {
            holder.tvAmount.text = "+$formattedAmount"
            // Only show the "money landed" green once the top-up is actually completed —
            // a pending/rejected top-up hasn't touched the balance yet.
            holder.tvAmount.setTextColor(
                if (isCompleted) Color.parseColor("#43A047") else Color.parseColor("#9E9E9E")
            )
        } else {
            holder.tvAmount.text = "-$formattedAmount"
            holder.tvAmount.setTextColor(Color.parseColor("#E53935"))
        }

        holder.tvStatus.setTextColor(
            when {
                isCompleted -> Color.parseColor("#43A047")
                isRejected -> Color.parseColor("#E53935")
                else -> Color.parseColor("#FB8C00")
            }
        )
    }

    override fun getItemCount(): Int = transactionList.size

    fun updateList(newList: List<Transaction>) {
        transactionList = newList
        notifyDataSetChanged()
    }
}