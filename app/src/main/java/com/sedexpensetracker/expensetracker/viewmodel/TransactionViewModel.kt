package com.sedexpensetracker.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import com.sedexpensetracker.expensetracker.data.repository.SettingsRepository
import com.sedexpensetracker.expensetracker.data.repository.TransactionRepository
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption { NEWEST, OLDEST, HIGHEST_AMOUNT, LOWEST_AMOUNT }

data class GroupedTransactions(
    val dateString: String,
    val transactions: List<TransactionModel>
)

data class TransactionUiState(
    val searchQuery: String = "",
    val selectedTypeFilter: String = "EXPENSE", // Default "EXPENSE" -> Order: EXPENSE, INCOME, ALL
    val selectedCategoryFilter: String = "ALL",
    val selectedPaymentMethodFilter: String = "ALL",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val sortOption: SortOption = SortOption.NEWEST,
    val groupedTransactions: List<GroupedTransactions> = emptyList(),
    val totalCount: Int = 0,
    val selectedTransactionDetail: TransactionModel? = null,
    val currencySymbol: String = "$",
    val dateFormat: String = "MMM dd, yyyy",
    val isLoading: Boolean = false
)

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow("EXPENSE") // Expense first
    private val _selectedCategory = MutableStateFlow("ALL")
    private val _selectedPaymentMethod = MutableStateFlow("ALL")
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    private val _selectedDetail = MutableStateFlow<TransactionModel?>(null)

    val uiState: StateFlow<TransactionUiState> = combine(
        transactionRepository.allTransactions,
        settingsRepository.settings,
        combine(_searchQuery, _selectedType, _selectedCategory, _selectedPaymentMethod, ::FiveFilters),
        combine(_startDate, _endDate, _sortOption, _selectedDetail, ::FourFilters)
    ) { allTx, settings, five, four ->

        var filtered = allTx.asSequence()

        // Search Filter
        if (five.query.isNotBlank()) {
            val q = five.query.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                        it.categoryName.lowercase().contains(q) ||
                        (it.notes?.lowercase()?.contains(q) == true)
            }
        }

        // Type Filter
        if (five.type != "ALL") {
            filtered = filtered.filter { it.type == five.type }
        }

        // Category Filter
        if (five.category != "ALL") {
            filtered = filtered.filter { it.categoryName == five.category }
        }

        // Payment Method Filter
        if (five.paymentMethod != "ALL") {
            filtered = filtered.filter { it.paymentMethod == five.paymentMethod }
        }

        // Date Range Filter
        if (four.start != null) {
            filtered = filtered.filter { it.dateTimeEpochMillis >= four.start }
        }
        if (four.end != null) {
            filtered = filtered.filter { it.dateTimeEpochMillis <= four.end }
        }

        // Sorting
        val sortedList = when (four.sort) {
            SortOption.NEWEST -> filtered.sortedByDescending { it.dateTimeEpochMillis }
            SortOption.OLDEST -> filtered.sortedBy { it.dateTimeEpochMillis }
            SortOption.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amountMinorUnits }
            SortOption.LOWEST_AMOUNT -> filtered.sortedBy { it.amountMinorUnits }
        }.toList()

        // Group by Date String
        val groupedMap = sortedList.groupBy {
            CurrencyFormatter.formatDate(it.dateTimeEpochMillis, settings.dateFormat)
        }

        val groupedList = groupedMap.map { (dateStr, txList) ->
            GroupedTransactions(dateString = dateStr, transactions = txList)
        }

        TransactionUiState(
            searchQuery = five.query,
            selectedTypeFilter = five.type,
            selectedCategoryFilter = five.category,
            selectedPaymentMethodFilter = five.paymentMethod,
            startDateMillis = four.start,
            endDateMillis = four.end,
            sortOption = four.sort,
            groupedTransactions = groupedList,
            totalCount = sortedList.size,
            selectedTransactionDetail = four.detail,
            currencySymbol = settings.currencySymbol,
            dateFormat = settings.dateFormat,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTypeFilterChange(type: String) {
        _selectedType.value = type
    }

    fun onCategoryFilterChange(category: String) {
        _selectedCategory.value = category
    }

    fun onPaymentMethodFilterChange(paymentMethod: String) {
        _selectedPaymentMethod.value = paymentMethod
    }

    fun onDateRangeChange(startMillis: Long?, endMillis: Long?) {
        _startDate.value = startMillis
        _endDate.value = endMillis
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
    }

    fun selectTransactionDetail(tx: TransactionModel?) {
        _selectedDetail.value = tx
    }

    fun deleteTransaction(tx: TransactionModel) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(tx)
            if (_selectedDetail.value?.id == tx.id) {
                _selectedDetail.value = null
            }
        }
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedType.value = "EXPENSE"
        _selectedCategory.value = "ALL"
        _selectedPaymentMethod.value = "ALL"
        _startDate.value = null
        _endDate.value = null
        _sortOption.value = SortOption.NEWEST
    }
}

private data class FiveFilters(
    val query: String,
    val type: String,
    val category: String,
    val paymentMethod: String
)

private data class FourFilters(
    val start: Long?,
    val end: Long?,
    val sort: SortOption,
    val detail: TransactionModel?
)
