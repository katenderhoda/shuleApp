package com.example.shulecard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.models.Transaction
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class PendingTopUpAdapter(
    private var topUpList: List<Transaction>,
    private val onApprove: (Transaction) -> Unit,
    private val onReject: (Transaction) -> Unit
) : RecyclerView.Adapter<PendingTopUpAdapter.TopUpViewHolder>() {

    inner class TopUpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudent: TextView = itemView.findViewById(R.id.tvTopUpStudent)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTopUpAmount)
        val tvMethod: TextView = itemView.findViewById(R.id.tvTopUpMethod)
        val tvDate: TextView = itemView.findViewById(R.id.tvTopUpDate)
        val btnApprove: MaterialButton = itemView.findViewById(R.id.btnApproveTopUp)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btnRejectTopUp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopUpViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_topup, parent, false)
        return TopUpViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopUpViewHolder, position: Int) {
        val topUp = topUpList[position]

        holder.tvStudent.text = "${topUp.studentName} (${topUp.studentId})"
        holder.tvAmount.text = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(topUp.amount)}"
        holder.tvMethod.text = topUp.vendor
        holder.tvDate.text = topUp.dateTime

        holder.btnApprove.setOnClickListener { onApprove(topUp) }
        holder.btnReject.setOnClickListener { onReject(topUp) }
    }

    override fun getItemCount(): Int = topUpList.size

    fun updateList(newList: List<Transaction>) {
        topUpList = newList
        notifyDataSetChanged()
    }
}
