package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Revenue::class,
        Expense::class,
        Category::class,
        AppSettings::class,
        MonthlyRevenue::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun revenueDao(): RevenueDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun monthlyRevenueDao(): MonthlyRevenueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `monthly_revenues` (
                        `month` TEXT NOT NULL PRIMARY KEY,
                        `salary` REAL NOT NULL,
                        `extraIncome` REAL NOT NULL
                    )
                """.trimIndent())

                // Safely migrate any old revenues if the table exists and has entries
                try {
                    db.execSQL("""
                        INSERT OR REPLACE INTO `monthly_revenues` (`month`, `salary`, `extraIncome`)
                        SELECT 
                            substr(date, 1, 7) as m,
                            COALESCE(SUM(CASE WHEN LOWER(description) LIKE '%sal_rio%' OR LOWER(description) LIKE '%salario%' THEN amount ELSE 0 END), 0),
                            COALESCE(SUM(CASE WHEN LOWER(description) NOT LIKE '%sal_rio%' AND LOWER(description) NOT LIKE '%salario%' THEN amount ELSE 0 END), 0)
                        FROM revenues
                        WHERE length(date) >= 7
                        GROUP BY m
                    """.trimIndent())
                } catch (e: Exception) {
                    // Ignore if revenues table was empty or not formatted
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `isDarkMode` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `notificationsEnabled` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `sumWithOtherMonths` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `monthly_revenues` ADD COLUMN `investmentPercentage` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `monthly_revenues` ADD COLUMN `leisurePercentage` REAL DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `categoryLimitName` TEXT NOT NULL DEFAULT 'Lazer'")
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `categoryLimitPercentage` REAL NOT NULL DEFAULT 10.0")
                db.execSQL("ALTER TABLE `monthly_revenues` ADD COLUMN `categoryLimitName` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `monthly_revenues` ADD COLUMN `categoryLimitPercentage` REAL DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meu_financeiro_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                // Populate default categories without duplicates
                val defaultCategoryNames = listOf(
                    "Alimentação",
                    "Água",
                    "Compras",
                    "Empréstimo",
                    "Energia",
                    "Imposto",
                    "Internet",
                    "Lazer",
                    "Moradia",
                    "Outros",
                    "Saúde",
                    "Transporte"
                )

                for (catName in defaultCategoryNames) {
                    val existing = database.categoryDao().getCategoryByName(catName)
                    if (existing == null) {
                        database.categoryDao().insertCategory(Category(name = catName, isCustom = false))
                    }
                }
                database.categoryDao().deleteDuplicateCategories()

                // Populate default settings (10% Investment, 10% Leisure)
                database.settingsDao().saveSettings(
                    AppSettings(
                        id = 1,
                        investmentPercentage = 10.0,
                        leisurePercentage = 10.0
                    )
                )
            }
        }
    }
}
