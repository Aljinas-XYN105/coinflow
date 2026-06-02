package com.example.core.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.db.AppDatabase
import com.example.core.db.CoinflowRepository
import com.example.core.db.RecurringFrequency
import com.example.core.db.RecurringRule
import com.example.core.db.Transaction
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = CoinflowRepository(
                categoryDao = db.categoryDao(),
                walletDao = db.walletDao(),
                transactionDao = db.transactionDao(),
                budgetDao = db.budgetDao(),
                recurringRuleDao = db.recurringRuleDao()
            )
            RecurringScheduler.checkAndMaterializeRecurringRules(repository)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

object RecurringScheduler {

    fun schedulePeriodicWork(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            12, TimeUnit.HOURS // checks twice a day
        ).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "recurring_transactions_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    suspend fun checkAndMaterializeRecurringRules(
        repository: CoinflowRepository,
        now: Long = System.currentTimeMillis()
    ) {
        val activeRules = repository.recurringRuleDao.getActiveRulesSync()
        
        for (rule in activeRules) {
            if (rule.nextRunDate > now) {
                continue
            }

            // Exclude already materialized occurrences to be idempotent
            val existingTxList = repository.transactionDao.getTransactionsByRuleIdSync(rule.id)
            val existingOccuredDates = existingTxList.map { it.occurredAt }.toSet()

            var currentNext = rule.nextRunDate
            var tempLast = rule.lastRunDate
            val newTransactions = mutableListOf<Transaction>()

            val interval = if (rule.interval <= 0) 1 else rule.interval

            while (currentNext <= now) {
                // If we exceed our endDate, we stop.
                if (rule.endDate != null && currentNext > rule.endDate) {
                    break
                }

                // If not already materialized (idempotent check), create transaction
                if (!existingOccuredDates.contains(currentNext)) {
                    newTransactions.add(
                        Transaction(
                            walletId = rule.walletId,
                            categoryId = rule.categoryId,
                            amountMinor = rule.amountMinor,
                            type = rule.type,
                            note = rule.note ?: "Recurring transaction",
                            occurredAt = currentNext,
                            recurringRuleId = rule.id
                        )
                    )
                }

                tempLast = currentNext
                currentNext = calculateNextOccurrence(currentNext, rule.freq, interval)
            }

            // Write all transactions to DB
            for (newTx in newTransactions) {
                repository.insertTransaction(newTx)
            }

            // Update rule timestamps & next run parameters
            val updatedRule = rule.copy(
                nextRunDate = currentNext,
                lastRunDate = tempLast,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateRule(updatedRule)
        }
    }

    fun calculateNextOccurrence(fromTime: Long, freq: RecurringFrequency, interval: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = fromTime
        val cleanInterval = if (interval <= 0) 1 else interval
        
        when (freq) {
            RecurringFrequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, cleanInterval)
            RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, cleanInterval)
            RecurringFrequency.MONTHLY -> calendar.add(Calendar.MONTH, cleanInterval)
            RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, cleanInterval)
        }
        return calendar.timeInMillis
    }
}
