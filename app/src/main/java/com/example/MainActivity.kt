package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Expense
import com.example.data.model.Revenue
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.ExpenseNotificationManager

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ExpenseNotificationManager.createNotificationChannel(this)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isDark = when (settings.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: FinanceViewModel) {
    var currentScreenIndex by remember { mutableIntStateOf(0) } // 0 = Home, 1 = Despesas, 2 = Configurações

    var isAddSheetVisible by remember { mutableStateOf(false) }
    var isPercentagesSheetVisible by remember { mutableStateOf(false) }
    var isRevenueSheetVisible by remember { mutableStateOf(false) }

    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingRevenue by remember { mutableStateOf<Revenue?>(null) }

    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val currentExpenses by viewModel.currentMonthExpenses.collectAsStateWithLifecycle()
    val currentRevenues by viewModel.currentMonthRevenues.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryTotals by viewModel.categoryTotals.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val expenseFilter by viewModel.expenseFilter.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                NavigationBarItem(
                    selected = currentScreenIndex == 0,
                    onClick = { currentScreenIndex = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") },
                    modifier = Modifier.testTag("nav_item_home")
                )
                NavigationBarItem(
                    selected = currentScreenIndex == 1,
                    onClick = { currentScreenIndex = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Despesas") },
                    label = { Text("Despesas") },
                    modifier = Modifier.testTag("nav_item_expenses")
                )
                NavigationBarItem(
                    selected = currentScreenIndex == 2,
                    onClick = { currentScreenIndex = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Configurações") },
                    label = { Text("Ajustes") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentScreenIndex != 2) {
                FloatingActionButton(
                    onClick = { isAddSheetVisible = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova Transação")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreenIndex) {
                0 -> HomeScreen(
                    selectedYearMonth = selectedYearMonth,
                    summary = summary,
                    revenuesCount = currentRevenues.size,
                    categoryTotals = categoryTotals,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onResetCurrentMonth = { viewModel.resetCurrentMonth() },
                    onEditRevenue = { isRevenueSheetVisible = true },
                    onEditPercentages = { isPercentagesSheetVisible = true },
                    onToggleSumWithOtherMonths = { viewModel.toggleSumWithOtherMonths(it) }
                )
                1 -> TransactionsScreen(
                    selectedYearMonth = selectedYearMonth,
                    expenses = currentExpenses,
                    summary = summary,
                    currentFilter = expenseFilter,
                    onFilterSelected = { viewModel.setFilter(it) },
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onResetCurrentMonth = { viewModel.resetCurrentMonth() },
                    onTogglePaid = { viewModel.toggleExpensePaid(it) },
                    onEditExpense = { editingExpense = it },
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )
                2 -> SettingsScreen(
                    settings = settings,
                    categories = categories,
                    onUpdateTheme = { viewModel.updateTheme(it) },
                    onUpdateNotifications = { viewModel.updateNotifications(it) },
                    onAddCategory = { viewModel.addCategory(it) },
                    onDeleteCategory = { viewModel.deleteCategory(it) }
                )
            }
        }
    }

    // Modal Nova Transação
    if (isAddSheetVisible) {
        AddTransactionSheet(
            categories = categories,
            onDismiss = { isAddSheetVisible = false },
            onAddExpense = { desc, amount, date, cat, isMonthly, installments, notes ->
                viewModel.addExpense(desc, amount, date, cat, isMonthly, installments, notes)
            },
            onAddRevenue = { desc, amount, date, isRecurring ->
                viewModel.addRevenue(desc, amount, date, isRecurring)
            }
        )
    }

    // Modal Percentuais e Limite
    if (isPercentagesSheetVisible) {
        MonthlyPercentagesSheet(
            selectedYearMonth = selectedYearMonth,
            currentInvestmentPercent = summary.investmentPercentage,
            currentCategoryLimitPercent = summary.categoryLimitPercentage,
            currentCategoryLimitName = summary.categoryLimitName,
            totalRevenue = summary.totalRevenue,
            categories = categories,
            onDismiss = { isPercentagesSheetVisible = false },
            onSavePercentages = { invest, limit, catName ->
                viewModel.updateMonthlyPercentages(invest, limit, catName)
            }
        )
    }

    // Modal Gerenciamento de Receitas
    if (isRevenueSheetVisible) {
        MonthlyRevenueSheet(
            selectedYearMonth = selectedYearMonth,
            revenues = currentRevenues,
            onDismiss = { isRevenueSheetVisible = false },
            onEditRevenue = { editingRevenue = it },
            onDeleteRevenue = { viewModel.deleteRevenue(it) }
        )
    }

    // Diálogos de Edição
    editingExpense?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            categories = categories,
            onDismiss = { editingExpense = null },
            onSave = {
                viewModel.updateExpense(it)
                editingExpense = null
            }
        )
    }

    editingRevenue?.let { revenue ->
        EditRevenueDialog(
            revenue = revenue,
            onDismiss = { editingRevenue = null },
            onSave = {
                viewModel.updateRevenue(it)
                editingRevenue = null
            }
        )
    }
}
