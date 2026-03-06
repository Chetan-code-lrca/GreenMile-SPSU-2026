package com.spsu.greenmile.utils

import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StreakManager {

    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun today(): String = dateFormat.format(Date())

    fun yesterday(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        return dateFormat.format(cal.time)
    }

    // ── Call this after every successful activity log ──
    fun updateStreak(userId: String, onDone: () -> Unit = {}) {
        if (userId.isEmpty()) return

        val todayStr = today()
        val yesterdayStr = yesterday()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val lastActiveDate = doc.getString("lastActiveDate") ?: ""
                val currentStreak = (doc.getLong("currentStreak") ?: 0L).toInt()

                val newStreak = when (lastActiveDate) {
                    todayStr -> {
                        // Already logged today — don't change streak
                        currentStreak
                    }
                    yesterdayStr -> {
                        // Logged yesterday — extend streak
                        currentStreak + 1
                    }
                    else -> {
                        // Missed a day or first time — reset to 1
                        1
                    }
                }

                // ── Only update if lastActiveDate is not today ──
                if (lastActiveDate != todayStr) {
                    db.collection("users").document(userId)
                        .update(
                            mapOf(
                                "currentStreak" to newStreak,
                                "lastActiveDate" to todayStr
                            )
                        )
                        .addOnSuccessListener { onDone() }
                        .addOnFailureListener { onDone() }
                } else {
                    onDone()
                }
            }
            .addOnFailureListener { onDone() }
    }

    // ── Call this on app start to break streak if user missed a day ──
    fun checkAndBreakStreakIfMissed(userId: String) {
        if (userId.isEmpty()) return

        val todayStr = today()
        val yesterdayStr = yesterday()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val lastActiveDate = doc.getString("lastActiveDate") ?: ""

                // If last active was not today or yesterday — streak is broken
                if (lastActiveDate.isNotEmpty() &&
                    lastActiveDate != todayStr &&
                    lastActiveDate != yesterdayStr
                ) {
                    db.collection("users").document(userId)
                        .update("currentStreak", 0)
                }
            }
    }
}