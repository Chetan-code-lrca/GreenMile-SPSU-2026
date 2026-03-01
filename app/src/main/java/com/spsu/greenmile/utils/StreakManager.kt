package com.spsu.greenmile

import com.google.firebase.firestore.FirebaseFirestore

object StreakManager {

    fun updateStreak(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val today = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val lastActiveDate = doc.getString("lastActiveDate") ?: ""
                val currentStreak = (doc.getLong("currentStreak") ?: 0).toInt()

                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val todayDate = sdf.parse(today)

                val newStreak = when {
                    lastActiveDate.isEmpty() -> 1
                    lastActiveDate == today -> currentStreak // already logged today
                    else -> {
                        val lastDate = try { sdf.parse(lastActiveDate) } catch (e: Exception) { null }
                        if (lastDate != null && todayDate != null) {
                            val diff = (todayDate.time - lastDate.time) / (1000 * 60 * 60 * 24)
                            when {
                                diff == 1L -> currentStreak + 1  // consecutive day
                                else -> 1                         // streak broken
                            }
                        } else 1
                    }
                }

                // Only update if not already logged today
                if (lastActiveDate != today) {
                    db.collection("users").document(userId).update(
                        mapOf(
                            "currentStreak" to newStreak,
                            "lastActiveDate" to today
                        )
                    )
                }
            }
    }
}