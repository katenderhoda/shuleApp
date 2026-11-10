package com.example.shulecard.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class StudentProfile(
    val studentId: String,
    val studentName: String,
    /** A PBKDF2 hash produced by [PasswordHasher.hash] — never a raw password. */
    val studentPasswordHash: String,
    val studentClass: String,
    val parentName: String,
    val parentPhone: String,
    val parentEmail: String,
    val initialBalance: Float,
    val securityQuestion: String = "",
    /** A PBKDF2 hash of the normalized answer — see [SecurityQuestions.normalize]. */
    val securityAnswerHash: String = "",
    /**
     * When this student was registered, for the admin dashboard's "+N this month" stat.
     * 0 means unknown (legacy record from before this field existed) — callers computing a
     * this-month delta should treat 0 as "don't count" rather than assuming "just registered".
     */
    val registeredAt: Long = 0L
)

object StudentRegistryHelper {

    private const val KEY_STUDENTS = "students_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences("StudentRegistry", Context.MODE_PRIVATE)

    /**
     * Older builds stored exactly one student under flat keys (studentId, studentName, ...).
     * If that legacy data exists and hasn't been migrated yet, fold it into the students list
     * so upgrading the app doesn't silently drop an already-registered student.
     */
    private fun migrateLegacyStudentIfNeeded(context: Context) {
        val p = prefs(context)
        if (p.contains(KEY_STUDENTS)) return

        val legacyStudentId = p.getString("studentId", "")?.trim().orEmpty()
        if (legacyStudentId.isEmpty()) {
            // Nothing to migrate; mark as migrated with an empty list.
            p.edit().putString(KEY_STUDENTS, JSONArray().toString()).apply()
            return
        }

        val legacyInitialBalance = when (val raw = p.all["initialBalance"]) {
            is Float -> raw
            is Int -> raw.toFloat()
            is Long -> raw.toFloat()
            is String -> raw.toFloatOrNull() ?: 0f
            else -> 0f
        }

        // The legacy value may be plaintext; PasswordHasher.verify() falls back to a direct
        // comparison for unhashed values, and verifyPassword() upgrades it on next login.
        val migrated = StudentProfile(
            studentId = legacyStudentId,
            studentName = p.getString("studentName", "Student") ?: "Student",
            studentPasswordHash = p.getString("studentPassword", "") ?: "",
            studentClass = p.getString("studentClass", "") ?: "",
            parentName = p.getString("parentName", "") ?: "",
            parentPhone = p.getString("parentPhone", "") ?: "",
            parentEmail = p.getString("parentEmail", "") ?: "",
            initialBalance = legacyInitialBalance
        )

        saveAllStudents(context, listOf(migrated))
    }

    fun getAllStudents(context: Context): List<StudentProfile> {
        migrateLegacyStudentIfNeeded(context)

        val jsonString = prefs(context).getString(KEY_STUDENTS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val students = mutableListOf<StudentProfile>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            students.add(
                StudentProfile(
                    studentId = obj.optString("studentId"),
                    studentName = obj.optString("studentName"),
                    studentPasswordHash = obj.optString("studentPassword"),
                    studentClass = obj.optString("studentClass"),
                    parentName = obj.optString("parentName"),
                    parentPhone = obj.optString("parentPhone"),
                    parentEmail = obj.optString("parentEmail"),
                    initialBalance = obj.optDouble("initialBalance", 0.0).toFloat(),
                    securityQuestion = obj.optString("securityQuestion"),
                    securityAnswerHash = obj.optString("securityAnswer"),
                    registeredAt = obj.optLong("registeredAt", 0L)
                )
            )
        }

        return students
    }

    fun saveAllStudents(context: Context, students: List<StudentProfile>) {
        val jsonArray = JSONArray()

        students.forEach { student ->
            val jsonObject = JSONObject().apply {
                put("studentId", student.studentId)
                put("studentName", student.studentName)
                put("studentPassword", student.studentPasswordHash)
                put("studentClass", student.studentClass)
                put("parentName", student.parentName)
                put("parentPhone", student.parentPhone)
                put("parentEmail", student.parentEmail)
                put("initialBalance", student.initialBalance.toDouble())
                put("securityQuestion", student.securityQuestion)
                put("securityAnswer", student.securityAnswerHash)
                put("registeredAt", student.registeredAt)
            }
            jsonArray.put(jsonObject)
        }

        prefs(context).edit().putString(KEY_STUDENTS, jsonArray.toString()).apply()
    }

    /** Adds a new student, or replaces the existing one with the same studentId. */
    fun upsertStudent(context: Context, student: StudentProfile) {
        val current = getAllStudents(context).toMutableList()
        val index = current.indexOfFirst { it.studentId.equals(student.studentId, ignoreCase = true) }

        if (index >= 0) {
            current[index] = student
        } else {
            current.add(student)
        }

        saveAllStudents(context, current)
    }

    fun getStudentById(context: Context, studentId: String): StudentProfile? {
        if (studentId.isBlank()) return null
        return getAllStudents(context).find { it.studentId.equals(studentId, ignoreCase = true) }
    }

    /**
     * Removes a student's registry entry only — mirrors [com.example.shulecard.activities.admin.ViewVendorsActivity]'s
     * vendor delete, which likewise leaves historical transactions and balance/card prefs alone
     * (they're keyed by studentId, not by registry membership, so they simply become orphaned).
     */
    fun deleteStudent(context: Context, studentId: String) {
        val current = getAllStudents(context).filterNot { it.studentId.equals(studentId, ignoreCase = true) }
        saveAllStudents(context, current)
    }

