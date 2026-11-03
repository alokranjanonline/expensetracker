package com.sedexpensetracker.expensetracker.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sedexpensetracker.expensetracker.data.models.AppSettingsModel
import com.sedexpensetracker.expensetracker.data.models.CategoryModel
import com.sedexpensetracker.expensetracker.data.repository.*
import com.sedexpensetracker.expensetracker.export.BackupRestoreManager
import com.sedexpensetracker.expensetracker.export.CsvExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettingsModel = AppSettingsModel(),
    val categories: List<CategoryModel> = emptyList(),
    val userDisplayName: String = "",
    val userEmail: String = "",
    val isUserVerified: Boolean = false,
    val isExporting: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val actionResultMessage: String? = null,
    val isActionSuccess: Boolean = true
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val budgetRepository: BudgetRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _infoMessage = MutableStateFlow<String?>(null)
    private val _actionResultMessage = MutableStateFlow<String?>(null)
    private val _isActionSuccess = MutableStateFlow(true)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        categoryRepository.allCategories,
        _infoMessage,
        _errorMessage
    ) { settings, categories, info, err ->

        val user = authRepository.currentUser
        val verified = user?.isEmailVerified == true

        SettingsUiState(
            settings = settings,
            categories = categories,
            userDisplayName = user?.displayName ?: "Personal Account",
            userEmail = user?.email ?: "local@expensetracker.app",
            isUserVerified = verified,
            infoMessage = info,
            errorMessage = err,
            actionResultMessage = _actionResultMessage.value,
            isActionSuccess = _isActionSuccess.value
        )
    }.combine(_actionResultMessage) { state, actionMsg ->
        state.copy(actionResultMessage = actionMsg)
    }.combine(_isActionSuccess) { state, isSuccess ->
        state.copy(isActionSuccess = isSuccess)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearActionResultMessage() {
        _actionResultMessage.value = null
    }

    fun onThemeChange(themeMode: String) {
        viewModelScope.launch {
            val current = uiState.value.settings
            settingsRepository.saveSettings(current.copy(themeMode = themeMode))
        }
    }

    fun onCurrencyChange(symbol: String, code: String) {
        viewModelScope.launch {
            val current = uiState.value.settings
            settingsRepository.saveSettings(current.copy(currencySymbol = symbol, currencyCode = code))
        }
    }

    fun onDateFormatChange(dateFormatPattern: String) {
        viewModelScope.launch {
            val current = uiState.value.settings
            settingsRepository.saveSettings(current.copy(dateFormat = dateFormatPattern))
        }
    }

    fun onToggleDailyReminder(enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.settings
            settingsRepository.saveSettings(current.copy(dailyReminderEnabled = enabled))
        }
    }

    fun onToggleBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.settings
            settingsRepository.saveSettings(current.copy(budgetAlertsEnabled = enabled))
        }
    }

    fun onToggleRecurringAlerts(enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.settings
            settingsRepository.saveSettings(current.copy(recurringAlertsEnabled = enabled))
        }
    }

    fun addCustomCategory(name: String, type: String, iconName: String, colorHex: Long): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _errorMessage.value = "Category name cannot be blank."
            return false
        }

        val existing = uiState.value.categories.any {
            it.type.equals(type, ignoreCase = true) && it.name.trim().equals(trimmed, ignoreCase = true)
        }

        if (existing) {
            _errorMessage.value = "A category named '$trimmed' already exists for $type."
            return false
        }

        _errorMessage.value = null
        viewModelScope.launch {
            val category = CategoryModel(
                name = trimmed,
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                isCustom = true
            )
            categoryRepository.saveCategory(category)
        }
        return true
    }

    fun updateCategory(category: CategoryModel): Boolean {
        val trimmed = category.name.trim()
        if (trimmed.isBlank()) {
            _errorMessage.value = "Category name cannot be blank."
            return false
        }

        val existing = uiState.value.categories.any {
            it.id != category.id && it.type.equals(category.type, ignoreCase = true) && it.name.trim().equals(trimmed, ignoreCase = true)
        }

        if (existing) {
            _errorMessage.value = "A category named '$trimmed' already exists for ${category.type}."
            return false
        }

        _errorMessage.value = null
        viewModelScope.launch {
            categoryRepository.saveCategory(category.copy(name = trimmed))
        }
        return true
    }

    fun deleteCategory(category: CategoryModel) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun exportToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val transactions = transactionRepository.allTransactions.first()
            val success = CsvExporter.writeCsvToUri(
                context, uri, transactions,
                uiState.value.settings.currencySymbol,
                uiState.value.settings.dateFormat
            )
            if (success) {
                _isActionSuccess.value = true
                _actionResultMessage.value = "Transactions exported to CSV successfully!"
            } else {
                _isActionSuccess.value = false
                _actionResultMessage.value = "Failed to export transactions to CSV."
            }
        }
    }

    fun backupData(context: Context, uri: Uri) {
        viewModelScope.launch {
            val transactions = transactionRepository.allTransactions.first()
            val categories = categoryRepository.allCategories.first()
            val budgets = budgetRepository.getBudgetsForMonth("").first()
            val recurring = recurringRepository.allRecurringTransactions.first()
            val settings = settingsRepository.settings.first()

            val success = BackupRestoreManager.backupToUri(
                context, uri, transactions, categories, budgets, recurring, settings
            )
            if (success) {
                _isActionSuccess.value = true
                _actionResultMessage.value = "Backup file created successfully!"
            } else {
                _isActionSuccess.value = false
                _actionResultMessage.value = "Failed to create backup file."
            }
        }
    }

    fun restoreData(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = BackupRestoreManager.restoreFromUri(
                context, uri, transactionRepository, categoryRepository, budgetRepository, recurringRepository, settingsRepository
            )
            result.onSuccess {
                _isActionSuccess.value = true
                _actionResultMessage.value = "Data restored successfully from backup file!"
            }.onFailure { error ->
                _isActionSuccess.value = false
                _actionResultMessage.value = "Failed to restore data: ${error.localizedMessage}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            transactionRepository.clearAll()
            _isActionSuccess.value = true
            _actionResultMessage.value = "All saved transaction records have been cleared."
        }
    }

    fun deleteAccountData(onComplete: () -> Unit) {
        viewModelScope.launch {
            transactionRepository.clearAll()
            val userCategories = categoryRepository.allCategories.first().filter { it.isCustom }
            for (cat in userCategories) {
                categoryRepository.deleteCategory(cat)
            }
            val userBudgets = budgetRepository.getBudgetsForMonth("").first()
            for (b in userBudgets) {
                budgetRepository.deleteBudget(b)
            }
            val recurring = recurringRepository.allRecurringTransactions.first()
            for (r in recurring) {
                recurringRepository.deleteRecurringTransaction(r)
            }
            _isActionSuccess.value = true
            _actionResultMessage.value = "Your account data has been wiped successfully."
            onComplete()
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
