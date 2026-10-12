package com.dailybudget.app.ui

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formatage des montants (stockés en centimes) dans la monnaie choisie et la langue du téléphone. */
class Money(currencyCode: String, private val locale: Locale = Locale.getDefault()) {
    val currency: Currency = runCatching { Currency.getInstance(currencyCode) }.getOrElse { Currency.getInstance("EUR") }
    val symbol: String = currency.getSymbol(locale)

    private val format: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        currency = this@Money.currency
        val digits = this@Money.currency.defaultFractionDigits.coerceIn(0, 2)
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }

    fun format(cents: Long): String = format.format(cents / 100.0)

    /** Valeur pré-remplie dans un champ de saisie, avec le séparateur décimal local. */
    fun editable(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = Math.abs(cents)
        val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        return if (abs % 100 == 0L) "$sign${abs / 100}" else "$sign${abs / 100}$sep${"%02d".format(abs % 100)}"
    }

    companion object {
        /** Monnaies proposées en tête de liste. */
        val popular = listOf("MAD", "EUR", "USD", "GBP", "CAD", "CHF", "XOF", "TND", "DZD", "AED", "SAR")

        fun all(): List<Currency> {
            val all = Currency.getAvailableCurrencies().sortedBy { it.currencyCode }
            val top = popular.mapNotNull { code -> all.firstOrNull { it.currencyCode == code } }
            return top + (all - top.toSet())
        }
    }
}
