package com.example.shulecard

import android.content.Context
import android.widget.CheckBox
import android.widget.RadioButton
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shulecard.TestData.ctx
import com.example.shulecard.activities.admin.IssueCardActivity
import com.example.shulecard.activities.admin.PendingTopUpsActivity
import com.example.shulecard.activities.admin.RegisterStudentActivity
import com.example.shulecard.activities.auth.AdminSetupActivity
import com.example.shulecard.activities.auth.SignupActivity
import com.example.shulecard.activities.auth.SplashScreenActivity
import com.example.shulecard.activities.parent.LoadMoneyActivity
import com.example.shulecard.activities.parent.ParentDashboardActivity
import com.example.shulecard.activities.student.OrderQRDisplayActivity
import com.example.shulecard.activities.student.PaymentConfirmationActivity
import com.example.shulecard.activities.vendor.RedeemOrderActivity
import com.example.shulecard.activities.vendor.ScanPaymentActivity
import com.example.shulecard.models.Order
import com.example.shulecard.models.OrderItem
import com.example.shulecard.models.Transaction
import com.example.shulecard.utils.AdminRegistryHelper
import com.example.shulecard.utils.NotificationHelper
import com.example.shulecard.utils.OrderRegistryHelper
import com.example.shulecard.utils.ParentRegistryHelper
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.QrCodeGenerator
import com.example.shulecard.utils.StudentRegistryHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** TC01–TC14 (Table 18 / Appendix VIII), driven through the real screens on synthetic data. */
@RunWith(AndroidJUnit4::class)
class FunctionalTests {

    @Before
    fun setUp() {
        Ui.finishAll()
        TestData.resetAll()
    }

    @After
    fun tearDown() = Ui.finishAll()

    @Test
    fun tc01_firstLaunchOpensAdminSetup() = runCase("TC01") {
        Ui.start(SplashScreenActivity::class.java)
        val setup = Ui.waitFor(AdminSetupActivity::class.java, 5000)
            ?: return@runCase "AdminSetup did not open" to false
        Ui.type(setup, R.id.etAdminFullName, "Test Admin")
        Ui.type(setup, R.id.etAdminEmail, TestData.ADMIN_EMAIL)
        Ui.type(setup, R.id.etAdminPassword, TestData.ADMIN_PASSWORD)
        Ui.type(setup, R.id.etAdminConfirmPassword, TestData.ADMIN_PASSWORD)
        Ui.type(setup, R.id.etAdminSecurityAnswer, "kiteezi")
        Ui.click(setup, R.id.btnCreateAdmin)
        Ui.idle()
        val created = AdminRegistryHelper.hasAdminAccount(ctx)
        "AdminSetup opened on first launch; administrator account created=$created" to created
    }

