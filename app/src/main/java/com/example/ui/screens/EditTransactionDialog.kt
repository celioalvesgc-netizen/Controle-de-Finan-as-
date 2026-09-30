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
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Expense
import com.example.data.model.Revenue
import com.example.ui.theme.LeisurePurple
import com.example.util.CurrencyUtils
import java.util.Calendar
import java.util.Locale

@Composable
fun EditExpenseDialog(
    expense: Expense,
    categories: List<Category> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    var description by remember { mutableStateOf(expense.description) }
    var amountText by remember { mutableStateOf(expense.amount.toString().replace(".", ",")) }
    var dueDate by remember { mutableStateOf(expense.dueDate) }
    var isPaid by remember { mutableStateOf(expense.isPaid) }
    var selectedCategory by remember { mutableStateOf(if (expense.category.isNotBlank()) expense.category else "Outros") }

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

    val context = LocalContext.current

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val parts = dueDate.split("-")
        if (parts.size == 3) {
            parts[0].toIntOrNull()?.let { calendar.set(Calendar.YEAR, it) }
            parts[1].toIntOrNull()?.let { calendar.set(Calendar.MONTH, it - 1) }
            parts[2].toIntOrNull()?.let { calendar.set(Calendar.DAY_OF_MONTH, it) }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dueDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Despesa", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_input_description")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$)") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_input_amount")
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OutlinedTextField(
                        value = CurrencyUtils.formatToDisplayDate(dueDate),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Data de Vencimento") },
                        supportingText = { Text("Toque para escolher o dia no calendário") },
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
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_input_due_date")
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker() }
                    )
                }

                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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

                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isPaid,
                        onClick = { isPaid = false },
                        label = { Text("○ A Pagar", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isPaid,
                        onClick = { isPaid = true },
                        label = { Text("✓ Paga", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = CurrencyUtils.parseAmount(amountText)
                    if (description.isNotBlank() && amount > 0.0) {
                        onConfirm(
                            expense.copy(
                                description = description.trim(),
                                amount = amount,
                                dueDate = dueDate.trim(),
                                isPaid = isPaid,
                                category = selectedCategory,
                                paidDate = if (isPaid) (expense.paidDate ?: CurrencyUtils.todayIso()) else null
                            )
                        )
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditRevenueDialog(
    revenue: Revenue,
    onDismiss: () -> Unit,
    onConfirm: (Revenue) -> Unit
) {
    var description by remember { mutableStateOf(revenue.description) }
    var amountText by remember { mutableStateOf(revenue.amount.toString().replace(".", ",")) }
    var date by remember { mutableStateOf(revenue.date) }

    val context = LocalContext.current

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val parts = date.split("-")
        if (parts.size == 3) {
            parts[0].toIntOrNull()?.let { calendar.set(Calendar.YEAR, it) }
            parts[1].toIntOrNull()?.let { calendar.set(Calendar.MONTH, it - 1) }
            parts[2].toIntOrNull()?.let { calendar.set(Calendar.DAY_OF_MONTH, it) }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Receita", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OutlinedTextField(
                        value = CurrencyUtils.formatToDisplayDate(date),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Data") },
                        supportingText = { Text("Toque para escolher o dia no calendário") },
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
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker() }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = CurrencyUtils.parseAmount(amountText)
                    if (description.isNotBlank() && amount > 0.0) {
                        onConfirm(
                            revenue.copy(
                                description = description.trim(),
                                amount = amount,
                                date = date.trim()
                            )
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
