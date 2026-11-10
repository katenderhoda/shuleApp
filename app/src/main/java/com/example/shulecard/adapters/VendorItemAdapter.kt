package com.example.shulecard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.utils.VendorItem
import java.text.NumberFormat
import java.util.Locale

class VendorItemAdapter(
    private var itemList: List<VendorItem>,
    private val onClick: (VendorItem) -> Unit
) : RecyclerView.Adapter<VendorItemAdapter.VendorItemViewHolder>() {

    inner class VendorItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        val tvDescription: TextView = itemView.findViewById(R.id.tvItemDescription)
        val tvUnavailable: TextView = itemView.findViewById(R.id.tvItemUnavailable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vendor_item, parent, false)
        return VendorItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: VendorItemViewHolder, position: Int) {
        val item = itemList[position]

        holder.tvName.text = item.name
        holder.tvPrice.text = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(item.price)}"

        if (item.description.isBlank()) {
            holder.tvDescription.visibility = View.GONE
        } else {
            holder.tvDescription.visibility = View.VISIBLE
            holder.tvDescription.text = item.description
        }

        holder.tvUnavailable.visibility = if (item.available) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = itemList.size

    fun updateList(newList: List<VendorItem>) {
        itemList = newList
        notifyDataSetChanged()
    }
}
