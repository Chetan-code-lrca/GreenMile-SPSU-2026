package com.spsu.greenmile

import android.content.Context

// Simplified — no service, just SharedPreferences helper
object StepCounterService {

    const val PREFS_NAME       = "StepPrefs"
    const val KEY_STEPS_TODAY  = "steps_today"
    const val KEY_STEPS_OFFSET = "steps_offset"
    const val KEY_LAST_DATE    = "last_date"
    const val KEY_OFFSET_SET   = "offset_set"

    fun todayString(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

    fun getStepsToday(context: Context): Int {
        val prefs    = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today    = todayString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        if (lastDate != today) return 0
        return prefs.getInt(KEY_STEPS_TODAY, 0)
    }

    fun scheduleMidnightReset(context: Context) {
        // no-op
    }
}