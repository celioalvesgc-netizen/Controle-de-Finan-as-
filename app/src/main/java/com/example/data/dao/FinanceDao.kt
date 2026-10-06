package com.example.data.dao

import androidx.room.*
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.FinanceSettings
import com.example.data.model.MonthlySettings
import com.example.data.model.Revenue
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE yearMonth = :yearMonth ORDER BY dueDate ASC")
    fun getExpensesForMonth(yearMonth: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY dueDate ASC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE recurringGroupId = :groupId ORDER BY dueDate ASC")
    fun getExpensesByRecurringGroup(groupId: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>): List<Long>

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE recurringGroupId = :groupId AND isPaid = 0")
    suspend fun deletePendingRecurringExpenses(groupId: String)

    @Query("SELECT * FROM expenses WHERE isPaid = 0")
    suspend fun getAllPendingExpenses(): List<Expense>
}

@Dao
interface RevenueDao {
    @Query("SELECT * FROM revenues WHERE yearMonth = :yearMonth ORDER BY date ASC")
    fun getRevenuesForMonth(yearMonth: String): Flow<List<Revenue>>

    @Query("SELECT * FROM revenues ORDER BY date ASC")
    fun getAllRevenues(): Flow<List<Revenue>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevenue(revenue: Revenue): Long

    @Update
    suspend fun updateRevenue(revenue: Revenue)

    @Delete
    suspend fun deleteRevenue(revenue: Revenue)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface MonthlySettingsDao {
    @Query("SELECT * FROM monthly_settings WHERE yearMonth = :yearMonth")
    fun getSettingsForMonth(yearMonth: String): Flow<MonthlySettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: MonthlySettings)
}

@Dao
interface FinanceSettingsDao {
    @Query("SELECT * FROM finance_settings WHERE id = 1")
    fun getSettings(): Flow<FinanceSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: FinanceSettings)
}
