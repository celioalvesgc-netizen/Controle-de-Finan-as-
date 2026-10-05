package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingYellow
import com.example.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

enum class TransactionType {
    RECEITA,
    DESPESA
}

enum class RecurrenceType {
    UNICA,
    MENSAL,
    PARCELADO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    sheetState: SheetState,
    categories: List<Category> = emptyList(),
    selectedYearMonth: YearMonth,
    currentSalary: Double = 0.0,
    currentExtraIncome: Double = 0.0,
    leisureLimit: Double = 0.0,
    leisureSpent: Double = 0.0,
    initialType: TransactionType = TransactionType.DESPESA,
    onDismiss: () -> Unit,
    onSaveMonthlyRevenue: (salary: Double, extraIncome: Double) -> Unit = { _, _ -> },
    onAddRevenue: (description: String, amount: Double, date: String) -> Unit = { _, _, _ -> },
    onAddExpense: (
        description: String,
        amount: Double,
        dueDate: String,
        isPaid: Boolean,
        recurrenceMonths: Int,
        category: String,
        isMonthlyRecurring: Boolean
    ) -> Unit
) {
    var transactionType by remember(initialType) { mutableStateOf(initialType) }

    val initialDate = remember(selectedYearMonth) {
        val now = LocalDate.now()
        if (now.year == selectedYearMonth.year && now.monthValue == selectedYearMonth.monthValue) {
            CurrencyUtils.todayIso()
        } else {
            "${selectedYearMonth.year}-${String.format("%02d", selectedYearMonth.monthValue)}-01"
        }
    }

    // Revenue state
    var revenueDescription by remember { mutableStateOf("") }
    var revenueAmountText by remember { mutableStateOf("") }
    var revenueDateIso by remember { mutableStateOf(initialDate) }
    var revenueHasError by remember { mutableStateOf(false) }

    // Expense state
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf(false) }
    var dateIso by remember { mutableStateOf(initialDate) }
    var isExpensePaid by remember { mutableStateOf(false) }

    // Recorrência: Única, Mensal, Parcelado (Requisito 6)
    var recurrenceType by remember { mutableStateOf(RecurrenceType.UNICA) }
    var installmentsText by remember { mutableStateOf("2") }
    var hasError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun showDatePicker(isForRevenue: Boolean) {
        val calendar = Calendar.getInstance()
        val targetDate = if (isForRevenue) revenueDateIso else dateIso
        val parts = targetDate.split("-")
        if (parts.size == 3) {
            parts[0].toIntOrNull()?.let { calendar.set(Calendar.YEAR, it) }
            parts[1].toIntOrNull()?.let { calendar.set(Calendar.MONTH, it - 1) }
            parts[2].toIntOrNull()?.let { calendar.set(Calendar.DAY_OF_MONTH, it) }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                if (isForRevenue) {
                    revenueDateIso = chosen
                    revenueHasError = false
                } else {
                    dateIso = chosen
                    hasError = false
                }
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
                    label = { Text("💳 Despesa", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_type_despesa"),
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (transactionType == TransactionType.DESPESA) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                FilterChip(
                    selected = transactionType == TransactionType.RECEITA,
                    onClick = { transactionType = TransactionType.RECEITA },
                    label = { Text("💰 Receita", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_type_receita"),
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (transactionType == TransactionType.RECEITA) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (transactionType == TransactionType.RECEITA) {
                // ============================================
                // CADASTRO DE RECEITAS (Requisito 2):
                // Múltiplas receitas livres com Descrição, Valor e Data
                // ============================================
                val revenueSuggestions = listOf(
                    "Salário",
                    "Hora extra",
                    "Comissão",
                    "Venda",
                    "Aluguel recebido",
                    "Cashback",
                    "Restituição",
                    "Décimo terceiro",
                    "Presente",
                    "Outros"
                )

                OutlinedTextField(
                    value = revenueDescription,
                    onValueChange = {
                        revenueDescription = it
                        revenueHasError = false
                    },
                    label = { Text("Descrição da Receita *") },
                    placeholder = { Text("Ex: Salário, Venda, Restituição...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_revenue_description"),
                    isError = revenueHasError && revenueDescription.isBlank(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sugestões rápidas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    revenueSuggestions.forEach { suggestion ->
                        val isSelected = revenueDescription.equals(suggestion, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                revenueDescription = suggestion
                                revenueHasError = false
                            },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = revenueAmountText,
                    onValueChange = {
                        revenueAmountText = it
                        revenueHasError = false
                    },
                    label = { Text("Valor da Receita (R$) *") },
                    prefix = { Text("R$ ") },
                    placeholder = { Text("0,00") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_revenue_amount"),
                    isError = revenueHasError && CurrencyUtils.parseAmount(revenueAmountText) <= 0.0,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Data da Receita
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OutlinedTextField(
                        value = CurrencyUtils.formatToDisplayDate(revenueDateIso),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Data do Recebimento") },
                        supportingText = { Text("Toque para alterar a data") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker(isForRevenue = true) }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Abrir calendário",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_revenue_date"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker(isForRevenue = true) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = CurrencyUtils.parseAmount(revenueAmountText)
                        val isDescBlank = revenueDescription.isBlank()
                        val isAmountInvalid = amount <= 0.0

                        if (isDescBlank || isAmountInvalid) {
                            revenueHasError = true
                            Toast.makeText(context, "Preencha a descrição e um valor válido.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        onAddRevenue(
                            revenueDescription.trim(),
                            amount,
                            revenueDateIso
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_save_revenue"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("SALVAR RECEITA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // ============================================
                // CADASTRO DE DESPESAS (Requisitos 1, 3, 6):
                // 1. Descrição
                // 2. Valor da parcela
                // 3. Data de vencimento
                // 4. Categoria
                // 5. Status inicial
                // 6. Recorrência: Única | Mensal | Parcelado (com número livre de parcelas)
                // ============================================

                // 1. Descrição
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        hasError = false
                    },
                    label = { Text("Descrição (ex: Aluguel, Netflix, Televisão)") },
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

                // 2. Valor (Valor da despesa ou valor da parcela)
                val isParcelado = recurrenceType == RecurrenceType.PARCELADO
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        hasError = false
                    },
                    label = { Text(if (isParcelado) "Valor da Parcela (R$)" else "Valor (R$)") },
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

                // 3. Data de vencimento
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OutlinedTextField(
                        value = CurrencyUtils.formatToDisplayDate(dateIso),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Data do 1º Vencimento") },
                        supportingText = {
                            Text("Toque para escolher a data no calendário")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker(isForRevenue = false) }) {
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

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker(isForRevenue = false) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Categoria
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categoria *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (categoryError && selectedCategory.isNullOrBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedCategory.isNullOrBlank()) {
                        Text(
                            text = "Obrigatório",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (categoryError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = selectedCategory ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                val defaultCategories = listOf(
                    "Alimentação",
                    "Água",
                    "Combustível",
                    "Compras",
                    "Empréstimo",
                    "Energia",
                    "Imposto",
                    "Internet",
                    "Lazer",
                    "Mercado",
                    "Moradia",
                    "Outros",
                    "Saúde",
                    "Transporte"
                )
                val availableCategories = remember(categories) {
                    val collator = java.text.Collator.getInstance(Locale("pt", "BR")).apply {
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
                        val isSelected = selectedCategory != null && selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = cat
                                categoryError = false
                            },
                            label = { Text(text = cat, fontSize = 12.sp) }
                        )
                    }
                }

                if (categoryError && selectedCategory.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Selecione uma categoria para continuar.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("error_category_required")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Status Inicial (Paga ou A Pagar)
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

                Spacer(modifier = Modifier.height(16.dp))

                // 6. RECORRÊNCIA E PARCELAMENTO (Requisito 6)
                Text(
                    text = "Tipo de Lançamento / Recorrência",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = recurrenceType == RecurrenceType.UNICA,
                        onClick = { recurrenceType = RecurrenceType.UNICA },
                        label = { Text("À vista", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = recurrenceType == RecurrenceType.MENSAL,
                        onClick = { recurrenceType = RecurrenceType.MENSAL },
                        label = { Text("Mensal", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = recurrenceType == RecurrenceType.PARCELADO,
                        onClick = { recurrenceType = RecurrenceType.PARCELADO },
                        label = { Text("Parcelado", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (recurrenceType) {
                    RecurrenceType.UNICA -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Lançamento único com vencimento apenas neste mês.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    RecurrenceType.MENSAL -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Recorrência Mensal (sem prazo definido)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Ex: Netflix, Aluguel, Internet. O sistema cria os lançamentos mensais contínuos até você encerrar ou excluir.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    RecurrenceType.PARCELADO -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Compra Parcelada / Financiada",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Informe livremente a quantidade total de parcelas (aceita qualquer número inteiro > 0).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = installmentsText,
                                    onValueChange = {
                                        // Aceita apenas números inteiros positivos
                                        val filtered = it.filter { ch -> ch.isDigit() }
                                        installmentsText = filtered
                                    },
                                    label = { Text("Quantidade de Parcelas") },
                                    placeholder = { Text("Ex: 2, 4, 5, 7, 10, 15, 18, 24, 36...") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_installments_count")
                                )

                                val parsedCount = installmentsText.toIntOrNull() ?: 1
                                val unitAmount = CurrencyUtils.parseAmount(amountText)
                                val totalCost = unitAmount * parsedCount

                                if (parsedCount > 1 && unitAmount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Total: $parsedCount parcelas de ${CurrencyUtils.formatCurrency(unitAmount)} = ${CurrencyUtils.formatCurrency(totalCost)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Serão criadas $parsedCount parcelas mensais futuras automáticas, preservando histórico e controle individual.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = CurrencyUtils.parseAmount(amountText)
                        val isDescBlank = description.isBlank()
                        val isAmountInvalid = amount <= 0.0
                        val isCategoryMissing = selectedCategory.isNullOrBlank()

                        if (isDescBlank || isAmountInvalid) {
                            hasError = true
                        }
                        if (isCategoryMissing) {
                            categoryError = true
                            Toast.makeText(context, "Selecione uma categoria para continuar.", Toast.LENGTH_SHORT).show()
                        }
                        if (isDescBlank || isAmountInvalid || isCategoryMissing) {
                            return@Button
                        }

                        val categoryChosen = selectedCategory!!
                        val isMonthly = recurrenceType == RecurrenceType.MENSAL
                        val count = if (recurrenceType == RecurrenceType.PARCELADO) {
                            max(1, installmentsText.toIntOrNull() ?: 1)
                        } else {
                            1
                        }

                        onAddExpense(
                            description.trim(),
                            amount,
                            dateIso,
                            isExpensePaid,
                            count,
                            categoryChosen,
                            isMonthly
                        )

                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_add_transaction"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("SALVAR DESPESA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
