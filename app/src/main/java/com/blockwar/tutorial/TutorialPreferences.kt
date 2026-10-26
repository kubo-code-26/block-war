package com.blockwar.tutorial

import android.content.Context
import androidx.core.content.edit

class TutorialPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("tutorial_onboarding_v1", Context.MODE_PRIVATE)
    val seen: Boolean get() = prefs.getBoolean("seen", false)
    fun finish(completed: Boolean) {
        prefs.edit {
            putBoolean("seen", true)
            putBoolean("completed", completed || prefs.getBoolean("completed", false))
        }
    }
}
