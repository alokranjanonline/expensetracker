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
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val budgetRepository: BudgetRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        categoryRepository.allCategories
    ) { settings, categories ->

        val user = authRepository.currentUser
        val verified = user?.isEmailVerified == true

        SettingsUiState(
            settings = settings,
            categories = categories,
            userDisplayName = user?.displayName ?: "Personal Account",
            userEmail = user?.email ?: "local@expensetracker.app",
            isUserVerified = verified
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

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

    fun addCustomCategory(name: String, type: String, iconName: String, colorHex: Long) {
        viewModelScope.launch {
            val category = CategoryModel(
                name = name,
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                isCustom = true
            )
            categoryRepository.saveCategory(category)
        }
    }

    fun updateCategory(category: CategoryModel) {
        viewModelScope.launch {
            categoryRepository.saveCategory(category)
        }
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
                setMessage("Transactions exported to CSV successfully!")
            } else {
                setError("Failed to export CSV.")
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
                setMessage("Backup created successfully!")
            } else {
                setError("Failed to create backup file.")
            }
        }
    }

    fun restoreData(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = BackupRestoreManager.restoreFromUri(
                context, uri, transactionRepository, categoryRepository, budgetRepository, recurringRepository, settingsRepository
            )
            result.onSuccess {
                setMessage("Data restored successfully!")
            }.onFailure { error ->
                setError(error.localizedMessage ?: "Failed to restore data.")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            transactionRepository.clearAll()
            setMessage("All transaction data cleared.")
        }
    }

    fun setMessage(msg: String) {
        // Log or show toast
    }

    fun setError(err: String) {
        // Log or show toast
    }

    fun logout() {
        authRepository.logout()
    }
}
