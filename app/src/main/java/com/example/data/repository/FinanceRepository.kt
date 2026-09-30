package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.MonthlyRevenueDao
import com.example.data.dao.RevenueDao
import com.example.data.dao.SettingsDao
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.MonthlyRevenue
import com.example.data.model.Revenue
import com.example.util.CurrencyUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class FinanceRepository(
    private val revenueDao: RevenueDao,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val settingsDao: SettingsDao,
    private val monthlyRevenueDao: MonthlyRevenueDao
) {

    fun getMonthlyRevenue(month: String): Flow<MonthlyRevenue?> =
        monthlyRevenueDao.getRevenueForMonth(month)

    suspend fun getMonthlyRevenueDirect(month: String): MonthlyRevenue? =
        monthlyRevenueDao.getRevenueForMonthDirect(month)

    fun getAllMonthlyRevenues(): Flow<List<MonthlyRevenue>> =
        monthlyRevenueDao.getAllMonthlyRevenues()

    suspend fun saveMonthlyRevenue(month: String, salary: Double, extraIncome: Double) {
        val existing = monthlyRevenueDao.getRevenueForMonthDirect(month)
        monthlyRevenueDao.saveMonthlyRevenue(
            MonthlyRevenue(
                month = month,
                salary = kotlin.math.max(0.0, salary),
                extraIncome = kotlin.math.max(0.0, extraIncome),
                investmentPercentage = existing?.investmentPercentage,
                leisurePercentage = existing?.leisurePercentage
            )
        )
    }

    suspend fun saveMonthlyPercentages(month: String, investmentPercentage: Double?, leisurePercentage: Double?) {
        val existing = monthlyRevenueDao.getRevenueForMonthDirect(month)
        monthlyRevenueDao.saveMonthlyRevenue(
            MonthlyRevenue(
                month = month,
                salary = existing?.salary ?: 0.0,
                extraIncome = existing?.extraIncome ?: 0.0,
                investmentPercentage = investmentPercentage,
                leisurePercentage = leisurePercentage
            )
        )
    }

    fun getRevenuesForMonth(monthPrefix: String): Flow<List<Revenue>> =
        revenueDao.getRevenuesByMonth(monthPrefix)

    fun getAllRevenues(): Flow<List<Revenue>> =
        revenueDao.getAllRevenues()

    fun getExpensesForMonth(monthPrefix: String): Flow<List<Expense>> =
        expenseDao.getExpensesByMonth(monthPrefix)

    fun getAllExpenses(): Flow<List<Expense>> =
        expenseDao.getAllExpenses()

    suspend fun getUnpaidExpensesDirect(): List<Expense> =
        expenseDao.getUnpaidExpensesDirect()

    fun getCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()

    fun getSettings(): Flow<AppSettings?> =
        settingsDao.getSettings()

    suspend fun saveSettings(settings: AppSettings) {
        settingsDao.saveSettings(settings)
    }

    suspend fun updateSumWithOtherMonths(enabled: Boolean) {
        val current = settingsDao.getSettingsDirect() ?: AppSettings()
        settingsDao.saveSettings(current.copy(sumWithOtherMonths = enabled))
    }

    suspend fun addRevenue(revenue: Revenue): Long {
        return revenueDao.insertRevenue(revenue)
    }

    suspend fun updateRevenue(revenue: Revenue) {
        revenueDao.updateRevenue(revenue)
    }

    suspend fun deleteRevenue(revenue: Revenue) {
        revenueDao.deleteRevenue(revenue)
    }

    suspend fun addExpense(
        description: String,
        amount: Double,
        dueDate: String,
        isPaid: Boolean = false,
        recurrenceMonths: Int = 1,
        category: String = "Geral"
    ) {
        val paidDate = if (isPaid) CurrencyUtils.todayIso() else null
        if (recurrenceMonths <= 1) {
            val single = Expense(
                description = description,
                amount = amount,
                dueDate = dueDate,
                category = category,
                isPaid = isPaid,
                paidDate = paidDate,
                recurringGroupId = null,
                recurringIndex = 1,
                recurringTotal = 1
            )
            expenseDao.insertExpense(single)
        } else {
            val groupId = UUID.randomUUID().toString()
            val total = if (recurrenceMonths > 12) -1 else recurrenceMonths
            val countToGenerate = recurrenceMonths
            val items = (0 until countToGenerate).map { index ->
                val monthDueDate = CurrencyUtils.addMonthsToIsoDate(dueDate, index.toLong())
                val itemIsPaid = if (index == 0) isPaid else false
                val itemPaidDate = if (index == 0 && isPaid) paidDate else null
                Expense(
                    description = description,
                    amount = amount,
                    dueDate = monthDueDate,
                    category = category,
                    isPaid = itemIsPaid,
                    paidDate = itemPaidDate,
                    recurringGroupId = groupId,
                    recurringIndex = index + 1,
                    recurringTotal = total
                )
            }
            expenseDao.insertExpenses(items)
        }
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun toggleExpensePaymentStatus(expense: Expense) {
        val updated = if (expense.isPaid) {
            // Revert to pending
            expense.copy(isPaid = false, paidDate = null)
        } else {
            // Mark as paid
            expense.copy(isPaid = true, paidDate = CurrencyUtils.todayIso())
        }
        expenseDao.updateExpense(updated)
    }

    suspend fun cancelRecurringExpense(groupId: String, afterDate: String) {
        expenseDao.cancelFutureRecurringExpenses(groupId, afterDate)
    }

    suspend fun addCategory(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val existing = categoryDao.getCategoryByName(trimmed)
        if (existing != null) {
            return existing.id
        }
        return categoryDao.insertCategory(Category(name = trimmed, isCustom = true))
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    suspend fun ensureInitialData() {
        // 1. Purge any duplicate categories that already exist in SQLite
        categoryDao.deleteDuplicateCategories()

        // 2. Insert standard default categories only if not already present
        val defaultCategoryNames = listOf(
            "Alimentação",
            "Moradia",
            "Água",
            "Energia",
            "Internet",
            "Transporte",
            "Saúde",
            "Compras",
            "Lazer",
            "Outros"
        )
        for (catName in defaultCategoryNames) {
            val existing = categoryDao.getCategoryByName(catName)
            if (existing == null) {
                categoryDao.insertCategory(Category(name = catName, isCustom = false))
            }
        }

        // Clean up once more to guarantee no duplication
        categoryDao.deleteDuplicateCategories()

        // 3. Ensure default settings if not yet saved
        val existingSettings = settingsDao.getSettingsDirect()
        if (existingSettings == null) {
            settingsDao.saveSettings(
                AppSettings(
                    id = 1,
                    investmentPercentage = 10.0,
                    leisurePercentage = 10.0
                )
            )
        }
    }
}
