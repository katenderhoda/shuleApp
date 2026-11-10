package com.example.shulecard

import android.app.Activity
import android.widget.RadioButton
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shulecard.TestData.ctx
import com.example.shulecard.activities.admin.AdminDashboardActivity
import com.example.shulecard.activities.admin.PendingTopUpsActivity
import com.example.shulecard.activities.auth.LoginActivity
import com.example.shulecard.activities.parent.LoadMoneyActivity
import com.example.shulecard.activities.student.PaymentConfirmationActivity
import com.example.shulecard.activities.vendor.ScanPaymentActivity
import com.example.shulecard.models.OrderItem
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.OrderRegistryHelper
import com.example.shulecard.utils.PrefsManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** IT01–IT09 (Table 20) plus ST04/ST05 (Table 19), driven through the real screens. */
@RunWith(AndroidJUnit4::class)
class IntegrityTests {

    @Before
    fun setUp() {
        Ui.finishAll()
        TestData.resetAll()
    }

    @After
    fun tearDown() = Ui.finishAll()

    // ---- Top-up input validation -------------------------------------------------------------

    private fun submitTopUp(amount: String): Activity {
        TestData.seedAdmin(); TestData.seedStudent(balance = 0f); TestData.seedParent()
        TestData.login("parent")
        val a = Ui.open(LoadMoneyActivity::class.java)
        Ui.type(a, R.id.etAmount, amount)
        Ui.main { a.findViewById<RadioButton>(R.id.rbMobileMoney).isChecked = true }
        Ui.type(a, R.id.etPhoneNumber, "0770000000")
        Ui.click(a, R.id.btnContinueLoad)
        Ui.idle()
        return a
    }

    private fun topUpRejected(amount: String): String {
        val a = submitTopUp(amount)
        val stillOpen = !a.isFinishing && Ui.resumed() === a
        val saved = TestData.transactions().size
        Ui.finishAll(); TestData.resetAll()
        return "amount '$amount': transactions saved=$saved, screen still open (no crash)=$stillOpen".also {
            if (saved != 0 || !stillOpen) throw AssertionError(it)
        }
    }

    @Test
    fun it01_zeroTopUp() = runCase("IT01") {
        try { topUpRejected("0") to true } catch (e: AssertionError) { e.message!! to false }
    }

    @Test
    fun it02_negativeTopUp() = runCase("IT02") {
        try { topUpRejected("-5000") to true } catch (e: AssertionError) { e.message!! to false }
    }

    @Test
    fun it03_nonNumericOrBlankTopUp() = runCase("IT03") {
        try { "${topUpRejected("abc")}; ${topUpRejected("")}" to true } catch (e: AssertionError) { e.message!! to false }
    }

    // ---- Direct charge boundaries ------------------------------------------------------------

    private fun openCharge(): ScanPaymentActivity {
        TestData.seedVendor()
        TestData.login("vendor")
        val a = Ui.open(ScanPaymentActivity::class.java) {
            it.putExtra("vendorId", TestData.VENDOR_ID); it.putExtra("vendorName", TestData.VENDOR_NAME)
        }
        Ui.type(a, R.id.etStudentId, TestData.STUDENT_ID)
        Ui.click(a, R.id.btnLookupStudent)
        return a
    }

    private fun charge(amount: String) {
        val a = openCharge()
        Ui.type(a, R.id.etAmountVendor, amount)
        Ui.click(a, R.id.btnProcessPayment)
        Ui.idle()
    }

    @Test
    fun it04_chargeEqualToBalance() = runCase("IT04") {
        TestData.seedStudent(balance = 5000f)
        charge("5000")
        "Balance after charging 5,000=${TestData.balance()}; payments=${TestData.transactions().size}" to
            (TestData.balance() == 0.0 && TestData.transactions().size == 1)
    }

    @Test
    fun it05_chargeJustAboveBalance() = runCase("IT05") {
        TestData.seedStudent(balance = 5000f)
        charge("5001")
        "Balance after charging 5,001=${TestData.balance()}; payments=${TestData.transactions().size}" to
            (TestData.balance() == 5000.0 && TestData.transactions().isEmpty())
    }

    @Test
    fun it06_zeroAndNegativeCharge() = runCase("IT06") {
        TestData.seedStudent(balance = 5000f)
        charge("0")
        val afterZero = TestData.balance()
        Ui.finishAll()
        charge("-1000")
        "Balance after 0: $afterZero; after -1,000: ${TestData.balance()}; transactions=${TestData.transactions().size}" to
            (afterZero == 5000.0 && TestData.balance() == 5000.0 && TestData.transactions().isEmpty())
    }

    @Test
    fun it07_duplicateDirectCharge() = runCase("IT07") {
        TestData.seedStudent(balance = 5000f)
        val a = openCharge()
        Ui.type(a, R.id.etAmountVendor, "1000")
        // Two taps delivered back to back, before the success dialog can appear.
        Ui.main {
            val b = a.findViewById<android.view.View>(R.id.btnProcessPayment)
            b.performClick(); b.performClick()
        }
        Ui.idle()
        val n = TestData.transactions().size
        "Direct charge, two quick taps: balance=${TestData.balance()}; transactions=$n" to
            (TestData.balance() == 4000.0 && n == 1)
    }

