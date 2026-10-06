package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinanceSummary
import com.example.ui.components.FinancialMetricCard
import com.example.ui.components.MonthSelectorCard
import com.example.ui.theme.*
import com.example.util.CurrencyUtils
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    selectedYearMonth: YearMonth,
    summary: FinanceSummary,
    revenuesCount: Int,
    categoryTotals: Map<String, Double>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onEditRevenue: () -> Unit,
    onEditPercentages: () -> Unit,
    onToggleSumWithOtherMonths: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Seletor de Mês
        item {
            MonthSelectorCard(
                selectedYearMonth = selectedYearMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onResetCurrentMonth = onResetCurrentMonth
            )
        }

        // 1. RECEITA DO MÊS (Paleta Azul Tonal)
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
                if (revenuesCount > 1) {
                    "$revenuesCount fontes de receita • Toque para ver"
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

        // 2. DESPESAS DO MÊS (Paleta Vermelha com PAGO, A PAGAR, VENCIDO)
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
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = expIcon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = CurrencyUtils.formatCurrency(summary.totalExpenses),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = expAmount
                    )

                    if (expensePercentage != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "$expensePercentage% da receita total deste mês",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (expensePercentage > 100) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = expBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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

        // 3. INVESTIMENTO
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

        // 4. LIMITE POR CATEGORIA
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
                                .background(accentColor.copy(alpha = 0.12f), CircleShape),
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
                            color = if (isOverLimit) OverdueRedBg else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$percentConsumed%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverLimit) OverdueRedText else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { categoryRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isOverLimit) OverdueRed else LeisurePurple,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gasto: ${CurrencyUtils.formatCurrency(summary.categoryLimitSpent)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isOverLimit) {
                                "Excedeu: +${CurrencyUtils.formatCurrency(overAmount)}"
                            } else {
                                "Resta: ${CurrencyUtils.formatCurrency(summary.categoryLimitAmount - summary.categoryLimitSpent)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverLimit) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isOverLimit) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OverdueRedBg,
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

        // 5. SALDO DISPONÍVEL (Com destaque aprimorado e quebra de linha organizada no botão "Somar outros meses")
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Linha inferior com quebra de linha suave e botão interativo destacado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
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

                        // Botão interativo destacado com quebra de linha e superfície com borda e contraste elegante
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSumming) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSumming) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleSumWithOtherMonths(!isSumming) }
                                .testTag("btn_container_somar_outros_meses")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Somar outros\nmeses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSumming) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSumming) FontWeight.Bold else FontWeight.SemiBold,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isSumming,
                                    onCheckedChange = { onToggleSumWithOtherMonths(it) },
                                    modifier = Modifier
                                        .scale(0.85f)
                                        .testTag("switch_somar_outros_meses")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. GASTOS POR CATEGORIA
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

                        val total = categoryTotals.values.sum().coerceAtLeast(1.0)
                        categoryTotals.entries.sortedByDescending { it.value }.forEach { entry ->
                            val pct = ((entry.value / total) * 100).roundToInt()
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = entry.key,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${CurrencyUtils.formatCurrency(entry.value)} ($pct%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (entry.value / total).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
