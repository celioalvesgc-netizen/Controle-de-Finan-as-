package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.model.Expense
import com.example.data.model.Revenue
import com.example.data.repository.FinanceRepository
import com.example.ui.screens.AddTransactionSheet
import com.example.ui.screens.EditExpenseDialog
import com.example.ui.screens.EditRevenueDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory
import kotlinx.coroutines.launch

enum class MainTab {
    INICIO,
    LANCAMENTOS,
    AJUSTES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val repository = remember {
                FinanceRepository(
                    revenueDao = database.revenueDao(),
                    expenseDao = database.expenseDao(),
                    categoryDao = database.categoryDao(),
                    settingsDao = database.settingsDao(),
                    monthlyRevenueDao = database.monthlyRevenueDao()
                )
            }

            val viewModel: FinanceViewModel = viewModel(
                factory = FinanceViewModelFactory(repository)
            )

            val settings by viewModel.settings.collectAsStateWithLifecycle()

            // Inicializar notificações de despesas caso estejam ativadas
            androidx.compose.runtime.LaunchedEffect(settings.notificationsEnabled) {
                if (settings.notificationsEnabled) {
                    com.example.util.ExpenseNotificationManager.createNotificationChannel(context)
                    com.example.util.ExpenseNotificationManager.scheduleDailyReminder(context)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.util.ExpenseNotificationManager.checkAndNotifyExpenses(context)
                    }
                }
            }

            MyApplicationTheme(darkTheme = settings.isDarkMode) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.INICIO) }
    var isAddSheetVisible by remember { mutableStateOf(false) }
    var isRevenueSheetVisible by remember { mutableStateOf(false) }
    var isPercentagesSheetVisible by remember { mutableStateOf(false) }
    var addTransactionInitialType by remember { mutableStateOf(com.example.ui.screens.TransactionType.DESPESA) }

    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var revenueToEdit by remember { mutableStateOf<Revenue?>(null) }

    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val currentMonthlyRevenue by viewModel.currentMonthlyRevenue.collectAsStateWithLifecycle()
    val allMonthlyRevenues by viewModel.allMonthlyRevenues.collectAsStateWithLifecycle()
    val currentExpenses by viewModel.currentMonthExpenses.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val expenseFilter by viewModel.expenseFilter.collectAsStateWithLifecycle()
    val currentRevenues by viewModel.currentMonthRevenues.collectAsStateWithLifecycle()

    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = currentTab != MainTab.INICIO) {
        currentTab = MainTab.INICIO
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Meu Financeiro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") },
                    selected = currentTab == MainTab.INICIO,
                    onClick = { currentTab = MainTab.INICIO },
                    modifier = Modifier.testTag("nav_item_inicio")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Payments, contentDescription = "Lançamentos") },
                    label = { Text("Lançamentos") },
                    selected = currentTab == MainTab.LANCAMENTOS,
                    onClick = { currentTab = MainTab.LANCAMENTOS },
                    modifier = Modifier.testTag("nav_item_lancamentos")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    selected = currentTab == MainTab.AJUSTES,
                    onClick = { currentTab = MainTab.AJUSTES },
                    modifier = Modifier.testTag("nav_item_ajustes")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.INICIO -> {
                    HomeScreen(
                        summary = summary,
                        expenses = currentExpenses,
                        selectedYearMonth = selectedYearMonth,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onResetCurrentMonth = { viewModel.goToCurrentMonth() },
                        onNavigateToTransactions = { currentTab = MainTab.LANCAMENTOS },
                        onEditRevenue = { isRevenueSheetVisible = true },
                        onEditPercentages = { isPercentagesSheetVisible = true },
                        onToggleSumWithOtherMonths = { viewModel.toggleSumWithOtherMonths(it) }
                    )
                }
                MainTab.LANCAMENTOS -> {
                    TransactionsScreen(
                        expenses = currentExpenses,
                        allExpenses = allExpenses,
                        summary = summary,
                        selectedYearMonth = selectedYearMonth,
                        currentFilter = expenseFilter,
                        onFilterChange = { viewModel.setFilter(it) },
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onResetCurrentMonth = { viewModel.goToCurrentMonth() },
                        onToggleExpensePayment = {
                            viewModel.toggleExpensePayment(it)
                            com.example.util.ExpenseNotificationManager.cancelNotificationForExpense(context, it.id)
                        },
                        onEditExpense = { expenseToEdit = it },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onStopRecurrence = { gid, date -> viewModel.stopRecurrence(gid, date) },
                        onEditRevenue = { isRevenueSheetVisible = true },
                        onDeleteRevenue = { },
                        onAddExpenseClick = {
                            addTransactionInitialType = com.example.ui.screens.TransactionType.DESPESA
                            isAddSheetVisible = true
                        },
                        onAddRevenueClick = {
                            addTransactionInitialType = com.example.ui.screens.TransactionType.RECEITA
                            isAddSheetVisible = true
                        }
                    )
                }
                MainTab.AJUSTES -> {
                    SettingsScreen(
                        settings = settings,
                        allMonthlyRevenues = allMonthlyRevenues,
                        selectedYearMonth = selectedYearMonth,
                        categories = categories,
                        emergencyFundAccumulated = summary.emergencyFundAccumulated,
                        onSaveMonthPercentages = { month, inv, leisure ->
                            viewModel.updateMonthlyPercentages(month, inv, leisure)
                        },
                        onAddCategory = { viewModel.addCategory(it) },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onToggleTheme = { viewModel.updateTheme(it) },
                        onToggleNotifications = { viewModel.updateNotificationsEnabled(it) }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for adjusting monthly percentages and category limit
    if (isPercentagesSheetVisible) {
        com.example.ui.screens.MonthlyPercentagesSheet(
            selectedYearMonth = selectedYearMonth,
            currentInvestmentPercent = summary.investmentPercentage,
            currentLeisurePercent = summary.leisurePercentage,
            defaultInvestmentPercent = 20.0,
            defaultLeisurePercent = 10.0,
            totalRevenue = summary.totalRevenue,
            availableCategories = categories,
            currentCategoryLimitName = summary.categoryLimitName,
            currentCategoryLimitPercent = summary.categoryLimitPercentage,
            onSavePercentages = { inv, catName, catPercent ->
                viewModel.updateMonthlyPercentages(
                    month = selectedYearMonth.toString(),
                    invPercent = inv,
                    leisurePercent = catPercent,
                    categoryLimitName = catName,
                    categoryLimitPercent = catPercent
                )
            },
            onResetToDefault = {
                viewModel.resetMonthlyPercentagesToDefault(selectedYearMonth.toString())
            },
            onDismiss = { isPercentagesSheetVisible = false }
        )
    }

    // Modal Bottom Sheet for Monthly Revenues management (Multiple Revenues)
    if (isRevenueSheetVisible) {
        com.example.ui.screens.MonthlyRevenueSheet(
            selectedYearMonth = selectedYearMonth,
            revenues = currentRevenues,
            currentSalary = summary.salary,
            currentExtraIncome = summary.extraIncome,
            onDismiss = { isRevenueSheetVisible = false },
            onAddRevenue = { desc, amount, date ->
                viewModel.addRevenue(desc, amount, date)
            },
            onEditRevenue = { rev ->
                revenueToEdit = rev
            },
            onDeleteRevenue = { rev ->
                viewModel.deleteRevenue(rev)
            },
            onSave = { sal, extra ->
                viewModel.updateMonthlyRevenue(sal, extra)
            }
        )
    }

    // Modal Bottom Sheet for adding transactions
    if (isAddSheetVisible) {
        AddTransactionSheet(
            sheetState = addSheetState,
            categories = categories,
            selectedYearMonth = selectedYearMonth,
            currentSalary = summary.salary,
            currentExtraIncome = summary.extraIncome,
            leisureLimit = summary.leisureLimit,
            leisureSpent = summary.leisureSpent,
            initialType = addTransactionInitialType,
            onDismiss = { isAddSheetVisible = false },
            onSaveMonthlyRevenue = { sal, extra ->
                viewModel.updateMonthlyRevenue(sal, extra)
            },
            onAddRevenue = { desc, amount, date ->
                viewModel.addRevenue(desc, amount, date)
            },
            onAddExpense = { desc, amount, date, isPaid, rec, cat, isMonthly ->
                viewModel.addExpense(
                    description = desc,
                    amount = amount,
                    dueDate = date,
                    isPaid = isPaid,
                    recurrenceMonths = rec,
                    category = cat,
                    isMonthlyRecurring = isMonthly
                )
            }
        )
    }

    // Dialog for editing an expense
    expenseToEdit?.let { exp ->
        EditExpenseDialog(
            expense = exp,
            categories = categories,
            leisureLimit = summary.leisureLimit,
            leisureSpent = summary.leisureSpent,
            onDismiss = { expenseToEdit = null },
            onConfirm = { updated ->
                viewModel.updateExpense(updated)
                expenseToEdit = null
            }
        )
    }

    // Dialog for editing a revenue
    revenueToEdit?.let { rev ->
        EditRevenueDialog(
            revenue = rev,
            onDismiss = { revenueToEdit = null },
            onConfirm = { updated ->
                viewModel.updateRevenue(updated)
                revenueToEdit = null
            }
        )
    }
}

