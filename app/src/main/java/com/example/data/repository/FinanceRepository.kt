package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class FinanceRepository(private val database: AppDatabase) {
    private val expenseDao = database.expenseDao()
    private val revenueDao = database.revenueDao()
    private val categoryDao = database.categoryDao()
    private val monthlySettingsDao = database.monthlySettingsDao()
    private val financeSettingsDao = database.financeSettingsDao()

    fun getExpensesForMonth(yearMonth: YearMonth): Flow<List<Expense>> {
        val ymString = "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)
        return expenseDao.getExpensesForMonth(ymString)
    }

    fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses()
    }

    fun getExpensesByRecurringGroup(groupId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByRecurringGroup(groupId)
    }

    suspend fun addExpense(
        description: String,
        amount: Double,
        dueDate: LocalDate,
        category: String,
        isMonthlyRecurring: Boolean = false,
        recurrenceMonths: Int = 1,
        notes: String = ""
    ) {
        val baseYearMonth = YearMonth.from(dueDate)

        if (isMonthlyRecurring) {
            // Recorrência mensal fixa (gera 12 meses inicialmente com recurringTotal = -1)
            val groupId = UUID.randomUUID().toString()
            val expenses = (0 until 12).map { offset ->
                val targetDate = dueDate.plusMonths(offset.toLong())
                val ym = YearMonth.from(targetDate)
                Expense(
                    description = description,
                    amount = amount,
                    dueDate = targetDate,
                    category = category,
                    yearMonth = "%04d-%02d".format(ym.year, ym.monthValue),
                    recurringGroupId = groupId,
                    recurringIndex = offset + 1,
                    recurringTotal = -1,
                    notes = notes
                )
            }
            expenseDao.insertExpenses(expenses)
        } else if (recurrenceMonths > 1) {
            // Parcelamento com quantidade arbitrária de parcelas
            val groupId = UUID.randomUUID().toString()
            val installmentAmount = amount / recurrenceMonths
            val expenses = (0 until recurrenceMonths).map { index ->
                val targetDate = dueDate.plusMonths(index.toLong())
                val ym = YearMonth.from(targetDate)
                Expense(
                    description = description,
                    amount = installmentAmount,
                    dueDate = targetDate,
                    category = category,
                    yearMonth = "%04d-%02d".format(ym.year, ym.monthValue),
                    recurringGroupId = groupId,
                    recurringIndex = index + 1,
                    recurringTotal = recurrenceMonths,
                    notes = notes
                )
            }
            expenseDao.insertExpenses(expenses)
        } else {
            // Despesa única
            val ymString = "%04d-%02d".format(baseYearMonth.year, baseYearMonth.monthValue)
            expenseDao.insertExpense(
                Expense(
                    description = description,
                    amount = amount,
                    dueDate = dueDate,
                    category = category,
                    yearMonth = ymString,
                    notes = notes
                )
            )
        }
    }

    suspend fun toggleExpensePaid(expense: Expense) {
        val updated = expense.copy(
            isPaid = !expense.isPaid,
            paidDate = if (!expense.isPaid) LocalDate.now() else null
        )
        expenseDao.updateExpense(updated)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteFutureRecurringExpenses(groupId: String) {
        expenseDao.deletePendingRecurringExpenses(groupId)
    }

    // Revenues
    fun getRevenuesForMonth(yearMonth: YearMonth): Flow<List<Revenue>> {
        val ymString = "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)
        return revenueDao.getRevenuesForMonth(ymString)
    }

    fun getAllRevenues(): Flow<List<Revenue>> {
        return revenueDao.getAllRevenues()
    }

    suspend fun addRevenue(revenue: Revenue) {
        revenueDao.insertRevenue(revenue)
    }

    suspend fun updateRevenue(revenue: Revenue) {
        revenueDao.updateRevenue(revenue)
    }

    suspend fun deleteRevenue(revenue: Revenue) {
        revenueDao.deleteRevenue(revenue)
    }

    // Categories
    fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    // Settings
    fun getMonthlySettings(yearMonth: YearMonth): Flow<MonthlySettings?> {
        val ymString = "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)
        return monthlySettingsDao.getSettingsForMonth(ymString)
    }

    suspend fun updateMonthlySettings(settings: MonthlySettings) {
        monthlySettingsDao.insertOrUpdate(settings)
    }

    fun getFinanceSettings(): Flow<FinanceSettings?> {
        return financeSettingsDao.getSettings()
    }

    suspend fun updateFinanceSettings(settings: FinanceSettings) {
        financeSettingsDao.insertOrUpdate(settings)
    }

    suspend fun getAllPendingExpenses(): List<Expense> {
        return expenseDao.getAllPendingExpenses()
    }
}
