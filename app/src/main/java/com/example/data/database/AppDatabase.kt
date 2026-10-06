package com.example.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }
}

@Database(
    entities = [
        Expense::class,
        Revenue::class,
        Category::class,
        MonthlySettings::class,
        FinanceSettings::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun revenueDao(): RevenueDao
    abstract fun categoryDao(): CategoryDao
    abstract fun monthlySettingsDao(): MonthlySettingsDao
    abstract fun financeSettingsDao(): FinanceSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meu_financeiro_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                val defaultCategories = listOf(
                    Category(name = "Alimentação", iconName = "Restaurant", colorHex = "#EF4444"),
                    Category(name = "Moradia", iconName = "Home", colorHex = "#3B82F6"),
                    Category(name = "Transporte", iconName = "DirectionsCar", colorHex = "#F59E0B"),
                    Category(name = "Saúde", iconName = "LocalHospital", colorHex = "#10B981"),
                    Category(name = "Lazer", iconName = "SportsEsports", colorHex = "#8B5CF6"),
                    Category(name = "Educação", iconName = "School", colorHex = "#06B6D4"),
                    Category(name = "Outros", iconName = "Category", colorHex = "#6B7280")
                )
                defaultCategories.forEach { database.categoryDao().insertCategory(it) }

                database.financeSettingsDao().insertOrUpdate(
                    FinanceSettings(
                        id = 1,
                        themeMode = "LIGHT",
                        notificationEnabled = true,
                        reminderDaysBefore = 2,
                        defaultInvestmentPercent = 10.0,
                        defaultCategoryLimitPercent = 10.0,
                        defaultCategoryLimitName = "Lazer"
                    )
                )
            }
        }
    }
}
