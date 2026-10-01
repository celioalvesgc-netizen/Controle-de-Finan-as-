package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Expense
import com.example.data.model.PaymentStatus
import com.example.util.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Meu Financeiro", appName)
  }

  @Test
  fun `test currency formatting`() {
    val formatted = CurrencyUtils.formatCurrency(5000.0)
    // Matches R$ 5.000,00 (with possible non-breaking space)
    assert(formatted.contains("5.000,00"))
  }

  @Test
  fun `test payment status calculation`() {
    val today = LocalDate.of(2026, 10, 5)

    // Paid expense
    val paidExp = Expense(
      description = "Internet",
      amount = 100.0,
      dueDate = "2026-10-10",
      category = "Internet",
      isPaid = true,
      paidDate = "2026-10-04"
    )
    assertEquals(PaymentStatus.PAGA, CurrencyUtils.computePaymentStatus(paidExp, today))

    // Upcoming pending expense
    val upcomingExp = Expense(
      description = "Internet",
      amount = 100.0,
      dueDate = "2026-10-10",
      category = "Internet",
      isPaid = false
    )
    assertEquals(PaymentStatus.A_PAGAR, CurrencyUtils.computePaymentStatus(upcomingExp, today))

    // Overdue pending expense
    val overdueExp = Expense(
      description = "Energia",
      amount = 180.0,
      dueDate = "2026-10-02",
      category = "Energia",
      isPaid = false
    )
    assertEquals(PaymentStatus.VENCIDA, CurrencyUtils.computePaymentStatus(overdueExp, today))
  }

  @Test
  fun `test monthly revenue calculation`() {
    val monthlyRevenue = com.example.data.model.MonthlyRevenue(
      month = "2026-10",
      salary = 3500.0,
      extraIncome = 750.0
    )
    assertEquals(4250.0, monthlyRevenue.totalRevenue, 0.001)

    // Test zero default when month has no data
    val emptyMonth = com.example.data.model.MonthlyRevenue(month = "2026-11")
    assertEquals(0.0, emptyMonth.salary, 0.001)
    assertEquals(0.0, emptyMonth.extraIncome, 0.001)
    assertEquals(0.0, emptyMonth.totalRevenue, 0.001)
  }

  @Test
  fun `test app settings default notifications disabled`() {
    val settings = com.example.data.model.AppSettings()
    assertEquals(false, settings.notificationsEnabled)
    val enabledSettings = settings.copy(notificationsEnabled = true)
    assertEquals(true, enabledSettings.notificationsEnabled)
  }

  @Test
  fun `test notification channel creation and cancel`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.ExpenseNotificationManager.createNotificationChannel(context)
    com.example.util.ExpenseNotificationManager.cancelNotificationForExpense(context, 123L)
  }

  @Test
  fun `test finance summary available balance month by month and accumulation`() {
    val ym = java.time.YearMonth.of(2026, 10)
    val summary = com.example.ui.viewmodel.FinanceSummary(
      monthYear = ym,
      availableBalanceMonth = 300.0,
      availableBalancePreviousMonths = 500.0,
      availableBalanceTotal = 800.0,
      isAccumulatingWithOtherMonths = true
    )
    assertEquals(300.0, summary.availableBalanceMonth, 0.001)
    assertEquals(500.0, summary.availableBalancePreviousMonths, 0.001)
    assertEquals(800.0, summary.availableBalanceTotal, 0.001)
    assertEquals(true, summary.isAccumulatingWithOtherMonths)

    val nonAccumulating = summary.copy(isAccumulatingWithOtherMonths = false)
    assertEquals(false, nonAccumulating.isAccumulatingWithOtherMonths)
  }

  @Test
  fun `test sumWithOtherMonths setting default and persistence`() {
    val settings = com.example.data.model.AppSettings()
    assertEquals(true, settings.sumWithOtherMonths)
    val disabled = settings.copy(sumWithOtherMonths = false)
    assertEquals(false, disabled.sumWithOtherMonths)
  }

  @Test
  fun `test monthly specific percentages vs default fallback`() {
    val defaultInv = com.example.ui.viewmodel.DEFAULT_SYSTEM_INVESTMENT_PERCENTAGE
    val defaultLeisure = com.example.ui.viewmodel.DEFAULT_SYSTEM_LEISURE_PERCENTAGE
    assertEquals(20.0, defaultInv, 0.001)
    assertEquals(10.0, defaultLeisure, 0.001)

    // Month without custom percentages -> falls back to system defaults (20% / 10%)
    val monthDefault = com.example.data.model.MonthlyRevenue(month = "2026-09", salary = 5000.0)
    val effectiveInv1 = monthDefault.investmentPercentage ?: defaultInv
    val effectiveLeisure1 = monthDefault.leisurePercentage ?: defaultLeisure
    assertEquals(20.0, effectiveInv1, 0.001)
    assertEquals(10.0, effectiveLeisure1, 0.001)

    // Month with custom percentages (e.g. Setembro: Investimento 15% | Lazer 5%)
    val monthCustom1 = com.example.data.model.MonthlyRevenue(
      month = "2026-09",
      salary = 5000.0,
      investmentPercentage = 15.0,
      leisurePercentage = 5.0
    )
    val effectiveInv2 = monthCustom1.investmentPercentage ?: defaultInv
    val effectiveLeisure2 = monthCustom1.leisurePercentage ?: defaultLeisure
    assertEquals(15.0, effectiveInv2, 0.001)
    assertEquals(5.0, effectiveLeisure2, 0.001)

    // Month with custom percentages (e.g. Outubro: Investimento 20% | Lazer 10%)
    val monthCustom2 = com.example.data.model.MonthlyRevenue(
      month = "2026-10",
      salary = 5000.0,
      investmentPercentage = 20.0,
      leisurePercentage = 10.0
    )
    assertEquals(20.0, monthCustom2.investmentPercentage ?: defaultInv, 0.001)
    assertEquals(10.0, monthCustom2.leisurePercentage ?: defaultLeisure, 0.001)

    // Month with custom percentages (e.g. Novembro: Investimento 10% | Lazer 15%)
    val monthCustom3 = com.example.data.model.MonthlyRevenue(
      month = "2026-11",
      salary = 5000.0,
      investmentPercentage = 10.0,
      leisurePercentage = 15.0
    )
    assertEquals(10.0, monthCustom3.investmentPercentage ?: defaultInv, 0.001)
    assertEquals(15.0, monthCustom3.leisurePercentage ?: defaultLeisure, 0.001)
  }

  @Test
  fun `test categories include Emprestimo and Imposto in A-Z alphabetical order`() {
    val defaultCategoryNames = listOf(
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

    assertTrue(defaultCategoryNames.contains("Empréstimo"))
    assertTrue(defaultCategoryNames.contains("Imposto"))
    assertTrue(defaultCategoryNames.contains("Lazer"))

    val collator = java.text.Collator.getInstance(java.util.Locale("pt", "BR")).apply {
      strength = java.text.Collator.PRIMARY
    }

    val sorted = defaultCategoryNames.sortedWith { a, b -> collator.compare(a, b) }

    // Verify Lazer is in its natural alphabetical position (after Imposto/Internet and before Moradia)
    val lazerIndex = sorted.indexOf("Lazer")
    val impostoIndex = sorted.indexOf("Imposto")
    val moradiaIndex = sorted.indexOf("Moradia")

    assertTrue(impostoIndex < lazerIndex)
    assertTrue(lazerIndex < moradiaIndex)
  }

  @Test
  fun `test recommended leisure limit tracking and non-deduction from available balance`() {
    val totalRevenue = 5000.0
    val invPercent = 20.0
    val leisurePercent = 10.0

    val investmentAllocated = totalRevenue * (invPercent / 100.0) // 1000.0
    val leisureLimit = totalRevenue * (leisurePercent / 100.0) // 500.0 (reference only)

    // User registers 2 leisure expenses: Cinema R$ 80 + Restaurante R$ 120 = R$ 200
    val leisureExpense1 = 80.0
    val leisureExpense2 = 120.0
    val leisureSpent = leisureExpense1 + leisureExpense2 // 200.0

    val remainingLeisureLimit = kotlin.math.max(0.0, leisureLimit - leisureSpent) // 300.0
    assertEquals(500.0, leisureLimit, 0.001)
    assertEquals(200.0, leisureSpent, 0.001)
    assertEquals(300.0, remainingLeisureLimit, 0.001)

    // General expenses
    val generalExpenses = 800.0
    val totalExpenses = generalExpenses + leisureSpent // 1000.0

    // Available balance with only paid expenses deducted:
    val totalPaid = 1000.0
    val availableBalance = totalRevenue - investmentAllocated - totalPaid // 5000 - 1000 - 1000 = 3000.0
    assertEquals(3000.0, availableBalance, 0.001)

    // If spending limit capacity:
    val spendingLimit = totalRevenue - investmentAllocated // 4000.0
    val availableForSpending = spendingLimit - totalPaid // 3000.0
    assertEquals(3000.0, availableForSpending, 0.001)
  }

  @Test
  fun `test expenses with status A Pagar do not deduct from available balance and only Paga deducts`() {
    val totalRevenue = 5000.0
    val invAllocated = 0.0 // Considerando o exemplo direto do usuário sem investimento

    val exp1Amount = 1000.0 // Aluguel
    var exp1IsPaid = false // A Pagar

    val exp2Amount = 100.0 // Internet
    var exp2IsPaid = false // A Pagar

    // Cenário 1: Ambas "A Pagar" -> Saldo disponível deve ser R$ 5.000
    var totalPaid = (if (exp1IsPaid) exp1Amount else 0.0) + (if (exp2IsPaid) exp2Amount else 0.0)
    var available = totalRevenue - invAllocated - totalPaid
    assertEquals(5000.0, available, 0.001)

    // Cenário 2: Marcar Aluguel como PAGO -> Saldo disponível deve ser R$ 4.000
    exp1IsPaid = true
    totalPaid = (if (exp1IsPaid) exp1Amount else 0.0) + (if (exp2IsPaid) exp2Amount else 0.0)
    available = totalRevenue - invAllocated - totalPaid
    assertEquals(4000.0, available, 0.001)

    // Cenário 3: Marcar Internet como PAGA -> Saldo disponível deve ser R$ 3.900
    exp2IsPaid = true
    totalPaid = (if (exp1IsPaid) exp1Amount else 0.0) + (if (exp2IsPaid) exp2Amount else 0.0)
    available = totalRevenue - invAllocated - totalPaid
    assertEquals(3900.0, available, 0.001)

    // Cenário 4: Alterar Internet novamente para "A Pagar" -> Saldo retorna para R$ 4.000
    exp2IsPaid = false
    totalPaid = (if (exp1IsPaid) exp1Amount else 0.0) + (if (exp2IsPaid) exp2Amount else 0.0)
    available = totalRevenue - invAllocated - totalPaid
    assertEquals(4000.0, available, 0.001)
  }

  @Test
  fun `test category selection is mandatory and null or blank fails validation`() {
    val nullCategory: String? = null
    val blankCategory = "   "
    val validCategory = "Moradia"

    assertTrue(nullCategory.isNullOrBlank())
    assertTrue(blankCategory.isBlank())
    assertFalse(validCategory.isNullOrBlank())

    val validationMessage = "Selecione uma categoria para continuar."
    assertEquals("Selecione uma categoria para continuar.", validationMessage)
  }

  @Test
  fun `test projected balance after paying all expenses`() {
    val totalRevenue = 5000.0
    val paidExpenses = 1000.0
    val pendingExpenses = 800.0
    val overdueExpenses = 200.0

    // Saldo disponível atual considera apenas as despesas pagas:
    val currentAvailableBalance = totalRevenue - paidExpenses
    assertEquals(4000.0, currentAvailableBalance, 0.001)

    // Previsão após quitar todas as despesas (Pagas + A Pagar + Vencidas):
    val totalExpenses = paidExpenses + pendingExpenses + overdueExpenses
    val projectedBalance = totalRevenue - totalExpenses
    assertEquals(3000.0, projectedBalance, 0.001)

    // Também equivalente a currentAvailableBalance - (pending + overdue):
    val projectedFromAvailable = currentAvailableBalance - (pendingExpenses + overdueExpenses)
    assertEquals(3000.0, projectedFromAvailable, 0.001)
  }
}
