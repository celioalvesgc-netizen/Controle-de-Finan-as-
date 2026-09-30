package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "revenues")
data class Revenue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: String // Format: YYYY-MM-DD
)

@Entity(tableName = "monthly_revenues")
data class MonthlyRevenue(
    @PrimaryKey
    val month: String, // Format: YYYY-MM
    val salary: Double = 0.0,
    val extraIncome: Double = 0.0,
    val investmentPercentage: Double? = null,
    val leisurePercentage: Double? = null
) {
    val totalRevenue: Double
        get() = salary + extraIncome
}

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val dueDate: String, // Format: YYYY-MM-DD
    val category: String,
    val isPaid: Boolean = false, // Always starts as false (PENDENTE)
    val paidDate: String? = null, // Format: YYYY-MM-DD when paid
    val recurringGroupId: String? = null, // Links recurring occurrences
    val recurringIndex: Int = 1, // e.g., 1 of 12
    val recurringTotal: Int = 1 // e.g., 12, or -1 for until canceled
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isCustom: Boolean = false
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val investmentPercentage: Double = 10.0,
    val leisurePercentage: Double = 10.0,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val sumWithOtherMonths: Boolean = true
)

enum class PaymentStatus {
    PAGA,
    A_PAGAR,
    VENCIDA
}

enum class ExpenseFilter {
    TODAS,
    A_PAGAR,
    PAGAS,
    VENCIDAS,
    RECEITAS
}
