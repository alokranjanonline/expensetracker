package com.sedexpensetracker.expensetracker.data.models

data class AppSettingsModel(
    var currencySymbol: String = "$",
    var currencyCode: String = "USD",
    var dateFormat: String = "MMM dd, yyyy",
    var themeMode: String = "SYSTEM",
    var dailyReminderEnabled: Boolean = true,
    var budgetAlertsEnabled: Boolean = true,
    var recurringAlertsEnabled: Boolean = true,
    var reminderHour: Int = 20,
    var reminderMinute: Int = 0
)
