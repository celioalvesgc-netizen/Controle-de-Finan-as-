package com.example.util

import com.example.data.model.Expense
import com.example.data.model.PaymentStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object CurrencyUtils {
    private val ptBrLocale = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun parseCurrency(input: String): Double {
        val clean = input.replace("[^0-9,]".toRegex(), "").replace(",", ".")
        return clean.toDoubleOrNull() ?: 0.0
    }

    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    fun formatToDisplayDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    fun formatMonthYear(yearMonth: YearMonth): String {
        val monthName = yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, ptBrLocale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBrLocale) else it.toString() }
        return "$monthName ${yearMonth.year}"
    }

    fun computePaymentStatus(expense: Expense): PaymentStatus {
        if (expense.isPaid) return PaymentStatus.PAGA
        val today = LocalDate.now()
        return if (expense.dueDate.isBefore(today)) {
            PaymentStatus.VENCIDA
        } else {
            PaymentStatus.A_PAGAR
        }
    }
}
