package com.example.shulecard.utils

import android.content.Context

/**
 * Wraps the vendor data that [com.example.shulecard.activities.admin.RegisterVendorActivity]
 * writes directly into "VendorRegistry" SharedPreferences (there's no JSON-list registry object
 * for vendors like there is for students/parents — just per-ID flat keys) so the vendor
 * dashboard/profile screens don't have to poke at raw preference keys themselves.
 */
object VendorRegistryHelper {

    private fun prefs(context: Context) =
        context.getSharedPreferences("VendorRegistry", Context.MODE_PRIVATE)

    fun getVendorName(context: Context, vendorId: String): String =
        prefs(context).getString("vendorName_$vendorId", "Vendor") ?: "Vendor"

    fun getVendorEmail(context: Context, vendorId: String): String =
        prefs(context).getString("vendorContactEmail_$vendorId", "") ?: ""

    fun getVendorCategory(context: Context, vendorId: String): String =
        prefs(context).getString("vendorCategory_$vendorId", "") ?: ""

    fun getVendorLocation(context: Context, vendorId: String): String =
        prefs(context).getString("vendorLocation_$vendorId", "") ?: ""

    fun getContactPerson(context: Context, vendorId: String): String =
        prefs(context).getString("vendorContactPerson_$vendorId", "") ?: ""

    fun getContactPhone(context: Context, vendorId: String): String =
        prefs(context).getString("vendorContactPhone_$vendorId", "") ?: ""

    /** Updates the vendor's editable profile fields (used by the Profile screen's Edit Profile). */
    fun updateProfile(
        context: Context,
        vendorId: String,
        contactPerson: String,
        contactPhone: String,
        contactEmail: String
    ) {
        prefs(context).edit()
            .putString("vendorContactPerson_$vendorId", contactPerson)
            .putString("vendorContactPhone_$vendorId", contactPhone)
            .putString("vendorContactEmail_$vendorId", contactEmail)
            .apply()
    }

    fun verifyPassword(context: Context, vendorId: String, rawPassword: String): Boolean {
        val savedHash = prefs(context).getString("vendorPasswordHash_$vendorId", "") ?: ""
        val matches = PasswordHasher.verify(rawPassword, savedHash)

        if (matches && !PasswordHasher.isHashed(savedHash)) {
            prefs(context).edit()
                .putString("vendorPasswordHash_$vendorId", PasswordHasher.hash(rawPassword))
                .apply()
        }

        return matches
    }

    /**
     * Changes the vendor password from within an active session, verifying the current password
     * first. Distinct from the security-question-based recovery flow used when logged out.
     */
    fun changePassword(context: Context, vendorId: String, currentPassword: String, newPassword: String): Boolean {
        if (!verifyPassword(context, vendorId, currentPassword)) return false

        prefs(context).edit()
            .putString("vendorPasswordHash_$vendorId", PasswordHasher.hash(newPassword))
            .apply()
        return true
    }
}
