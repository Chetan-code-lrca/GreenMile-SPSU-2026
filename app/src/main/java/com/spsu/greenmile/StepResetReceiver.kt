package com.spsu.greenmile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StepResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        when (intent?.action) {
            // ── Reschedule alarm after phone reboot ──
            Intent.ACTION_BOOT_COMPLETED -> {
                StepCounterService.scheduleMidnightReset(context)
            }
            // ── Midnight reset ──
            StepCounterService.ACTION_MIDNIGHT_RESET -> {
                val prefs = context.getSharedPreferences(
                    StepCounterService.PREFS_NAME, Context.MODE_PRIVATE
                )
                val today = java.text.SimpleDateFormat(
                    "yyyy-MM-dd", java.util.Locale.getDefault()
                ).format(java.util.Date())

                val currentOffset = prefs.getFloat(StepCounterService.KEY_STEPS_OFFSET, 0f) +
                        prefs.getInt(StepCounterService.KEY_STEPS_TODAY, 0)

                prefs.edit()
                    .putInt(StepCounterService.KEY_STEPS_TODAY, 0)
                    .putFloat(StepCounterService.KEY_STEPS_OFFSET, currentOffset)
                    .putString(StepCounterService.KEY_LAST_DATE, today)
                    .apply()
            }
        }
    }
}