package com.example.shulecard.activities.vendor

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.models.Order
import com.example.shulecard.utils.OrderRegistryHelper
import com.example.shulecard.utils.PrefsManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Locale

class RedeemOrderActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etOrderCode: TextInputEditText
    private lateinit var btnLookupOrder: View
    private lateinit var cardOrderPreview: MaterialCardView
    private lateinit var tvOrderPreviewStudent: TextView
    private lateinit var tvOrderPreviewStatus: TextView
    private lateinit var containerOrderPreviewItems: LinearLayout
    private lateinit var tvOrderPreviewTotal: TextView
    private lateinit var btnIssueItems: View

    private var vendorId: String = ""
    private var loadedOrder: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redeem_order)

        vendorId = intent.getStringExtra("vendorId")?.ifBlank { null } ?: PrefsManager(this).getVendorId()

        toolbar = findViewById(R.id.toolbarRedeemOrder)
        etOrderCode = findViewById(R.id.etOrderCode)
        btnLookupOrder = findViewById(R.id.btnLookupOrder)
        cardOrderPreview = findViewById(R.id.cardOrderPreview)
        tvOrderPreviewStudent = findViewById(R.id.tvOrderPreviewStudent)
        tvOrderPreviewStatus = findViewById(R.id.tvOrderPreviewStatus)
        containerOrderPreviewItems = findViewById(R.id.containerOrderPreviewItems)
        tvOrderPreviewTotal = findViewById(R.id.tvOrderPreviewTotal)
        btnIssueItems = findViewById(R.id.btnIssueItems)

        toolbar.setNavigationOnClickListener { finish() }

        btnLookupOrder.setOnClickListener { lookupOrder() }
        btnIssueItems.setOnClickListener { issueItems() }
    }

    private fun lookupOrder() {
        val code = etOrderCode.text?.toString()?.trim().orEmpty()
        if (code.isEmpty()) {
            etOrderCode.error = "Enter an order code"
            etOrderCode.requestFocus()
            return
        }

        val order = OrderRegistryHelper.getOrderById(this, code)
        if (order == null) {
            cardOrderPreview.visibility = View.GONE
            showMessage("Order Not Found", "No order matched that code. Ask the student to show their order code again.")
            return
        }

        if (!order.vendorId.equals(vendorId, ignoreCase = true)) {
            cardOrderPreview.visibility = View.GONE
            showMessage("Wrong Vendor", "This order was placed with a different vendor.")
            return
        }

        loadedOrder = order
        renderOrderPreview(order)
    }

    private fun renderOrderPreview(order: Order) {
        cardOrderPreview.visibility = View.VISIBLE
        tvOrderPreviewStudent.text = "Student: ${order.studentName} (${order.studentId})"
        tvOrderPreviewTotal.text = "Total: ${formatCurrency(order.totalAmount)}"

        val isIssued = order.status == Order.STATUS_ISSUED
        tvOrderPreviewStatus.text = if (isIssued) "Status: Already Issued" else "Status: Pending Pickup"
        tvOrderPreviewStatus.setTextColor(
            getColor(if (isIssued) R.color.status_danger else R.color.status_warning)
        )
        btnIssueItems.isEnabled = !isIssued

        containerOrderPreviewItems.removeAllViews()
        order.items.forEach { item ->
            val line = TextView(this).apply {
                text = "${item.quantity}x ${item.name}  —  ${formatCurrency(item.lineTotal)}"
                textSize = 14f
                setTextColor(getColor(R.color.text_primary))
                gravity = Gravity.START
                setPadding(0, 8, 0, 8)
            }
            containerOrderPreviewItems.addView(line)
        }
    }

    private fun issueItems() {
        val order = loadedOrder ?: return

        val issued = OrderRegistryHelper.markIssued(this, order.orderId)
        if (!issued) {
            Toast.makeText(this, "This order was already issued", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(
            this,
            "Items issued to ${order.studentName}",
            Toast.LENGTH_LONG
        ).show()

        renderOrderPreview(OrderRegistryHelper.getOrderById(this, order.orderId) ?: return)
    }

    private fun showMessage(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatCurrency(amount: Double): String {
        return "UGX ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
    }
}
