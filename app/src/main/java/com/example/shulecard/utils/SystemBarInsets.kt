package com.example.shulecard.utils

import android.app.Activity
import android.app.Application
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.shulecard.R

/**
 * Keeps every screen's content clear of the status bar, navigation bar and keyboard.
 *
 * targetSdk 35+ forces edge-to-edge drawing, so without this each toolbar slid up *under* the
 * status bar — the back arrow sat beneath the clock/icons and taps on it went to the system bar,
 * which is why back navigation looked broken. Applied once here for all activities rather than
 * per layout. The strip behind the status bar is painted brand_primary_dark (what
 * android:statusBarColor used to do before edge-to-edge made it a no-op).
 */
object SystemBarInsets : Application.ActivityLifecycleCallbacks {

    // Done in onStarted rather than onCreated: AppCompatActivity swaps in its own
    // android.R.id.content view during setContentView, so hooking earlier targets a stale view.
    override fun onActivityStarted(activity: Activity) {
        val content = activity.findViewById<View>(android.R.id.content) ?: return
        if (content.getTag(R.id.tagSystemBarInsets) != null) return
        content.setTag(R.id.tagSystemBarInsets, true)

        val background = LayerDrawable(
            arrayOf(
                ColorDrawable(ContextCompat.getColor(activity, R.color.surface)),
                ColorDrawable(ContextCompat.getColor(activity, R.color.brand_primary_dark))
            )
        ).apply { setLayerGravity(1, Gravity.TOP or Gravity.FILL_HORIZONTAL) }
        content.background = background

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            background.setLayerHeight(1, bars.top)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(content)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
