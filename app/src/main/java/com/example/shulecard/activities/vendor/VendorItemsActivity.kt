package com.example.shulecard.activities.vendor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.adapters.VendorItemAdapter
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.VendorItem
import com.example.shulecard.utils.VendorItemRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class VendorItemsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvNoItems: View
    private lateinit var rvVendorItems: RecyclerView
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var itemAdapter: VendorItemAdapter

    private var vendorId: String = ""
    private var vendorName: String = "Vendor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_items)

        val prefsManager = PrefsManager(this)
        vendorId = intent.getStringExtra("vendorId")?.ifBlank { null } ?: prefsManager.getVendorId()
        vendorName = intent.getStringExtra("vendorName")?.ifBlank { null }
            ?: prefsManager.getUserName().ifBlank { "Vendor" }

        toolbar = findViewById(R.id.toolbarVendorItems)
        tvNoItems = findViewById(R.id.tvNoItems)
        rvVendorItems = findViewById(R.id.rvVendorItems)
        fabAddItem = findViewById(R.id.fabAddItem)

        toolbar.setNavigationOnClickListener { finish() }

        rvVendorItems.layoutManager = LinearLayoutManager(this)
        itemAdapter = VendorItemAdapter(emptyList()) { item -> showItemOptions(item) }
        rvVendorItems.adapter = itemAdapter

        fabAddItem.setOnClickListener { showItemForm(null) }

        loadItems()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun loadItems() {
        val items = VendorItemRegistryHelper.getItemsForVendor(this, vendorId)
        itemAdapter.updateList(items)

        tvNoItems.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        rvVendorItems.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showItemOptions(item: VendorItem) {
        val options = arrayOf("Edit Item", "Delete Item")

        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showItemForm(item)
                    1 -> confirmDeleteItem(item)
                }
            }
            .show()
    }

    private fun showItemForm(existing: VendorItem?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_vendor_item_form, null)
        val etName = view.findViewById<TextInputEditText>(R.id.etItemName)
        val etPrice = view.findViewById<TextInputEditText>(R.id.etItemPrice)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etItemDescription)
        val cbAvailable = view.findViewById<CheckBox>(R.id.cbItemAvailable)

        if (existing != null) {
            etName.setText(existing.name)
            etPrice.setText(existing.price.toString())
            etDescription.setText(existing.description)
            cbAvailable.isChecked = existing.available
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Item" else "Edit Item")
            .setView(view)
            .setPositiveButton(if (existing == null) "Add" else "Save") { _, _ ->
                val name = etName.text?.toString()?.trim().orEmpty()
                val priceText = etPrice.text?.toString()?.trim().orEmpty()
                val description = etDescription.text?.toString()?.trim().orEmpty()
                val price = priceText.toDoubleOrNull()

                if (name.isEmpty()) {
                    Toast.makeText(this, "Enter an item name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (price == null || price <= 0) {
                    Toast.makeText(this, "Enter a valid price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val item = VendorItem(
                    itemId = existing?.itemId ?: VendorItemRegistryHelper.generateItemId(),
                    vendorId = vendorId,
                    name = name,
                    price = price,
                    description = description,
                    available = cbAvailable.isChecked,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )

                VendorItemRegistryHelper.upsertItem(this, item)
                loadItems()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteItem(item: VendorItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete \"${item.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                VendorItemRegistryHelper.deleteItem(this, item.itemId)
                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                loadItems()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
