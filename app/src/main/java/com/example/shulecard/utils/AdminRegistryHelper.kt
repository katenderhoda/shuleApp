package com.example.shulecard.utils

import android.content.Context

/**
 * Holds the school's single admin account. Unlike students/parents there is no hardcoded
 * fallback credential — the app ships with no admin account at all, and the first run must
 * create one via a setup screen (see AdminSetupActivity) before anything else can happen.
 */
object AdminRegistryHelper {

    private fun prefs(context: Context) =
        context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)

    fun hasAdminAccount(context: Context): Boolean {
        return prefs(context).getString("adminEmail", "")?.isNotBlank() == true
    }

    fun createAdminAccount(
        context: Context,
        fullName: String,
        email: String,
        rawPassword: String,
        securityQuestion: String,
        securityAnswer: String
    ) {
        prefs(context).edit()
            .putString("adminFullName", fullName)
            .putString("adminEmail", email)
            .putString("adminPasswordHash", PasswordHasher.hash(rawPassword))
            .putString("adminSecurityQuestion", securityQuestion)
            .putString("adminSecurityAnswerHash", PasswordHasher.hash(SecurityQuestions.normalize(securityAnswer)))
            .apply()
    }

    fun getSecurityQuestion(context: Context, email: String): String? {
        val p = prefs(context)
        val savedEmail = p.getString("adminEmail", "") ?: ""
        if (savedEmail.isBlank() || !savedEmail.equals(email, ignoreCase = true)) return null
        return p.getString("adminSecurityQuestion", null)
    }

    /**
     * Resets the admin password via their security answer — the only recovery path available,
     * since there is no higher-privilege account that could reset it on the admin's behalf.
     */
    fun resetPassword(context: Context, email: String, securityAnswer: String, newPassword: String): Boolean {
        val p = prefs(context)
        val savedEmail = p.getString("adminEmail", "") ?: ""
        if (savedEmail.isBlank() || !savedEmail.equals(email, ignoreCase = true)) return false

        val savedAnswerHash = p.getString("adminSecurityAnswerHash", "") ?: ""
        if (savedAnswerHash.isBlank()) return false
        if (!PasswordHasher.verify(SecurityQuestions.normalize(securityAnswer), savedAnswerHash)) return false

        p.edit().putString("adminPasswordHash", PasswordHasher.hash(newPassword)).apply()
        return true
    }

    fun verifyPassword(context: Context, email: String, rawPassword: String): Boolean {
        val p = prefs(context)
        val savedEmail = p.getString("adminEmail", "") ?: ""
        if (savedEmail.isBlank() || !savedEmail.equals(email, ignoreCase = true)) return false

        val savedHash = p.getString("adminPasswordHash", "") ?: ""
        val matches = PasswordHasher.verify(rawPassword, savedHash)

        if (matches && !PasswordHasher.isHashed(savedHash)) {
            p.edit().putString("adminPasswordHash", PasswordHasher.hash(rawPassword)).apply()
        }

        return matches
    }

    fun getAdminName(context: Context): String {
        return prefs(context).getString("adminFullName", "School Administrator") ?: "School Administrator"
    }

    fun getAdminEmail(context: Context): String {
        return prefs(context).getString("adminEmail", "") ?: ""
    }

    fun getSchoolName(context: Context): String {
        return prefs(context).getString("schoolName", "ShuleCard School") ?: "ShuleCard School"
    }

    fun setSchoolName(context: Context, schoolName: String) {
        prefs(context).edit().putString("schoolName", schoolName).apply()
    }

    /** Updates the admin's display name and email (used by the Profile screen's Edit Profile). */
    fun updateProfile(context: Context, fullName: String, email: String) {
        prefs(context).edit()
            .putString("adminFullName", fullName)
            .putString("adminEmail", email)
            .apply()
    }

    /**
     * Changes the admin password from within an active session, verifying the current password
     * first. Distinct from [resetPassword], which is the security-question-based recovery path
     * used when the admin is logged out and can't supply their current password.
     */
    fun changePassword(context: Context, currentPassword: String, newPassword: String): Boolean {
        val email = getAdminEmail(context)
        if (!verifyPassword(context, email, currentPassword)) return false

        prefs(context).edit().putString("adminPasswordHash", PasswordHasher.hash(newPassword)).apply()
        return true
    }
}
