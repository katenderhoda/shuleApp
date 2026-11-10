package com.example.shulecard

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shulecard.TestData.ctx
import com.example.shulecard.activities.admin.AdminDashboardActivity
import com.example.shulecard.activities.auth.LoginActivity
import com.example.shulecard.activities.parent.ParentDashboardActivity
import com.example.shulecard.activities.student.StudentDashboardActivity
import com.example.shulecard.activities.vendor.VendorDashboardActivity
import com.example.shulecard.utils.PasswordHasher
import com.example.shulecard.utils.StudentRegistryHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** ST01–ST07 (Table 19). ST08 (forced close between writes) is a manual test and is not automated here. */
@RunWith(AndroidJUnit4::class)
class SecurityTests {

    @Before
    fun setUp() {
        Ui.finishAll()
        TestData.resetAll()
    }

    @After
    fun tearDown() = Ui.finishAll()

    private fun allPrefsText(): String =
        listOf("AdminPrefs", "StudentRegistry", "ParentPrefs", "VendorRegistry")
            .joinToString("\n") { TestData.rawPrefsFile(it) }

    @Test
    fun st01_onlyHashesStored() = runCase("ST01") {
        TestData.seedAdmin(); TestData.seedStudent(); TestData.seedParent(); TestData.seedVendor()
        Thread.sleep(1000) // let the asynchronous apply() reach the file on disk
        val raw = allPrefsText()
        val plain = listOf(TestData.ADMIN_PASSWORD, TestData.STUDENT_PASSWORD, TestData.PARENT_PASSWORD, TestData.VENDOR_PASSWORD)
            .filter { raw.contains(it) }
        val hashes = Regex("pbkdf2\\$").findAll(raw).count()
        "Plaintext passwords found in storage: ${plain.size}; PBKDF2 hashes found: $hashes" to
            (plain.isEmpty() && hashes >= 4)
    }

    private fun login(id: String, password: String) {
        val a = Ui.open(LoginActivity::class.java)
        Ui.type(a, R.id.etUsername, id)
        Ui.type(a, R.id.etPassword, password)
        Ui.click(a, R.id.btnLogin)
    }

    @Test
    fun st02_legacyPlaintextUpgraded() = runCase("ST02") {
        TestData.seedAdmin(); TestData.seedStudent()
        val legacy = StudentRegistryHelper.getStudentById(ctx, TestData.STUDENT_ID)!!
            .copy(studentPasswordHash = "legacy123")
        StudentRegistryHelper.upsertStudent(ctx, legacy)
        login(TestData.STUDENT_ID, "legacy123")
        val dash = Ui.waitFor(StudentDashboardActivity::class.java, 6000) != null
        val stored = StudentRegistryHelper.getStudentById(ctx, TestData.STUDENT_ID)!!.studentPasswordHash
        "Login succeeded=$dash; stored value is now a PBKDF2 hash=${PasswordHasher.isHashed(stored)}" to
            (dash && PasswordHasher.isHashed(stored))
    }

    @Test
    fun st03_roleRouting() = runCase("ST03") {
        TestData.seedAdmin(); TestData.seedStudent(); TestData.seedParent(); TestData.seedVendor()
        val cases = listOf(
            Triple(TestData.STUDENT_ID, TestData.STUDENT_PASSWORD, StudentDashboardActivity::class.java),
            Triple(TestData.PARENT_EMAIL, TestData.PARENT_PASSWORD, ParentDashboardActivity::class.java),
            Triple(TestData.VENDOR_ID, TestData.VENDOR_PASSWORD, VendorDashboardActivity::class.java),
            Triple(TestData.ADMIN_EMAIL, TestData.ADMIN_PASSWORD, AdminDashboardActivity::class.java)
        )
        val outcomes = cases.map { (id, pw, dash) ->
            TestData.ctx.getSharedPreferences("ShuleCardPrefs", Context.MODE_PRIVATE).edit()
                .remove("isLoggedIn").remove("userRole").commit()
            login(id, pw)
            val reached = Ui.waitFor(dash, 6000) != null
            Ui.finishAll()
            "${dash.simpleName.removeSuffix("Activity")}=$reached"
        }
        "Each identifier opened its own dashboard: ${outcomes.joinToString(", ")}" to outcomes.all { it.endsWith("true") }
    }

    @Test
    fun st06_dataAtRestEncrypted() = runCase("ST06") {
        TestData.seedStudent(balance = 5000f)
        Thread.sleep(1000) // let the asynchronous apply() reach the file on disk
        val pinVisible = TestData.rawPrefsFile("StudentRegistry").contains("\">${TestData.STUDENT_PIN}<")
        val balanceVisible = TestData.rawPrefsFile("ShuleCardPrefs").contains("balance_${TestData.STUDENT_ID}\" value=\"5000.0\"")
        "SharedPreferences stored as plain XML; card PIN readable=$pinVisible; balance readable=$balanceVisible" to
            (!pinVisible && !balanceVisible)
    }

    @Test
    fun st07_backupDisabled() = runCase("ST07") {
        val allowBackup = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        "allowBackup flag set on installed app=$allowBackup" to !allowBackup
    }
}
