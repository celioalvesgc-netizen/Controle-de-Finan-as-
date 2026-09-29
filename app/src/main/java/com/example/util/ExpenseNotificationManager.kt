package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import java.util.Calendar

object ExpenseNotificationManager {

    const val CHANNEL_ID = "channel_expenses_reminders"
    const val CHANNEL_NAME = "Lembretes de Despesas"
    const val PREFS_NAME = "expense_notifications_prefs"
    private const val ALARM_REQUEST_CODE = 8801

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lembretes de contas vencendo hoje ou vencidas"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ExpenseReminderReceiver::class.java).apply {
            action = "com.example.meufinanceiro.CHECK_EXPENSES"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Agendar para as 09:00 diariamente
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            // Log fallback
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ExpenseReminderReceiver::class.java).apply {
            action = "com.example.meufinanceiro.CHECK_EXPENSES"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelNotificationForExpense(context: Context, expenseId: Long) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(expenseId.toInt())
            notificationManager.cancel((expenseId + 100000).toInt())
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun checkAndNotifyExpenses(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val settings = db.settingsDao().getSettingsDirect()

        // Verificar se o usuário ativou as notificações
        if (settings?.notificationsEnabled != true) {
            return
        }

        createNotificationChannel(context)

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        val todayIso = CurrencyUtils.todayIso()
        val unpaidExpenses = db.expenseDao().getUnpaidExpensesDirect()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        for (expense in unpaidExpenses) {
            // Se foi paga, garantir que não envia
            if (expense.isPaid) continue

            // 1. Notificação no dia do vencimento
            if (expense.dueDate == todayIso) {
                val dueKey = "due_notified_${expense.id}_${todayIso}"
                if (!prefs.getBoolean(dueKey, false)) {
                    val notifId = expense.id.toInt()
                    val formattedAmount = CurrencyUtils.formatCurrency(expense.amount)
                    val content = "${expense.description} — $formattedAmount"

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        notifId,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("🔔 Conta vencendo hoje")
                        .setContentText(content)
                        .setStyle(NotificationCompat.BigTextStyle().bigText("$content\nVence hoje!"))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()

                    try {
                        notificationManager.notify(notifId, notification)
                        prefs.edit().putBoolean(dueKey, true).apply()
                    } catch (e: SecurityException) {
                        // Permission revoked
                    }
                }
            }
            // 3. Notificação para despesa vencida (enviada apenas uma vez por ocorrência)
            else if (expense.dueDate < todayIso) {
                val overdueKey = "overdue_notified_${expense.id}_${expense.dueDate}"
                if (!prefs.getBoolean(overdueKey, false)) {
                    val notifId = (expense.id + 100000).toInt()
                    val formattedAmount = CurrencyUtils.formatCurrency(expense.amount)
                    val content = "${expense.description} — $formattedAmount"

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        notifId,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle("⚠️ Conta vencida")
                        .setContentText(content)
                        .setStyle(NotificationCompat.BigTextStyle().bigText("$content\nEsta despesa ultrapassou a data de vencimento."))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()

                    try {
                        notificationManager.notify(notifId, notification)
                        prefs.edit().putBoolean(overdueKey, true).apply()
                    } catch (e: SecurityException) {
                        // Permission revoked
                    }
                }
            }
        }
    }
}
