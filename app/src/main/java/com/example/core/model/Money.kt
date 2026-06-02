package com.example.core.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class FormatException(message: String) : RuntimeException(message)

data class Money(
    val minor: Int,
    val currency: String
) {
    companion object {
        fun getExponent(currencyCode: String): Int {
            return try {
                val cur = Currency.getInstance(currencyCode.uppercase())
                cur.defaultFractionDigits
            } catch (e: Exception) {
                // Return default 2 if currency is invalid or unlisted
                2
            }
        }

        fun parse(text: String, currency: String): Money {
            val clean = text.trim()
            if (clean.isEmpty()) throw FormatException("Empty input")

            val isNegative = clean.startsWith("-")
            val absText = if (isNegative) clean.substring(1) else clean

            if (absText.isEmpty()) throw FormatException("Invalid numeric characters")

            // Ensure we have only digits and a single decimal point
            if (!absText.all { it.isDigit() || it == '.' }) {
                throw FormatException("Garbage in input: $text")
            }

            val dotIndex = absText.indexOf('.')
            val wholePart: String
            val fractionalPart: String
            if (dotIndex == -1) {
                wholePart = absText
                fractionalPart = ""
            } else {
                if (absText.lastIndexOf('.') != dotIndex) {
                    throw FormatException("Multiple decimal points: $text")
                }
                wholePart = absText.substring(0, dotIndex)
                fractionalPart = absText.substring(dotIndex + 1)
            }

            if (wholePart.isEmpty() && fractionalPart.isEmpty()) {
                throw FormatException("Numeric parts are empty")
            }

            if ((wholePart.isNotEmpty() && !wholePart.all { it.isDigit() }) ||
                (fractionalPart.isNotEmpty() && !fractionalPart.all { it.isDigit() })
            ) {
                throw FormatException("Invalid numeric parts: $text")
            }

            val exp = getExponent(currency)
            val finalFraction = if (fractionalPart.length > exp) {
                fractionalPart.substring(0, exp) // truncate
            } else {
                fractionalPart.padEnd(exp, '0') // pad
            }

            val wholeVal = if (wholePart.isEmpty()) 0L else wholePart.toLong()
            val fractionVal = if (finalFraction.isEmpty()) 0L else finalFraction.toLong()

            var factor = 1L
            repeat(exp) { factor *= 10 }

            val totalMinorLong = wholeVal * factor + fractionVal
            if (totalMinorLong > Int.MAX_VALUE || totalMinorLong < Int.MIN_VALUE) {
                throw FormatException("Overflow error")
            }

            val totalMinor = if (isNegative) -totalMinorLong.toInt() else totalMinorLong.toInt()
            return Money(totalMinor, currency.uppercase())
        }
    }

    private fun assertSameCurrency(other: Money) {
        if (this.currency.uppercase() != other.currency.uppercase()) {
            throw IllegalArgumentException("Currency mismatch: ${this.currency} vs ${other.currency}")
        }
    }

    operator fun plus(other: Money): Money {
        assertSameCurrency(other)
        return Money(this.minor + other.minor, currency)
    }

    operator fun minus(other: Money): Money {
        assertSameCurrency(other)
        return Money(this.minor - other.minor, currency)
    }

    operator fun unaryMinus(): Money {
        return Money(-minor, currency)
    }

    fun abs(): Money {
        return Money(abs(minor), currency)
    }

    fun scale(factor: Double): Money {
        return Money((minor * factor).roundToInt(), currency)
    }

    fun ratioOf(total: Money): Double {
        assertSameCurrency(total)
        if (total.minor == 0) return 0.0
        return this.minor.toDouble() / total.minor.toDouble()
    }

    fun format(showSign: Boolean = false, locale: Locale = Locale.getDefault()): String {
        val exp = getExponent(currency)
        val divisor = Math.pow(10.0, exp.toDouble())
        val decimalVal = minor.toDouble() / divisor

        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = Currency.getInstance(this@Money.currency)
            this.minimumFractionDigits = exp
            this.maximumFractionDigits = exp
        }

        // Format positive value without sign first, as format will deal with the negative sign.
        var formatted = formatter.format(abs(decimalVal))
        
        // Let's standardise formatting with proper sign prepended
        if (minor < 0) {
            formatted = "-$formatted"
        } else if (minor > 0 && showSign) {
            formatted = "+$formatted"
        }
        return formatted
    }
}
