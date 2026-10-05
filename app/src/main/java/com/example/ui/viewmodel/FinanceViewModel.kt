package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseFilter
import com.example.data.model.MonthlyRevenue
import com.example.data.model.Revenue
import com.example.data.repository.FinanceRepository
import com.example.util.CurrencyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

const val DEFAULT_SYSTEM_INVESTMENT_PERCENTAGE = 20.0
const val DEFAULT_SYSTEM_LEISURE_PERCENTAGE = 10.0

data class FinanceSummary(
    val monthYear: YearMonth,
    val salary: Double = 0.0,
    val extraIncome: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val revenuesCount: Int = 0,
    val investmentPercentage: Double = DEFAULT_SYSTEM_INVESTMENT_PERCENTAGE,
    val investmentAllocated: Double = 0.0,
    val leisurePercentage: Double = DEFAULT_SYSTEM_LEISURE_PERCENTAGE,
    val leisureLimit: Double = 0.0,
    val leisureSpent: Double = 0.0,
    val leisureAvailable: Double = 0.0,
    val leisureOverBudget: Double = 0.0,
    // Limite por categoria flexível (substitui/generaliza o limite de lazer)
    val categoryLimitName: String = "Lazer",
    val categoryLimitPercentage: Double = DEFAULT_SYSTEM_LEISURE_PERCENTAGE,
    val categoryLimitAmount: Double = 0.0,
    val categoryLimitSpent: Double = 0.0,
    val categoryLimitAvailable: Double = 0.0,
    val categoryLimitOverBudget: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val generalExpenses: Double = 0.0,
    val spendingLimit: Double = 0.0,
    val availableForSpending: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalPending: Double = 0.0,
    val totalOverdue: Double = 0.0,
    val emergencyFundMonth: Double = 0.0,
    val emergencyFundAccumulated: Double = 0.0,
    val availableBalanceMonth: Double = 0.0,
    val availableBalancePreviousMonths: Double = 0.0,
    val availableBalanceTotal: Double = 0.0,
    val isAccumulatingWithOtherMonths: Boolean = true,
    val projectedBalanceMonth: Double = 0.0,
    val projectedBalanceTotal: Double = 0.0
)

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth.asStateFlow()

    private val _expenseFilter = MutableStateFlow(ExpenseFilter.TODAS)
    val expenseFilter: StateFlow<ExpenseFilter> = _expenseFilter.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialData()
        }
    }

    val settings: StateFlow<AppSettings> = repository.getSettings()
        .flatMapLatest { s ->
            MutableStateFlow(s ?: AppSettings(id = 1, investmentPercentage = 10.0, leisurePercentage = 10.0, sumWithOtherMonths = true))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun toggleSumWithOtherMonths(enabled: Boolean? = null) {
        val target = enabled ?: !settings.value.sumWithOtherMonths
        viewModelScope.launch {
            repository.updateSumWithOtherMonths(target)
        }
    }

    val currentMonthlyRevenue: StateFlow<MonthlyRevenue> = _selectedYearMonth
        .flatMapLatest { ym ->
            repository.getMonthlyRevenue(ym.toString()).map {
                it ?: MonthlyRevenue(month = ym.toString(), salary = 0.0, extraIncome = 0.0)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MonthlyRevenue(month = YearMonth.now().toString(), salary = 0.0, extraIncome = 0.0)
        )

    val allMonthlyRevenues: StateFlow<List<MonthlyRevenue>> = repository.getAllMonthlyRevenues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthExpenses: StateFlow<List<Expense>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getExpensesForMonth(ym.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthRevenues: StateFlow<List<Revenue>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getRevenuesForMonth(ym.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRevenues: StateFlow<List<Revenue>> = repository.getAllRevenues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.getCategories()
        .map { list ->
            val collator = java.text.Collator.getInstance(java.util.Locale("pt", "BR")).apply {
                strength = java.text.Collator.PRIMARY
            }
            list.distinctBy { it.name.trim().lowercase() }
                .sortedWith { a, b -> collator.compare(a.name, b.name) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class MonthData(
        val ym: YearMonth,
        val monthlyRev: MonthlyRevenue,
        val revenues: List<Revenue>,
        val expenses: List<Expense>
    )

    private val currentMonthData = combine(
        _selectedYearMonth,
        currentMonthlyRevenue,
        currentMonthRevenues,
        currentMonthExpenses
    ) { ym, monthlyRev, revenues, expenses ->
        MonthData(ym, monthlyRev, revenues, expenses)
    }

    val summary: StateFlow<FinanceSummary> = combine(
        currentMonthData,
        allMonthlyRevenues,
        allRevenues,
        allExpenses,
        settings
    ) { monthData, allMonthlyRevs, allRevs, allExp, sett ->
        calculateSummary(
            yearMonth = monthData.ym,
            monthlyRev = monthData.monthlyRev,
            revenues = monthData.revenues,
            expenses = monthData.expenses,
            allMonthlyRevs = allMonthlyRevs,
            allRevs = allRevs,
            allExp = allExp,
            settings = sett
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinanceSummary(monthYear = YearMonth.now())
    )

    private fun calculateSummary(
        yearMonth: YearMonth,
        monthlyRev: MonthlyRevenue,
        revenues: List<Revenue>,
        expenses: List<Expense>,
        allMonthlyRevs: List<MonthlyRevenue>,
        allRevs: List<Revenue>,
        allExp: List<Expense>,
        settings: AppSettings
    ): FinanceSummary {
        val revenuesSum = revenues.sumOf { it.amount }
        val totalRevenue = if (revenues.isNotEmpty()) {
            revenuesSum
        } else {
            monthlyRev.salary + monthlyRev.extraIncome
        }
        val revenuesCount = if (revenues.isNotEmpty()) revenues.size else (if (totalRevenue > 0) 1 else 0)

        val salary = if (revenues.isNotEmpty()) revenuesSum else monthlyRev.salary
        val extraIncome = if (revenues.isNotEmpty()) 0.0 else monthlyRev.extraIncome

        // Monthly specific percentage or fallback to system defaults:
        val invPercent = monthlyRev.investmentPercentage ?: DEFAULT_SYSTEM_INVESTMENT_PERCENTAGE
        val catLimitName = (monthlyRev.categoryLimitName ?: settings.categoryLimitName).ifBlank { "Lazer" }
        val catLimitPercent = monthlyRev.categoryLimitPercentage
            ?: monthlyRev.leisurePercentage
            ?: settings.categoryLimitPercentage
        val isSummingWithOtherMonths = settings.sumWithOtherMonths

        val investmentAllocated = if (totalRevenue > 0) totalRevenue * (invPercent / 100.0) else 0.0

        // Limite por categoria flexível:
        val categoryLimitAmount = if (totalRevenue > 0) totalRevenue * (catLimitPercent / 100.0) else 0.0
        val categoryLimitSpent = expenses
            .filter { it.category.equals(catLimitName, ignoreCase = true) }
            .sumOf { it.amount }
        val categoryLimitAvailable = max(0.0, categoryLimitAmount - categoryLimitSpent)
        val categoryLimitOverBudget = max(0.0, categoryLimitSpent - categoryLimitAmount)

        // Para retrocompatibilidade de campos de lazer:
        val leisureLimit = categoryLimitAmount
        val leisureSpent = categoryLimitSpent
        val leisureAvailable = categoryLimitAvailable
        val leisureOverBudget = categoryLimitOverBudget

        val totalExpenses = expenses.sumOf { it.amount }
        val generalExpenses = expenses
            .filter { !it.category.equals(catLimitName, ignoreCase = true) }
            .sumOf { it.amount }

        val spendingLimit = max(0.0, totalRevenue - investmentAllocated)

        val today = LocalDate.now()
        var totalPaid = 0.0
        var totalPending = 0.0
        var totalOverdue = 0.0

        expenses.forEach { exp ->
            if (exp.isPaid) {
                totalPaid += exp.amount
            } else {
                val status = CurrencyUtils.computePaymentStatus(exp, today)
                if (status == com.example.data.model.PaymentStatus.VENCIDA) {
                    totalOverdue += exp.amount
                } else {
                    totalPending += exp.amount
                }
            }
        }

        val availableForSpending = max(0.0, spendingLimit - totalPaid)

        // Saldo disponível calculado para o mês selecionado (mês a mês):
        val availableMonth = if (totalRevenue > 0) {
            max(0.0, totalRevenue - investmentAllocated - totalPaid)
        } else {
            0.0
        }

        // Saldo disponível acumulado dos meses anteriores ao mês selecionado:
        val currentMonthStr = yearMonth.toString()
        val previousMonthsBalance = calculatePreviousMonthsAvailableBalance(
            currentMonthStr = currentMonthStr,
            allMonthlyRevs = allMonthlyRevs,
            allRevs = allRevs,
            allExp = allExp
        )

        val totalAccumulated = previousMonthsBalance + availableMonth
        val effectiveAccumulated = if (isSummingWithOtherMonths) totalAccumulated else availableMonth

        val projectedBalanceMonth = if (totalRevenue > 0) {
            max(0.0, totalRevenue - investmentAllocated - totalExpenses)
        } else {
            0.0
        }
        val remainingToPay = totalPending + totalOverdue
        val projectedBalanceTotal = max(0.0, totalAccumulated - remainingToPay)

        return FinanceSummary(
            monthYear = yearMonth,
            salary = salary,
            extraIncome = extraIncome,
            totalRevenue = totalRevenue,
            revenuesCount = revenuesCount,
            investmentPercentage = invPercent,
            investmentAllocated = investmentAllocated,
            leisurePercentage = catLimitPercent,
            leisureLimit = leisureLimit,
            leisureSpent = leisureSpent,
            leisureAvailable = leisureAvailable,
            leisureOverBudget = leisureOverBudget,
            categoryLimitName = catLimitName,
            categoryLimitPercentage = catLimitPercent,
            categoryLimitAmount = categoryLimitAmount,
            categoryLimitSpent = categoryLimitSpent,
            categoryLimitAvailable = categoryLimitAvailable,
            categoryLimitOverBudget = categoryLimitOverBudget,
            totalExpenses = totalExpenses,
            generalExpenses = generalExpenses,
            spendingLimit = spendingLimit,
            availableForSpending = availableForSpending,
            totalPaid = totalPaid,
            totalPending = totalPending,
            totalOverdue = totalOverdue,
            emergencyFundMonth = availableMonth,
            emergencyFundAccumulated = effectiveAccumulated,
            availableBalanceMonth = availableMonth,
            availableBalancePreviousMonths = previousMonthsBalance,
            availableBalanceTotal = totalAccumulated,
            isAccumulatingWithOtherMonths = isSummingWithOtherMonths,
            projectedBalanceMonth = projectedBalanceMonth,
            projectedBalanceTotal = projectedBalanceTotal
        )
    }

    private fun calculatePreviousMonthsAvailableBalance(
        currentMonthStr: String,
        allMonthlyRevs: List<MonthlyRevenue>,
        allRevs: List<Revenue>,
        allExp: List<Expense>
    ): Double {
        val revByMonth = allMonthlyRevs.associateBy { it.month }
        val revListByMonth = allRevs.groupBy { if (it.date.length >= 7) it.date.take(7) else "" }
        val expByMonth = allExp.groupBy { it.dueDate.take(7) }
        val previousMonthKeys = (revByMonth.keys + revListByMonth.keys + expByMonth.keys)
            .filter { it.isNotBlank() && it < currentMonthStr }
            .distinct()
            .sorted()

        var accumulated = 0.0
        for (m in previousMonthKeys) {
            val revObj = revByMonth[m]
            val fromList = revListByMonth[m]?.sumOf { it.amount } ?: 0.0
            val rev = if (fromList > 0.0) fromList else (revObj?.totalRevenue ?: 0.0)
            if (rev > 0) {
                val invPercent = revObj?.investmentPercentage ?: DEFAULT_SYSTEM_INVESTMENT_PERCENTAGE
                val inv = rev * (invPercent / 100.0)
                val totalPaidExp = expByMonth[m]?.filter { it.isPaid }?.sumOf { it.amount } ?: 0.0
                val sobra = max(0.0, rev - inv - totalPaidExp)
                accumulated += sobra
            }
        }
        return accumulated
    }

    fun setFilter(filter: ExpenseFilter) {
        _expenseFilter.value = filter
    }

    fun nextMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.minusMonths(1)
    }

    fun goToCurrentMonth() {
        _selectedYearMonth.value = YearMonth.now()
    }

    fun updateMonthlyRevenue(salary: Double, extraIncome: Double) {
        val month = _selectedYearMonth.value.toString()
        viewModelScope.launch {
            repository.saveMonthlyRevenue(month, salary, extraIncome)
        }
    }

    fun updateMonthlyPercentages(
        month: String = _selectedYearMonth.value.toString(),
        invPercent: Double?,
        leisurePercent: Double?,
        categoryLimitName: String? = null,
        categoryLimitPercent: Double? = null
    ) {
        viewModelScope.launch {
            repository.saveMonthlyPercentages(
                month = month,
                investmentPercentage = invPercent,
                leisurePercentage = leisurePercent,
                categoryLimitName = categoryLimitName,
                categoryLimitPercentage = categoryLimitPercent
            )
        }
    }

    fun saveCategoryLimit(
        month: String = _selectedYearMonth.value.toString(),
        categoryName: String,
        percentage: Double
    ) {
        viewModelScope.launch {
            val currentRev = repository.getMonthlyRevenueDirect(month)
            repository.saveMonthlyPercentages(
                month = month,
                investmentPercentage = currentRev?.investmentPercentage,
                leisurePercentage = percentage,
                categoryLimitName = categoryName,
                categoryLimitPercentage = percentage
            )
        }
    }

    fun resetMonthlyPercentagesToDefault(month: String = _selectedYearMonth.value.toString()) {
        viewModelScope.launch {
            repository.saveMonthlyPercentages(month, null, null, null, null)
        }
    }

    fun updateSalary(salary: Double) {
        val month = _selectedYearMonth.value.toString()
        val currentExtra = currentMonthlyRevenue.value.extraIncome
        viewModelScope.launch {
            repository.saveMonthlyRevenue(month, salary, currentExtra)
        }
    }

    fun updateExtraIncome(extraIncome: Double) {
        val month = _selectedYearMonth.value.toString()
        val currentSalary = currentMonthlyRevenue.value.salary
        viewModelScope.launch {
            repository.saveMonthlyRevenue(month, currentSalary, extraIncome)
        }
    }

    fun addRevenue(description: String, amount: Double, date: String) {
        viewModelScope.launch {
            val safeDate = if (date.isNotBlank()) date else "${_selectedYearMonth.value}-01"
            val safeDesc = if (description.isNotBlank()) description.trim() else "Receita"
            repository.addRevenue(
                Revenue(
                    description = safeDesc,
                    amount = max(0.0, amount),
                    date = safeDate
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

    fun addExpense(
        description: String,
        amount: Double,
        dueDate: String,
        isPaid: Boolean = false,
        recurrenceMonths: Int = 1,
        category: String = "Geral",
        isMonthlyRecurring: Boolean = false
    ) {
        viewModelScope.launch {
            repository.addExpense(
                description = description.trim(),
                amount = amount,
                dueDate = dueDate,
                isPaid = isPaid,
                recurrenceMonths = recurrenceMonths,
                category = category,
                isMonthlyRecurring = isMonthlyRecurring
            )
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

    fun toggleExpensePayment(expense: Expense) {
        viewModelScope.launch {
            repository.toggleExpensePaymentStatus(expense)
        }
    }

    fun stopRecurrence(groupId: String, currentDueDate: String) {
        viewModelScope.launch {
            repository.cancelRecurringExpense(groupId, currentDueDate)
        }
    }

    fun updateSettings(investmentPercent: Double, leisurePercent: Double) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(
                current.copy(
                    investmentPercentage = investmentPercent,
                    leisurePercentage = leisurePercent
                )
            )
        }
    }

    fun updateDefaultCategoryLimit(categoryName: String, percentage: Double) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(
                current.copy(
                    categoryLimitName = categoryName,
                    categoryLimitPercentage = percentage,
                    leisurePercentage = percentage
                )
            )
        }
    }

    fun updateTheme(isDarkMode: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(
                current.copy(
                    isDarkMode = isDarkMode
                )
            )
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(
                current.copy(
                    notificationsEnabled = enabled
                )
            )
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.addCategory(name)
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}

class FinanceViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
