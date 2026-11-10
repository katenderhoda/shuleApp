package com.example.shulecard.utils

import android.content.Context
import com.example.shulecard.models.Transaction
import java.util.Calendar

enum class NotificationCategory { INFO, SUCCESS, WARNING, CRITICAL }

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val category: NotificationCategory,
    /** 0 means "ongoing state" (e.g. a card is currently frozen) rather than a one-time event. */
    val timestamp: Long
)

/**
 * Builds every role's notification feed from real app state instead of stored/fake messages —
 * there's no backend to persist discrete notification events, so this recomputes each feed from
 * the same data the dashboards already read (StudentRegistryHelper, CardPrefs, PrefsManager
 * balances/transactions).
 *
 * Read-state (unread count, the enabled toggle) is scoped per *identity* — a parent's email, a
 * student's ID, a vendor's ID, or the fixed admin identity — since multiple accounts can log in
 * on the same device over time and shouldn't share one read/unread ledger.
 */
object NotificationHelper {

    private const val PREFS = "AdminNotifPrefs"
    private const val LOW_BALANCE_THRESHOLD = 10000.0
    private const val LARGE_TRANSACTION_THRESHOLD = 20000.0

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isEnabled(context: Context, identity: String): Boolean =
        prefs(context).getBoolean("enabled_$identity", true)

    private fun setEnabled(context: Context, identity: String, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled_$identity", enabled).apply()
    }

    /**
     * Ongoing-state alerts (timestamp 0) always count as unread since there's no per-item read
     * ledger — they represent a real unresolved condition (frozen card, low balance) that should
     * keep surfacing until it's actually fixed, not just dismissed. One-time events (registrations)
     * drop out once seen.
     */
    private fun unreadCount(context: Context, identity: String, notifications: List<AppNotification>): Int {
        val lastSeen = prefs(context).getLong("lastSeen_$identity", 0L)
        return notifications.count { it.timestamp == 0L || it.timestamp > lastSeen }
    }

    private fun markSeen(context: Context, identity: String) {
        prefs(context).edit().putLong("lastSeen_$identity", System.currentTimeMillis()).apply()
    }

