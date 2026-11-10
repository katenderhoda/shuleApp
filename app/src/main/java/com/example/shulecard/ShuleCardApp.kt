package com.example.shulecard

import android.app.Application
import com.example.shulecard.utils.RoleGuard
import com.example.shulecard.utils.SystemBarInsets

class ShuleCardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(SystemBarInsets)
        registerActivityLifecycleCallbacks(RoleGuard)
    }
}
