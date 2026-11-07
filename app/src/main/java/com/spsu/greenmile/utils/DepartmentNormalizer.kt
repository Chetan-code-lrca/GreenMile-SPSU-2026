package com.spsu.greenmile.utils

import java.util.Locale

/**
 * Maps recognized department aliases to stable leaderboard labels.
 *
 * Matching uses complete aliases or full phrases. Substring matching on short
 * codes is deliberately avoided because it can misclassify names such as
 * "Physics" as CSE or "Management" as Mechanical Engineering.
 */
object DepartmentNormalizer {
    fun normalize(input: String): String {
        val original = input.trim()
        if (original.isEmpty()) return ""

        val normalized = original
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

        return when {
            normalized == "cse" ||
                normalized == "cs" ||
                normalized.startsWith("computer science") ||
                normalized == "computer engineering" -> "CSE"

            normalized == "ece" ||
                normalized.contains("electronics and communication") -> "ECE"

            normalized == "me" ||
                normalized.contains("mechanical engineering") ||
                normalized == "mechanical" -> "ME"

            normalized == "ce" ||
                normalized.contains("civil engineering") ||
                normalized == "civil" -> "CE"

            normalized == "it" ||
                normalized.contains("information technology") -> "IT"

            normalized == "eee" ||
                normalized.contains("electrical and electronics") -> "EEE"

            normalized == "mca" ||
                normalized.contains("master of computer applications") -> "MCA"

            normalized == "mba" ||
                normalized.contains("master of business administration") -> "MBA"

            else -> original.uppercase(Locale.ROOT)
        }
    }
}