    private fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    }

    // ===================== Admin =====================

    private const val ADMIN_IDENTITY = "admin"

    fun isAdminNotificationsEnabled(context: Context) = isEnabled(context, ADMIN_IDENTITY)

    fun setAdminNotificationsEnabled(context: Context, enabled: Boolean) =
        setEnabled(context, ADMIN_IDENTITY, enabled)

    fun getAdminNotifications(context: Context): List<AppNotification> {
        if (!isEnabled(context, ADMIN_IDENTITY)) return emptyList()

        val notifications = mutableListOf<AppNotification>()
        val students = StudentRegistryHelper.getAllStudents(context)
        val cardPrefs = context.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE)
        val prefsManager = PrefsManager(context)

        for (student in students) {
            val studentId = student.studentId

            if (student.registeredAt > 0L) {
                notifications.add(
                    AppNotification(
                        id = "reg_$studentId",
                        title = "New student registered",
                        message = "${student.studentName} ($studentId) was added to the system.",
                        category = NotificationCategory.INFO,
                        timestamp = student.registeredAt
                    )
                )
            }

            notifications.addAll(cardAndBalanceAlerts(context, cardPrefs, prefsManager, student))
        }

        notifications.addAll(pendingTopUpAlerts(prefsManager))

        return notifications.sortedByDescending { it.timestamp }
    }

    fun getAdminUnreadCount(context: Context): Int =
        unreadCount(context, ADMIN_IDENTITY, getAdminNotifications(context))

    fun markAdminSeen(context: Context) = markSeen(context, ADMIN_IDENTITY)

    // ===================== Parent =====================

    private fun parentIdentity(email: String) = "parent_${email.lowercase()}"

    fun isParentNotificationsEnabled(context: Context, email: String) =
        isEnabled(context, parentIdentity(email))

    fun setParentNotificationsEnabled(context: Context, email: String, enabled: Boolean) =
        setEnabled(context, parentIdentity(email), enabled)

    fun getParentNotifications(context: Context, email: String, studentIds: List<String>): List<AppNotification> {
        if (!isEnabled(context, parentIdentity(email))) return emptyList()

        val notifications = mutableListOf<AppNotification>()
        val cardPrefs = context.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE)
        val prefsManager = PrefsManager(context)

        for (studentId in studentIds) {
            val student = StudentRegistryHelper.getStudentById(context, studentId) ?: continue
            notifications.addAll(cardAndBalanceAlerts(context, cardPrefs, prefsManager, student))
        }

        notifications.addAll(pendingTopUpAlerts(prefsManager) { it.studentId in studentIds })
        notifications.addAll(rejectedTopUpAlerts(prefsManager) { it.studentId in studentIds })

        return notifications.sortedByDescending { it.timestamp }
    }

    fun getParentUnreadCount(context: Context, email: String, studentIds: List<String>): Int =
        unreadCount(context, parentIdentity(email), getParentNotifications(context, email, studentIds))

    fun markParentSeen(context: Context, email: String) = markSeen(context, parentIdentity(email))

    // ===================== Student =====================

    private fun studentIdentity(studentId: String) = "student_${studentId.lowercase()}"

    fun isStudentNotificationsEnabled(context: Context, studentId: String) =
        isEnabled(context, studentIdentity(studentId))

    fun setStudentNotificationsEnabled(context: Context, studentId: String, enabled: Boolean) =
        setEnabled(context, studentIdentity(studentId), enabled)

    fun getStudentNotifications(context: Context, studentId: String): List<AppNotification> {
        if (!isEnabled(context, studentIdentity(studentId))) return emptyList()

        val student = StudentRegistryHelper.getStudentById(context, studentId) ?: return emptyList()
        val cardPrefs = context.getSharedPreferences("CardPrefs", Context.MODE_PRIVATE)
        val prefsManager = PrefsManager(context)

        return cardAndBalanceAlerts(context, cardPrefs, prefsManager, student)
            .sortedByDescending { it.timestamp }
    }

    fun getStudentUnreadCount(context: Context, studentId: String): Int =
        unreadCount(context, studentIdentity(studentId), getStudentNotifications(context, studentId))

    fun markStudentSeen(context: Context, studentId: String) = markSeen(context, studentIdentity(studentId))

    // ===================== Vendor =====================

    private fun vendorIdentity(vendorId: String) = "vendor_${vendorId.lowercase()}"

    fun isVendorNotificationsEnabled(context: Context, vendorId: String) =
        isEnabled(context, vendorIdentity(vendorId))

    fun setVendorNotificationsEnabled(context: Context, vendorId: String, enabled: Boolean) =
        setEnabled(context, vendorIdentity(vendorId), enabled)

    /**
     * Vendors have no "alerts" concept like frozen/blocked cards — the only real signal
     * available without inventing data is their own transaction history, so today's larger
     * sales are surfaced as informational notices.
     */
    fun getVendorNotifications(context: Context, vendorId: String, vendorName: String): List<AppNotification> {
        if (!isEnabled(context, vendorIdentity(vendorId))) return emptyList()

        val prefsManager = PrefsManager(context)
        return prefsManager.getAllTransactions()
            .filter { it.vendor == vendorName }
            .filter { it.type.equals("payment", ignoreCase = true) }
            .filter { isToday(it.timestamp) }
            .filter { it.amount >= LARGE_TRANSACTION_THRESHOLD }
            .map {
                AppNotification(
                    id = "txn_${it.transactionId}",
                    title = "Large transaction",
                    message = "${it.studentName} paid UGX ${it.amount.toInt()}.",
                    category = NotificationCategory.INFO,
                    timestamp = it.timestamp
                )
            }
            .sortedByDescending { it.timestamp }
    }

    fun getVendorUnreadCount(context: Context, vendorId: String, vendorName: String): Int =
        unreadCount(context, vendorIdentity(vendorId), getVendorNotifications(context, vendorId, vendorName))

    fun markVendorSeen(context: Context, vendorId: String) = markSeen(context, vendorIdentity(vendorId))

    // ===================== Shared alert derivation (Admin/Parent/Student) =====================

    private fun cardAndBalanceAlerts(
        context: Context,
        cardPrefs: android.content.SharedPreferences,
        prefsManager: PrefsManager,
        student: StudentProfile
    ): List<AppNotification> {
        val notifications = mutableListOf<AppNotification>()
        val studentId = student.studentId

        val cardStatus = cardPrefs.getString("cardStatus_$studentId", "Active") ?: "Active"
        when {
            cardStatus.equals("Blocked", ignoreCase = true) -> notifications.add(
                AppNotification(
                    id = "blocked_$studentId",
                    title = "Card blocked",
                    message = "${student.studentName}'s card was reported lost and is blocked.",
                    category = NotificationCategory.CRITICAL,
                    timestamp = 0L
                )
            )
            cardStatus.equals("Frozen", ignoreCase = true) -> notifications.add(
                AppNotification(
                    id = "frozen_$studentId",
                    title = "Card frozen",
                    message = "${student.studentName}'s card is currently frozen.",
                    category = NotificationCategory.WARNING,
                    timestamp = 0L
                )
            )
        }

        if (cardPrefs.getBoolean("newCardRequested_$studentId", false)) {
            notifications.add(
                AppNotification(
                    id = "cardreq_$studentId",
                    title = "Card replacement requested",
                    message = "A new card was requested for ${student.studentName}.",
                    category = NotificationCategory.WARNING,
                    timestamp = 0L
                )
            )
        }

        val balance = prefsManager.getBalanceForStudent(studentId, student.initialBalance)
        if (balance < LOW_BALANCE_THRESHOLD) {
            notifications.add(
                AppNotification(
                    id = "lowbal_$studentId",
                    title = "Low balance",
                    message = "${student.studentName}'s wallet balance is below UGX ${LOW_BALANCE_THRESHOLD.toInt()}.",
                    category = NotificationCategory.WARNING,
                    timestamp = 0L
                )
            )
        }

        return notifications
    }

    /**
     * One ongoing-state (unresolved) alert per top-up still awaiting admin approval — mirrors
     * how frozen/blocked cards stay flagged until the underlying state actually changes.
     */
    private fun pendingTopUpAlerts(
        prefsManager: PrefsManager,
        filter: (Transaction) -> Boolean = { true }
    ): List<AppNotification> {
        return prefsManager.getAllTransactions()
            .filter { it.type.equals("topup", ignoreCase = true) && it.status.equals("pending", ignoreCase = true) }
            .filter(filter)
            .map {
                AppNotification(
                    id = "topup_pending_${it.transactionId}",
                    title = "Top-up awaiting approval",
                    message = "${it.studentName}'s top-up of UGX ${it.amount.toInt()} is awaiting admin approval.",
                    category = NotificationCategory.WARNING,
                    timestamp = 0L
                )
            }
    }

    /** A one-time notice per top-up an admin has declined — a discrete, already-resolved event. */
    private fun rejectedTopUpAlerts(
        prefsManager: PrefsManager,
        filter: (Transaction) -> Boolean = { true }
    ): List<AppNotification> {
        return prefsManager.getAllTransactions()
            .filter { it.type.equals("topup", ignoreCase = true) && it.status.equals("rejected", ignoreCase = true) }
            .filter(filter)
            .map {
                AppNotification(
                    id = "topup_rejected_${it.transactionId}",
                    title = "Top-up declined",
                    message = "The top-up of UGX ${it.amount.toInt()} for ${it.studentName} was declined by the admin.",
                    category = NotificationCategory.CRITICAL,
                    timestamp = it.timestamp
                )
            }
    }
}
