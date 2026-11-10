package com.example.shulecard.utils

import android.app.Activity
import android.content.Intent

/**
 * Shared tab-switch logic for the role bottom navigation bars, so back navigation behaves the
 * same everywhere: the role's dashboard ("home") always stays at the bottom of the back stack.
 *
 * - From the dashboard, a tab is opened *on top of* it (dashboard is not finished), so the back
 *   arrow / system back returns to the dashboard instead of closing the app.
 * - Between two non-home tabs, the current tab is replaced rather than stacked, so hopping between
 *   tabs repeatedly doesn't grow the back stack unbounded.
 * - Going to Home brings the existing dashboard back to the front (CLEAR_TOP + SINGLE_TOP) instead
 *   of creating a second copy of it.
 */
object BottomNavRouter {

    fun navigate(activity: Activity, target: Class<out Activity>, home: Class<out Activity>, intent: Intent = Intent(activity, target)) {
        if (target == home) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            activity.startActivity(intent)
            activity.finish()
            return
        }

        activity.startActivity(intent)
        if (activity.javaClass != home) {
            activity.finish()
        }
    }
}
