package com.sedexpensetracker.expensetracker.data.models

data class CategoryModel(
    var id: String = "",
    var name: String = "",
    var type: String = "EXPENSE",
    var iconName: String = "Category",
    var colorHex: Long = 0xFF607D8B,
    var isCustom: Boolean = false
)
