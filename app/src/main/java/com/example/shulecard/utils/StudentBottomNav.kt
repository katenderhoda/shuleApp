package com.example.shulecard.utils

import android.app.Activity
import android.content.Intent
import com.example.shulecard.R
import com.example.shulecard.activities.student.QRCodeDisplayActivity
import com.example.shulecard.activities.student.StudentDashboardActivity
import com.example.shulecard.activities.student.StudentProfileActivity
import com.example.shulecard.activities.student.TransactionHistoryActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/** Wires the shared student bottom navigation bar (see menu/bottom_nav_student.xml). */
object StudentBottomNav {

    fun setup(activity: Activity, bottomNav: BottomNavigationView, selectedItemId: Int) {
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val target = when (item.itemId) {
                R.id.navHome -> StudentDashboardActivity::class.java
                R.id.navCard -> QRCodeDisplayActivity::class.java
                R.id.navTransactions -> TransactionHistoryActivity::class.java
                R.id.navProfile -> StudentProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            BottomNavRouter.navigate(activity, target, StudentDashboardActivity::class.java)
            // Not selected here: this screen is either finishing or (if it's the dashboard)
            // staying underneath, and should still show its own tab when the user comes back.
            false
        }
    }
}
