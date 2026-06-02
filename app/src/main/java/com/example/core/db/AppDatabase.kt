package com.example.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Category::class,
        Wallet::class,
        Transaction::class,
        Budget::class,
        RecurringRule::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coinflow_db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Enforce Foreign Keys explicitly
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default categories and wallet on database creation
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context.applicationContext)
                                seedDatabase(database)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            val walletDao = db.walletDao()
            val categoryDao = db.categoryDao()

            // Seed Category defaults
            val defaultCategories = listOf(
                Category(
                    name = "Food",
                    icon = "🍔",
                    colorHex = "#FF5722", // deep orange
                    type = TransactionType.EXPENSE,
                    sortOrder = 1
                ),
                Category(
                    name = "Transport",
                    icon = "🚗",
                    colorHex = "#2196F3", // blue
                    type = TransactionType.EXPENSE,
                    sortOrder = 2
                ),
                Category(
                    name = "Shopping",
                    icon = "🛍️",
                    colorHex = "#E91E63", // pink
                    type = TransactionType.EXPENSE,
                    sortOrder = 3
                ),
                Category(
                    name = "Bills",
                    icon = "🧾",
                    colorHex = "#9C27B0", // purple
                    type = TransactionType.EXPENSE,
                    sortOrder = 4
                ),
                Category(
                    name = "Entertainment",
                    icon = "🎮",
                    colorHex = "#4CAF50", // green
                    type = TransactionType.EXPENSE,
                    sortOrder = 5
                ),
                Category(
                    name = "Salary",
                    icon = "💰",
                    colorHex = "#009688", // teal
                    type = TransactionType.INCOME,
                    sortOrder = 6
                )
            )

            for (cat in defaultCategories) {
                categoryDao.insertCategory(cat)
            }

            // Seed default "Cash" USD wallet
            val defaultWallet = Wallet(
                name = "Cash",
                currencyCode = "USD",
                openingBalanceMinor = 100000, // $1,000.00 seeding default for immediate utility
                icon = "💵",
                colorHex = "#4CAF50"
            )
            walletDao.insertWallet(defaultWallet)
        }
    }
}
