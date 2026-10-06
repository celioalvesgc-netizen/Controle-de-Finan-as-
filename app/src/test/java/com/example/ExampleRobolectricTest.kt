package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ExampleRobolectricTest {

    @Test
    fun `test launch MainActivity`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assert(activity != null)
    }

    @Test
    fun `test multiple revenues summation`() {
        val revenues = listOf(3500.0, 500.0, 250.0)
        val total = revenues.sum()
        assertEquals(4250.0, total, 0.001)
    }

    @Test
    fun `test category limit and available balance calculations`() {
        val totalRevenue = 5000.0
        val investPercent = 15.0
        val catLimitPercent = 10.0

        val investAllocated = (totalRevenue * investPercent) / 100.0
        val catLimitAmount = (totalRevenue * catLimitPercent) / 100.0

        assertEquals(750.0, investAllocated, 0.001)
        assertEquals(500.0, catLimitAmount, 0.001)

        val paidExpenses = 1200.0
        val availableMonth = totalRevenue - investAllocated - paidExpenses
        assertEquals(3050.0, availableMonth, 0.001)
    }

    @Test
    fun `test installments calculation`() {
        val totalPurchase = 1200.0
        val installmentsCount = 10
        val installmentValue = totalPurchase / installmentsCount
        assertEquals(120.0, installmentValue, 0.001)
    }
}
