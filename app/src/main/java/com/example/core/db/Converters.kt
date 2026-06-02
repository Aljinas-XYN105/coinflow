package com.example.core.db

import androidx.room.TypeConverter

enum class TransactionType {
    INCOME, EXPENSE
}

enum class BudgetPeriod {
    WEEKLY, MONTHLY, YEARLY
}

enum class RecurringFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

class RoomConverters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod): String = value.name

    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod = BudgetPeriod.valueOf(value)

    @TypeConverter
    fun fromRecurringFrequency(value: RecurringFrequency): String = value.name

    @TypeConverter
    fun toRecurringFrequency(value: String): RecurringFrequency = RecurringFrequency.valueOf(value)
}
