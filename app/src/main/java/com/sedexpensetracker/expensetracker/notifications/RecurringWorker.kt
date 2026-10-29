package com.sedexpensetracker.expensetracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sedexpensetracker.expensetracker.data.repository.RecurringRepository

class RecurringWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = RecurringRepository()
            repository.processDueRecurringTransactions()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
