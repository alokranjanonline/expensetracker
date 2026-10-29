package com.sedexpensetracker.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sedexpensetracker.expensetracker.data.models.BudgetModel
import com.sedexpensetracker.expensetracker.data.models.CategoryModel
import com.sedexpensetracker.expensetracker.data.repository.BudgetRepository
import com.sedexpensetracker.expensetracker.data.repository.CategoryRepository
import com.sedexpensetracker.expensetracker.data.repository.SettingsRepository
import com.sedexpensetracker.expensetracker.data.repository.TransactionRepository
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BudgetStatus { NORMAL, WARNING, EXCEEDED }

data class CategoryBudgetUiModel(
    val category: CategoryModel,
    val budgetAmountMinorUnits: Long,
    val spentAmountMinorUnits: Long,
    val remainingMinorUnits: Long,
    val usagePercentage: Float,
    val status: BudgetStatus
)

data class BudgetUiState(
    val yearMonth: String = CurrencyFormatter.getYearMonthString(),
    val overallBudgetAmountMinorUnits: Long = 0,
    val overallSpentAmountMinorUnits: Long = 0,
    val overallRemainingMinorUnits: Long = 0,
    val overallUsagePercentage: Float = 0f,
    val overallStatus: BudgetStatus = BudgetStatus.NORMAL,
    val categoryBudgets: List<CategoryBudgetUiModel> = emptyList(),
    val expenseCategories: List<CategoryModel> = emptyList(),
    val currencySymbol: String = "$",
    val dateFormat: String = "MMM dd, yyyy",
    val isLoading: Boolean = false
)

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val currentYearMonth = CurrencyFormatter.getYearMonthString()
    private val startOfMonth = CurrencyFormatter.getStartOfMonthMillis()
    private val endOfMonth = CurrencyFormatter.getEndOfMonthMillis()

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRepository.getBudgetsForMonth(currentYearMonth),
        transactionRepository.allTransactions,
        categoryRepository.allCategories,
        settingsRepository.settings
    ) { budgets, allTx, allCategories, settings ->

        val expenseCategories = allCategories.filter { it.type == "EXPENSE" }

        val monthExpenses = allTx.filter {
            it.type == "EXPENSE" && it.dateTimeEpochMillis in startOfMonth..endOfMonth
        }

        val totalSpent = monthExpenses.sumOf { it.amountMinorUnits }
        val overallBudgetEntity = budgets.find { it.categoryId.isNullOrBlank() || it.categoryId == "overall" }
        val overallLimit = overallBudgetEntity?.amountMinorUnits ?: 0L
        val overallRemaining = if (overallLimit > 0) overallLimit - totalSpent else 0L
        val overallPct = if (overallLimit > 0) (totalSpent.toFloat() / overallLimit.toFloat()) * 100f else 0f
        val overallStat = when {
            overallPct >= 100f -> BudgetStatus.EXCEEDED
            overallPct >= 80f -> BudgetStatus.WARNING
            else -> BudgetStatus.NORMAL
        }

        // Category Budgets
        val categoryBudgetModels = mutableListOf<CategoryBudgetUiModel>()
        val categorySpentMap = monthExpenses.groupBy { it.categoryId }

        for (category in expenseCategories) {
            val budgetEnt = budgets.find { it.categoryId == category.id }
            val limit = budgetEnt?.amountMinorUnits ?: 0L
            val spent = categorySpentMap[category.id]?.sumOf { it.amountMinorUnits } ?: 0L
            val rem = if (limit > 0) limit - spent else 0L
            val pct = if (limit > 0) (spent.toFloat() / limit.toFloat()) * 100f else 0f
            val status = when {
                pct >= 100f -> BudgetStatus.EXCEEDED
                pct >= 80f -> BudgetStatus.WARNING
                else -> BudgetStatus.NORMAL
            }

            categoryBudgetModels.add(
                CategoryBudgetUiModel(
                    category = category,
                    budgetAmountMinorUnits = limit,
                    spentAmountMinorUnits = spent,
                    remainingMinorUnits = rem,
                    usagePercentage = pct,
                    status = status
                )
            )
        }

        BudgetUiState(
            yearMonth = currentYearMonth,
            overallBudgetAmountMinorUnits = overallLimit,
            overallSpentAmountMinorUnits = totalSpent,
            overallRemainingMinorUnits = overallRemaining,
            overallUsagePercentage = overallPct,
            overallStatus = overallStat,
            categoryBudgets = categoryBudgetModels,
            expenseCategories = expenseCategories,
            currencySymbol = settings.currencySymbol,
            dateFormat = settings.dateFormat,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(isLoading = true)
    )

    fun setOverallBudget(amountString: String) {
        val amountMinorUnits = CurrencyFormatter.parseToMinorUnits(amountString)
        viewModelScope.launch {
            val budget = BudgetModel(
                categoryId = "overall",
                amountMinorUnits = amountMinorUnits,
                yearMonth = currentYearMonth
            )
            budgetRepository.saveBudget(budget)
        }
    }

    fun setCategoryBudget(categoryId: String, amountString: String) {
        val amountMinorUnits = CurrencyFormatter.parseToMinorUnits(amountString)
        viewModelScope.launch {
            val budget = BudgetModel(
                categoryId = categoryId,
                amountMinorUnits = amountMinorUnits,
                yearMonth = currentYearMonth
            )
            budgetRepository.saveBudget(budget)
        }
    }
}
