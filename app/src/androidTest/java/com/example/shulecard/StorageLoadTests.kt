package com.example.shulecard

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shulecard.TestData.ctx
import com.example.shulecard.models.Order
import com.example.shulecard.models.OrderItem
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.OrderRegistryHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentRegistryHelper
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Times the storage and logic work behind each Table 22 operation — the part whose cost depends on
 * how many transactions are stored — without screen rendering, which on the emulator varies too
 * much between runs to show the trend. Each step mirrors the calls the corresponding screen makes.
 * Logged as TIMING|PTxx-store|volume|mean|median|samples.
 */
@RunWith(AndroidJUnit4::class)
class StorageLoadTests {

    private val volumes = listOf(20, 100, 1000, 5000)
    private val reps = 15

    private fun seed(volume: Int) {
        TestData.resetAll()
        TestData.seedStudent(balance = 1_000_000f)
        TestData.seedVendor()
        val day = 24L * 60 * 60 * 1000
        val start = System.currentTimeMillis() - 30 * day
        PrefsManager(ctx).saveAllTransactions((0 until volume).map { i ->
            Transaction("SYN-$i", "STU%03d".format(i % 10 + 1), "Synthetic", if (i % 4 == 0) "topup" else "payment",
                (500 + i % 20 * 100).toDouble(), TestData.VENDOR_NAME, "01 Oct 2026, 10:00 AM", "completed",
                start + i * (29 * day / volume))
        }.reversed())
    }

    private fun time(id: String, volume: Int, prepare: () -> Unit = {}, op: () -> Unit) {
        val samples = mutableListOf<Double>()
        repeat(reps + 1) { i ->
            prepare()
            val t0 = System.nanoTime(); op(); val ms = (System.nanoTime() - t0) / 1e6
            if (i > 0) samples += ms // first run is warm-up
        }
        Record.timing("$id-store", volume, samples)
    }

    @Test
    fun storageCostByVolume() = volumes.forEach { n ->
        seed(n)
        val pm = PrefsManager(ctx)
        val sid = TestData.STUDENT_ID

        // PT01: password check at login (StudentRegistryHelper.verifyPassword, as LoginActivity calls it)
        time("PT01", n) { StudentRegistryHelper.verifyPassword(ctx, sid, TestData.STUDENT_PASSWORD) }

        // PT02: direct charge — card status, balance, today's spend for the daily limit, then two writes
        time("PT02", n) {
            val card = ctx.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE)
            card.getString("cardStatus_$sid", "Active")
            val bal = pm.getBalanceForStudent(sid, 0f)
            pm.getAllTransactions().filter { it.studentId == sid && it.type == "payment" }.sumOf { it.amount }
            pm.saveBalanceForStudent(sid, bal - 1)
            pm.addTransaction(Transaction("VND-${System.nanoTime()}", sid, "S", "payment", 1.0, "V", "now", "completed"))
        }
        seed(n)

        // PT03: cart checkout — PIN and balance check, two writes, then the order record
        time("PT03", n) {
            StudentRegistryHelper.getCardPinForStudent(ctx, sid)
            val bal = pm.getBalanceForStudent(sid, 0f)
            pm.saveBalanceForStudent(sid, bal - 1)
            val tx = Transaction("TRX-${System.nanoTime()}", sid, "S", "payment", 1.0, "V", "now", "completed")
            pm.addTransaction(tx)
            OrderRegistryHelper.upsertOrder(ctx, Order(OrderRegistryHelper.generateOrderId() + System.nanoTime(), sid, "S",
                TestData.VENDOR_ID, "V", listOf(OrderItem("I", "Item", 1.0, 1)), 1.0, tx.transactionId,
                System.currentTimeMillis(), Order.STATUS_PENDING))
        }
        seed(n)

        // PT04: approve a pending top-up — re-read status, credit, update status, reload pending list
        var k = 0
        time("PT04", n, prepare = {
            pm.addTransaction(Transaction("TOP-${k++}", sid, "S", "topup", 5000.0, "Parent", "now", "pending"))
        }) {
            val id = "TOP-${k - 1}"
            pm.getAllTransactions().first { it.transactionId == id }
            pm.saveBalanceForStudent(sid, pm.getBalanceForStudent(sid, 0f) + 5000)
            pm.updateTransactionStatus(id, "completed")
            pm.getAllTransactions().filter { it.type == "topup" && it.status == "pending" }.sortedByDescending { it.timestamp }
        }
        seed(n)

        // PT05: open transaction history — load, sort and total every transaction
        time("PT05", n) {
            val all = pm.getAllTransactions().sortedByDescending { it.timestamp }
            all.sumOf { it.amount }
        }
    }
}
