package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import com.example.data.model.PaymentStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val pendingExpenses = database.expenseDao().getAllPendingExpenses()

            val overdue = pendingExpenses.filter { CurrencyUtils.computePaymentStatus(it) == PaymentStatus.VENCIDA }
            val pending = pendingExpenses.filter { CurrencyUtils.computePaymentStatus(it) == PaymentStatus.A_PAGAR }

            if (overdue.isNotEmpty()) {
                ExpenseNotificationManager.showReminderNotification(
                    context,
                    "Atenção: Despesas Vencidas",
                    "Você possui ${overdue.size} despesa(s) vencida(s) no valor total de ${CurrencyUtils.formatCurrency(overdue.sumOf { it.amount })}.",
                    notificationId = 2001
                )
            } else if (pending.isNotEmpty()) {
                ExpenseNotificationManager.showReminderNotification(
                    context,
                    "Controle Financeiro",
                    "Você possui ${pending.size} despesa(s) a pagar neste mês.",
                    notificationId = 2002
                )
            }
        }
    }
}
