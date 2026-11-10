package com.example.shulecard

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.EditText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.shulecard.utils.AdminRegistryHelper
import com.example.shulecard.utils.PasswordHasher
import com.example.shulecard.utils.PrefsManager
import com.example.shulecard.utils.StudentProfile
import com.example.shulecard.utils.StudentRegistryHelper
import com.example.shulecard.utils.SecurityQuestions
import com.example.shulecard.utils.ParentProfile
import com.example.shulecard.utils.ParentRegistryHelper
import java.io.File

/**
 * Synthetic test data and helpers shared by the instrumented test classes. Every test starts
 * from empty storage, so results never depend on data left by a previous test or by manual use.
 */
object TestData {

    const val ADMIN_EMAIL = "admin@test.school"
    const val ADMIN_PASSWORD = "Admin#2026"
    const val STUDENT_ID = "STU300"
    const val STUDENT_NAME = "Test Student"
    const val STUDENT_PASSWORD = "Stud#2026"
    const val STUDENT_PIN = "4321"
    const val PARENT_EMAIL = "parent@test.school"
    const val PARENT_PASSWORD = "Par#2026"
    const val VENDOR_ID = "VND001"
    const val VENDOR_NAME = "Test Canteen"
    const val VENDOR_PASSWORD = "Ven#2026"
    const val OTHER_VENDOR_ID = "VND002"

    val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Clears every SharedPreferences file the app has written. */
    fun resetAll() {
        val dir = File(ctx.applicationInfo.dataDir, "shared_prefs")
        dir.listFiles()?.forEach { file ->
            ctx.getSharedPreferences(file.name.removeSuffix(".xml"), Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    fun seedAdmin() {
        AdminRegistryHelper.createAdminAccount(
            ctx, "Test Admin", ADMIN_EMAIL, ADMIN_PASSWORD,
            SecurityQuestions.QUESTIONS.first(), "kiteezi"
        )
    }

    /** Registers a student, issues an active card with a known PIN and sets the balance. */
    fun seedStudent(id: String = STUDENT_ID, balance: Float = 5000f, pin: String = STUDENT_PIN) {
        StudentRegistryHelper.upsertStudent(
            ctx,
            StudentProfile(
                studentId = id,
                studentName = STUDENT_NAME,
                studentPasswordHash = PasswordHasher.hash(STUDENT_PASSWORD),
                studentClass = "S.3",
                parentName = "Test Parent",
                parentPhone = "0700000000",
                parentEmail = PARENT_EMAIL,
                initialBalance = 0f,
                registeredAt = System.currentTimeMillis()
            )
        )
        ctx.getSharedPreferences("StudentRegistry", Context.MODE_PRIVATE).edit()
            .putString("issuedCardType_$id", "Student Card")
            .putString("issuedCardNumber_$id", "SC-1000-2000")
            .putString("issuedCardPin_$id", pin)
            .commit()
        setCardStatus(id, "Active")
        ctx.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE).edit()
            .putInt("dailyLimit_$id", 10000)
            .commit()
        PrefsManager(ctx).saveBalanceForStudent(id, balance)
    }

    fun seedParent(studentId: String = STUDENT_ID) {
        ParentRegistryHelper.upsertParent(
            ctx,
            ParentProfile(
                email = PARENT_EMAIL,
                passwordHash = PasswordHasher.hash(PARENT_PASSWORD),
                fullName = "Test Parent",
                phone = "0700000000",
                relationship = "Mother",
                studentIds = listOf(studentId),
                receiveReminders = true
            )
        )
    }

    fun seedVendor(id: String = VENDOR_ID, name: String = VENDOR_NAME) {
        val p = ctx.getSharedPreferences("VendorRegistry", Context.MODE_PRIVATE)
        val ids = p.getString("vendorIds", "").orEmpty()
        p.edit()
            .putString("vendorName_$id", name)
            .putString("vendorPasswordHash_$id", PasswordHasher.hash(VENDOR_PASSWORD))
            .putString("vendorCategory_$id", "Canteen")
            .putString("vendorStatus_$id", "Active")
            .putString("vendorIds", if (ids.isEmpty()) id else "$ids,$id")
            .commit()
    }

    fun setCardStatus(id: String, status: String) {
        ctx.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE).edit()
            .putString("cardStatus_$id", status)
            .putBoolean("cardFrozen_$id", status == "Frozen")
            .commit()
    }

    /** Stores a logged-in session exactly as LoginActivity.handleSuccessfulLogin does. */
    fun login(role: String, studentId: String = STUDENT_ID, vendorId: String = VENDOR_ID) {
        val pm = PrefsManager(ctx)
        pm.setLoggedIn(true)
        pm.saveUserRole(role)
        when (role) {
            "student" -> { pm.saveUserName(STUDENT_NAME); pm.saveStudentId(studentId) }
            "parent" -> { pm.saveUserName("Test Parent"); pm.saveUserEmail(PARENT_EMAIL); pm.saveStudentId(studentId) }
            "vendor" -> { pm.saveUserName(VENDOR_NAME); pm.saveVendorId(vendorId) }
            "admin" -> pm.saveUserName("Test Admin")
        }
    }

    fun balance(id: String = STUDENT_ID): Double =
        PrefsManager(ctx).getBalanceForStudent(id, 0f).toDouble()

    fun transactions() = PrefsManager(ctx).getAllTransactions()

    /** Raw XML of a SharedPreferences file, as it sits on the device's storage. */
    fun rawPrefsFile(name: String): String =
        File(ctx.applicationInfo.dataDir, "shared_prefs/$name.xml").takeIf { it.exists() }?.readText().orEmpty()
}

/** Writes one result line to logcat (tag SHULE_TEST) so the run can be copied into Appendix XII. */
object Record {
    fun result(id: String, observed: String, pass: Boolean) {
        Log.i("SHULE_TEST", "RESULT|$id|${if (pass) "Pass" else "Fail"}|$observed")
    }

