package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val dueDate: LocalDate,
    val isPaid: Boolean = false,
    val paidDate: LocalDate? = null,
    val category: String = "Geral",
    val yearMonth: String, // Formato "YYYY-MM"
    val recurringGroupId: String? = null,
    val recurringIndex: Int = 1,
    val recurringTotal: Int = 1, // -1 para mensal contínuo, >1 para parcelamento fixo
    val notes: String = ""
)

@Entity(tableName = "revenues")
data class Revenue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val yearMonth: String, // Formato "YYYY-MM"
    val isRecurring: Boolean = false
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "Category",
    val colorHex: String = "#0A6847",
    val isCustom: Boolean = false
)

@Entity(tableName = "monthly_settings")
data class MonthlySettings(
    @PrimaryKey
    val yearMonth: String, // Formato "YYYY-MM"
    val investmentPercentage: Double = 10.0,
    val categoryLimitPercentage: Double = 10.0,
    val categoryLimitName: String = "Lazer",
    val isAccumulatingWithOtherMonths: Boolean = false
)

@Entity(tableName = "finance_settings")
data class FinanceSettings(
    @PrimaryKey
    val id: Int = 1,
    val themeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    val notificationEnabled: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val defaultInvestmentPercent: Double = 10.0,
    val defaultCategoryLimitPercent: Double = 10.0,
    val defaultCategoryLimitName: String = "Lazer"
)

enum class PaymentStatus {
    PAGA,
    A_PAGAR,
    VENCIDA
}

enum class ExpenseFilter {
    TODAS,
    PAGAS,
    A_PAGAR,
    VENCIDAS
}

data class FinanceSummary(
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalPending: Double = 0.0,
    val totalOverdue: Double = 0.0,
    val investmentPercentage: Double = 10.0,
    val investmentAllocated: Double = 0.0,
    val categoryLimitPercentage: Double = 10.0,
    val categoryLimitName: String = "Lazer",
    val categoryLimitAmount: Double = 0.0,
    val categoryLimitSpent: Double = 0.0,
    val availableBalanceMonth: Double = 0.0,
    val availableBalancePreviousMonths: Double = 0.0,
    val availableBalanceTotal: Double = 0.0,
    val projectedBalanceMonth: Double = 0.0,
    val projectedBalanceTotal: Double = 0.0,
    val isAccumulatingWithOtherMonths: Boolean = false
)
