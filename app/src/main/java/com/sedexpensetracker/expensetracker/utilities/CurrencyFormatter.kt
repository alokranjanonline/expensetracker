package com.sedexpensetracker.expensetracker.utilities

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object CurrencyFormatter {

    fun formatAmount(minorUnits: Long, currencySymbol: String = "$"): String {
        val amount = minorUnits / 100.0
        val symbol = if (currencySymbol.isBlank()) "$" else currencySymbol
        return try {
            val formatted = String.format(Locale.US, "%.2f", amount)
            "$symbol$formatted"
        } catch (e: Exception) {
            "$symbol${String.format(Locale.US, "%.2f", amount)}"
        }
    }

    fun parseToMinorUnits(amountString: String): Long {
        val cleanString = amountString.replace("[^\\d.]".toRegex(), "")
        if (cleanString.isBlank()) return 0L
        val doubleVal = cleanString.toDoubleOrNull() ?: 0.0
        return (doubleVal * 100).toLong()
    }

    fun formatDate(epochMillis: Long, pattern: String = "MMM dd, yyyy"): String {
        val safePattern = if (pattern.isBlank()) "MMM dd, yyyy" else pattern
        return try {
            val sdf = SimpleDateFormat(safePattern, Locale.getDefault())
            sdf.format(Date(epochMillis))
        } catch (e: Exception) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(epochMillis))
        }
    }

    fun formatTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }

    fun formatDateWithTime(epochMillis: Long, pattern: String = "MMM dd, yyyy"): String {
        val datePart = formatDate(epochMillis, pattern)
        val timePart = formatTime(epochMillis)
        return "$datePart - $timePart"
    }

    fun getYearMonthString(epochMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }

    fun getStartOfMonthMillis(epochMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfMonthMillis(epochMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