    fun timing(id: String, volume: Int, millis: List<Double>) {
        val mean = millis.average()
        val median = millis.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }
        Log.i(
            "SHULE_TEST",
            "TIMING|$id|$volume|${"%.1f".format(mean)}|${"%.1f".format(median)}|${millis.joinToString(",") { "%.1f".format(it) }}"
        )
    }
}

object Ui {
    private val instr get() = InstrumentationRegistry.getInstrumentation()

    fun resumed(): Activity? {
        var activity: Activity? = null
        instr.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return activity
    }

    /** Polls until an Activity of [cls] is resumed, returning it, or null on timeout. */
    fun waitFor(cls: Class<out Activity>, timeoutMs: Long = 5000): Activity? {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            instr.waitForIdleSync()
            val a = resumed()
            if (a != null && cls.isInstance(a)) return a
            Thread.sleep(25)
        }
        return null
    }

    fun main(block: () -> Unit) = instr.runOnMainSync(block)

    fun idle() = instr.waitForIdleSync()

    fun type(activity: Activity, id: Int, text: String) =
        main { activity.findViewById<EditText>(id).setText(text) }

    fun click(activity: Activity, id: Int) =
        main { activity.findViewById<android.view.View>(id).performClick() }

    /** Starts [cls] in a fresh task, the way a launcher or `adb shell am start` would. */
    fun start(cls: Class<out Activity>, extras: (android.content.Intent) -> Unit = {}) {
        val intent = android.content.Intent(TestData.ctx, cls).addFlags(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        extras(intent)
        TestData.ctx.startActivity(intent)
    }

    fun <T : Activity> open(cls: Class<T>, extras: (android.content.Intent) -> Unit = {}): T {
        start(cls, extras)
        @Suppress("UNCHECKED_CAST")
        return (waitFor(cls, 20000) ?: error("${cls.simpleName} did not open")) as T
    }

    fun finishAll() {
        main {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED, Stage.STARTED)
                .flatMap { monitor.getActivitiesInStage(it) }
                .forEach { it.finish() }
        }
        idle()
        Thread.sleep(300)
    }

    /** True if any TextView currently shown by [activity] contains [text]. */
    fun showsText(activity: Activity, text: String): Boolean {
        var found = false
        main {
            fun walk(v: android.view.View) {
                if (v is android.widget.TextView && v.isShown && v.text.toString().contains(text)) found = true
                if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
            walk(activity.window.decorView)
        }
        return found
    }

    /** Taps a button in the AlertDialog currently on screen. */
    fun dialogButton(label: String) {
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(label))
            .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
            .perform(androidx.test.espresso.action.ViewActions.click())
        idle()
    }

    fun recyclerRowButton(activity: Activity, recyclerId: Int, row: Int, buttonId: Int): android.view.View {
        var button: android.view.View? = null
        val end = System.currentTimeMillis() + 3000
        while (button == null && System.currentTimeMillis() < end) {
            idle()
            main {
                val rv = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(recyclerId)
                button = rv.findViewHolderForAdapterPosition(row)?.itemView?.findViewById(buttonId)
            }
            if (button == null) Thread.sleep(50)
        }
        return button ?: error("Row $row not shown")
    }
}

/** Runs one test step, logs its observed result for Appendix XII, then asserts it. */
fun runCase(id: String, block: () -> Pair<String, Boolean>) {
    val (observed, pass) = try {
        block()
    } catch (e: Throwable) {
        "Error: ${e.javaClass.simpleName}: ${e.message}" to false
    }
    Record.result(id, observed, pass)
    org.junit.Assert.assertTrue("$id: $observed", pass)
}
