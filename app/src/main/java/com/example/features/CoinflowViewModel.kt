package com.example.features

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.db.AppDatabase
import com.example.core.db.Budget
import com.example.core.db.BudgetPeriod
import com.example.core.db.Category
import com.example.core.db.CoinflowRepository
import com.example.core.db.DateUtils
import com.example.core.db.RecurringFrequency
import com.example.core.db.RecurringRule
import com.example.core.db.Transaction
import com.example.core.db.TransactionType
import com.example.core.db.TransactionWithDetails
import com.example.core.db.Wallet
import com.example.core.model.Money
import com.example.core.scheduler.RecurringScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoinflowViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = CoinflowRepository(
        categoryDao = db.categoryDao(),
        walletDao = db.walletDao(),
        transactionDao = db.transactionDao(),
        budgetDao = db.budgetDao(),
        recurringRuleDao = db.recurringRuleDao()
    )

    private val prefs: SharedPreferences = application.getSharedPreferences("coinflow_prefs", Context.MODE_PRIVATE)

    // Dynamic preferences matching M3 specs
    val baseCurrency = MutableStateFlow(prefs.getString("base_currency", "USD") ?: "USD")
    val biometricLockEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true)) // default dark mode for visual richness

    // App state
    val currentScreen = MutableStateFlow("Home") // Home, Stats, Transactions, Budgets, Settings
    val isAppLocked = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))

    // Transactions filtering state
    val searchQuery = MutableStateFlow("")
    val filterCategory = MutableStateFlow<Category?>(null)
    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)

    init {
        // Run recurring rules catch up immediately on app initialization
        viewModelScope.launch(Dispatchers.IO) {
            RecurringScheduler.checkAndMaterializeRecurringRules(repository)
            RecurringScheduler.schedulePeriodicWork(getApplication())
        }
    }

    // Filtered transaction list stream
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        repository.allTransactionsWithDetails,
        searchQuery,
        filterCategory,
        filterStartDate,
        filterEndDate
    ) { transactions, query, category, start, end ->
        transactions.filter { item ->
            val t = item.transaction
            val matchesQuery = query.isEmpty() ||
                    (t.note?.contains(query, ignoreCase = true) == true) ||
                    (item.category?.name?.contains(query, ignoreCase = true) == true) ||
                    (item.wallet?.name?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == null || t.categoryId == category.id
            val matchesDate = (start == null || t.occurredAt >= start) && (end == null || t.occurredAt <= end)
            matchesQuery && matchesCategory && matchesDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Save preferences
    fun setBaseCurrency(currency: String) {
        prefs.edit().putString("base_currency", currency).apply()
        baseCurrency.value = currency
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        biometricLockEnabled.value = enabled
        if (!enabled) {
            isAppLocked.value = false
        }
    }

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
        darkMode.value = dark
    }

    fun unlockApp() {
        isAppLocked.value = false
    }

    fun lockApp() {
        if (biometricLockEnabled.value) {
            isAppLocked.value = true
        }
    }

    // Core Mutators inside ViewModel coroutine scope
    fun addTransaction(
        walletId: Long,
        categoryId: Long,
        amountMinor: Int,
        type: TransactionType,
        note: String?,
        occurredAt: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Signed Amount rule: income positive, expense negative
            val signedAmount = if (type == TransactionType.EXPENSE) {
                -Math.abs(amountMinor)
            } else {
                Math.abs(amountMinor)
            }

            val tx = Transaction(
                walletId = walletId,
                categoryId = categoryId,
                amountMinor = signedAmount,
                type = type,
                note = note,
                occurredAt = occurredAt
            )
            repository.insertTransaction(tx)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransactionById(id)
        }
    }

    // Wallet mutators
    fun addWallet(name: String, currency: String, openingBalanceMinor: Int, icon: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val wallet = Wallet(
                name = name,
                currencyCode = currency,
                openingBalanceMinor = openingBalanceMinor,
                icon = icon,
                colorHex = colorHex
            )
            repository.insertWallet(wallet)
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWallet(wallet)
        }
    }

    // Category mutators
    fun addCategory(name: String, icon: String, colorHex: String, type: TransactionType) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalCatCount = repository.allCategories.first().size
            val category = Category(
                name = name,
                icon = icon,
                colorHex = colorHex,
                type = type,
                sortOrder = totalCatCount + 1
            )
            repository.insertCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    // Budget mutators
    fun addOrUpdateBudget(categoryId: Long, limitMinor: Int, period: BudgetPeriod) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if budget exists for this category
            val existing = repository.budgetDao.getBudgetByCategory(categoryId)
            if (existing != null) {
                val updated = existing.budget.copy(
                    limitMinor = limitMinor,
                    period = period,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateBudget(updated)
            } else {
                val budget = Budget(
                    categoryId = categoryId,
                    limitMinor = limitMinor,
                    period = period,
                    startDate = System.currentTimeMillis()
                )
                repository.insertBudget(budget)
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBudget(budget)
        }
    }

    // Recurring transactions rule mutators
    fun addRecurringRule(
        walletId: Long,
        categoryId: Long,
        amountMinor: Int,
        type: TransactionType,
        note: String?,
        freq: RecurringFrequency,
        interval: Int,
        startDate: Long,
        endDate: Long?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val signedAmount = if (type == TransactionType.EXPENSE) {
                -Math.abs(amountMinor)
            } else {
                Math.abs(amountMinor)
            }

            val rule = RecurringRule(
                walletId = walletId,
                categoryId = categoryId,
                amountMinor = signedAmount,
                type = type,
                note = note,
                freq = freq,
                interval = interval,
                startDate = startDate,
                endDate = endDate,
                nextRunDate = startDate // Initially nextRunDate is the startDate
            )
            val newRuleId = repository.insertRule(rule)

            // Trigger immediate check to materialize rule instances if startDate is <= now
            RecurringScheduler.checkAndMaterializeRecurringRules(repository)
        }
    }

    fun deleteRecurringRule(rule: RecurringRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRule(rule)
        }
    }

    // Export CSV and share
    fun exportCSVAndShare(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = repository.transactionDao.getAllTransactions().first()
                val csv = StringBuilder()
                csv.append("TransactionID,Date,Wallet,Category,Amount,Type,Note\n")

                for (item in list) {
                    val t = item.transaction
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(t.occurredAt))
                    val walletName = item.wallet?.name ?: "Unknown"
                    val categoryName = item.category?.name ?: "Unknown"
                    // Display double formatting at boundary edge
                    val walletCurrency = item.wallet?.currencyCode ?: "USD"
                    val exponent = Money.getExponent(walletCurrency)
                    val divisor = Math.pow(10.0, exponent.toDouble())
                    val amountFormatted = t.amountMinor.toDouble() / divisor

                    csv.append("${t.id},\"$dateFormatted\",\"$walletName\",\"$categoryName\",$amountFormatted,${t.type},\"${t.note ?: ""}\"\n")
                }

                // Share Intent
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, csv.toString())
                    putExtra(Intent.EXTRA_SUBJECT, "Coinflow Expense Export")
                    type = "text/csv"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share CSV Export via")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)

            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
