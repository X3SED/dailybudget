package com.dailybudget.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class BudgetTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 12)

    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()
    private var nextId = 0L
    private fun entry(type: EntryType, euros: Int, date: LocalDate = today, hour: Int = 10) =
        Entry(nextId++, type, euros * 100L, at(date, hour))

    @Test
    fun nextPaydayAndDaysLeft() {
        assertEquals(LocalDate.of(2026, 11, 1), Budget.nextPayday(today, 1))
        assertEquals(20, Budget.daysLeft(today, 1))
        assertEquals(LocalDate.of(2026, 10, 25), Budget.nextPayday(today, 25))
        // Le jour de paie démarre une nouvelle période d'un mois.
        assertEquals(LocalDate.of(2026, 11, 12), Budget.nextPayday(today, 12))
        // Jour 31 ramené au dernier jour d'un mois court.
        assertEquals(LocalDate.of(2027, 2, 28), Budget.nextPayday(LocalDate.of(2027, 2, 1), 31))
        assertEquals(1, Budget.daysLeft(LocalDate.of(2026, 10, 31), 1))
    }

    @Test
    fun noEntriesGivesNoSummary() {
        assertNull(Budget.summarize(emptyList(), Settings(), today, zone))
    }

    @Test
    fun balanceOnlyDividesByDaysLeft() {
        val s = Budget.summarize(listOf(entry(EntryType.BALANCE, 1000)), Settings(payday = 1), today, zone)!!
        assertEquals(100000L, s.balanceCents)
        assertEquals(20, s.daysLeft)
        assertEquals(5000L, s.dailyCents)
        assertEquals(0L, s.spentTodayCents)
        assertEquals(5000L, s.remainingTodayCents)
    }

    @Test
    fun spendingTodayKeepsDailyBudgetStable() {
        val yesterday = today.minusDays(1)
        val entries = listOf(
            entry(EntryType.BALANCE, 1000, yesterday),
            entry(EntryType.EXPENSE, 20, today, 9),
            entry(EntryType.BALANCE, 970, today, 18), // 10 € de plus dépensés, constatés via le solde
        )
        val s = Budget.summarize(entries, Settings(payday = 1), today, zone)!!
        assertEquals(5000L, s.dailyCents)
        assertEquals(3000L, s.spentTodayCents)
        assertEquals(2000L, s.remainingTodayCents)
        assertEquals(97000L / 19, s.tomorrowDailyCents)
    }

    @Test
    fun incomeAndBalanceIncreaseRaiseTodaysBudget() {
        val entries = listOf(
            entry(EntryType.BALANCE, 100, today.minusDays(3)),
            entry(EntryType.INCOME, 1900, today, 8),
            entry(EntryType.EXPENSE, 40, today, 12),
        )
        val s = Budget.summarize(entries, Settings(payday = 1), today, zone)!!
        assertEquals(200000L / 20, s.dailyCents)
        assertEquals(4000L, s.spentTodayCents)

        val viaBalance = listOf(
            entry(EntryType.BALANCE, 100, today.minusDays(3)),
            entry(EntryType.BALANCE, 2000, today, 8),
        )
        assertEquals(200000L / 20, Budget.summarize(viaBalance, Settings(payday = 1), today, zone)!!.dailyCents)
    }

    @Test
    fun excludedExpensesDoNotCountAsSpentToday() {
        val yesterday = today.minusDays(1)
        val rent = Entry(nextId++, EntryType.EXPENSE, 400000, at(today, 9), category = "rent", excluded = true)
        val entries = listOf(
            entry(EntryType.BALANCE, 10000, yesterday),
            rent,
            entry(EntryType.EXPENSE, 50, today, 12),
        )
        val s = Budget.summarize(entries, Settings(payday = 1), today, zone)!!
        assertEquals(595000L, s.balanceCents)
        assertEquals(600000L / 20, s.dailyCents)
        assertEquals(5000L, s.spentTodayCents)
        assertEquals(400000L, s.excludedTodayCents)
        assertEquals(30000L - 5000L, s.remainingTodayCents)
    }

    @Test
    fun excludedBalanceDropIsNotCountedAsSpent() {
        val yesterday = today.minusDays(1)
        val drop = Entry(nextId++, EntryType.BALANCE, 600000, at(today, 9), excluded = true)
        val entries = listOf(entry(EntryType.BALANCE, 10000, yesterday), drop, entry(EntryType.BALANCE, 5950, today, 18))
        val s = Budget.summarize(entries, Settings(payday = 1), today, zone)!!
        assertEquals(600000L / 20, s.dailyCents)
        assertEquals(5000L, s.spentTodayCents)
        assertEquals(400000L, s.excludedTodayCents)
    }

    @Test
    fun reserveIsExcluded() {
        val s = Budget.summarize(
            listOf(entry(EntryType.BALANCE, 1000)),
            Settings(payday = 1, reserveCents = 20000),
            today, zone,
        )!!
        assertEquals(80000L / 20, s.dailyCents)
    }

    @Test
    fun parseCents() {
        assertEquals(1250L, Budget.parseCents("12,5"))
        assertEquals(1250L, Budget.parseCents("12.50"))
        assertEquals(123456L, Budget.parseCents("1 234,56 €"))
        assertNull(Budget.parseCents("abc"))
        assertNull(Budget.parseCents("1,234"))
        assertNull(Budget.parseCents(""))
        assertNull(Budget.parseCents("-50"))
        assertNull(Budget.parseCents("1x2"))
        assertEquals(123450L, Budget.parseCents("1,234.50"))
        assertEquals(123450L, Budget.parseCents("1.234,50 MAD"))
        assertEquals(5000L, Budget.parseCents("MAD 50"))
        assertEquals(-5050L, Budget.parseCents("-50,5", allowNegative = true))
    }
}
