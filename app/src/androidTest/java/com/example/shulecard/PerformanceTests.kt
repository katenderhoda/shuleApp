package com.example.shulecard

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shulecard.TestData.ctx
import com.example.shulecard.activities.admin.PendingTopUpsActivity
import com.example.shulecard.activities.admin.TransactionLogsActivity
import com.example.shulecard.activities.auth.LoginActivity
import com.example.shulecard.activities.student.PaymentConfirmationActivity
import com.example.shulecard.activities.student.StudentDashboardActivity
import com.example.shulecard.activities.vendor.ScanPaymentActivity
import com.example.shulecard.models.OrderItem
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.OrderRegistryHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PT01–PT05 (Tables 21 and 22): on-device response time at 20, 100, 1,000 and 5,000 stored
 * transactions. Synthetic transactions are inserted before timing begins (the "debug-only
 * routine" described in Section 5.8.3). Each operation is timed REPS times after one warm-up run.
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTests {

    private val volumes = listOf(20, 100, 1000, 5000)
    private val reps = 7

    @After
    fun tearDown() = Ui.finishAll()

    private fun seed(volume: Int) {
        Ui.finishAll()
        TestData.resetAll()
        TestData.seedAdmin()
        TestData.seedVendor()
        for (i in 1..10) TestData.seedStudent(id = "STU%03d".format(i), balance = 1_000_000f)
        TestData.seedStudent(balance = 1_000_000f)
        // Raise the limit so the synthetic history never trips the daily spending check.
        ctx.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE).edit()
            .putInt("dailyLimit_${TestData.STUDENT_ID}", Int.MAX_VALUE).commit()
        val day = 24L * 60 * 60 * 1000
        val start = System.currentTimeMillis() - 30 * day
        val txns = (0 until volume).map { i ->
            val sid = if (i % 11 == 0) TestData.STUDENT_ID else "STU%03d".format(i % 10 + 1)
            val topup = i % 4 == 0
            Transaction(
                transactionId = "SYN-$i", studentId = sid, studentName = "Synthetic $sid",
                type = if (topup) "topup" else "payment", amount = (500 + i % 20 * 100).toDouble(),
                vendor = if (topup) "Parent Top-up (Mobile Money)" else TestData.VENDOR_NAME,
                dateTime = "01 Oct 2026, 10:00 AM", status = "completed",
                timestamp = start + i * (29 * day / volume.coerceAtLeast(1))
            )
        }.reversed()
        PrefsManager(ctx).saveAllTransactions(txns)
    }

    private fun ms(block: () -> Unit): Double {
        val t0 = System.nanoTime(); block(); return (System.nanoTime() - t0) / 1e6
    }

    private fun measure(id: String, volume: Int, once: () -> Double) {
        once() // warm-up
        Record.timing(id, volume, (1..reps).map { once() })
    }

    @Test
    fun pt01_loginPasswordVerification() = volumes.forEach { n ->
        seed(n)
        measure("PT01", n) { ms { StudentRegistryHelper.verifyPassword(ctx, TestData.STUDENT_ID, TestData.STUDENT_PASSWORD) } }
        // Full login from tapping Login to the dashboard being on screen (includes the
        // fixed 800 ms delay LoginActivity adds before checking credentials).
        measure("PT01-full", n) {
            Ui.finishAll()
            ctx.getSharedPreferences("ShuleCardPrefs", Context.MODE_PRIVATE).edit()
                .remove("isLoggedIn").remove("userRole").commit()
            val a = Ui.open(LoginActivity::class.java)
            Ui.type(a, R.id.etUsername, TestData.STUDENT_ID)
            Ui.type(a, R.id.etPassword, TestData.STUDENT_PASSWORD)
            ms {
                Ui.click(a, R.id.btnLogin)
                pollFor(StudentDashboardActivity::class.java)
            }
        }
    }

    private fun pollFor(cls: Class<out android.app.Activity>) {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            val a = Ui.resumed()
            if (a != null && cls.isInstance(a)) { Ui.idle(); return }
            Thread.sleep(2)
        }
        error("${cls.simpleName} not reached")
    }

    @Test
    fun pt02_directCharge() = volumes.forEach { n ->
        seed(n)
        TestData.login("vendor")
        measure("PT02", n) {
            Ui.finishAll()
            val a = Ui.open(ScanPaymentActivity::class.java) {
                it.putExtra("vendorId", TestData.VENDOR_ID); it.putExtra("vendorName", TestData.VENDOR_NAME)
            }
            ms {
                Ui.type(a, R.id.etStudentId, TestData.STUDENT_ID)
                Ui.click(a, R.id.btnLookupStudent)
                Ui.type(a, R.id.etAmountVendor, "1000")
                Ui.click(a, R.id.btnProcessPayment)
                Ui.idle()
            }
        }
    }

    @Test
    fun pt03_cartCheckout() = volumes.forEach { n ->
        seed(n)
        TestData.login("student")
        measure("PT03", n) {
            Ui.finishAll()
            val a = Ui.open(PaymentConfirmationActivity::class.java) {
                it.putExtra("vendorId", TestData.VENDOR_ID)
                it.putExtra("vendor", TestData.VENDOR_NAME)
                it.putExtra("cartItemsJson", OrderRegistryHelper.serializeItems(listOf(OrderItem("ITM-1", "Chapati", 1000.0, 1))))
            }
            Ui.type(a, R.id.etPin, TestData.STUDENT_PIN)
            ms {
                Ui.click(a, R.id.btnConfirmPayment)
                pollFor(com.example.shulecard.activities.student.OrderQRDisplayActivity::class.java)
            }
        }
    }

    @Test
    fun pt04_approveTopUp() = volumes.forEach { n ->
        seed(n)
        TestData.login("admin")
        var k = 0
        measure("PT04", n) {
            Ui.finishAll()
            PrefsManager(ctx).addTransaction(
                Transaction("TOP-PT-${k++}", TestData.STUDENT_ID, TestData.STUDENT_NAME, "topup", 5000.0,
                    "Parent Top-up (Mobile Money)", "today", "pending", System.currentTimeMillis())
            )
            val a = Ui.open(PendingTopUpsActivity::class.java)
            val approve = Ui.recyclerRowButton(a, R.id.rvPendingTopUps, 0, R.id.btnApproveTopUp)
            ms { Ui.main { approve.performClick() }; Ui.idle() }
        }
    }

    @Test
    fun pt05_openTransactionHistory() = volumes.forEach { n ->
        seed(n)
        TestData.login("admin")
        measure("PT05", n) {
            Ui.finishAll()
            ms {
                Ui.start(TransactionLogsActivity::class.java)
                pollFor(TransactionLogsActivity::class.java)
            }
        }
    }

    @Test
    fun pt06_openStudentDashboard() = volumes.forEach { n ->
        seed(n)
        TestData.login("student")
        measure("PT06", n) {
            Ui.finishAll()
            ms {
                Ui.start(StudentDashboardActivity::class.java)
                pollFor(StudentDashboardActivity::class.java)
            }
        }
    }
}
