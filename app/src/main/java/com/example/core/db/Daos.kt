package com.example.core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Combined Data Classes for UI Convenience
data class TransactionWithDetails(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?,
    @Relation(
        parentColumn = "walletId",
        entityColumn = "id"
    )
    val wallet: Wallet?
)

data class BudgetWithCategory(
    @Embedded val budget: Budget,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)

data class RecurringRuleWithDetails(
    @Embedded val rule: RecurringRule,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?,
    @Relation(
        parentColumn = "walletId",
        entityColumn = "id"
    )
    val wallet: Wallet?
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY id ASC")
    fun getAllWallets(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): Wallet?

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletByIdFlow(id: Long): Flow<Wallet?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: Wallet): Long

    @Update
    suspend fun updateWallet(wallet: Wallet)

    @Delete
    suspend fun deleteWallet(wallet: Wallet)
}

@Dao
interface TransactionDao {
    @RoomTransaction
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionWithDetails>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY occurredAt DESC")
    fun getTransactionsByWallet(walletId: Long): Flow<List<TransactionWithDetails>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE occurredAt BETWEEN :startDate AND :endDate ORDER BY occurredAt DESC")
    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionWithDetails>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE recurringRuleId = :ruleId")
    suspend fun getTransactionsByRuleIdSync(ruleId: Long): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}

@Dao
interface BudgetDao {
    @RoomTransaction
    @Query("SELECT * FROM budgets ORDER BY id DESC")
    fun getAllBudgets(): Flow<List<BudgetWithCategory>>

    @RoomTransaction
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetByCategory(categoryId: Long): BudgetWithCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)
}

@Dao
interface RecurringRuleDao {
    @RoomTransaction
    @Query("SELECT * FROM recurring_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<RecurringRuleWithDetails>>

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1")
    suspend fun getActiveRulesSync(): List<RecurringRule>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): RecurringRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecurringRule): Long

    @Update
    suspend fun updateRule(rule: RecurringRule)

    @Delete
    suspend fun deleteRule(rule: RecurringRule)
}
