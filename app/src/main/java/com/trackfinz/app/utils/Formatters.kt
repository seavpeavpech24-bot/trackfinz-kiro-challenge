package com.trackfinz.app.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatCurrency(amount: Double, currencyCode: String = "USD"): String {
    // Custom currency symbols map
    val customSymbols = mapOf(
        "KHR" to "៛",
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "CNY" to "¥",
        "THB" to "฿",
        "VND" to "₫",
        "INR" to "₹",
        "AUD" to "A$",
        "CAD" to "C$",
        "SGD" to "S$",
        "HKD" to "HK$",
        "KRW" to "₩",
        "MYR" to "RM",
        "PHP" to "₱",
        "IDR" to "Rp",
        "LAK" to "₭",
        "MMK" to "K",
        "BND" to "B$",
        "CHF" to "CHF",
        "SEK" to "kr",
        "NOK" to "kr",
        "DKK" to "kr",
        "RUB" to "₽",
        "BRL" to "R$",
        "ZAR" to "R",
        "MXN" to "MX$",
        "NZD" to "NZ$",
        "TRY" to "₺",
        "PLN" to "zł",
        "AED" to "د.إ",
        "SAR" to "﷼"
    )
    
    return try {
        val symbol = customSymbols[currencyCode.uppercase()]
        if (symbol != null) {
            // Use custom symbol
            val formatted = String.format("%.2f", amount)
            if (currencyCode.uppercase() == "KHR") {
                // For KHR, put symbol after amount
                "$formatted$symbol"
            } else {
                // For most currencies, put symbol before amount
                "$symbol$formatted"
            }
        } else {
            // Fall back to system formatting
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = Currency.getInstance(currencyCode)
            format.format(amount)
        }
    } catch (e: Exception) {
        "$currencyCode ${String.format("%.2f", amount)}"
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatMonth(month: Int, year: Int): String {
    val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
}

fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

fun monthStartMillis(month: Int, year: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun monthEndMillis(month: Int, year: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1, 23, 59, 59)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}
