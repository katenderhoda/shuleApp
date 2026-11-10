package com.example.shulecard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.utils.VendorItem
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

/**
 * Shows a vendor's available items with a quantity stepper so a student can build a
 * multi-item cart. Quantities are kept by the caller (keyed by itemId) — this adapter just
 * renders them and reports +/- taps back via [onQuantityChanged].
 */
class VendorMenuItemAdapter(
    private var itemList: List<VendorItem>,
    private val quantities: Map<String, Int>,
    private val onQuantityChanged: (VendorItem, Int) -> Unit
) : RecyclerView.Adapter<VendorMenuItemAdapter.MenuItemViewHolder>() {

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvMenuItemName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvMenuItemPrice)
        val tvDescription: TextView = itemView.findViewById(R.id.tvMenuItemDescription)
        val tvQty: TextView = itemView.findViewById(R.id.tvMenuItemQty)
        val btnDecrease: MaterialButton = itemView.findViewById(R.id.btnDecreaseQty)
        val btnIncrease: MaterialButton = itemView.findViewById(R.id.btnIncreaseQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vendor_menu_item, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val item = itemList[position]
        val qty = quantities[item.itemId] ?: 0

        holder.tvName.text = item.name
        holder.tvPrice.text = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(item.price)}"
        holder.tvQty.text = qty.toString()

        if (item.description.isBlank()) {
            holder.tvDescription.visibility = View.GONE
        } else {
            holder.tvDescription.visibility = View.VISIBLE
            holder.tvDescription.text = item.description
        }

        holder.btnDecrease.isEnabled = qty > 0
        holder.btnDecrease.setOnClickListener {
            if (qty > 0) onQuantityChanged(item, qty - 1)
        }
        holder.btnIncrease.setOnClickListener {
            onQuantityChanged(item, qty + 1)
        }
    }

    override fun getItemCount(): Int = itemList.size

    fun updateList(newList: List<VendorItem>) {
        itemList = newList
        notifyDataSetChanged()
    }

    fun refreshQuantities() {
        notifyDataSetChanged()
    }
}
