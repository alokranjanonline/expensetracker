package com.sedexpensetracker.expensetracker.data.models

data class RecurringTransactionModel(
    var id: String = "",
    var title: String = "",
    var amountMinorUnits: Long = 0L,
    var type: String = "EXPENSE",
    var categoryId: String = "",
    var categoryName: String = "",
    var categoryIcon: String = "Restaurant",
    var categoryColor: Long = 0xFFFF5722,
    var interval: String = "MONTHLY",
    var nextDueDateEpochMillis: Long = System.currentTimeMillis(),
    var notes: String? = null,
    var paymentMethod: String? = "Cash"
)
