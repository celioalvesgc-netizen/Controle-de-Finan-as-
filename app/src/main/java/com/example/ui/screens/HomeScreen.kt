package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.components.FinancialMetricCard
import com.example.ui.components.MonthSelector
import com.example.ui.theme.EmergencyTeal
import com.example.ui.theme.ExpenseCardDarkAmount
import com.example.ui.theme.ExpenseCardDarkBg
import com.example.ui.theme.ExpenseCardDarkBorder
import com.example.ui.theme.ExpenseCardDarkIcon
import com.example.ui.theme.ExpenseCardDarkIconBg
import com.example.ui.theme.ExpenseCardDarkTitle
import com.example.ui.theme.ExpenseCardLightAmount
import com.example.ui.theme.ExpenseCardLightBg
import com.example.ui.theme.ExpenseCardLightBorder
import com.example.ui.theme.ExpenseCardLightIcon
import com.example.ui.theme.ExpenseCardLightIconBg
import com.example.ui.theme.ExpenseCardLightTitle
import com.example.ui.theme.InvestmentBlue
import com.example.ui.theme.LeisurePurple
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingYellow
import com.example.ui.theme.RevenueCardDarkAmount
import com.example.ui.theme.RevenueCardDarkBg
import com.example.ui.theme.RevenueCardDarkBorder
import com.example.ui.theme.RevenueCardDarkIcon
import com.example.ui.theme.RevenueCardDarkIconBg
import com.example.ui.theme.RevenueCardDarkSubtitle
import com.example.ui.theme.RevenueCardDarkTitle
import com.example.ui.theme.RevenueCardLightAmount
import com.example.ui.theme.RevenueCardLightBg
import com.example.ui.theme.RevenueCardLightBorder
import com.example.ui.theme.RevenueCardLightIcon
import com.example.ui.theme.RevenueCardLightIconBg
import com.example.ui.theme.RevenueCardLightSubtitle
import com.example.ui.theme.RevenueCardLightTitle
import com.example.ui.viewmodel.FinanceSummary
import com.example.util.CurrencyUtils
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    summary: FinanceSummary,
    expenses: List<Expense> = emptyList(),
    selectedYearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onEditRevenue: () -> Unit = {},
    onEditPercentages: () -> Unit = {},
    onToggleSumWithOtherMonths: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Gastos por Categoria (Regra 5)
    val categoryTotals = remember(expenses) {
        if (expenses.isEmpty()) emptyList()
        else {
            expenses
                .groupBy { if (it.category.isNotBlank()) it.category else "Outros" }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Navegação de Mês
        item {
            MonthSelector(
                selectedYearMonth = selectedYearMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onResetCurrentMonth = onResetCurrentMonth
            )
        }

        // 1. RECEITA DO MÊS (Paleta Azul, múltiplas receitas, sem Salário/Renda Extra fixos)
        item {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

            val revBg = if (isDark) RevenueCardDarkBg else RevenueCardLightBg
            val revBorder = if (isDark) RevenueCardDarkBorder else RevenueCardLightBorder
            val revTitle = if (isDark) RevenueCardDarkTitle else RevenueCardLightTitle
            val revIcon = if (isDark) RevenueCardDarkIcon else RevenueCardLightIcon
            val revIconBg = if (isDark) RevenueCardDarkIconBg else RevenueCardLightIconBg
            val revAmount = if (isDark) RevenueCardDarkAmount else RevenueCardLightAmount
            val revSubtitle = if (isDark) RevenueCardDarkSubtitle else RevenueCardLightSubtitle

            val revenueSubtitle = if (summary.totalRevenue > 0) {
                if (summary.revenuesCount > 1) {
                    "${summary.revenuesCount} receitas cadastradas • Toque para gerenciar"
                } else if (summary.revenuesCount == 1) {
                    "1 receita cadastrada • Toque para gerenciar"
                } else {
                    "Toque para ver ou gerenciar receitas"
                }
            } else {
                "Toque para adicionar receitas do mês"
            }

            FinancialMetricCard(
                title = "Receita do Mês",
                amount = summary.totalRevenue,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconColor = revIcon,
                iconBgColor = revIconBg,
                containerColor = revBg,
                border = BorderStroke(1.dp, revBorder),
                titleColor = revTitle,
                amountColor = revAmount,
                subtitleColor = revSubtitle,
                subtitle = revenueSubtitle,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onEditRevenue() }
                    .testTag("metric_card_receita_do_mes")
            )
        }

        // 2. DESPESAS DO MÊS (Paleta Vermelha, Percentual da Receita, com PAGO, A PAGAR, VENCIDO)
        item {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

            val expBg = if (isDark) ExpenseCardDarkBg else ExpenseCardLightBg
            val expBorder = if (isDark) ExpenseCardDarkBorder else ExpenseCardLightBorder
            val expTitle = if (isDark) ExpenseCardDarkTitle else ExpenseCardLightTitle
            val expIcon = if (isDark) ExpenseCardDarkIcon else ExpenseCardLightIcon
            val expIconBg = if (isDark) ExpenseCardDarkIconBg else ExpenseCardLightIconBg
            val expAmount = if (isDark) ExpenseCardDarkAmount else ExpenseCardLightAmount

            val expensePercentage = if (summary.totalRevenue > 0) {
                ((summary.totalExpenses / summary.totalRevenue) * 100).roundToInt()
            } else null

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_despesas_resumo"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = expBg),
                border = BorderStroke(1.dp, expBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DESPESAS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = expTitle,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(expIconBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = expIcon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CurrencyUtils.formatCurrency(summary.totalExpenses),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = expAmount
                        )

                        if (expensePercentage != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color(0xFF3D1619) else Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF5A1A1E) else Color(0xFFFECACA))
                            ) {
                                Text(
                                    text = "$expensePercentage% da receita",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = expBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 colunas: PAGO | A PAGAR | VENCIDO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // PAGO
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(PaidGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PAGO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaidGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(summary.totalPaid),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // A PAGAR
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(PendingYellow, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "A PAGAR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PendingYellow
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(summary.totalPending),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // VENCIDO
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(OverdueRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VENCIDO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OverdueRed
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(summary.totalOverdue),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.totalOverdue > 0) OverdueRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. INVESTIMENTO (Percentual e valor destinados)
        item {
            FinancialMetricCard(
                title = "Investimento",
                amount = summary.investmentAllocated,
                icon = Icons.Default.Savings,
                iconColor = InvestmentBlue,
                subtitle = "${String.format("%.0f", summary.investmentPercentage)}% da receita • Toque para ajustar",
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onEditPercentages() }
                    .testTag("metric_card_investimento")
            )
        }

        // 4. LIMITE POR CATEGORIA (Flexível: o usuário pode escolher qualquer categoria para definir o teto percentual)
        item {
            val catLimitName = summary.categoryLimitName.ifBlank { "Lazer" }
            val isOverLimit = summary.categoryLimitAmount > 0 && summary.categoryLimitSpent > summary.categoryLimitAmount
            val overAmount = if (isOverLimit) summary.categoryLimitSpent - summary.categoryLimitAmount else 0.0
            val percentConsumed = if (summary.categoryLimitAmount > 0) {
                ((summary.categoryLimitSpent / summary.categoryLimitAmount) * 100).roundToInt()
            } else 0
            val categoryRatio = if (summary.categoryLimitAmount > 0) {
                (summary.categoryLimitSpent / summary.categoryLimitAmount).toFloat().coerceIn(0f, 1f)
            } else 0f

            val accentColor = if (isOverLimit) OverdueRed else LeisurePurple

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onEditPercentages() }
                    .testTag("card_lazer_resumo"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = if (isOverLimit) BorderStroke(1.dp, OverdueRed.copy(alpha = 0.8f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIMITE POR CATEGORIA • ${catLimitName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverLimit) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format("%.0f", summary.categoryLimitPercentage)}% da receita (${CurrencyUtils.formatCurrency(summary.categoryLimitAmount)}) • Toque para alterar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    accentColor.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (catLimitName.equals("Lazer", ignoreCase = true)) Icons.Default.SportsEsports else Icons.Default.Category,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Linha com Percentual Consumido em destaque
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOverLimit) "Acima do limite estabelecido" else "Consumo do limite de $catLimitName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$percentConsumed% consumido",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { categoryRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Métricas: Limite | Utilizado | Restante
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Limite",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(summary.categoryLimitAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Utilizado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(summary.categoryLimitSpent),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverLimit) OverdueRed else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restante",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(summary.categoryLimitAvailable),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.categoryLimitAvailable > 0) PaidGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Aviso quando ultrapassar o limite recomendado
                    if (isOverLimit) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = OverdueRed.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, OverdueRed.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = OverdueRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Você ultrapassou o limite estabelecido para $catLimitName deste mês em ${CurrencyUtils.formatCurrency(overAmount)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OverdueRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. SALDO DISPONÍVEL (Calculado mês a mês, podendo somar com o saldo de outros meses)
        item {
            val isSumming = summary.isAccumulatingWithOtherMonths
            val displayedAmount = if (isSumming) summary.availableBalanceTotal else summary.availableBalanceMonth

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_fundo_emergencia"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SALDO DISPONÍVEL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(EmergencyTeal.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmergencyTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = CurrencyUtils.formatCurrency(displayedAmount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val projectedAmount = if (isSumming) summary.projectedBalanceTotal else summary.projectedBalanceMonth
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Após quitar tudo: ${CurrencyUtils.formatCurrency(projectedAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Deste mês: ${CurrencyUtils.formatCurrency(summary.availableBalanceMonth)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Outros meses: ${CurrencyUtils.formatCurrency(summary.availableBalancePreviousMonths)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onToggleSumWithOtherMonths(!isSumming) }
                        ) {
                            Text(
                                text = "Somar outros meses",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSumming) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = isSumming,
                                onCheckedChange = { onToggleSumWithOtherMonths(it) },
                                modifier = Modifier.testTag("switch_somar_outros_meses")
                            )
                        }
                    }
                }
            }
        }

        // 6. GASTOS POR CATEGORIA (No final da página - compacto e simples - Regra 1)
        if (categoryTotals.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_gastos_por_categoria"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GASTOS POR CATEGORIA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val totalAmount = categoryTotals.sumOf { it.second }
                        val categoryColors = listOf(
                            Color(0xFF0A6847),
                            InvestmentBlue,
                            LeisurePurple,
                            EmergencyTeal,
                            Color(0xFFD97706),
                            Color(0xFFEA580C),
                            Color(0xFFE11D48),
                            Color(0xFF6366F1)
                        )

                        categoryTotals.forEachIndexed { index, (category, amount) ->
                            val percentage = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f
                            val percentText = String.format("%.0f%%", percentage * 100)
                            val barColor = categoryColors[index % categoryColors.size]

                            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = CurrencyUtils.formatCurrency(amount),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "($percentText)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            if (index < categoryTotals.size - 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
