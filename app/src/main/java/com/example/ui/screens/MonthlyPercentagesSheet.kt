package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.theme.InvestmentBlue
import com.example.ui.theme.LeisurePurple
import com.example.util.CurrencyUtils
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyPercentagesSheet(
    selectedYearMonth: YearMonth,
    currentInvestmentPercent: Double,
    currentLeisurePercent: Double,
    defaultInvestmentPercent: Double = 20.0,
    defaultLeisurePercent: Double = 10.0,
    totalRevenue: Double,
    availableCategories: List<Category> = emptyList(),
    currentCategoryLimitName: String? = null,
    currentCategoryLimitPercent: Double? = null,
    onSavePercentages: (invPercent: Double, categoryName: String, categoryPercent: Double) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var invSlider by remember(currentInvestmentPercent) {
        mutableFloatStateOf(currentInvestmentPercent.toFloat())
    }

    val initialCatName = remember(currentCategoryLimitName) {
        if (!currentCategoryLimitName.isNullOrBlank()) currentCategoryLimitName else "Lazer"
    }
    var selectedCategory by remember { mutableStateOf(initialCatName) }

    val initialCatPercent = remember(currentCategoryLimitPercent, currentLeisurePercent) {
        val p = currentCategoryLimitPercent ?: currentLeisurePercent
        if (p > 0) p.toFloat() else 10f
    }
    var catSlider by remember { mutableFloatStateOf(initialCatPercent) }

    val monthName = selectedYearMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
    val formattedMonthYear = "$monthName de ${selectedYearMonth.year}"

    // Lista de categorias sugeridas e cadastradas
    val defaultCategories = listOf(
        "Alimentação",
        "Combustível",
        "Mercado",
        "Lazer",
        "Saúde",
        "Transporte",
        "Moradia",
        "Compras",
        "Água",
        "Energia",
        "Internet",
        "Outros"
    )
    val allCategories = remember(availableCategories) {
        val collator = java.text.Collator.getInstance(Locale("pt", "BR")).apply {
            strength = java.text.Collator.PRIMARY
        }
        val fromDb = availableCategories.map { it.name.trim() }.filter { it.isNotBlank() }
        (defaultCategories + fromDb).distinct().sortedWith { a, b -> collator.compare(a, b) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("monthly_percentages_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Limite & Percentuais",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Configuração para $formattedMonthYear",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_percentages_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (totalRevenue > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Receita base do mês: ${CurrencyUtils.formatCurrency(totalRevenue)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. CARD PRINCIPAL: LIMITE POR CATEGORIA (Requisito 3)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_sheet_category_limit"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, LeisurePurple.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(LeisurePurple.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedCategory.equals("Lazer", ignoreCase = true)) Icons.Default.SportsEsports else Icons.Default.Category,
                                    contentDescription = null,
                                    tint = LeisurePurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Limite por Categoria",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Categoria ativa: $selectedCategory",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LeisurePurple,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${catSlider.roundToInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LeisurePurple
                            )
                            if (totalRevenue > 0) {
                                val valor = totalRevenue * (catSlider / 100.0)
                                Text(
                                    text = CurrencyUtils.formatCurrency(valor),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Escolha a categoria a monitorar:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allCategories.forEach { cat ->
                            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LeisurePurple.copy(alpha = 0.2f),
                                    selectedLabelColor = LeisurePurple
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = LeisurePurple
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Definir percentual da receita:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = catSlider,
                        onValueChange = { catSlider = it },
                        valueRange = 1f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = LeisurePurple,
                            activeTrackColor = LeisurePurple,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("slider_category_limit_sheet")
                    )

                    val limitCalculated = if (totalRevenue > 0) totalRevenue * (catSlider / 100.0) else 0.0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Teto para $selectedCategory: ${catSlider.roundToInt()}% = ${CurrencyUtils.formatCurrency(limitCalculated)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "O sistema calculará automaticamente o valor gasto e o restante disponível para $selectedCategory neste mês.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. CARD INVESTIMENTO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_sheet_investimento"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(InvestmentBlue.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = InvestmentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Investimento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${invSlider.roundToInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = InvestmentBlue
                            )
                            if (totalRevenue > 0) {
                                val valor = totalRevenue * (invSlider / 100.0)
                                Text(
                                    text = CurrencyUtils.formatCurrency(valor),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = invSlider,
                        onValueChange = { invSlider = it },
                        valueRange = 0f..50f,
                        steps = 49,
                        colors = SliderDefaults.colors(
                            thumbColor = InvestmentBlue,
                            activeTrackColor = InvestmentBlue,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("slider_investimento_sheet")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botão Salvar para este mês
            Button(
                onClick = {
                    onSavePercentages(
                        invSlider.toDouble(),
                        selectedCategory,
                        catSlider.toDouble()
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_percentages_sheet"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar para $monthName", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão Restaurar Padrão
            OutlinedButton(
                onClick = {
                    onResetToDefault()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_reset_percentages_sheet"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Restaurar padrão",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
