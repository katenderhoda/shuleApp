package com.example.shulecard.utils

import android.app.Activity
import android.content.Intent
import com.example.shulecard.R
import com.example.shulecard.activities.parent.CardManagementActivity
import com.example.shulecard.activities.parent.ParentDashboardActivity
import com.example.shulecard.activities.parent.ParentProfileActivity
import com.example.shulecard.activities.parent.ParentTransactionHistoryActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/** Wires the shared parent bottom navigation bar (see menu/bottom_nav_parent.xml). */
object ParentBottomNav {

    fun setup(activity: Activity, bottomNav: BottomNavigationView, selectedItemId: Int) {
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val target = when (item.itemId) {
                R.id.navHome -> ParentDashboardActivity::class.java
                R.id.navTransactions -> ParentTransactionHistoryActivity::class.java
                R.id.navCard -> CardManagementActivity::class.java
                R.id.navProfile -> ParentProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            BottomNavRouter.navigate(activity, target, ParentDashboardActivity::class.java)
            // Not selected here: this screen is either finishing or (if it's the dashboard)
            // staying underneath, and should still show its own tab when the user comes back.
            false
        }
    }
}
