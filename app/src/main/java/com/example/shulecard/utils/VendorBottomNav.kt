package com.example.shulecard.utils

import android.app.Activity
import android.content.Intent
import com.example.shulecard.R
import com.example.shulecard.activities.vendor.ScanPaymentActivity
import com.example.shulecard.activities.vendor.VendorDashboardActivity
import com.example.shulecard.activities.vendor.VendorProfileActivity
import com.example.shulecard.activities.vendor.VendorTransactionHistoryActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Wires the shared vendor bottom navigation bar (see menu/bottom_nav_vendor.xml). Vendor session
 * identity isn't just carried in PrefsManager (added late, see [PrefsManager.getVendorId]) — it's
 * also threaded through as intent extras the way [ScanPaymentActivity] already expected it, so
 * every destination gets both.
 */
object VendorBottomNav {

    const val EXTRA_VENDOR_ID = "vendorId"
    const val EXTRA_VENDOR_NAME = "vendorName"

    fun setup(
        activity: Activity,
        bottomNav: BottomNavigationView,
        selectedItemId: Int,
        vendorId: String,
        vendorName: String
    ) {
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val target = when (item.itemId) {
                R.id.navHome -> VendorDashboardActivity::class.java
                R.id.navScan -> ScanPaymentActivity::class.java
                R.id.navTransactions -> VendorTransactionHistoryActivity::class.java
                R.id.navProfile -> VendorProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            val intent = Intent(activity, target).apply {
                putExtra(EXTRA_VENDOR_ID, vendorId)
                putExtra(EXTRA_VENDOR_NAME, vendorName)
            }
            BottomNavRouter.navigate(activity, target, VendorDashboardActivity::class.java, intent)
            // Not selected here: this screen is either finishing or (if it's the dashboard)
            // staying underneath, and should still show its own tab when the user comes back.
            false
        }
    }
}
