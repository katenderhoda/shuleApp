package com.example.shulecard.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ParentProfile(
    val email: String,
    /** A PBKDF2 hash produced by [PasswordHasher.hash] — never a raw password. */
    val passwordHash: String,
    val fullName: String,
    val phone: String,
    val relationship: String,
    /** Every child linked to this parent — a parent can have multiple (siblings). */
    val studentIds: List<String>,
    val receiveReminders: Boolean,
    val securityQuestion: String = "",
    /** A PBKDF2 hash of the normalized answer — see [SecurityQuestions.normalize]. */
    val securityAnswerHash: String = ""
)

enum class LinkChildResult { SUCCESS, ALREADY_LINKED, STUDENT_NOT_FOUND, PARENT_NOT_FOUND }

object ParentRegistryHelper {

    private const val KEY_PARENTS = "parents_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences("ParentPrefs", Context.MODE_PRIVATE)

    /**
     * Older builds stored exactly one parent under flat keys (registeredParentEmail, ...).
     * If that legacy data exists and hasn't been migrated yet, fold it into the parents list
     * so upgrading the app doesn't silently drop an already-registered parent.
     */
    private fun migrateLegacyParentIfNeeded(context: Context) {
        val p = prefs(context)
        if (p.contains(KEY_PARENTS)) return

        val legacyEmail = p.getString("registeredParentEmail", "")?.trim().orEmpty()
        if (legacyEmail.isEmpty()) {
            p.edit().putString(KEY_PARENTS, JSONArray().toString()).apply()
            return
        }

        // The legacy value may be plaintext; PasswordHasher.verify() falls back to a direct
        // comparison for unhashed values, and verifyPassword() upgrades it on next login.
        val legacyStudentId = p.getString("registeredStudentId", "")?.trim().orEmpty()
        val migrated = ParentProfile(
            email = legacyEmail,
            passwordHash = p.getString("registeredParentPassword", "") ?: "",
            fullName = p.getString("registeredParentName", "Parent") ?: "Parent",
            phone = p.getString("registeredParentPhone", "") ?: "",
            relationship = p.getString("registeredRelationship", "") ?: "",
            studentIds = if (legacyStudentId.isNotEmpty()) listOf(legacyStudentId) else emptyList(),
            receiveReminders = p.getBoolean("receiveReminders", false)
        )

        saveAllParents(context, listOf(migrated))
    }

    fun getAllParents(context: Context): List<ParentProfile> {
        migrateLegacyParentIfNeeded(context)

        val jsonString = prefs(context).getString(KEY_PARENTS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val parents = mutableListOf<ParentProfile>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            parents.add(
                ParentProfile(
                    email = obj.optString("email"),
                    passwordHash = obj.optString("password"),
                    fullName = obj.optString("fullName"),
                    phone = obj.optString("phone"),
                    relationship = obj.optString("relationship"),
                    studentIds = parseStudentIds(obj),
                    receiveReminders = obj.optBoolean("receiveReminders", false),
                    securityQuestion = obj.optString("securityQuestion"),
                    securityAnswerHash = obj.optString("securityAnswer")
                )
            )
        }

        return parents
    }

    /**
     * Older builds stored a single "studentId" string per parent instead of the "studentIds"
     * array; fall back to that so upgrading doesn't drop an already-linked child.
     */
    private fun parseStudentIds(obj: JSONObject): List<String> {
        val array = obj.optJSONArray("studentIds")
        if (array != null) {
            return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        }

        val legacySingle = obj.optString("studentId", "")
        return if (legacySingle.isNotBlank()) listOf(legacySingle) else emptyList()
    }

    fun saveAllParents(context: Context, parents: List<ParentProfile>) {
        val jsonArray = JSONArray()

        parents.forEach { parent ->
            val jsonObject = JSONObject().apply {
                put("email", parent.email)
                put("password", parent.passwordHash)
                put("fullName", parent.fullName)
                put("phone", parent.phone)
                put("relationship", parent.relationship)
                put("studentIds", JSONArray(parent.studentIds))
                put("receiveReminders", parent.receiveReminders)
                put("securityQuestion", parent.securityQuestion)
                put("securityAnswer", parent.securityAnswerHash)
            }
            jsonArray.put(jsonObject)
        }

        prefs(context).edit().putString(KEY_PARENTS, jsonArray.toString()).apply()
    }

