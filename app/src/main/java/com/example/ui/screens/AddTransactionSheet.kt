package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.theme.LeisurePurple
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingYellow
import com.example.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import java.util.Locale

enum class TransactionType {
    RECEITA,
    DESPESA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    sheetState: SheetState,
    categories: List<Category> = emptyList(),
    selectedYearMonth: YearMonth,
    currentSalary: Double = 0.0,
    currentExtraIncome: Double = 0.0,
    initialType: TransactionType = TransactionType.DESPESA,
    onDismiss: () -> Unit,
    onSaveMonthlyRevenue: (salary: Double, extraIncome: Double) -> Unit = { _, _ -> },
    onAddRevenue: (description: String, amount: Double, date: String) -> Unit = { _, _, _ -> },
    onAddExpense: (description: String, amount: Double, dueDate: String, isPaid: Boolean, recurrenceMonths: Int, category: String) -> Unit
) {
    var transactionType by remember(initialType) { mutableStateOf(initialType) }

    // Revenue state
    var salaryText by remember(currentSalary) {
        mutableStateOf(if (currentSalary > 0.0) String.format("%.2f", currentSalary).replace(".", ",") else "")
    }
    var extraIncomeText by remember(currentExtraIncome) {
        mutableStateOf(if (currentExtraIncome > 0.0) String.format("%.2f", currentExtraIncome).replace(".", ",") else "")
    }

    // Expense state
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alimentação") }

    val initialDate = remember(selectedYearMonth) {
        val now = LocalDate.now()
        if (now.year == selectedYearMonth.year && now.monthValue == selectedYearMonth.monthValue) {
            CurrencyUtils.todayIso()
        } else {
            "${selectedYearMonth.year}-${String.format("%02d", selectedYearMonth.monthValue)}-01"
        }
    }

    var dateIso by remember { mutableStateOf(initialDate) }
    var isExpensePaid by remember { mutableStateOf(false) } // Default: A Pagar (false)
    var recurrenceOption by remember { mutableIntStateOf(1) } // 1 = à vista, 2..12 = parcelas/meses
    var hasError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val parts = dateIso.split("-")
        if (parts.size == 3) {
            parts[0].toIntOrNull()?.let { calendar.set(Calendar.YEAR, it) }
            parts[1].toIntOrNull()?.let { calendar.set(Calendar.MONTH, it - 1) }
            parts[2].toIntOrNull()?.let { calendar.set(Calendar.DAY_OF_MONTH, it) }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dateIso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                hasError = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
                .testTag("add_transaction_sheet")
        ) {
            Text(
                text = "Novo Lançamento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Selector: DESPESA or RECEITA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = transactionType == TransactionType.DESPESA,
                    onClick = { transactionType = TransactionType.DESPESA },
                    label = { Text("💳 Despesa", fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_type_despesa"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                )

                FilterChip(
                    selected = transactionType == TransactionType.RECEITA,
                    onClick = { transactionType = TransactionType.RECEITA },
                    label = { Text("💰 Receita", fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_type_receita"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (transactionType == TransactionType.RECEITA) {
                // ============================================
                // ULTRA-SIMPLIFIED RECEITA: SALÁRIO + RENDA EXTRA
                // ============================================
                val parsedSalary = CurrencyUtils.parseAmount(salaryText)
                val parsedExtra = CurrencyUtils.parseAmount(extraIncomeText)
                val totalRevenue = parsedSalary + parsedExtra

                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it },
                    label = { Text("Salário (R$)") },
                    prefix = { Text("R$ ") },
                    placeholder = { Text("0,00") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_salary"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = extraIncomeText,
                    onValueChange = { extraIncomeText = it },
                    label = { Text("Renda Extra (R$)") },
                    prefix = { Text("R$ ") },
                    placeholder = { Text("0,00") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_extra_income"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "RECEITA DO MÊS = SALÁRIO + RENDA EXTRA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatCurrency(totalRevenue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSaveMonthlyRevenue(parsedSalary, parsedExtra)
                        onAddRevenue("Salário", parsedSalary, "${selectedYearMonth}-01")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_confirm_save_revenue"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("SALVAR RECEITA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // ============================================
                // ULTRA-SIMPLIFIED DESPESA (Regra 1):
                // 1. Descrição
                // 2. Valor
                // 3. Data de vencimento
                // 4. Status (Paga ou A Pagar)
                // 5. Opção de recorrência
                // ============================================

                // 1. Descrição
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        hasError = false
                    },
                    label = { Text("Descrição (ex: Aluguel, Luz)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_description"),
                    isError = hasError && description.isBlank(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Valor
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        hasError = false
                    },
                    label = { Text("Valor (R$)") },
                    prefix = { Text("R$ ") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CreditCard, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_amount"),
                    isError = hasError && CurrencyUtils.parseAmount(amountText) <= 0.0,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Data de vencimento com pequeno calendário
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OutlinedTextField(
                        value = CurrencyUtils.formatToDisplayDate(dateIso),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Data de Vencimento") },
                        supportingText = {
                            Text("Toque para escolher o dia no calendário")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Abrir calendário",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_date"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Overlay invisível que captura o clique em qualquer parte do campo para abrir o calendário
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Categoria
                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val defaultCategories = listOf(
                    "Alimentação",
                    "Água",
                    "Compras",
                    "Empréstimo",
                    "Energia",
                    "Imposto",
                    "Internet",
                    "Lazer",
                    "Moradia",
                    "Outros",
                    "Saúde",
                    "Transporte"
                )
                val availableCategories = remember(categories) {
                    val collator = java.text.Collator.getInstance(java.util.Locale("pt", "BR")).apply {
                        strength = java.text.Collator.PRIMARY
                    }
                    val fromDb = categories.map { it.name.trim() }.filter { it.isNotBlank() }
                    (defaultCategories + fromDb).distinct().sortedWith { a, b -> collator.compare(a, b) }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { cat ->
                        val isLazer = cat.equals("Lazer", ignoreCase = true)
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isLazer) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = if (isLazer) {
                                FilterChipDefaults.filterChipColors(
                                    containerColor = LeisurePurple.copy(alpha = 0.08f),
                                    labelColor = LeisurePurple,
                                    selectedContainerColor = LeisurePurple.copy(alpha = 0.22f),
                                    selectedLabelColor = LeisurePurple
                                )
                            } else {
                                FilterChipDefaults.filterChipColors()
                            },
                            border = if (isLazer) {
                                BorderStroke(1.dp, LeisurePurple.copy(alpha = if (isSelected) 0.8f else 0.4f))
                            } else {
                                FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Status (Paga ou A Pagar)
                Text(
                    text = "Status Inicial",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isExpensePaid,
                        onClick = { isExpensePaid = false },
                        label = { Text("○ A Pagar", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_status_a_pagar"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PendingYellow.copy(alpha = 0.2f),
                            selectedLabelColor = PendingYellow
                        )
                    )

                    FilterChip(
                        selected = isExpensePaid,
                        onClick = { isExpensePaid = true },
                        label = { Text("✓ Já Paga", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_status_paga"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaidGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PaidGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Opção de recorrência / parcelamento
                Text(
                    text = "Repetir ou Parcelar",
                    style = MaterialTheme.typography.labelMedium,
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
                    FilterChip(
                        selected = recurrenceOption == 1,
                        onClick = { recurrenceOption = 1 },
                        label = { Text("À vista (este mês)") }
                    )
                    FilterChip(
                        selected = recurrenceOption == 12,
                        onClick = { recurrenceOption = 12 },
                        label = { Text("Mensal fixa (12 meses)") }
                    )
                    FilterChip(
                        selected = recurrenceOption == 2,
                        onClick = { recurrenceOption = 2 },
                        label = { Text("2x") }
                    )
                    FilterChip(
                        selected = recurrenceOption == 3,
                        onClick = { recurrenceOption = 3 },
                        label = { Text("3x") }
                    )
                    FilterChip(
                        selected = recurrenceOption == 6,
                        onClick = { recurrenceOption = 6 },
                        label = { Text("6x") }
                    )
                    FilterChip(
                        selected = recurrenceOption == 10,
                        onClick = { recurrenceOption = 10 },
                        label = { Text("10x") }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = CurrencyUtils.parseAmount(amountText)
                        if (description.isBlank() || amount <= 0.0) {
                            hasError = true
                            return@Button
                        }

                        onAddExpense(
                            description,
                            amount,
                            dateIso,
                            isExpensePaid,
                            recurrenceOption,
                            selectedCategory
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_confirm_add_transaction"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("SALVAR DESPESA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