    /**
     * Verifies a login attempt against the stored (hashed) password, and opportunistically
     * upgrades any leftover legacy plaintext to a proper hash once it's confirmed correct.
     */
    fun verifyPassword(context: Context, studentId: String, rawPassword: String): Boolean {
        val student = getStudentById(context, studentId) ?: return false
        val matches = PasswordHasher.verify(rawPassword, student.studentPasswordHash)

        if (matches && !PasswordHasher.isHashed(student.studentPasswordHash)) {
            upsertStudent(context, student.copy(studentPasswordHash = PasswordHasher.hash(rawPassword)))
        }

        return matches
    }

    /**
     * Changes a student's password from within an active session, verifying the current password
     * first. Distinct from [resetPassword], the security-question-based recovery flow used when
     * logged out.
     */
    fun changePassword(context: Context, studentId: String, currentPassword: String, newPassword: String): Boolean {
        if (!verifyPassword(context, studentId, currentPassword)) return false
        val student = getStudentById(context, studentId) ?: return false
        upsertStudent(context, student.copy(studentPasswordHash = PasswordHasher.hash(newPassword)))
        return true
    }

    /**
     * Resets a student's password via their security answer, for use when they've forgotten
     * their password and have no admin readily available to re-register them. Returns false if
     * the student has no security question set (e.g. registered before this feature existed) or
     * the answer doesn't match.
     */
    fun resetPassword(context: Context, studentId: String, securityAnswer: String, newPassword: String): Boolean {
        val student = getStudentById(context, studentId) ?: return false
        if (student.securityAnswerHash.isBlank()) return false
        if (!PasswordHasher.verify(SecurityQuestions.normalize(securityAnswer), student.securityAnswerHash)) {
            return false
        }

        upsertStudent(context, student.copy(studentPasswordHash = PasswordHasher.hash(newPassword)))
        return true
    }

    /**
     * Kept for call sites that just need "a" representative student (e.g. a display-name
     * fallback) rather than one tied to a specific ID. Prefer [getStudentById] wherever the
     * actual student ID is known.
     */
    fun getRegisteredStudent(context: Context): StudentProfile? {
        return getAllStudents(context).firstOrNull()
    }

    /**
     * Resolves the student a parent-facing screen should show. With multiple parent accounts
     * now supported, the student tied to the *current login session* (set at login time from
     * the specific parent's linked child) is the only reliable per-parent signal; the old flat
     * ParentPrefs key is kept only as a legacy fallback for data saved before multi-parent
     * support existed.
     */
    fun getTargetStudent(context: Context): StudentProfile {
        val sessionStudentId = PrefsManager(context).getStudentId()
        getStudentById(context, sessionStudentId)?.let { return it }

        val parentPrefs = context.getSharedPreferences("ParentPrefs", Context.MODE_PRIVATE)
        val legacyLinkedStudentId = parentPrefs.getString("registeredStudentId", "")?.trim().orEmpty()
        if (legacyLinkedStudentId.isNotEmpty()) {
            getStudentById(context, legacyLinkedStudentId)?.let { return it }
        }

        getAllStudents(context).firstOrNull()?.let { return it }

        return StudentProfile(
            studentId = "STU001",
            studentName = "James M.",
            studentPasswordHash = "",
            studentClass = "",
            parentName = "",
            parentPhone = "",
            parentEmail = "",
            initialBalance = 45000f
        )
    }

    fun getDefaultBalanceForStudent(context: Context, studentId: String): Float {
        getStudentById(context, studentId)?.let { return it.initialBalance }
        return if (studentId == "STU001") 45000f else 0f
    }

    fun getStudentName(context: Context, studentId: String): String {
        getStudentById(context, studentId)?.let { return it.studentName }
        return if (studentId == "STU001") "James M." else "Student"
    }

    fun hasIssuedCardForStudent(context: Context, studentId: String): Boolean {
        val p = prefs(context)

        val cardNumber = p.getString("issuedCardNumber_$studentId", "") ?: ""
        if (cardNumber.isNotEmpty()) return true

        val issuedStudentId = p.getString("issuedCardStudentId", "") ?: ""
        val legacyCardNumber = p.getString("issuedCardNumber", "") ?: ""

        return issuedStudentId == studentId && legacyCardNumber.isNotEmpty()
    }

    fun getCardTypeForStudent(context: Context, studentId: String): String {
        val p = prefs(context)

        val newValue = p.getString("issuedCardType_$studentId", null)
        if (!newValue.isNullOrEmpty()) return newValue

        val issuedStudentId = p.getString("issuedCardStudentId", "") ?: ""
        if (issuedStudentId == studentId) {
            return p.getString("issuedCardType", "Student Card") ?: "Student Card"
        }

        return "Student Card"
    }

    fun getCardNumberForStudent(context: Context, studentId: String): String {
        val p = prefs(context)

        val newValue = p.getString("issuedCardNumber_$studentId", null)
        if (!newValue.isNullOrEmpty()) return newValue

        val issuedStudentId = p.getString("issuedCardStudentId", "") ?: ""
        if (issuedStudentId == studentId) {
            return p.getString("issuedCardNumber", "**** **** **** 1234") ?: "**** **** **** 1234"
        }

        return "Not issued yet"
    }

    fun getCardPinForStudent(context: Context, studentId: String): String {
        val p = prefs(context)

        val newValue = p.getString("issuedCardPin_$studentId", null)
        if (!newValue.isNullOrEmpty()) return newValue

        val issuedStudentId = p.getString("issuedCardStudentId", "") ?: ""
        if (issuedStudentId == studentId) {
            return p.getString("issuedCardPin", "1234") ?: "1234"
        }

        return "1234"
    }
}