    /** Adds a new parent, or replaces the existing one with the same email. */
    fun upsertParent(context: Context, parent: ParentProfile) {
        val current = getAllParents(context).toMutableList()
        val index = current.indexOfFirst { it.email.equals(parent.email, ignoreCase = true) }

        if (index >= 0) {
            current[index] = parent
        } else {
            current.add(parent)
        }

        saveAllParents(context, current)
    }

    fun getParentByEmail(context: Context, email: String): ParentProfile? {
        if (email.isBlank()) return null
        return getAllParents(context).find { it.email.equals(email, ignoreCase = true) }
    }

    /**
     * Links an additional child to an existing parent account (sibling support). Validates that
     * the student actually exists and isn't already linked before saving.
     */
    fun linkStudent(context: Context, email: String, studentId: String): LinkChildResult {
        val parent = getParentByEmail(context, email) ?: return LinkChildResult.PARENT_NOT_FOUND
        if (StudentRegistryHelper.getStudentById(context, studentId) == null) {
            return LinkChildResult.STUDENT_NOT_FOUND
        }
        if (parent.studentIds.any { it.equals(studentId, ignoreCase = true) }) {
            return LinkChildResult.ALREADY_LINKED
        }

        upsertParent(context, parent.copy(studentIds = parent.studentIds + studentId))
        return LinkChildResult.SUCCESS
    }

    /**
     * The child a parent-facing screen should default to showing. Prefers whichever child the
     * parent last actively selected (see [setActiveStudentId]), falling back to their first
     * linked child if nothing was recorded or that child is no longer linked.
     */
    fun getActiveStudentId(context: Context, parent: ParentProfile): String? {
        val remembered = prefs(context).getString(activeChildKey(parent.email), null)
        if (remembered != null && parent.studentIds.any { it.equals(remembered, ignoreCase = true) }) {
            return remembered
        }
        return parent.studentIds.firstOrNull()
    }

    fun setActiveStudentId(context: Context, email: String, studentId: String) {
        prefs(context).edit().putString(activeChildKey(email), studentId).apply()
    }

    private fun activeChildKey(email: String) = "activeChild_${email.lowercase()}"

    /**
     * Verifies a login attempt against the stored (hashed) password, and opportunistically
     * upgrades any leftover legacy plaintext to a proper hash once it's confirmed correct.
     */
    fun verifyPassword(context: Context, email: String, rawPassword: String): Boolean {
        val parent = getParentByEmail(context, email) ?: return false
        val matches = PasswordHasher.verify(rawPassword, parent.passwordHash)

        if (matches && !PasswordHasher.isHashed(parent.passwordHash)) {
            upsertParent(context, parent.copy(passwordHash = PasswordHasher.hash(rawPassword)))
        }

        return matches
    }

    /** Updates a parent's editable profile fields (used by the Profile screen's Edit Profile). */
    fun updateProfile(context: Context, email: String, fullName: String, phone: String): Boolean {
        val parent = getParentByEmail(context, email) ?: return false
        upsertParent(context, parent.copy(fullName = fullName, phone = phone))
        return true
    }

    /**
     * Changes a parent's password from within an active session, verifying the current password
     * first. Distinct from [resetPassword], the security-question-based recovery flow used when
     * logged out.
     */
    fun changePassword(context: Context, email: String, currentPassword: String, newPassword: String): Boolean {
        if (!verifyPassword(context, email, currentPassword)) return false
        val parent = getParentByEmail(context, email) ?: return false
        upsertParent(context, parent.copy(passwordHash = PasswordHasher.hash(newPassword)))
        return true
    }

    /**
     * Resets a parent's password via their security answer. Returns false if the parent has no
     * security question set (e.g. registered before this feature existed) or the answer doesn't
     * match.
     */
    fun resetPassword(context: Context, email: String, securityAnswer: String, newPassword: String): Boolean {
        val parent = getParentByEmail(context, email) ?: return false
        if (parent.securityAnswerHash.isBlank()) return false
        if (!PasswordHasher.verify(SecurityQuestions.normalize(securityAnswer), parent.securityAnswerHash)) {
            return false
        }

        upsertParent(context, parent.copy(passwordHash = PasswordHasher.hash(newPassword)))
        return true
    }
}