    @Test
    fun tc02_adminRegistersStudentAndIssuesCard() = runCase("TC02") {
        TestData.seedAdmin()
        TestData.login("admin")
        val reg = Ui.open(RegisterStudentActivity::class.java)
        Ui.type(reg, R.id.etStudentFullName, TestData.STUDENT_NAME)
        Ui.type(reg, R.id.etStudentId, TestData.STUDENT_ID)
        Ui.type(reg, R.id.etStudentPassword, TestData.STUDENT_PASSWORD)
        Ui.type(reg, R.id.etStudentSecurityAnswer, "kiteezi")
        Ui.type(reg, R.id.etParentName, "Test Parent")
        Ui.type(reg, R.id.etParentPhone, "0700000000")
        Ui.type(reg, R.id.etParentEmail, TestData.PARENT_EMAIL)
        Ui.type(reg, R.id.etInitialBalance, "0")
        Ui.click(reg, R.id.btnRegisterStudent)
        Ui.idle()
        val saved = StudentRegistryHelper.getStudentById(ctx, TestData.STUDENT_ID) != null

        val issue = Ui.open(IssueCardActivity::class.java)
        Ui.click(issue, R.id.btnGenerateCard)
        Ui.click(issue, R.id.btnIssueCard)
        Ui.idle()
        val status = ctx.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE)
            .getString("cardStatus_${TestData.STUDENT_ID}", null)
        val qr = QrCodeGenerator.generate(TestData.STUDENT_ID) != null
        "Student saved=$saved; card status=$status; QR generated=$qr" to (saved && status == "Active" && qr)
    }

    @Test
    fun tc03_parentSignsUpAndLinksChild() = runCase("TC03") {
        TestData.seedStudent()
        val s = Ui.open(SignupActivity::class.java)
        Ui.type(s, R.id.etFullName, "Test Parent")
        Ui.type(s, R.id.etEmail, TestData.PARENT_EMAIL)
        Ui.type(s, R.id.etPhone, "0700000000")
        Ui.type(s, R.id.etStudentId, TestData.STUDENT_ID)
        Ui.type(s, R.id.etPasswordSignup, TestData.PARENT_PASSWORD)
        Ui.type(s, R.id.etConfirmPassword, TestData.PARENT_PASSWORD)
        Ui.type(s, R.id.etSecurityAnswer, "kiteezi")
        Ui.main { s.findViewById<CheckBox>(R.id.cbTerms).isChecked = true }
        Ui.click(s, R.id.btnRegister)
        Ui.idle()
        val parent = ParentRegistryHelper.getParentByEmail(ctx, TestData.PARENT_EMAIL)
        val linked = parent?.studentIds?.contains(TestData.STUDENT_ID) == true

        TestData.login("parent")
        val dash = Ui.open(ParentDashboardActivity::class.java)
        val visible = Ui.showsText(dash, TestData.STUDENT_NAME) || Ui.showsText(dash, TestData.STUDENT_ID)
        "Parent created=${parent != null}; child linked=$linked; child shown on dashboard=$visible" to
            (linked && visible)
    }

    @Test
    fun tc04_parentSubmitsTopUp() = runCase("TC04") {
        TestData.seedAdmin(); TestData.seedStudent(balance = 0f); TestData.seedParent()
        TestData.login("parent")
        val a = Ui.open(LoadMoneyActivity::class.java)
        Ui.type(a, R.id.etAmount, "5000")
        Ui.main { a.findViewById<RadioButton>(R.id.rbMobileMoney).isChecked = true }
        Ui.type(a, R.id.etPhoneNumber, "0770000000")
        Ui.click(a, R.id.btnContinueLoad)
        Ui.dialogButton("Confirm")
        val tx = TestData.transactions().firstOrNull { it.type == "topup" }
        val notified = NotificationHelper.getAdminNotifications(ctx).isNotEmpty()
        "Top-up saved status=${tx?.status}; balance=${TestData.balance()}; admin notifications=$notified" to
            (tx?.status == "pending" && TestData.balance() == 0.0 && notified)
    }

    private fun seedPendingTopUp(id: String = "TOP-1", amount: Double = 5000.0) {
        PrefsManager(ctx).addTransaction(
            Transaction(id, TestData.STUDENT_ID, TestData.STUDENT_NAME, "topup", amount,
                "Parent Top-up (Mobile Money)", "today", "pending", System.currentTimeMillis())
        )
    }

    @Test
    fun tc05_adminApprovesTopUp() = runCase("TC05") {
        TestData.seedAdmin(); TestData.seedStudent(balance = 0f); seedPendingTopUp()
        TestData.login("admin")
        val a = Ui.open(PendingTopUpsActivity::class.java)
        val approve = Ui.recyclerRowButton(a, R.id.rvPendingTopUps, 0, R.id.btnApproveTopUp)
        Ui.main { approve.performClick() }
        Ui.idle()
        val status = TestData.transactions().first().status
        "Balance=${TestData.balance()}; status=$status" to (TestData.balance() == 5000.0 && status == "completed")
    }

    @Test
    fun tc06_adminRejectsTopUp() = runCase("TC06") {
        TestData.seedAdmin(); TestData.seedStudent(balance = 0f); TestData.seedParent(); seedPendingTopUp()
        TestData.login("admin")
        val a = Ui.open(PendingTopUpsActivity::class.java)
        val reject = Ui.recyclerRowButton(a, R.id.rvPendingTopUps, 0, R.id.btnRejectTopUp)
        Ui.main { reject.performClick() }
        Ui.dialogButton("Reject")
        val status = TestData.transactions().first().status
        val parentNotes = NotificationHelper.getParentNotifications(ctx, TestData.PARENT_EMAIL, listOf(TestData.STUDENT_ID))
        val notified = parentNotes.any { it.id.startsWith("topup_rejected") }
        "Status=$status; balance=${TestData.balance()}; parent notified=$notified" to
            (status == "rejected" && TestData.balance() == 0.0 && notified)
    }

    private fun openCart(): PaymentConfirmationActivity {
        TestData.seedVendor()
        TestData.login("student")
        val cart = listOf(OrderItem("ITM-1", "Chapati", 500.0, 1), OrderItem("ITM-2", "Juice", 500.0, 1))
        return Ui.open(PaymentConfirmationActivity::class.java) {
            it.putExtra("vendorId", TestData.VENDOR_ID)
            it.putExtra("vendor", TestData.VENDOR_NAME)
            it.putExtra("cartItemsJson", OrderRegistryHelper.serializeItems(cart))
        }
    }

    @Test
    fun tc07_cartPaymentWithCorrectPin() = runCase("TC07") {
        TestData.seedStudent(balance = 5000f)
        val a = openCart()
        Ui.type(a, R.id.etPin, TestData.STUDENT_PIN)
        Ui.click(a, R.id.btnConfirmPayment)
        val qrShown = Ui.waitFor(OrderQRDisplayActivity::class.java) != null
        val order = OrderRegistryHelper.getAllOrders(ctx).firstOrNull()
        val payments = TestData.transactions().count { it.type == "payment" }
        "Balance=${TestData.balance()}; payments=$payments; order status=${order?.status}; QR screen=$qrShown" to
            (TestData.balance() == 4000.0 && payments == 1 && order?.status == Order.STATUS_PENDING && qrShown)
    }

    @Test
    fun tc08_cartPaymentWithWrongPin() = runCase("TC08") {
        TestData.seedStudent(balance = 5000f)
        val a = openCart()
        Ui.type(a, R.id.etPin, "0000")
        Ui.click(a, R.id.btnConfirmPayment)
        Ui.idle()
        val orders = OrderRegistryHelper.getAllOrders(ctx).size
        "Balance=${TestData.balance()}; orders=$orders; transactions=${TestData.transactions().size}" to
            (TestData.balance() == 5000.0 && orders == 0 && TestData.transactions().isEmpty())
    }

    private fun seedOrder(status: String = Order.STATUS_PENDING): Order {
        val order = Order(
            orderId = "ORD-1001", studentId = TestData.STUDENT_ID, studentName = TestData.STUDENT_NAME,
            vendorId = TestData.VENDOR_ID, vendorName = TestData.VENDOR_NAME,
            items = listOf(OrderItem("ITM-1", "Chapati", 1000.0, 1)), totalAmount = 1000.0,
            transactionId = "TRX-1", timestamp = System.currentTimeMillis(), status = status
        )
        OrderRegistryHelper.upsertOrder(ctx, order)
        return order
    }

    private fun redeem(vendorId: String): RedeemOrderActivity {
        TestData.login("vendor", vendorId = vendorId)
        val a = Ui.open(RedeemOrderActivity::class.java) { it.putExtra("vendorId", vendorId) }
        Ui.type(a, R.id.etOrderCode, "ORD-1001")
        Ui.click(a, R.id.btnLookupOrder)
        Ui.idle()
        return a
    }

    @Test
    fun tc09_vendorRedeemsOrder() = runCase("TC09") {
        TestData.seedStudent(balance = 4000f); TestData.seedVendor(); seedOrder()
        val a = redeem(TestData.VENDOR_ID)
        Ui.click(a, R.id.btnIssueItems)
        Ui.idle()
        val status = OrderRegistryHelper.getOrderById(ctx, "ORD-1001")?.status
        "Order status=$status; balance=${TestData.balance()}" to
            (status == Order.STATUS_ISSUED && TestData.balance() == 4000.0)
    }

    @Test
    fun tc10_vendorRedeemsSameOrderAgain() = runCase("TC10") {
        TestData.seedStudent(balance = 4000f); TestData.seedVendor(); seedOrder(Order.STATUS_ISSUED)
        val a = redeem(TestData.VENDOR_ID)
        var enabled = true
        Ui.main { enabled = a.findViewById<android.view.View>(R.id.btnIssueItems).isEnabled }
        val shown = Ui.showsText(a, "Already Issued")
        val second = OrderRegistryHelper.markIssued(ctx, "ORD-1001")
        "Issue button enabled=$enabled; 'Already Issued' shown=$shown; second issue accepted=$second" to
            (!enabled && shown && !second)
    }

    @Test
    fun tc11_otherVendorCannotRedeem() = runCase("TC11") {
        TestData.seedStudent(balance = 4000f); TestData.seedVendor()
        TestData.seedVendor(TestData.OTHER_VENDOR_ID, "Other Shop"); seedOrder()
        val a = redeem(TestData.OTHER_VENDOR_ID)
        var previewVisible = true
        Ui.main { previewVisible = a.findViewById<android.view.View>(R.id.cardOrderPreview).visibility == android.view.View.VISIBLE }
        val status = OrderRegistryHelper.getOrderById(ctx, "ORD-1001")?.status
        "Preview shown=$previewVisible; order status=$status" to (!previewVisible && status == Order.STATUS_PENDING)
    }

    private fun charge(amount: String): ScanPaymentActivity {
        TestData.seedVendor()
        TestData.login("vendor")
        val a = Ui.open(ScanPaymentActivity::class.java) {
            it.putExtra("vendorId", TestData.VENDOR_ID); it.putExtra("vendorName", TestData.VENDOR_NAME)
        }
        Ui.type(a, R.id.etStudentId, TestData.STUDENT_ID)
        Ui.click(a, R.id.btnLookupStudent)
        Ui.type(a, R.id.etAmountVendor, amount)
        Ui.click(a, R.id.btnProcessPayment)
        Ui.idle()
        return a
    }

    @Test
    fun tc12_vendorChargesWithinBalance() = runCase("TC12") {
        TestData.seedStudent(balance = 4000f)
        charge("1500")
        val payments = TestData.transactions().count { it.type == "payment" }
        "Balance=${TestData.balance()}; payments saved=$payments" to (TestData.balance() == 2500.0 && payments == 1)
    }

    @Test
    fun tc13_vendorChargesAboveBalance() = runCase("TC13") {
        TestData.seedStudent(balance = 2500f)
        charge("3000")
        "Balance=${TestData.balance()}; transactions=${TestData.transactions().size}" to
            (TestData.balance() == 2500.0 && TestData.transactions().isEmpty())
    }

    @Test
    fun tc14_vendorChargesFrozenCard() = runCase("TC14") {
        TestData.seedStudent(balance = 2500f)
        TestData.setCardStatus(TestData.STUDENT_ID, "Frozen")
        charge("500")
        "Balance=${TestData.balance()}; transactions=${TestData.transactions().size}" to
            (TestData.balance() == 2500.0 && TestData.transactions().isEmpty())
    }
}
