package com.example

import com.example.core.db.BudgetPeriod
import com.example.core.db.RecurringFrequency
import com.example.core.model.FormatException
import com.example.core.model.Money
import com.example.core.scheduler.RecurringScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CoinflowUnitTest {

    @Test
    fun testMoneyParsing_Success() {
        // Exponent 2 (USD)
        val USD = "USD"
        val m1 = Money.parse("12.34", USD)
        assertEquals(1234, m1.minor)
        assertEquals("USD", m1.currency)

        // Truncation check (12.345 -> 12.34 for USD exponent 2)
        val m2 = Money.parse("12.345", USD)
        assertEquals(1234, m2.minor)

        // Padding check (12.3 -> 12.30)
        val m3 = Money.parse("12.3", USD)
        assertEquals(1230, m3.minor)

        // Negative value
        val m4 = Money.parse("-5.5", USD)
        assertEquals(-550, m4.minor)

        // Whole item without dot
        val m5 = Money.parse("15", USD)
        assertEquals(1500, m5.minor)

        // Exponent 0 (JPY)
        val mJPY = Money.parse("250", "JPY")
        assertEquals(250, mJPY.minor)
    }

    @Test
    fun testMoneyParsing_ThrowsFormatException() {
        assertThrows(FormatException::class.java) {
            Money.parse("abc", "USD")
        }
        assertThrows(FormatException::class.java) {
            Money.parse("12.34.56", "USD")
        }
        assertThrows(FormatException::class.java) {
            Money.parse("", "USD")
        }
        assertThrows(FormatException::class.java) {
            Money.parse("  ", "USD")
        }
    }

    @Test
    fun testMoneyArithmetic() {
        val m1 = Money(1500, "USD")
        val m2 = Money(500, "USD")

        val sum = m1 + m2
        assertEquals(2000, sum.minor)

        val diff = m1 - m2
        assertEquals(1000, diff.minor)

        val neg = -m1
        assertEquals(-1500, neg.minor)

        val absolute = neg.abs()
        assertEquals(1500, absolute.minor)
    }

    @Test
    fun testMoneyCurrencyMismatch_ThrowsException() {
        val m1 = Money(100, "USD")
        val m2 = Money(100, "EUR")

        assertThrows(IllegalArgumentException::class.java) {
            m1 + m2
        }
    }

    @Test
    fun testMoneyScalingAndRatio() {
        val m = Money(100, "USD")
        val scaled = m.scale(1.5) // rounds 150 to nearest minor unit
        assertEquals(150, scaled.minor)

        val total = Money(1000, "USD")
        val progressRatio = m.ratioOf(total)
        assertEquals(0.1, progressRatio, 0.0001)
    }

    @Test
    fun testRecurringOccurrenceCalculation() {
        val startDate = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Test DAILY adding
        val dailyNext = RecurringScheduler.calculateNextOccurrence(startDate, RecurringFrequency.DAILY, 1)
        val calendar = Calendar.getInstance().apply { timeInMillis = dailyNext }
        assertEquals(2, calendar.get(Calendar.DAY_OF_MONTH))

        // Test WEEKLY adding
        val weeklyNext = RecurringScheduler.calculateNextOccurrence(startDate, RecurringFrequency.WEEKLY, 2)
        calendar.apply { timeInMillis = weeklyNext }
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH)) // June 1 + 2 weeks = June 15

        // Test MONTHLY adding
        val monthlyNext = RecurringScheduler.calculateNextOccurrence(startDate, RecurringFrequency.MONTHLY, 3)
        calendar.apply { timeInMillis = monthlyNext }
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH)) // June + 3 months = September
    }
}
