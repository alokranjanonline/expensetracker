package com.sedexpensetracker.expensetracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sedexpensetracker.expensetracker.data.repository.*

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val authRepository by lazy { AuthRepository() }
    private val transactionRepository by lazy { TransactionRepository() }
    private val categoryRepository by lazy { CategoryRepository() }
    private val budgetRepository by lazy { BudgetRepository() }
    private val recurringRepository by lazy { RecurringRepository(transactionRepository) }
    private val settingsRepository by lazy { SettingsRepository() }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(transactionRepository, budgetRepository, settingsRepository) as T
            }
            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> {
                TransactionViewModel(transactionRepository, settingsRepository) as T
            }
            modelClass.isAssignableFrom(AddEditTransactionViewModel::class.java) -> {
                AddEditTransactionViewModel(transactionRepository, categoryRepository, recurringRepository, settingsRepository) as T
            }
            modelClass.isAssignableFrom(BudgetViewModel::class.java) -> {
                BudgetViewModel(budgetRepository, transactionRepository, categoryRepository, settingsRepository) as T
            }
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> {
                ReportsViewModel(transactionRepository, settingsRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(settingsRepository, categoryRepository, transactionRepository, recurringRepository, budgetRepository, authRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
