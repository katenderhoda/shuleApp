package com.example.shulecard.activities.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shulecard.R
import com.example.shulecard.adapters.VendorMenuItemAdapter
import com.example.shulecard.models.OrderItem
import com.example.shulecard.utils.OrderRegistryHelper
import com.example.shulecard.utils.VendorItem
import com.example.shulecard.utils.VendorItemRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class VendorMenuActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvNoItems: View
    private lateinit var rvMenu: RecyclerView
    private lateinit var cardCartBar: MaterialCardView
    private lateinit var tvCartSummary: TextView
    private lateinit var tvCartTotal: TextView
    private lateinit var btnProceedToPay: View
    private lateinit var itemAdapter: VendorMenuItemAdapter

    private var vendorId: String = ""
    private var vendorName: String = "Vendor"

    /** itemId -> selected quantity. Reset whenever this screen becomes visible again. */
    private val quantities = mutableMapOf<String, Int>()
    private var menuItems: List<VendorItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_menu)

        vendorId = intent.getStringExtra("vendorId").orEmpty()
        vendorName = intent.getStringExtra("vendorName")?.ifBlank { null } ?: "Vendor"

        toolbar = findViewById(R.id.toolbarVendorMenu)
        toolbar.title = vendorName
        toolbar.setNavigationOnClickListener { finish() }

        tvNoItems = findViewById(R.id.tvNoMenuItems)
        rvMenu = findViewById(R.id.rvVendorMenu)
        cardCartBar = findViewById(R.id.cardCartBar)
        tvCartSummary = findViewById(R.id.tvCartSummary)
        tvCartTotal = findViewById(R.id.tvCartTotal)
        btnProceedToPay = findViewById(R.id.btnProceedToPay)

        rvMenu.layoutManager = LinearLayoutManager(this)
        itemAdapter = VendorMenuItemAdapter(emptyList(), quantities) { item, newQty ->
            if (newQty <= 0) quantities.remove(item.itemId) else quantities[item.itemId] = newQty
            itemAdapter.refreshQuantities()
            updateCartBar()
        }
        rvMenu.adapter = itemAdapter

        btnProceedToPay.setOnClickListener { proceedToPay() }

        loadMenu()
    }

    override fun onResume() {
        super.onResume()
        quantities.clear()
        loadMenu()
        updateCartBar()
    }

    private fun loadMenu() {
        menuItems = VendorItemRegistryHelper.getAvailableItemsForVendor(this, vendorId)
        itemAdapter.updateList(menuItems)

        tvNoItems.visibility = if (menuItems.isEmpty()) View.VISIBLE else View.GONE
        rvMenu.visibility = if (menuItems.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateCartBar() {
        val cartCount = quantities.values.sum()
        val total = menuItems.sumOf { (quantities[it.itemId] ?: 0) * it.price }

        cardCartBar.visibility = if (cartCount > 0) View.VISIBLE else View.GONE
        tvCartSummary.text = if (cartCount == 1) "1 item" else "$cartCount items"
        tvCartTotal.text = "UGX ${NumberFormat.getNumberInstance(Locale.US).format(total)}"
    }

    private fun proceedToPay() {
        val cartLines = menuItems.mapNotNull { item ->
            val qty = quantities[item.itemId] ?: 0
            if (qty <= 0) return@mapNotNull null
            OrderItem(itemId = item.itemId, name = item.name, price = item.price, quantity = qty)
        }

        if (cartLines.isEmpty()) {
            Toast.makeText(this, "Select at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        startActivity(
            Intent(this, PaymentConfirmationActivity::class.java).apply {
                putExtra("vendorId", vendorId)
                putExtra("vendor", vendorName)
                putExtra("cartItemsJson", OrderRegistryHelper.serializeItems(cartLines))
            }
        )
    }
}
