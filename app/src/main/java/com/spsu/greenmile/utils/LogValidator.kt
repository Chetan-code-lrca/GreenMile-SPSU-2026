package com.spsu.greenmile.utils

object LogValidator {
    fun validate(
        selectedTravel: String,
        selectedFood: String,
        electricityHours: String
    ): String? {
        if (selectedTravel.isEmpty()) return "Please select a travel mode"
        if (selectedFood.isEmpty()) return "Please select a food type"
        val hours = electricityHours.toDoubleOrNull()
        if (electricityHours.isNotEmpty() && (hours == null || hours < 0 || hours > 24)) {
            return "Electricity hours must be between 0 and 24"
        }
        return null // null means valid
    }
}