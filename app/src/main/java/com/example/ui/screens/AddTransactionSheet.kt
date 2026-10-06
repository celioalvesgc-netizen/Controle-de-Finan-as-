package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.util.CurrencyUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAddExpense: (description: String, amount: Double, dueDate: LocalDate, category: String, isMonthlyRecurring: Boolean, recurrenceMonths: Int, notes: String) -> Unit,
    onAddRevenue: (description: String, amount: Double, date: LocalDate, isRecurring: Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Despesa, 1 = Receita

    // Campos comuns
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }

    // Despesa
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Geral") }
    var recurrenceType by remember { mutableIntStateOf(0) } // 0 = Única, 1 = Mensal, 2 = Parcelado
    var installmentsText by remember { mutableStateOf("2") }

    val context = LocalContext.current
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
    }

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
                    text = if (selectedTab == 0) "Nova Despesa" else "Nova Receita",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Despesa", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Receita", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Descrição
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_description")
            )

            // Chips de sugestão para Receita
            if (selectedTab == 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Salário", "Renda Extra", "Comissão", "Venda", "Aluguel", "13º Salário", "Restituição").forEach { suggestion ->
                        AssistChip(
                            onClick = { description = suggestion },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Valor
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.replace(",", ".") },
                label = { Text("Valor (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_amount")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Seletor de Data
            OutlinedTextField(
                value = CurrencyUtils.formatToDisplayDate(selectedDate),
                onValueChange = {},
                readOnly = true,
                label = { Text(if (selectedTab == 0) "Data de Vencimento" else "Data de Recebimento") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Selecionar data")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
                    .testTag("input_transaction_date")
            )

            if (selectedTab == 0) {
                // Categorias para Despesa
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Categoria", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Tipo de Pagamento", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("À Vista", "Mensal Fixa", "Parcelado").forEachIndexed { index, label ->
                        FilterChip(
                            selected = recurrenceType == index,
                            onClick = { recurrenceType = index },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (recurrenceType == 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = installmentsText,
                        onValueChange = { installmentsText = it.filter { char -> char.isDigit() } },
                        label = { Text("Quantidade de Parcelas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (description.isNotBlank() && amount > 0.0) {
                        if (selectedTab == 0) {
                            val isMonthly = recurrenceType == 1
                            val installments = if (recurrenceType == 2) installmentsText.toIntOrNull() ?: 2 else 1
                            onAddExpense(
                                description.trim(),
                                amount,
                                selectedDate,
                                selectedCategory,
                                isMonthly,
                                installments,
                                notes.trim()
                            )
                        } else {
                            onAddRevenue(
                                description.trim(),
                                amount,
                                selectedDate,
                                false
                            )
                        }
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_transaction")
            ) {
                Text(
                    text = if (selectedTab == 0) "Adicionar Despesa" else "Adicionar Receita",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
