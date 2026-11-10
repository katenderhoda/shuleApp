package com.example.shulecard.activities.parent

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shulecard.R
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import com.google.android.material.appbar.MaterialToolbar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoadMoneyActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvChildName: TextView
    private lateinit var etAmount: EditText
    private lateinit var rgPaymentMethod: RadioGroup
    private lateinit var rbMobileMoney: RadioButton
    private lateinit var rbBankCard: RadioButton
    private lateinit var rbBankTransfer: RadioButton
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnContinue: Button

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_money)

        prefsManager = PrefsManager(this)

        toolbar = findViewById(R.id.toolbarLoadMoney)
        toolbar.setNavigationOnClickListener { finish() }

        tvChildName = findViewById(R.id.tvChildName)
        etAmount = findViewById(R.id.etAmount)
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod)
        rbMobileMoney = findViewById(R.id.rbMobileMoney)
        rbBankCard = findViewById(R.id.rbBankCard)
        rbBankTransfer = findViewById(R.id.rbBankTransfer)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnContinue = findViewById(R.id.btnContinueLoad)

        loadStudentInfo()
        setupPaymentMethodVisibility()

        btnContinue.setOnClickListener {
            processTopUp()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStudentInfo()
    }

    private fun loadStudentInfo() {
        val targetStudent = StudentRegistryHelper.getTargetStudent(this)
        tvChildName.text = "Child: ${targetStudent.studentName} (${targetStudent.studentId})"
    }

    private fun setupPaymentMethodVisibility() {
        rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            etPhoneNumber.visibility =
                if (checkedId == R.id.rbMobileMoney) android.view.View.VISIBLE
                else android.view.View.GONE
        }
    }

    private fun processTopUp() {
        val amountText = etAmount.text.toString().trim()
        val phone = etPhoneNumber.text.toString().trim()

        if (amountText.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val paymentMethod = when (rgPaymentMethod.checkedRadioButtonId) {
            R.id.rbMobileMoney -> "Mobile Money"
            R.id.rbBankCard -> "Bank Card"
            R.id.rbBankTransfer -> "Bank Transfer"
            else -> ""
        }

        if (paymentMethod.isEmpty()) {
            Toast.makeText(this, "Select payment method", Toast.LENGTH_SHORT).show()
            return
        }

        if (paymentMethod == "Mobile Money" && phone.isEmpty()) {
            Toast.makeText(this, "Enter mobile money phone number", Toast.LENGTH_SHORT).show()
            return
        }

        val targetStudent = StudentRegistryHelper.getTargetStudent(this)

        AlertDialog.Builder(this)
            .setTitle("Confirm Top-up")
            .setMessage(
                "Load ${formatCurrency(amount)} to ${targetStudent.studentName} using $paymentMethod?"
            )
            .setPositiveButton("Confirm") { _, _ ->
                completeTopUp(amount, paymentMethod, targetStudent.studentId, targetStudent.studentName, targetStudent.initialBalance)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeTopUp(
        amount: Double,
        paymentMethod: String,
        studentId: String,
        studentName: String,
        initialBalance: Float
    ) {
        // Balance is intentionally left untouched here — it's credited only once an admin
        // approves this top-up from the Pending Top-ups screen.
        val now = System.currentTimeMillis()

        val transaction = Transaction(
            transactionId = "TOP-$now",
            studentId = studentId,
            studentName = studentName,
            type = "topup",
            amount = amount,
            vendor = "Parent Top-up ($paymentMethod)",
            dateTime = getCurrentDateTime(),
            status = "pending",
            timestamp = now
        )

        prefsManager.addTransaction(transaction)

        AlertDialog.Builder(this)
            .setTitle("Top-up Submitted")
            .setMessage(
                "Your top-up request has been submitted for admin approval.\n\n" +
                        "Student: $studentName\n" +
                        "Amount: ${formatCurrency(amount)}\n\n" +
                        "It will reflect in the student's balance once approved."
            )
            .setPositiveButton("OK") { _, _ ->
                clearForm()
                finish()
            }
            .show()
    }

    private fun clearForm() {
        etAmount.text?.clear()
        etPhoneNumber.text?.clear()
        rgPaymentMethod.clearCheck()
        etPhoneNumber.visibility = android.view.View.GONE
    }

    private fun formatCurrency(amount: Double): String {
        return "UGX ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
}