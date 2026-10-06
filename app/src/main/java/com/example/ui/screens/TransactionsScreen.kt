package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.model.FinanceSummary
import com.example.data.model.PaymentStatus
import com.example.ui.components.MonthSelectorCard
import com.example.ui.theme.*
import com.example.util.CurrencyUtils
import java.time.YearMonth

@Composable
fun TransactionsScreen(
    selectedYearMonth: YearMonth,
    expenses: List<Expense>,
    summary: FinanceSummary,
    currentFilter: ExpenseFilter,
    onFilterSelected: (ExpenseFilter) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onTogglePaid: (Expense) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExpenseForDetails by remember { mutableStateOf<Expense?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MonthSelectorCard(
                selectedYearMonth = selectedYearMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onResetCurrentMonth = onResetCurrentMonth
            )
        }

        item {
            MonthlyExpenseSummaryCard(summary = summary)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseFilter.entries.forEach { filter ->
                    val isSelected = filter == currentFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    ExpenseFilter.TODAS -> "Todas"
                                    ExpenseFilter.PAGAS -> "Pagas"
                                    ExpenseFilter.A_PAGAR -> "A Pagar"
                                    ExpenseFilter.VENCIDAS -> "Vencidas"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }
        }

        val filteredExpenses = expenses.filter { exp ->
            val status = CurrencyUtils.computePaymentStatus(exp)
            when (currentFilter) {
                ExpenseFilter.TODAS -> true
                ExpenseFilter.PAGAS -> status == PaymentStatus.PAGA
                ExpenseFilter.A_PAGAR -> status == PaymentStatus.A_PAGAR
                ExpenseFilter.VENCIDAS -> status == PaymentStatus.VENCIDA
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                EmptyStateCard(
                    message = when (currentFilter) {
                        ExpenseFilter.TODAS -> "Nenhuma despesa cadastrada neste mês."
                        ExpenseFilter.PAGAS -> "Nenhuma despesa paga neste mês."
                        ExpenseFilter.A_PAGAR -> "Nenhuma despesa pendente neste mês."
                        ExpenseFilter.VENCIDAS -> "Nenhuma despesa vencida. Tudo em dia!"
                    }
                )
            }
        } else {
            items(filteredExpenses, key = { it.id }) { expense ->
                IntuitiveExpenseCard(
                    expense = expense,
                    onTogglePaid = { onTogglePaid(expense) },
                    onCardClick = { selectedExpenseForDetails = expense }
                )
            }
        }
    }

    selectedExpenseForDetails?.let { expense ->
        ExpenseDetailsSheet(
            expense = expense,
            onDismiss = { selectedExpenseForDetails = null },
            onTogglePaid = {
                onTogglePaid(expense)
                selectedExpenseForDetails = null
            },
            onEdit = {
                onEditExpense(expense)
                selectedExpenseForDetails = null
            },
            onDelete = {
                onDeleteExpense(expense)
                selectedExpenseForDetails = null
            }
        )
    }
}

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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (status) {
                                PaymentStatus.PAGA -> Icons.Default.CheckCircle
                                PaymentStatus.A_PAGAR -> Icons.Default.Schedule
                                PaymentStatus.VENCIDA -> Icons.Default.ErrorOutline
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (expense.isPaid) "Paga" else dueDateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status == PaymentStatus.VENCIDA) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (status == PaymentStatus.VENCIDA) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    FilledTonalButton(
                        onClick = onTogglePaid,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_toggle_paid_${expense.id}"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (expense.isPaid) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = if (expense.isPaid) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (expense.isPaid) Icons.Default.Undo else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (expense.isPaid) "Desmarcar" else "Pagar",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsSheet(
    expense: Expense,
    onDismiss: () -> Unit,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val status = CurrencyUtils.computePaymentStatus(expense)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalhes da Despesa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatCurrency(expense.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Vencimento: ${CurrencyUtils.formatToDisplayDate(expense.dueDate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Categoria: ${expense.category}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Status: ${when (status) {
                            PaymentStatus.PAGA -> "Paga"
                            PaymentStatus.A_PAGAR -> "A Pagar (Pendente)"
                            PaymentStatus.VENCIDA -> "Vencida"
                        }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when (status) {
                            PaymentStatus.PAGA -> PaidGreen
                            PaymentStatus.A_PAGAR -> PendingYellow
                            PaymentStatus.VENCIDA -> OverdueRed
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTogglePaid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_details_toggle_paid"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (expense.isPaid) Icons.Default.Undo else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (expense.isPaid) "Desmarcar pagamento" else "Marcar como paga",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_details_edit"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar", maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_details_delete"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OverdueRed),
                        border = BorderStroke(1.dp, OverdueRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir", maxLines = 1, softWrap = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