    @Test
    fun it07b_duplicateCartConfirm() = runCase("IT07b") {
        TestData.seedStudent(balance = 5000f); TestData.seedVendor()
        TestData.login("student")
        val a = Ui.open(PaymentConfirmationActivity::class.java) {
            it.putExtra("vendorId", TestData.VENDOR_ID)
            it.putExtra("vendor", TestData.VENDOR_NAME)
            it.putExtra("cartItemsJson", OrderRegistryHelper.serializeItems(listOf(OrderItem("ITM-1", "Chapati", 1000.0, 1))))
        }
        Ui.type(a, R.id.etPin, TestData.STUDENT_PIN)
        Ui.main {
            val b = a.findViewById<android.view.View>(R.id.btnConfirmPayment)
            b.performClick(); b.performClick()
        }
        Ui.idle()
        val n = TestData.transactions().size
        val orders = OrderRegistryHelper.getAllOrders(ctx).size
        "Cart checkout, two quick taps: balance=${TestData.balance()}; transactions=$n; orders=$orders" to
            (TestData.balance() == 4000.0 && n == 1 && orders == 1)
    }

    @Test
    fun it08_repeatedApproval() = runCase("IT08") {
        TestData.seedAdmin(); TestData.seedStudent(balance = 0f)
        PrefsManager(ctx).addTransaction(
            Transaction("TOP-1", TestData.STUDENT_ID, TestData.STUDENT_NAME, "topup", 5000.0,
                "Parent Top-up (Mobile Money)", "today", "pending", System.currentTimeMillis())
        )
        TestData.login("admin")
        val a = Ui.open(PendingTopUpsActivity::class.java)
        val approve = Ui.recyclerRowButton(a, R.id.rvPendingTopUps, 0, R.id.btnApproveTopUp)
        Ui.main { approve.performClick(); approve.performClick() }
        Ui.idle()
        // A second approval attempt after the screen is reopened.
        Ui.finishAll()
        val again = Ui.open(PendingTopUpsActivity::class.java)
        val remaining = again.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPendingTopUps).adapter?.itemCount ?: 0
        "Approve tapped twice: balance=${TestData.balance()} (expected 5000); pending items left=$remaining" to
            (TestData.balance() == 5000.0 && remaining == 0)
    }

    // ---- Access control ----------------------------------------------------------------------

    private fun landsOn(cls: Class<out Activity>): String {
        Ui.start(cls)
        Thread.sleep(1500)
        Ui.idle()
        return Ui.resumed()?.javaClass?.simpleName ?: "none"
    }

    @Test
    fun it09_unauthorisedAccess() = runCase("IT09") {
        TestData.seedAdmin(); TestData.seedStudent()
        TestData.login("student")
        val asStudentDash = landsOn(AdminDashboardActivity::class.java)
        Ui.finishAll()
        val asStudentTopUps = landsOn(PendingTopUpsActivity::class.java)
        Ui.finishAll()
        PrefsManager(ctx).logout()
        val loggedOut = landsOn(AdminDashboardActivity::class.java)
        val login = LoginActivity::class.java.simpleName
        "Student opening AdminDashboard -> $asStudentDash; PendingTopUps -> $asStudentTopUps; " +
            "after logout AdminDashboard -> $loggedOut" to
            (asStudentDash == login && asStudentTopUps == login && loggedOut == login)
    }

    // ---- ST04 / ST05 -------------------------------------------------------------------------

    @Test
    fun st04_wrongPinRefused() = runCase("ST04") {
        TestData.seedStudent(balance = 5000f); TestData.seedVendor()
        TestData.login("student")
        val a = Ui.open(PaymentConfirmationActivity::class.java) {
            it.putExtra("vendorId", TestData.VENDOR_ID)
            it.putExtra("vendor", TestData.VENDOR_NAME)
            it.putExtra("cartItemsJson", OrderRegistryHelper.serializeItems(listOf(OrderItem("ITM-1", "Chapati", 1000.0, 1))))
        }
        Ui.type(a, R.id.etPin, "9999")
        Ui.click(a, R.id.btnConfirmPayment)
        Ui.idle()
        "Wrong PIN: balance=${TestData.balance()}; transactions=${TestData.transactions().size}" to
            (TestData.balance() == 5000.0 && TestData.transactions().isEmpty())
    }

    @Test
    fun st05_inactiveCardsDeclined() = runCase("ST05") {
        TestData.seedStudent(balance = 5000f)
        TestData.setCardStatus(TestData.STUDENT_ID, "Frozen")
        charge("500")
        val frozen = TestData.balance()
        Ui.finishAll()
        TestData.setCardStatus(TestData.STUDENT_ID, "Blocked")
        charge("500")
        "Frozen card: balance=$frozen; blocked card: balance=${TestData.balance()}; transactions=${TestData.transactions().size}" to
            (frozen == 5000.0 && TestData.balance() == 5000.0 && TestData.transactions().isEmpty())
    }
}
