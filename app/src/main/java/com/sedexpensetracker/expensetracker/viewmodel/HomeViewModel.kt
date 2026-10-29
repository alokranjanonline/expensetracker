package com.sedexpensetracker.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import com.sedexpensetracker.expensetracker.data.repository.BudgetRepository
import com.sedexpensetracker.expensetracker.data.repository.SettingsRepository
import com.sedexpensetracker.expensetracker.data.repository.TransactionRepository
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategoryBreakdownItem(
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val totalAmountMinorUnits: Long,
    val percentage: Float
)

data class DailyChartPoint(
    val label: String,
    val amountMinorUnits: Long
)

data class HomeUiState(
    val totalBalanceMinorUnits: Long = 0,
    val monthlyIncomeMinorUnits: Long = 0,
    val monthlyExpenseMinorUnits: Long = 0,
    val monthlyBudgetLimitMinorUnits: Long = 0,
    val remainingMonthlyBudgetMinorUnits: Long = 0,
    val budgetUsagePercentage: Float = 0f,
    val recentTransactions: List<TransactionModel> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val dailyChartPoints: List<DailyChartPoint> = emptyList(),
    val currencySymbol: String = "$",
    val dateFormat: String = "MMM dd, yyyy",
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val currentYearMonth = CurrencyFormatter.getYearMonthString()
    private val startOfMonth = CurrencyFormatter.getStartOfMonthMillis()
    private val endOfMonth = CurrencyFormatter.getEndOfMonthMillis()

    val uiState: StateFlow<HomeUiState> = combine(
        transactionRepository.allTransactions,
        budgetRepository.getBudgetsForMonth(currentYearMonth),
        settingsRepository.settings
    ) { allTx, budgets, settings ->

        val overallBudget = budgets.find { it.categoryId.isNullOrBlank() || it.categoryId == "overall" }

        // Total Balance = total income - total expenses across all time
        var totalBalance = 0L
        for (tx in allTx) {
            if (tx.type == "INCOME") {
                totalBalance += tx.amountMinorUnits
            } else {
                totalBalance -= tx.amountMinorUnits
            }
        }

        // Current Month Income & Expenses
        val currentMonthTx = allTx.filter {
            it.dateTimeEpochMillis in startOfMonth..endOfMonth
        }

        var monthlyIncome = 0L
        var monthlyExpense = 0L
        val categorySumMap = mutableMapOf<String, Triple<String, Long, Long>>() // name -> (icon, color, total)

        for (tx in currentMonthTx) {
            if (tx.type == "INCOME") {
                monthlyIncome += tx.amountMinorUnits
            } else {
                monthlyExpense += tx.amountMinorUnits
                val existing = categorySumMap[tx.categoryName]
                val currentSum = existing?.third ?: 0L
                categorySumMap[tx.categoryName] = Triple(tx.categoryIcon, tx.categoryColor, currentSum + tx.amountMinorUnits)
            }
        }

        // Budget Calculations
        val budgetLimit = overallBudget?.amountMinorUnits ?: 0L
        val remainingBudget = if (budgetLimit > 0) budgetLimit - monthlyExpense else 0L
        val budgetPercentage = if (budgetLimit > 0) (monthlyExpense.toFloat() / budgetLimit.toFloat()) * 100f else 0f

        // Recent Transactions (top 5)
        val recentTx = allTx.take(5)

        // Category Breakdown
        val breakdownList = mutableListOf<CategoryBreakdownItem>()
        if (monthlyExpense > 0) {
            categorySumMap.forEach { (catName, triple) ->
                val pct = (triple.third.toFloat() / monthlyExpense.toFloat()) * 100f
                breakdownList.add(
                    CategoryBreakdownItem(
                        categoryName = catName,
                        categoryIcon = triple.first,
                        categoryColor = triple.second,
                        totalAmountMinorUnits = triple.third,
                        percentage = pct
                    )
                )
            }
            breakdownList.sortByDescending { it.totalAmountMinorUnits }
        }

        // Chart Points (Last 7 Days)
        val chartPoints = mutableListOf<DailyChartPoint>()
        val daySdf = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val label = daySdf.format(dayCal.time)

            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            val dayStart = dayCal.timeInMillis

            dayCal.set(Calendar.HOUR_OF_DAY, 23)
            dayCal.set(Calendar.MINUTE, 59)
            dayCal.set(Calendar.SECOND, 59)
            val dayEnd = dayCal.timeInMillis

            val dayExpenseSum = allTx.filter {
                it.type == "EXPENSE" && it.dateTimeEpochMillis in dayStart..dayEnd
            }.sumOf { it.amountMinorUnits }

            chartPoints.add(DailyChartPoint(label, dayExpenseSum))
        }

        HomeUiState(
            totalBalanceMinorUnits = totalBalance,
            monthlyIncomeMinorUnits = monthlyIncome,
            monthlyExpenseMinorUnits = monthlyExpense,
            monthlyBudgetLimitMinorUnits = budgetLimit,
            remainingMonthlyBudgetMinorUnits = remainingBudget,
            budgetUsagePercentage = budgetPercentage,
            recentTransactions = recentTx,
            categoryBreakdown = breakdownList,
            dailyChartPoints = chartPoints,
            currencySymbol = settings.currencySymbol,
            dateFormat = settings.dateFormat,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )
}
