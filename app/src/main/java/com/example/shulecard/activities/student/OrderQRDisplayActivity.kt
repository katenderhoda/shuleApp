package com.example.shulecard.activities.student

import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.models.Order
import com.example.shulecard.utils.OrderRegistryHelper
import com.example.shulecard.utils.QrCodeGenerator
import com.google.android.material.appbar.MaterialToolbar
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class OrderQRDisplayActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivOrderQr: ImageView
    private lateinit var tvOrderCode: TextView
    private lateinit var tvOrderStudent: TextView
    private lateinit var tvOrderVendor: TextView
    private lateinit var containerOrderItems: LinearLayout
    private lateinit var tvOrderTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_qr_display)

        toolbar = findViewById(R.id.toolbarOrderQr)
        ivOrderQr = findViewById(R.id.ivOrderQr)
        tvOrderCode = findViewById(R.id.tvOrderCode)
        tvOrderStudent = findViewById(R.id.tvOrderStudent)
        tvOrderVendor = findViewById(R.id.tvOrderVendor)
        containerOrderItems = findViewById(R.id.containerOrderItems)
        tvOrderTotal = findViewById(R.id.tvOrderTotal)

        toolbar.setNavigationOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btnDoneOrderQr).setOnClickListener { finish() }

        val orderId = intent.getStringExtra("orderId").orEmpty()
        val order = OrderRegistryHelper.getOrderById(this, orderId)

        if (order == null) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        renderOrder(order)
    }

    private fun renderOrder(order: Order) {
        tvOrderCode.text = order.orderId
        tvOrderStudent.text = "Student: ${order.studentName} (${order.studentId})"
        tvOrderVendor.text = "Vendor: ${order.vendorName}"
        tvOrderTotal.text = "Total: ${formatCurrency(order.totalAmount)}"

        containerOrderItems.removeAllViews()
        order.items.forEach { item ->
            val line = TextView(this).apply {
                text = "${item.quantity}x ${item.name}  —  ${formatCurrency(item.lineTotal)}"
                textSize = 14f
                setTextColor(getColor(R.color.text_primary))
                gravity = Gravity.START
                setPadding(0, 8, 0, 8)
            }
            containerOrderItems.addView(line)
        }

        val qrBitmap = QrCodeGenerator.generate(buildQrPayload(order))
        if (qrBitmap != null) {
            ivOrderQr.setImageBitmap(qrBitmap)
        }
    }

    private fun buildQrPayload(order: Order): String {
        val itemsJson = JSONArray()
        order.items.forEach { item ->
            itemsJson.put(
                JSONObject().apply {
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("price", item.price)
                }
            )
        }

        return JSONObject().apply {
            put("orderId", order.orderId)
            put("studentId", order.studentId)
            put("studentName", order.studentName)
            put("vendorId", order.vendorId)
            put("vendorName", order.vendorName)
            put("items", itemsJson)
            put("totalAmount", order.totalAmount)
            put("timestamp", order.timestamp)
        }.toString()
    }

    private fun formatCurrency(amount: Double): String {
        return "UGX ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
    }
}
