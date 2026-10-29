package com.sedexpensetracker.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sedexpensetracker.expensetracker.data.models.CategoryModel
import com.sedexpensetracker.expensetracker.data.models.RecurringTransactionModel
import com.sedexpensetracker.expensetracker.data.models.TransactionModel
import com.sedexpensetracker.expensetracker.data.repository.CategoryRepository
import com.sedexpensetracker.expensetracker.data.repository.RecurringRepository
import com.sedexpensetracker.expensetracker.data.repository.SettingsRepository
import com.sedexpensetracker.expensetracker.data.repository.TransactionRepository
import com.sedexpensetracker.expensetracker.utilities.CurrencyFormatter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class FocusTarget { NONE, AMOUNT, TITLE }

data class AddEditTransactionUiState(
    val transactionId: String? = null,
    val title: String = "",
    val amountText: String = "",
    val type: String = "EXPENSE",
    val selectedCategory: CategoryModel? = null,
    val dateTimeEpochMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val paymentMethod: String = "Cash",
    val isRecurring: Boolean = false,
    val recurrenceInterval: String = "MONTHLY",
    val categories: List<CategoryModel> = emptyList(),
    val currencySymbol: String = "$",
    val dateFormat: String = "MMM dd, yyyy",
    val errorMessage: String? = null,
    val focusTarget: FocusTarget = FocusTarget.NONE,
    val saveSuccess: Boolean = false,
    val isLoading: Boolean = false
)

class AddEditTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _transactionType = MutableStateFlow("EXPENSE")
    private val _selectedCategoryId = MutableStateFlow<String?>(null)

    private val _title = MutableStateFlow("")
    private val _amountText = MutableStateFlow("")
    private val _dateTime = MutableStateFlow(System.currentTimeMillis())
    private val _notes = MutableStateFlow("")
    private val _paymentMethod = MutableStateFlow("Cash")
    private val _isRecurring = MutableStateFlow(false)
    private val _recurrenceInterval = MutableStateFlow("MONTHLY")
    private val _transactionId = MutableStateFlow<String?>(null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _focusTarget = MutableStateFlow(FocusTarget.NONE)
    private val _saveSuccess = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<AddEditTransactionUiState> = combine(
        categoryRepository.allCategories,
        settingsRepository.settings,
        combine(_transactionType, _selectedCategoryId, _title, _amountText, ::InputsPart1),
        combine(_dateTime, _notes, _paymentMethod, _isRecurring, ::InputsPart2),
        combine(_recurrenceInterval, _errorMessage, _focusTarget, _saveSuccess, _isLoading, ::InputsPart3)
    ) { allCategories, settings, p1, p2, p3 ->
        val filtered = allCategories.filter { it.type == p1.type }
        val selCat = if (!p1.selCatId.isNullOrBlank()) {
            filtered.find { it.id == p1.selCatId } ?: filtered.firstOrNull()
        } else {
            filtered.firstOrNull()
        }

        AddEditTransactionUiState(
            transactionId = _transactionId.value,
            title = p1.title,
            amountText = p1.amountText,
            type = p1.type,
            selectedCategory = selCat,
            dateTimeEpochMillis = p2.dateTime,
            notes = p2.notes,
            paymentMethod = p2.paymentMethod,
            isRecurring = p2.isRecurring,
            recurrenceInterval = p3.recurrenceInterval,
            categories = filtered,
            currencySymbol = settings.currencySymbol,
            dateFormat = settings.dateFormat,
            errorMessage = p3.errorMessage,
            focusTarget = p3.focusTarget,
            saveSuccess = p3.saveSuccess,
            isLoading = p3.isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditTransactionUiState()
    )

    fun loadExistingTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.allTransactions.first().find { it.id == transactionId }?.let { tx ->
                val amountDouble = tx.amountMinorUnits / 100.0
                _transactionId.value = tx.id
                _title.value = tx.title
                _amountText.value = String.format(Locale.US, "%.2f", amountDouble)
                _transactionType.value = tx.type
                _selectedCategoryId.value = tx.categoryId
                _dateTime.value = tx.dateTimeEpochMillis
                _notes.value = tx.notes ?: ""
                _paymentMethod.value = tx.paymentMethod ?: "Cash"
                _isRecurring.value = tx.isRecurring
                _recurrenceInterval.value = tx.recurrenceInterval ?: "MONTHLY"
            }
        }
    }

    fun stepDateByDays(days: Int) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _dateTime.value
        calendar.add(Calendar.DAY_OF_YEAR, days)
        _dateTime.value = calendar.timeInMillis
    }

    fun onTypeChange(type: String) {
        _transactionType.value = type
        _selectedCategoryId.value = null
    }

    fun onTitleChange(title: String) {
        _title.value = title
        _errorMessage.value = null
        _focusTarget.value = FocusTarget.NONE
    }

    fun onAmountChange(amount: String) {
        _amountText.value = amount
        _errorMessage.value = null
        _focusTarget.value = FocusTarget.NONE
    }

    fun onCategoryChange(category: CategoryModel) {
        _selectedCategoryId.value = category.id
        _errorMessage.value = null
        _focusTarget.value = FocusTarget.NONE
    }

    fun onDateTimeChange(millis: Long) {
        _dateTime.value = millis
    }

    fun onNotesChange(notes: String) {
        _notes.value = notes
    }

    fun onPaymentMethodChange(paymentMethod: String) {
        _paymentMethod.value = paymentMethod
    }

    fun onRecurringChange(isRecurring: Boolean) {
        _isRecurring.value = isRecurring
    }

    fun onRecurrenceIntervalChange(interval: String) {
        _recurrenceInterval.value = interval
    }

    fun saveTransaction() {
        val state = uiState.value
        val title = state.title.trim()
        val amountMinorUnits = CurrencyFormatter.parseToMinorUnits(state.amountText)
        val category = state.selectedCategory

        // Validate Amount FIRST - Focus on Amount box
        if (amountMinorUnits <= 0) {
            _focusTarget.value = FocusTarget.AMOUNT
            _errorMessage.value = "Please enter a valid amount."
            return
        }

        // Validate Title SECOND - Focus on Title box
        if (title.isBlank()) {
            _focusTarget.value = FocusTarget.TITLE
            _errorMessage.value = "Please enter a transaction title."
            return
        }

        // Validate Category THIRD
        if (category == null) {
            _focusTarget.value = FocusTarget.NONE
            _errorMessage.value = "Please select a category."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _focusTarget.value = FocusTarget.NONE

        val tx = TransactionModel(
            id = state.transactionId ?: "",
            title = title,
            amountMinorUnits = amountMinorUnits,
            type = state.type,
            categoryId = category.id,
            categoryName = category.name,
            categoryIcon = category.iconName,
            categoryColor = category.colorHex,
            dateTimeEpochMillis = state.dateTimeEpochMillis,
            notes = state.notes.ifBlank { null },
            paymentMethod = state.paymentMethod,
            isRecurring = state.isRecurring,
            recurrenceInterval = if (state.isRecurring) state.recurrenceInterval else null
        )

        transactionRepository.saveTransaction(tx) { success, errorMsg ->
            _isLoading.value = false
            if (success) {
                if (state.isRecurring) {
                    val recurring = RecurringTransactionModel(
                        title = title,
                        amountMinorUnits = amountMinorUnits,
                        type = state.type,
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryIcon = category.iconName,
                        categoryColor = category.colorHex,
                        interval = state.recurrenceInterval,
                        nextDueDateEpochMillis = state.dateTimeEpochMillis,
                        notes = state.notes.ifBlank { null },
                        paymentMethod = state.paymentMethod
                    )
                    recurringRepository.saveRecurringTransaction(recurring)
                }
                _saveSuccess.value = true
            } else {
                _errorMessage.value = errorMsg ?: "Failed to save transaction."
            }
        }
    }
}

private data class InputsPart1(
    val type: String,
    val selCatId: String?,
    val title: String,
    val amountText: String
)

private data class InputsPart2(
    val dateTime: Long,
    val notes: String,
    val paymentMethod: String,
    val isRecurring: Boolean
)

private data class InputsPart3(
    val recurrenceInterval: String,
    val errorMessage: String?,
    val focusTarget: FocusTarget,
    val saveSuccess: Boolean,
    val isLoading: Boolean
)
