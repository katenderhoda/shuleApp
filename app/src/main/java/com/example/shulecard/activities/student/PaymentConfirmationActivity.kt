package com.example.shulecard.activities.student

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.models.Order
import com.example.shulecard.models.OrderItem
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.OrderRegistryHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentConfirmationActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvVendor: TextView
    private lateinit var tvItem: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvBalance: TextView
    private lateinit var etPin: EditText
    private lateinit var btnConfirmPayment: Button
    private lateinit var btnCancelPayment: Button

    private lateinit var prefsManager: PrefsManager
    private var paymentSubmitted = false

    private var vendorId: String = ""
    private var vendorName: String = ""
    private var cartItems: List<OrderItem> = emptyList()
    private var total: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_confirmation)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarPaymentConfirmation)
        toolbar.setNavigationOnClickListener { finish() }

        tvVendor = findViewById(R.id.tvVendor)
        tvItem = findViewById(R.id.tvItem)
        tvAmount = findViewById(R.id.tvAmount)
        tvBalance = findViewById(R.id.tvBalance)
        etPin = findViewById(R.id.etPin)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        btnCancelPayment = findViewById(R.id.btnCancelPayment)

        vendorId = intent.getStringExtra("vendorId").orEmpty()
        vendorName = intent.getStringExtra("vendor") ?: "Vendor"
        cartItems = OrderRegistryHelper.parseItems(intent.getStringExtra("cartItemsJson").orEmpty())
        total = cartItems.sumOf { it.lineTotal }

        if (cartItems.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadPaymentDetails()

        btnConfirmPayment.setOnClickListener {
            confirmPayment()
        }

        btnCancelPayment.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPaymentDetails()
    }

    private fun loadPaymentDetails() {
        val studentId = prefsManager.getStudentId()
        val defaultBalance = StudentRegistryHelper.getDefaultBalanceForStudent(this, studentId)
        val currentBalance = prefsManager.getBalanceForStudent(studentId, defaultBalance).toDouble()

        tvVendor.text = "Vendor: $vendorName"
        tvItem.text = "Items:\n" + cartItems.joinToString("\n") {
            "${it.quantity}x ${it.name} — ${formatCurrency(it.lineTotal)}"
        }
        tvAmount.text = "Total: ${formatCurrency(total)}"
        tvBalance.text = "ShuleCard Balance: ${formatCurrency(currentBalance)}"
    }

    private fun confirmPayment() {
        // A second tap can arrive before this screen finishes; never charge the same cart twice.
        if (paymentSubmitted) return

        val enteredPin = etPin.text.toString().trim()
        val studentId = prefsManager.getStudentId()

        val studentName = prefsManager.getUserName().ifBlank {
            StudentRegistryHelper.getStudentById(this, studentId)?.studentName ?: "Student"
        }

        val expectedPin = StudentRegistryHelper.getCardPinForStudent(this, studentId)

        val cardPrefs = getSharedPreferences("CardPrefs", MODE_PRIVATE)
        val cardStatus = cardPrefs.getString("cardStatus_$studentId", "Active") ?: "Active"

        if (cardStatus.equals("Frozen", ignoreCase = true)) {
            AlertDialog.Builder(this)
                .setTitle("Payment Blocked")
                .setMessage("This card is currently frozen. Ask the parent to unfreeze it first.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        if (cardStatus.equals("Blocked", ignoreCase = true)) {
            AlertDialog.Builder(this)
                .setTitle("Payment Blocked")
                .setMessage("This card has been blocked and cannot be used for payments.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val defaultBalance = StudentRegistryHelper.getDefaultBalanceForStudent(this, studentId)
        val currentBalance = prefsManager.getBalanceForStudent(studentId, defaultBalance).toDouble()

        if (enteredPin.isEmpty()) {
            Toast.makeText(this, "Enter your PIN", Toast.LENGTH_SHORT).show()
            return
        }

        if (enteredPin != expectedPin) {
            Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentBalance < total) {
            AlertDialog.Builder(this)
                .setTitle("Insufficient Balance")
                .setMessage(
                    "Your balance is ${formatCurrency(currentBalance)}.\n" +
                            "You need ${formatCurrency(total - currentBalance)} more."
                )
                .setPositiveButton("OK", null)
                .show()
            return
        }

        paymentSubmitted = true
        btnConfirmPayment.isEnabled = false

        val newBalance = currentBalance - total
        prefsManager.saveBalanceForStudent(studentId, newBalance.toFloat())

        val now = System.currentTimeMillis()

        val transaction = Transaction(
            transactionId = "TRX-$now",
            studentId = studentId,
            studentName = studentName,
            type = "payment",
            amount = total,
            vendor = vendorName,
            dateTime = getCurrentDateTime(),
            status = "completed",
            timestamp = now
        )

        prefsManager.addTransaction(transaction)

        val orderId = OrderRegistryHelper.generateOrderId()
        val order = Order(
            orderId = orderId,
            studentId = studentId,
            studentName = studentName,
            vendorId = vendorId,
            vendorName = vendorName,
            items = cartItems,
            totalAmount = total,
            transactionId = transaction.transactionId,
            timestamp = now,
            status = Order.STATUS_PENDING
        )
        OrderRegistryHelper.upsertOrder(this, order)

        startActivity(
            Intent(this, OrderQRDisplayActivity::class.java).apply {
                putExtra("orderId", orderId)
            }
        )
        finish()
    }

    private fun formatCurrency(amount: Double): String {
        return "UGX ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
}
