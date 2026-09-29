package com.example.util

import com.example.data.model.Expense
import com.example.data.model.PaymentStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object CurrencyUtils {

    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(ptBrLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptBrLocale)
    private val monthNameFormatter = DateTimeFormatter.ofPattern("MMMM", ptBrLocale)

    fun formatCurrency(amount: Double?): String {
        if (amount == null || amount.isNaN() || amount.isInfinite()) {
            return "R$ 0,00"
        }
        return currencyFormatter.format(amount)
    }

    fun parseAmount(input: String): Double {
        if (input.isBlank()) return 0.0
        val cleaned = input.trim()
            .replace("R$", "")
            .replace(" ", "")

        val normalized = if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned.replace(".", "").replace(",", ".")
        } else if (cleaned.contains(",")) {
            cleaned.replace(",", ".")
        } else {
            cleaned
        }

        return normalized.toDoubleOrNull() ?: 0.0
    }

    fun todayIso(): String {
        return LocalDate.now().format(isoDateFormatter)
    }

    fun currentYearMonthIso(): String {
        return YearMonth.now().toString()
    }

    fun formatToDisplayDate(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            val parsed = LocalDate.parse(isoDate, isoDateFormatter)
            parsed.format(displayDateFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatToShortDate(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            val parsed = LocalDate.parse(isoDate, isoDateFormatter)
            parsed.format(shortDateFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatMonthYearHeader(yearMonth: YearMonth): String {
        val formatted = yearMonth.format(monthYearFormatter)
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBrLocale) else it.toString() }
    }

    fun formatDueDayNotice(dueDate: String, isRecurring: Boolean): String {
        return try {
            val parsed = LocalDate.parse(dueDate, isoDateFormatter)
            if (isRecurring) {
                "Vence todo dia ${parsed.dayOfMonth}"
            } else {
                "Vence em ${parsed.format(shortDateFormatter)}"
            }
        } catch (e: Exception) {
            "Vencimento: $dueDate"
        }
    }

    fun getDueUrgencyNotice(dueDate: String, referenceDate: LocalDate = LocalDate.now()): String? {
        return try {
            val due = LocalDate.parse(dueDate, isoDateFormatter)
            val days = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, due)
            when {
                days == 0L -> "Vence hoje!"
                days == 1L -> "Vence amanhã!"
                days in 2L..3L -> "Vence em $days dias"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getMonthNameFromIso(isoDate: String): String {
        return try {
            val parsed = LocalDate.parse(isoDate, isoDateFormatter)
            val name = parsed.format(monthNameFormatter)
            name.uppercase(ptBrLocale)
        } catch (e: Exception) {
            ""
        }
    }

    fun computePaymentStatus(expense: Expense, referenceDate: LocalDate = LocalDate.now()): PaymentStatus {
        if (expense.isPaid) {
            return PaymentStatus.PAGA
        }
        return try {
            val due = LocalDate.parse(expense.dueDate, isoDateFormatter)
            if (due.isBefore(referenceDate)) {
                PaymentStatus.VENCIDA
            } else {
                PaymentStatus.A_PAGAR
            }
        } catch (e: Exception) {
            PaymentStatus.A_PAGAR
        }
    }

    fun addMonthsToIsoDate(isoDate: String, monthsToAdd: Long): String {
        return try {
            val parsed = LocalDate.parse(isoDate, isoDateFormatter)
            val nextDate = parsed.plusMonths(monthsToAdd)
            nextDate.format(isoDateFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }
}
