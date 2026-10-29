package com.sedexpensetracker.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sedexpensetracker.expensetracker.data.repository.SettingsRepository
import com.sedexpensetracker.expensetracker.data.repository.TransactionRepository
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ReportPeriod { DAILY, WEEKLY, MONTHLY, THIS_YEAR, THIS_FY, CUSTOM }

data class PieSliceData(
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val amountMinorUnits: Long,
    val percentage: Float
)

data class BarChartDataPoint(
    val label: String,
    val incomeMinorUnits: Long,
    val expenseMinorUnits: Long
)

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTHLY,
    val customStartMillis: Long = CurrencyFormatter.getStartOfMonthMillis(),
    val customEndMillis: Long = CurrencyFormatter.getEndOfMonthMillis(),
    val totalIncomeMinorUnits: Long = 0,
    val totalExpenseMinorUnits: Long = 0,
    val netSavingsMinorUnits: Long = 0,
    val dailyAverageExpenseMinorUnits: Long = 0,
    val pieSlices: List<PieSliceData> = emptyList(),
    val barChartPoints: List<BarChartDataPoint> = emptyList(),
    val topSpendingCategories: List<PieSliceData> = emptyList(),
    val currencySymbol: String = "$",
    val dateFormat: String = "MMM dd, yyyy",
    val isLoading: Boolean = false
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    private val _customStart = MutableStateFlow(CurrencyFormatter.getStartOfMonthMillis())
    private val _customEnd = MutableStateFlow(CurrencyFormatter.getEndOfMonthMillis())

    val uiState: StateFlow<ReportsUiState> = combine(
        transactionRepository.allTransactions,
        settingsRepository.settings,
        combine(_selectedPeriod, _customStart, _customEnd, ::PeriodParams)
    ) { allTx, settings, params ->

        val now = System.currentTimeMillis()
        val (startMillis, endMillis) = when (params.period) {
            ReportPeriod.DAILY -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Pair(start, cal.timeInMillis)
            }
            ReportPeriod.WEEKLY -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.MONTHLY -> {
                Pair(CurrencyFormatter.getStartOfMonthMillis(now), CurrencyFormatter.getEndOfMonthMillis(now))
            }
            ReportPeriod.THIS_YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis

                val endCal = Calendar.getInstance()
                endCal.set(Calendar.MONTH, Calendar.DECEMBER)
                endCal.set(Calendar.DAY_OF_MONTH, 31)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                Pair(start, endCal.timeInMillis)
            }
            ReportPeriod.THIS_FY -> {
                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH)
                val fyStartYear = if (currentMonth >= Calendar.APRIL) cal.get(Calendar.YEAR) else cal.get(Calendar.YEAR) - 1

                val startCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, fyStartYear)
                    set(Calendar.MONTH, Calendar.APRIL)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }

                val endCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, fyStartYear + 1)
                    set(Calendar.MONTH, Calendar.MARCH)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            ReportPeriod.CUSTOM -> {
                Pair(params.start, params.end)
            }
        }

        val filteredTx = allTx.filter { it.dateTimeEpochMillis in startMillis..endMillis }

        var totalIncome = 0L
        var totalExpense = 0L
        val categoryExpenseMap = mutableMapOf<String, Triple<String, Long, Long>>()

        for (tx in filteredTx) {
            if (tx.type == "INCOME") {
                totalIncome += tx.amountMinorUnits
            } else {
                totalExpense += tx.amountMinorUnits
                val existing = categoryExpenseMap[tx.categoryName]
                val sum = (existing?.third ?: 0L) + tx.amountMinorUnits
                categoryExpenseMap[tx.categoryName] = Triple(tx.categoryIcon, tx.categoryColor, sum)
            }
        }

        val dayDiff = ((endMillis - startMillis) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        val dailyAvg = totalExpense / dayDiff

        val slices = mutableListOf<PieSliceData>()
        if (totalExpense > 0) {
            categoryExpenseMap.forEach { (catName, triple) ->
                val pct = (triple.third.toFloat() / totalExpense.toFloat()) * 100f
                slices.add(
                    PieSliceData(
                        categoryName = catName,
                        categoryIcon = triple.first,
                        categoryColor = triple.second,
                        amountMinorUnits = triple.third,
                        percentage = pct
                    )
                )
            }
            slices.sortByDescending { it.amountMinorUnits }
        }

        val barPoints = mutableListOf<BarChartDataPoint>()
        val cal = Calendar.getInstance()
        val labelSdf = SimpleDateFormat("MMM dd", Locale.getDefault())

        val intervalStep = ((endMillis - startMillis) / 6).coerceAtLeast(1)
        for (i in 0 until 6) {
            val segStart = startMillis + (i * intervalStep)
            val segEnd = (segStart + intervalStep).coerceAtMost(endMillis)

            cal.timeInMillis = segStart
            val label = labelSdf.format(cal.time)

            val segIncome = filteredTx.filter {
                it.type == "INCOME" && it.dateTimeEpochMillis in segStart..segEnd
            }.sumOf { it.amountMinorUnits }

            val segExpense = filteredTx.filter {
                it.type == "EXPENSE" && it.dateTimeEpochMillis in segStart..segEnd
            }.sumOf { it.amountMinorUnits }

            barPoints.add(BarChartDataPoint(label, segIncome, segExpense))
        }

        ReportsUiState(
            selectedPeriod = params.period,
            customStartMillis = startMillis,
            customEndMillis = endMillis,
            totalIncomeMinorUnits = totalIncome,
            totalExpenseMinorUnits = totalExpense,
            netSavingsMinorUnits = totalIncome - totalExpense,
            dailyAverageExpenseMinorUnits = dailyAvg,
            pieSlices = slices,
            barChartPoints = barPoints,
            topSpendingCategories = slices.take(5),
            currencySymbol = settings.currencySymbol,
            dateFormat = settings.dateFormat,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun onPeriodChange(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun onCustomDateRangeChange(startMillis: Long, endMillis: Long) {
        _selectedPeriod.value = ReportPeriod.CUSTOM
        _customStart.value = startMillis
        _customEnd.value = endMillis
    }
}

private data class PeriodParams(
    val period: ReportPeriod,
    val start: Long,
    val end: Long
)
