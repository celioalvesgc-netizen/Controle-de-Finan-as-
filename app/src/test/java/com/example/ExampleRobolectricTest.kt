package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Expense
import com.example.data.model.PaymentStatus
import com.example.util.CurrencyUtils
import org.junit.Assert.assertEquals
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
    val defaultSettings = com.example.data.model.AppSettings(
      investmentPercentage = 10.0,
      leisurePercentage = 10.0
    )

    // Month without custom percentages -> falls back to defaults
    val monthDefault = com.example.data.model.MonthlyRevenue(month = "2026-09", salary = 5000.0)
    val effectiveInv1 = monthDefault.investmentPercentage ?: defaultSettings.investmentPercentage
    val effectiveLeisure1 = monthDefault.leisurePercentage ?: defaultSettings.leisurePercentage
    assertEquals(10.0, effectiveInv1, 0.001)
    assertEquals(10.0, effectiveLeisure1, 0.001)

    // Month with custom percentages (e.g. Outubro: Investimento 20% | Lazer 10%)
    val monthCustom = com.example.data.model.MonthlyRevenue(
      month = "2026-10",
      salary = 5000.0,
      investmentPercentage = 20.0,
      leisurePercentage = 5.0
    )
    val effectiveInv2 = monthCustom.investmentPercentage ?: defaultSettings.investmentPercentage
    val effectiveLeisure2 = monthCustom.leisurePercentage ?: defaultSettings.leisurePercentage
    assertEquals(20.0, effectiveInv2, 0.001)
    assertEquals(5.0, effectiveLeisure2, 0.001)
  }
}
