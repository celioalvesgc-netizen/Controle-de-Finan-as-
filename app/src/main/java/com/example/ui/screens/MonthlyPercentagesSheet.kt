package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.util.CurrencyUtils
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyPercentagesSheet(
    selectedYearMonth: YearMonth,
    currentInvestmentPercent: Double,
    currentCategoryLimitPercent: Double,
    currentCategoryLimitName: String,
    totalRevenue: Double,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSavePercentages: (investPercent: Double, catLimitPercent: Double, catLimitName: String) -> Unit
) {
    var investSlider by remember { mutableFloatStateOf(currentInvestmentPercent.toFloat()) }
    var catLimitSlider by remember { mutableFloatStateOf(currentCategoryLimitPercent.toFloat()) }
    var selectedCategoryName by remember { mutableStateOf(currentCategoryLimitName.ifBlank { "Lazer" }) }

    val investAmount = (totalRevenue * investSlider) / 100.0
    val catLimitAmount = (totalRevenue * catLimitSlider) / 100.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ajustar Metas do Mês",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. INVESTIMENTO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Investimento", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${investSlider.toInt()}% (${CurrencyUtils.formatCurrency(investAmount)})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = investSlider,
                        onValueChange = { investSlider = it },
                        valueRange = 0f..50f,
                        steps = 49
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. LIMITE POR CATEGORIA
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Categoria com Limite", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryName == cat.name,
                                onClick = { selectedCategoryName = cat.name },
                                label = { Text(cat.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Teto para $selectedCategoryName", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${catLimitSlider.toInt()}% (${CurrencyUtils.formatCurrency(catLimitAmount)})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Slider(
                        value = catLimitSlider,
                        onValueChange = { catLimitSlider = it },
                        valueRange = 0f..50f,
                        steps = 49
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSavePercentages(
                        investSlider.toDouble(),
                        catLimitSlider.toDouble(),
                        selectedCategoryName
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_percentages")
            ) {
                Text(text = "Salvar Alterações", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
