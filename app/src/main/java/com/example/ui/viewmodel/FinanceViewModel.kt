package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.util.CurrencyUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(AppDatabase.getDatabase(application))

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth.asStateFlow()

    private val _expenseFilter = MutableStateFlow(ExpenseFilter.TODAS)
    val expenseFilter: StateFlow<ExpenseFilter> = _expenseFilter.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<FinanceSettings> = repository.getFinanceSettings()
        .map { it ?: FinanceSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceSettings())

    val currentMonthExpenses: StateFlow<List<Expense>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getExpensesForMonth(ym) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthRevenues: StateFlow<List<Revenue>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getRevenuesForMonth(ym) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRevenues: StateFlow<List<Revenue>> = repository.getAllRevenues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySettings: StateFlow<MonthlySettings?> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getMonthlySettings(ym) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Summary calculation
    private data class AllDataAux(
        val allExpenses: List<Expense>,
        val allRevenues: List<Revenue>,
        val settings: FinanceSettings,
        val currentYm: YearMonth
    )

    val summary: StateFlow<FinanceSummary> = combine(
        combine(currentMonthExpenses, currentMonthRevenues, monthlySettings) { exp, rev, mSet ->
            Triple(exp, rev, mSet)
        },
        combine(allExpenses, allRevenues, settings, _selectedYearMonth) { allExp, allRev, fSet, ym ->
            AllDataAux(allExp, allRev, fSet, ym)
        }
    ) { (currentExp, currentRev, mSettings), aux ->
        val allExp = aux.allExpenses
        val allRev = aux.allRevenues
        val fSettings = aux.settings
        val currentYm = aux.currentYm

        val totalRevenue = currentRev.sumOf { it.amount }
        val totalExpenses = currentExp.sumOf { it.amount }

        var totalPaid = 0.0
        var totalPending = 0.0
        var totalOverdue = 0.0

        currentExp.forEach { exp ->
            when (CurrencyUtils.computePaymentStatus(exp)) {
                PaymentStatus.PAGA -> totalPaid += exp.amount
                PaymentStatus.A_PAGAR -> totalPending += exp.amount
                PaymentStatus.VENCIDA -> totalOverdue += exp.amount
            }
        }

        val investPercent = mSettings?.investmentPercentage ?: fSettings.defaultInvestmentPercent
        val catLimitPercent = mSettings?.categoryLimitPercentage ?: fSettings.defaultCategoryLimitPercent
        val catLimitName = mSettings?.categoryLimitName ?: fSettings.defaultCategoryLimitName
        val isAccumulating = mSettings?.isAccumulatingWithOtherMonths ?: false

        val investAllocated = (totalRevenue * investPercent) / 100.0
        val catLimitAmount = (totalRevenue * catLimitPercent) / 100.0

        val catSpent = currentExp
            .filter { it.category.equals(catLimitName, ignoreCase = true) }
            .sumOf { it.amount }

        // Saldo disponível deste mês = Receita - Investimento Destinado - Despesas Pagas
        val availableMonth = (totalRevenue - investAllocated - totalPaid).coerceAtLeast(0.0)

        // Saldo dos outros meses anteriores
        val currentYmString = "%04d-%02d".format(currentYm.year, currentYm.monthValue)
        val prevRevenues = allRev.filter { it.yearMonth < currentYmString }.sumOf { it.amount }
        val prevPaidExpenses = allExp.filter { it.yearMonth < currentYmString && it.isPaid }.sumOf { it.amount }
        val prevInvestments = (prevRevenues * investPercent) / 100.0
        val availablePrevious = (prevRevenues - prevInvestments - prevPaidExpenses).coerceAtLeast(0.0)

        val availableTotal = availableMonth + availablePrevious

        // Saldo projetado após quitar tudo (descontando pendentes e vencidas)
        val remainingToPayMonth = totalPending + totalOverdue
        val projectedMonth = (totalRevenue - investAllocated - totalExpenses)
        val projectedTotal = availableTotal - remainingToPayMonth

        FinanceSummary(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            totalPaid = totalPaid,
            totalPending = totalPending,
            totalOverdue = totalOverdue,
            investmentPercentage = investPercent,
            investmentAllocated = investAllocated,
            categoryLimitPercentage = catLimitPercent,
            categoryLimitName = catLimitName,
            categoryLimitAmount = catLimitAmount,
            categoryLimitSpent = catSpent,
            availableBalanceMonth = availableMonth,
            availableBalancePreviousMonths = availablePrevious,
            availableBalanceTotal = availableTotal,
            projectedBalanceMonth = projectedMonth,
            projectedBalanceTotal = projectedTotal,
            isAccumulatingWithOtherMonths = isAccumulating
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceSummary())

    val categoryTotals: StateFlow<Map<String, Double>> = currentMonthExpenses.map { expenses ->
        expenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectMonth(yearMonth: YearMonth) {
        _selectedYearMonth.value = yearMonth
    }

    fun previousMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.plusMonths(1)
    }

    fun resetCurrentMonth() {
        _selectedYearMonth.value = YearMonth.now()
    }

    fun setFilter(filter: ExpenseFilter) {
        _expenseFilter.value = filter
    }

    fun addExpense(
        description: String,
        amount: Double,
        dueDate: LocalDate,
        category: String,
        isMonthlyRecurring: Boolean,
        recurrenceMonths: Int,
        notes: String
    ) {
        viewModelScope.launch {
            repository.addExpense(
                description,
                amount,
                dueDate,
                category,
                isMonthlyRecurring,
                recurrenceMonths,
                notes
            )
        }
    }

    fun toggleExpensePaid(expense: Expense) {
        viewModelScope.launch {
            repository.toggleExpensePaid(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addRevenue(description: String, amount: Double, date: LocalDate, isRecurring: Boolean = false) {
        viewModelScope.launch {
            val ym = YearMonth.from(date)
            repository.addRevenue(
                Revenue(
                    description = description,
                    amount = amount,
                    date = date,
                    yearMonth = "%04d-%02d".format(ym.year, ym.monthValue),
                    isRecurring = isRecurring
                )
            )
        }
    }

    fun updateRevenue(revenue: Revenue) {
        viewModelScope.launch {
            repository.updateRevenue(revenue)
        }
    }

    fun deleteRevenue(revenue: Revenue) {
        viewModelScope.launch {
            repository.deleteRevenue(revenue)
        }
    }

    fun updateMonthlyPercentages(investPercent: Double, catLimitPercent: Double, catLimitName: String) {
        viewModelScope.launch {
            val ym = _selectedYearMonth.value
            val ymString = "%04d-%02d".format(ym.year, ym.monthValue)
            val current = monthlySettings.value ?: MonthlySettings(yearMonth = ymString)
            repository.updateMonthlySettings(
                current.copy(
                    investmentPercentage = investPercent,
                    categoryLimitPercentage = catLimitPercent,
                    categoryLimitName = catLimitName
                )
            )
        }
    }

    fun toggleSumWithOtherMonths(accumulate: Boolean) {
        viewModelScope.launch {
            val ym = _selectedYearMonth.value
            val ymString = "%04d-%02d".format(ym.year, ym.monthValue)
            val current = monthlySettings.value ?: MonthlySettings(yearMonth = ymString)
            repository.updateMonthlySettings(
                current.copy(isAccumulatingWithOtherMonths = accumulate)
            )
        }
    }

    fun updateTheme(themeMode: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateFinanceSettings(current.copy(themeMode = themeMode))
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateFinanceSettings(current.copy(notificationEnabled = enabled))
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(Category(name = name, isCustom = true))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
