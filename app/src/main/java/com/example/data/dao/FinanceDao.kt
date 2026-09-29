package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.MonthlyRevenue
import com.example.data.model.Revenue
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyRevenueDao {
    @Query("SELECT * FROM monthly_revenues WHERE month = :month LIMIT 1")
    fun getRevenueForMonth(month: String): Flow<MonthlyRevenue?>

    @Query("SELECT * FROM monthly_revenues WHERE month = :month LIMIT 1")
    suspend fun getRevenueForMonthDirect(month: String): MonthlyRevenue?

    @Query("SELECT * FROM monthly_revenues")
    fun getAllMonthlyRevenues(): Flow<List<MonthlyRevenue>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMonthlyRevenue(revenue: MonthlyRevenue)

    @Query("DELETE FROM monthly_revenues WHERE month = :month")
    suspend fun deleteRevenueForMonth(month: String)
}

@Dao
interface RevenueDao {
    @Query("SELECT * FROM revenues WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getRevenuesByMonth(monthPrefix: String): Flow<List<Revenue>>

    @Query("SELECT * FROM revenues ORDER BY date DESC")
    fun getAllRevenues(): Flow<List<Revenue>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevenue(revenue: Revenue): Long

    @Update
    suspend fun updateRevenue(revenue: Revenue)

    @Delete
    suspend fun deleteRevenue(revenue: Revenue)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE dueDate LIKE :monthPrefix || '%' ORDER BY dueDate ASC")
    fun getExpensesByMonth(monthPrefix: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY dueDate DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Stop recurring expenses in the future without touching past or paid items
    @Query("DELETE FROM expenses WHERE recurringGroupId = :groupId AND dueDate > :afterDate AND isPaid = 0")
    suspend fun cancelFutureRecurringExpenses(groupId: String, afterDate: String)

    @Query("SELECT * FROM expenses WHERE isPaid = 0")
    suspend fun getUnpaidExpensesDirect(): List<Expense>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE id IN (SELECT MIN(id) FROM categories GROUP BY LOWER(TRIM(name))) ORDER BY id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Query("DELETE FROM categories WHERE id NOT IN (SELECT MIN(id) FROM categories GROUP BY LOWER(TRIM(name)))")
    suspend fun deleteDuplicateCategories()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}
