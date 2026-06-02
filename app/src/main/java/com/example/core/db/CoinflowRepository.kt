package com.example.core.db

import com.example.core.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

// UI Progress DTOs
data class WalletWithBalance(
    val wallet: Wallet,
    val currentBalanceMinor: Int
)

data class MonthlyTotals(
    val incomeMinor: Int,
    val spentMinor: Int
)

data class BudgetProgress(
    val budget: Budget,
    val category: Category?,
    val spentMinor: Int,
    val limitMinor: Int,
    val ratio: Double // 0.0 to 1.0+
)

object DateUtils {
    fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis - 1
        return Pair(start, end)
    }

    fun getPeriodRange(period: BudgetPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return when (period) {
            BudgetPeriod.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val start = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
            BudgetPeriod.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
            BudgetPeriod.YEARLY -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
        }
    }
}

class CoinflowRepository(
    val categoryDao: CategoryDao,
    val walletDao: WalletDao,
    val transactionDao: TransactionDao,
    val budgetDao: BudgetDao,
    val recurringRuleDao: RecurringRuleDao
) {
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allWallets: Flow<List<Wallet>> = walletDao.getAllWallets()
    val allTransactionsWithDetails: Flow<List<TransactionWithDetails>> = transactionDao.getAllTransactions()
    val allBudgetsWithCategories: Flow<List<BudgetWithCategory>> = budgetDao.getAllBudgets()
    val allRulesWithDetails: Flow<List<RecurringRuleWithDetails>> = recurringRuleDao.getAllRules()

    // 1. Calculate Wallet Balances dynamically
    val walletsWithBalances: Flow<List<WalletWithBalance>> = combine(
        walletDao.getAllWallets(),
        transactionDao.getAllTransactions()
    ) { wallets, transactions ->
        wallets.map { wallet ->
            val txSum = transactions
                .filter { it.transaction.walletId == wallet.id }
                .sumOf { it.transaction.amountMinor }
            WalletWithBalance(wallet, wallet.openingBalanceMinor + txSum)
        }
    }

    // 2. Calculate dynamic global balance in base currency (assuming USD if mixed; or summing minor units directly)
    val globalTotalBalanceMinor: Flow<Int> = walletsWithBalances.map { walletsWithBalance ->
        walletsWithBalance.sumOf { it.currentBalanceMinor }
    }

    // 3. Dynamic current month totals (income / spent)
    val monthlyTotals: Flow<MonthlyTotals> = transactionDao.getAllTransactions().map { transactions ->
        val (start, end) = DateUtils.getCurrentMonthRange()
        var income = 0
        var spent = 0
        for (tx in transactions) {
            val t = tx.transaction
            if (t.occurredAt in start..end) {
                if (t.amountMinor > 0) {
                    income += t.amountMinor
                } else {
                    // spent is expense (amountMinor is signed negative, sum as positive spent total)
                    spent += Math.abs(t.amountMinor)
                }
            }
        }
        MonthlyTotals(income, spent)
    }

    // 4. Dynamic budget progress
    val budgetProgressList: Flow<List<BudgetProgress>> = combine(
        budgetDao.getAllBudgets(),
        transactionDao.getAllTransactions()
    ) { budgetsWithCats, transactions ->
        budgetsWithCats.map { bgWithCat ->
            val budget = bgWithCat.budget
            val category = bgWithCat.category
            val (start, end) = DateUtils.getPeriodRange(budget.period)

            val spent = transactions
                .filter {
                    it.transaction.categoryId == budget.categoryId &&
                            it.transaction.occurredAt in start..end &&
                            it.transaction.amountMinor < 0
                }
                .sumOf { Math.abs(it.transaction.amountMinor) }

            BudgetProgress(
                budget = budget,
                category = category,
                spentMinor = spent,
                limitMinor = budget.limitMinor,
                ratio = if (budget.limitMinor > 0) spent.toDouble() / budget.limitMinor.toDouble() else 0.0
            )
        }
    }

    // Insert methods
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun insertWallet(wallet: Wallet) = walletDao.insertWallet(wallet)
    suspend fun updateWallet(wallet: Wallet) = walletDao.updateWallet(wallet)
    suspend fun deleteWallet(wallet: Wallet) = walletDao.deleteWallet(wallet)

    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    suspend fun deleteTransactionById(id: Long) = transactionDao.deleteTransactionById(id)

    suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    suspend fun insertRule(rule: RecurringRule) = recurringRuleDao.insertRule(rule)
    suspend fun updateRule(rule: RecurringRule) = recurringRuleDao.updateRule(rule)
    suspend fun deleteRule(rule: RecurringRule) = recurringRuleDao.deleteRule(rule)
}
