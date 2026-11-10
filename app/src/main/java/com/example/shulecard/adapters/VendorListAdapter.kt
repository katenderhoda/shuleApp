package com.example.shulecard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R

data class VendorListEntry(
    val vendorId: String,
    val vendorName: String,
    val category: String,
    val location: String
)

class VendorListAdapter(
    private var vendorList: List<VendorListEntry>,
    private val onClick: (VendorListEntry) -> Unit
) : RecyclerView.Adapter<VendorListAdapter.VendorViewHolder>() {

    inner class VendorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvVendorCardName)
        val tvDetails: TextView = itemView.findViewById(R.id.tvVendorCardDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vendor_card, parent, false)
        return VendorViewHolder(view)
    }

    override fun onBindViewHolder(holder: VendorViewHolder, position: Int) {
        val vendor = vendorList[position]
        holder.tvName.text = vendor.vendorName

        val details = listOf(vendor.category, vendor.location).filter { it.isNotBlank() }
        holder.tvDetails.text = if (details.isEmpty()) "" else details.joinToString(" • ")
        holder.tvDetails.visibility = if (details.isEmpty()) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onClick(vendor) }
    }

    override fun getItemCount(): Int = vendorList.size

    fun updateList(newList: List<VendorListEntry>) {
        vendorList = newList
        notifyDataSetChanged()
    }
}
