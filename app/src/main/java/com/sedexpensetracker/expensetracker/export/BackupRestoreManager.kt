package com.sedexpensetracker.expensetracker.export

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sedexpensetracker.expensetracker.data.models.*
import com.sedexpensetracker.expensetracker.data.repository.*

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val transactions: List<TransactionModel> = emptyList(),
    val categories: List<CategoryModel> = emptyList(),
    val budgets: List<BudgetModel> = emptyList(),
    val recurringTransactions: List<RecurringTransactionModel> = emptyList(),
    val settings: AppSettingsModel? = null
)

object BackupRestoreManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun createBackupJson(
        transactions: List<TransactionModel>,
        categories: List<CategoryModel>,
        budgets: List<BudgetModel>,
        recurring: List<RecurringTransactionModel>,
        settings: AppSettingsModel
    ): String {
        val data = BackupData(
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            recurringTransactions = recurring,
            settings = settings
        )
        return gson.toJson(data)
    }

    suspend fun backupToUri(
        context: Context,
        uri: Uri,
        transactions: List<TransactionModel>,
        categories: List<CategoryModel>,
        budgets: List<BudgetModel>,
        recurring: List<RecurringTransactionModel>,
        settings: AppSettingsModel
    ): Boolean {
        return try {
            val json = createBackupJson(transactions, categories, budgets, recurring, settings)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreFromUri(
        context: Context,
        uri: Uri,
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        budgetRepository: BudgetRepository,
        recurringRepository: RecurringRepository,
        settingsRepository: SettingsRepository
    ): Result<Unit> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return Result.failure(Exception("Failed to read backup file."))

            val backupData = gson.fromJson(jsonString, BackupData::class.java)
                ?: return Result.failure(Exception("Invalid backup format."))

            for (category in backupData.categories) {
                categoryRepository.saveCategory(category)
            }
            for (tx in backupData.transactions) {
                transactionRepository.saveTransaction(tx)
            }
            for (budget in backupData.budgets) {
                budgetRepository.saveBudget(budget)
            }
            for (rec in backupData.recurringTransactions) {
                recurringRepository.saveRecurringTransaction(rec)
            }
            backupData.settings?.let {
                settingsRepository.saveSettings(it)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
