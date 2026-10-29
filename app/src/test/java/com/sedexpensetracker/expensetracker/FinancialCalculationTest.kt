package com.sedexpensetracker.expensetracker

import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculationTest {

    @Test
    fun testTotalBalanceCalculation() {
        val transactions = listOf(
            TransactionModel(id = "1", title = "Salary", amountMinorUnits = 500000, type = "INCOME", categoryId = "c1", categoryName = "Salary", categoryIcon = "Payments", categoryColor = 0, dateTimeEpochMillis = 100000),
            TransactionModel(id = "2", title = "Groceries", amountMinorUnits = 12000, type = "EXPENSE", categoryId = "c2", categoryName = "Food & Dining", categoryIcon = "Restaurant", categoryColor = 0, dateTimeEpochMillis = 100000),
            TransactionModel(id = "3", title = "Rent", amountMinorUnits = 150000, type = "EXPENSE", categoryId = "c3", categoryName = "Rent", categoryIcon = "Home", categoryColor = 0, dateTimeEpochMillis = 100000),
            TransactionModel(id = "4", title = "Freelance", amountMinorUnits = 80000, type = "INCOME", categoryId = "c4", categoryName = "Freelance", categoryIcon = "Work", categoryColor = 0, dateTimeEpochMillis = 100000)
        )

        var totalBalance = 0L
        for (tx in transactions) {
            if (tx.type == "INCOME") {
                totalBalance += tx.amountMinorUnits
            } else {
                totalBalance -= tx.amountMinorUnits
            }
        }

        // 5000.00 + 800.00 - 120.00 - 1500.00 = 4180.00 = 418000 minor units
        assertEquals(418000L, totalBalance)
    }

    @Test
    fun testRemainingBudgetCalculation() {
        val budgetLimitMinorUnits = 200000L // $2,000.00
        val spentExpenseMinorUnits = 145000L // $1,450.00

        val remainingBudget = budgetLimitMinorUnits - spentExpenseMinorUnits
        val usagePercentage = (spentExpenseMinorUnits.toFloat() / budgetLimitMinorUnits.toFloat()) * 100f

        assertEquals(55000L, remainingBudget)
        assertEquals(72.5f, usagePercentage, 0.01f)
    }

    @Test
    fun testCurrencyMinorUnitsFormatting() {
        val minorUnits = 1050L
        val formatted = CurrencyFormatter.formatAmount(minorUnits, "$")
        assertEquals("$10.50", formatted)

        val parsed = CurrencyFormatter.parseToMinorUnits("10.50")
        assertEquals(1050L, parsed)
    }
}
