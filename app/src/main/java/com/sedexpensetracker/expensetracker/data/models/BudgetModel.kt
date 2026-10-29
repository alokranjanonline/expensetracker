package com.sedexpensetracker.expensetracker.data.models

data class BudgetModel(
    var id: String = "",
    var categoryId: String? = null,
    var amountMinorUnits: Long = 0L,
    var yearMonth: String = ""
)
