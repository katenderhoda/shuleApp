package com.example.shulecard.utils

import android.app.Activity
import android.content.Intent
import com.example.shulecard.R
import com.example.shulecard.activities.admin.AdminDashboardActivity
import com.example.shulecard.activities.admin.AdminProfileActivity
import com.example.shulecard.activities.admin.IssueCardActivity
import com.example.shulecard.activities.admin.RegisterStudentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Wires the shared admin bottom navigation bar (see menu/bottom_nav_admin.xml) the same way on
 * every admin screen that hosts it, so each screen doesn't repeat the same four click handlers.
 * Tabs reuse the existing Register Student / Issue Card screens rather than dedicated
 * "management" list screens, which don't exist yet.
 */
object AdminBottomNav {

    fun setup(activity: Activity, bottomNav: BottomNavigationView, selectedItemId: Int) {
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val target = when (item.itemId) {
                R.id.navHome -> AdminDashboardActivity::class.java
                R.id.navStudents -> RegisterStudentActivity::class.java
                R.id.navCards -> IssueCardActivity::class.java
                R.id.navProfile -> AdminProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            BottomNavRouter.navigate(activity, target, AdminDashboardActivity::class.java)
            // Not selected here: this screen is either finishing or (if it's the dashboard)
            // staying underneath, and should still show its own tab when the user comes back.
            false
        }
    }
}
