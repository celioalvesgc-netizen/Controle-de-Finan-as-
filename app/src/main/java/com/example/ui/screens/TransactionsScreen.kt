package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.ExpenseFilter
import com.example.data.model.PaymentStatus
import com.example.data.model.Revenue
import com.example.ui.components.MonthSelector
import com.example.ui.components.StatusBadge
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingYellow
import com.example.ui.viewmodel.FinanceSummary
import com.example.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    revenues: List<Revenue> = emptyList(),
    expenses: List<Expense>,
    allExpenses: List<Expense> = emptyList(),
    summary: FinanceSummary,
    selectedYearMonth: YearMonth,
    currentFilter: ExpenseFilter,
    onFilterChange: (ExpenseFilter) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onToggleExpensePayment: (Expense) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onStopRecurrence: (String, String) -> Unit,
    onEditRevenue: (Revenue) -> Unit = {},
    onDeleteRevenue: (Revenue) -> Unit = {},
    onAddExpenseClick: () -> Unit = {},
    onAddRevenueClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expenseToViewDetails by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var recurringToStop by remember { mutableStateOf<Expense?>(null) }
    var showNewTransactionChoice by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }

    // Ordenação Inteligente:
    // 1. Vencidas primeiro
    // 2. A Pagar
    // 3. Pagas
    // Dentro de cada grupo: por data de vencimento
    val sortedExpenses = remember(expenses, today) {
        expenses.sortedWith(
            compareBy<Expense> { exp ->
                val status = CurrencyUtils.computePaymentStatus(exp, today)
                when (status) {
                    PaymentStatus.VENCIDA -> 0
                    PaymentStatus.A_PAGAR -> 1
                    PaymentStatus.PAGA -> 2
                }
            }.thenBy { it.dueDate }
        )
    }

    // Filtragem conforme a aba selecionada (Regra 8)
    val filteredExpenses = remember(sortedExpenses, currentFilter, today) {
        when (currentFilter) {
            ExpenseFilter.TODAS -> sortedExpenses
            ExpenseFilter.A_PAGAR -> sortedExpenses.filter {
                !it.isPaid && CurrencyUtils.computePaymentStatus(it, today) == PaymentStatus.A_PAGAR
            }
            ExpenseFilter.PAGAS -> sortedExpenses.filter { it.isPaid }
            ExpenseFilter.VENCIDAS -> sortedExpenses.filter {
                !it.isPaid && CurrencyUtils.computePaymentStatus(it, today) == PaymentStatus.VENCIDA
            }
            else -> sortedExpenses
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Navegação de Mês
        item {
            MonthSelector(
                selectedYearMonth = selectedYearMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onResetCurrentMonth = onResetCurrentMonth
            )
        }

        // 2. Botão para adicionar despesa/receita bem visível e fácil de acessar (Regra 3)
        item {
            Button(
                onClick = { showNewTransactionChoice = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_header_add_expense"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Novo Lançamento",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Resumo simples e limpo do mês (Regras 6 e 7)
        item {
            MonthlyExpenseSummaryCard(summary = summary)
        }

        // 4. Filtros simples e objetivos: Todas | A Pagar | Pagas | Vencidas (Regra 8)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter == ExpenseFilter.TODAS,
                    onClick = { onFilterChange(ExpenseFilter.TODAS) },
                    label = { Text("Todas", style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentFilter == ExpenseFilter.TODAS) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("filter_chip_todas")
                )

                FilterChip(
                    selected = currentFilter == ExpenseFilter.A_PAGAR,
                    onClick = { onFilterChange(ExpenseFilter.A_PAGAR) },
                    label = { Text("A Pagar", style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentFilter == ExpenseFilter.A_PAGAR) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("filter_chip_a_pagar")
                )

                FilterChip(
                    selected = currentFilter == ExpenseFilter.PAGAS,
                    onClick = { onFilterChange(ExpenseFilter.PAGAS) },
                    label = { Text("Pagas", style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentFilter == ExpenseFilter.PAGAS) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("filter_chip_pagas")
                )

                FilterChip(
                    selected = currentFilter == ExpenseFilter.VENCIDAS,
                    onClick = { onFilterChange(ExpenseFilter.VENCIDAS) },
                    label = { Text("Vencidas", style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentFilter == ExpenseFilter.VENCIDAS) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("filter_chip_vencidas")
                )
            }
        }

        // 5. Lista de Despesas (Regras 4, 5, 9, 10)
        if (filteredExpenses.isEmpty()) {
            item {
                EmptyStateCard(
                    message = when (currentFilter) {
                        ExpenseFilter.A_PAGAR -> "Nenhuma conta a pagar neste mês! 🎉"
                        ExpenseFilter.PAGAS -> "Nenhuma conta paga registrada neste filtro."
                        ExpenseFilter.VENCIDAS -> "Nenhuma conta vencida! Tudo em dia. 👍"
                        else -> "Nenhuma despesa cadastrada neste mês."
                    }
                )
            }
        } else {
            itemsIndexed(filteredExpenses, key = { _, exp -> "exp_${exp.id}" }) { index, exp ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    IntuitiveExpenseCard(
                        expense = exp,
                        onTogglePaid = { onToggleExpensePayment(exp) },
                        onCardClick = { expenseToViewDetails = exp }
                    )
                    if (index < filteredExpenses.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Detalhes e Ações da Despesa
    expenseToViewDetails?.let { exp ->
        val groupOccurrences = remember(exp.recurringGroupId, allExpenses) {
            if (exp.recurringGroupId != null) {
                allExpenses
                    .filter { it.recurringGroupId == exp.recurringGroupId }
                    .sortedBy { it.dueDate }
            } else {
                emptyList()
            }
        }

        ExpenseDetailsSheet(
            expense = exp,
            groupOccurrences = groupOccurrences,
            onDismiss = { expenseToViewDetails = null },
            onTogglePaid = {
                onToggleExpensePayment(exp)
                expenseToViewDetails = null
            },
            onEdit = {
                onEditExpense(exp)
                expenseToViewDetails = null
            },
            onDelete = {
                expenseToDelete = exp
                expenseToViewDetails = null
            },
            onStopRecurrence = {
                recurringToStop = exp
                expenseToViewDetails = null
            }
        )
    }

    // Confirmação de Exclusão da Despesa
    expenseToDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Excluir Despesa", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente excluir \"${exp.description}\"? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(exp)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmação para Encerrar Recorrência
    recurringToStop?.let { exp ->
        AlertDialog(
            onDismissRequest = { recurringToStop = null },
            title = { Text("Encerrar Recorrência?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Esta ação cancelará apenas os lançamentos futuros não pagos desta despesa. " +
                    "Todos os meses anteriores e pagamentos já realizados permanecerão intactos no histórico."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        exp.recurringGroupId?.let { gid ->
                            onStopRecurrence(gid, exp.dueDate)
                        }
                        recurringToStop = null
                    }
                ) {
                    Text("Encerrar Programação Futura")
                }
            },
            dismissButton = {
                TextButton(onClick = { recurringToStop = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de escolha de tipo de lançamento: Despesa ou Receita
    if (showNewTransactionChoice) {
        ModalBottomSheet(
            onDismissRequest = { showNewTransactionChoice = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Novo Lançamento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Escolha o que deseja cadastrar:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Opção 1: Despesa
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showNewTransactionChoice = false
                            onAddExpenseClick()
                        }
                        .testTag("choice_option_despesa"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Despesa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Contas a pagar, boletos, compras ou gastos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opção 2: Receita
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showNewTransactionChoice = false
                            onAddRevenueClick()
                        }
                        .testTag("choice_option_receita"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Receita",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Salário, renda extra ou entradas financeiras",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Resumo mensal limpo, objetivo e espaçoso de despesas (Regras 6 e 7)
 */
@Composable
fun MonthlyExpenseSummaryCard(
    summary: FinanceSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_expense_summary"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Linha Principal: Total de Despesas do Mês
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL DO MÊS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = CurrencyUtils.formatCurrency(summary.totalExpenses),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Sub-métricas: A Pagar | Pagas | Vencidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // A Pagar
                Column {
                    Text(
                        text = "A Pagar",
                        style = MaterialTheme.typography.labelSmall,
                        color = PendingYellow,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyUtils.formatCurrency(summary.totalPending),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Pagas
                Column {
                    Text(
                        text = "Pagas",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaidGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyUtils.formatCurrency(summary.totalPaid),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Vencidas
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vencidas",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (summary.totalOverdue > 0) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyUtils.formatCurrency(summary.totalOverdue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.totalOverdue > 0) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Card de Despesa Ultra-Intuitivo e Limpo
 * Exibe APENAS (Regra 4):
 * - Descrição
 * - Valor
 * - Data de vencimento
 * - Status
 *
 * Com ação direta na lista (Regra 5):
 * - Botão "Marcar como Paga" diretamente acessível
 * - Despesas recorrentes sinalizadas de forma sutil sem poluição (Regra 9)
 */
@Composable
fun IntuitiveExpenseCard(
    expense: Expense,
    onTogglePaid: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = CurrencyUtils.computePaymentStatus(expense)
    val isRecurring = expense.recurringGroupId != null
    val dueDateText = "Vence ${CurrencyUtils.formatToDisplayDate(expense.dueDate)}"
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Cores dos status: 🟢 Paga | 🟡 A Pagar | 🔴 Vencida
    val statusColor = when (status) {
        PaymentStatus.PAGA -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
        PaymentStatus.A_PAGAR -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        PaymentStatus.VENCIDA -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 1. Faixa lateral colorida no card (5.dp) para identificação rápida imediata
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(statusColor)
            )

            // Conteúdo principal do Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                // Linha 1: Descrição e Valor (com indicador colorido próximo ao valor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = expense.description,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (isRecurring) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = "Recorrente",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (expense.recurringTotal > 1) {
                                val remaining = kotlin.math.max(0, expense.recurringTotal - expense.recurringIndex)
                                Text(
                                    text = "Parcela ${expense.recurringIndex} de ${expense.recurringTotal} • $remaining restantes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (expense.recurringTotal == -1) {
                                Text(
                                    text = "Mensal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // 2. Indicador visual colorido próximo ao valor
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CurrencyUtils.formatCurrency(expense.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (expense.isPaid || status == PaymentStatus.VENCIDA) {
                                statusColor
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Linha 2: Data de Vencimento com ícone colorido + Botão de Ação Direta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 3. Indicador colorido e ícone próximo à data de vencimento
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(statusColor.copy(alpha = if (isDark) 0.22f else 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (status) {
                                    PaymentStatus.PAGA -> Icons.Default.Check
                                    PaymentStatus.A_PAGAR -> Icons.Default.CalendarToday
                                    PaymentStatus.VENCIDA -> Icons.Default.Close
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dueDateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status == PaymentStatus.VENCIDA && !expense.isPaid) {
                                statusColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (status == PaymentStatus.VENCIDA && !expense.isPaid) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // 4. Botão de Ação: Diferenciação destacada entre 🟢 Paga (botão) e 🟡 A Pagar (botão)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (status) {
                            PaymentStatus.PAGA -> if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)
                            PaymentStatus.A_PAGAR -> if (isDark) Color(0xFF422006).copy(alpha = 0.55f) else Color(0xFFFEF3C7)
                            PaymentStatus.VENCIDA -> if (isDark) Color(0xFF450A0A).copy(alpha = 0.55f) else Color(0xFFFEE2E2)
                        },
                        border = BorderStroke(
                            1.dp,
                            statusColor.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onTogglePaid() }
                            .testTag(if (expense.isPaid) "btn_unmark_paid_${expense.id}" else "btn_quick_mark_paid_${expense.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = when (status) {
                                    PaymentStatus.PAGA -> Icons.Default.Check
                                    PaymentStatus.A_PAGAR -> Icons.Default.RadioButtonUnchecked
                                    PaymentStatus.VENCIDA -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = if (expense.isPaid) "Despesa Paga" else "Marcar como Paga",
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * BottomSheet de Detalhes da Despesa: simples, limpo e direto
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsSheet(
    expense: Expense,
    groupOccurrences: List<Expense>,
    onDismiss: () -> Unit,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStopRecurrence: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val status = CurrencyUtils.computePaymentStatus(expense)
    val isRecurring = expense.recurringGroupId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("expense_details_sheet")
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalhes da Despesa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cartão de Informações Essenciais (Regra 4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = CurrencyUtils.formatCurrency(expense.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (expense.isPaid) PaidGreen else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailItemRow(
                        label = "Categoria:",
                        value = if (expense.category.isNotBlank()) expense.category else "Outros",
                        icon = Icons.Default.Category
                    )

                    DetailItemRow(
                        label = "Data de Vencimento:",
                        value = CurrencyUtils.formatToDisplayDate(expense.dueDate),
                        icon = Icons.Default.CalendarToday
                    )

                    DetailItemRow(
                        label = "Status:",
                        customContent = { StatusBadge(status = status) }
                    )

                    if (expense.isPaid && !expense.paidDate.isNullOrBlank()) {
                        DetailItemRow(
                            label = "Data do Pagamento:",
                            value = CurrencyUtils.formatToDisplayDate(expense.paidDate),
                            icon = Icons.Default.Payments,
                            highlightColor = PaidGreen
                        )
                    }
                }
            }

            // Seção de recorrência quando aplicável (Requisito 6)
            if (isRecurring && groupOccurrences.isNotEmpty()) {
                val isInstallment = expense.recurringTotal > 1
                val remainingInstallments = if (isInstallment) kotlin.math.max(0, expense.recurringTotal - expense.recurringIndex) else 0

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EventRepeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isInstallment) "PARCELAMENTO" else "RECORRÊNCIA MENSAL",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isInstallment) {
                                Text(
                                    text = "Parcela atual: ${expense.recurringIndex} de ${expense.recurringTotal} • $remainingInstallments restantes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Sem prazo definido (contínua até encerramento)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "Encerrar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable { onStopRecurrence() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        groupOccurrences.forEachIndexed { index, occ ->
                            val occStatus = CurrencyUtils.computePaymentStatus(occ)
                            val monthName = CurrencyUtils.getMonthNameFromIso(occ.dueDate)
                            val shortDate = CurrencyUtils.formatToShortDate(occ.dueDate)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$monthName ($shortDate)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = CurrencyUtils.formatCurrency(occ.amount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                StatusBadge(status = occStatus)
                            }

                            if (index < groupOccurrences.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botões de Ação
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Marcar / Desmarcar como Paga
                Button(
                    onClick = onTogglePaid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_details_toggle_paid"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (expense.isPaid) MaterialTheme.colorScheme.secondary else PaidGreen
                    )
                ) {
                    Icon(
                        imageVector = if (expense.isPaid) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (expense.isPaid) "Desmarcar Pagamento" else "MARCAR COMO PAGA",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Editar e Excluir lado a lado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailItemRow(
    label: String,
    value: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    highlightColor: Color = MaterialTheme.colorScheme.onSurface,
    customContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (customContent != null) {
            customContent()
        } else if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = highlightColor
            )
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
