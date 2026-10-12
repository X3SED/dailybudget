package com.dailybudget.app

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Montants stockés en centimes pour éviter les erreurs d'arrondi. */
enum class EntryType { BALANCE, EXPENSE, INCOME }

data class Entry(
    val id: Long,
    val type: EntryType,
    val cents: Long,
    val timestamp: Long,
    val note: String = "",
    /** Clé de catégorie facultative (voir ui.Category). */
    val category: String? = null,
    /**
     * Dépense fixe/exceptionnelle (loyer, factures, virement…) : elle réduit le solde mais n'est pas
     * comptée dans ce qui a été dépensé aujourd'hui. Pour un BALANCE, s'applique à la baisse constatée.
     */
    val excluded: Boolean = false,
)

data class Settings(
    /** Jour du mois où le salaire tombe (1–31, ramené au dernier jour si le mois est plus court). */
    val payday: Int = 1,
    /** Somme à garder de côté, exclue du budget journalier. */
    val reserveCents: Long = 0,
    /** Code ISO 4217 (MAD, EUR, USD…). */
    val currency: String = "EUR",
)

data class Summary(
    val balanceCents: Long,
    val nextPayday: LocalDate,
    /** Jours restants avant le salaire, aujourd'hui inclus. */
    val daysLeft: Int,
    /** Budget journalier fixé à partir de l'argent disponible en début de journée. */
    val dailyCents: Long,
    val spentTodayCents: Long,
    /** Dépenses fixes du jour, retirées du solde sans compter dans le budget du jour. */
    val excludedTodayCents: Long,
    val remainingTodayCents: Long,
    /** Budget par jour à partir de demain si rien d'autre n'est dépensé aujourd'hui (null la veille du salaire). */
    val tomorrowDailyCents: Long?,
)

object Budget {

    fun paydayIn(month: YearMonth, payday: Int): LocalDate =
        month.atDay(payday.coerceIn(1, month.lengthOfMonth()))

    /** Prochain jour de salaire strictement après [today] : le jour de paie commence une nouvelle période. */
    fun nextPayday(today: LocalDate, payday: Int): LocalDate {
        val thisMonth = paydayIn(YearMonth.from(today), payday)
        return if (thisMonth.isAfter(today)) thisMonth else paydayIn(YearMonth.from(today).plusMonths(1), payday)
    }

    fun daysLeft(today: LocalDate, payday: Int): Int =
        ChronoUnit.DAYS.between(today, nextPayday(today, payday)).toInt()

    fun dateOf(entry: Entry, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalDate()

    /** Retourne null tant qu'aucun solde n'a été saisi. */
    fun summarize(
        entries: List<Entry>,
        settings: Settings,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Summary? {
        val sorted = entries.sortedBy { it.timestamp }

        // Solde à la fin d'hier.
        var balance: Long? = null
        val todays = mutableListOf<Entry>()
        for (e in sorted) {
            if (dateOf(e, zone).isBefore(today)) balance = apply(balance, e) else todays += e
        }

        // available = argent disponible en début de journée + rentrées du jour.
        // L'invariant available - balance = dépensé aujourd'hui est maintenu à chaque étape.
        // Une dépense exclue réduit available autant que le solde : elle ne compte donc pas comme dépensée.
        var available: Long? = balance
        var excluded = 0L
        for (e in todays) {
            when (e.type) {
                EntryType.BALANCE -> {
                    val current = balance
                    if (current == null) available = e.cents
                    else if (e.cents > current) available = (available ?: 0) + (e.cents - current)
                    else if (e.excluded) {
                        available = (available ?: 0) - (current - e.cents)
                        excluded += current - e.cents
                    }
                    balance = e.cents
                }
                EntryType.EXPENSE -> {
                    if (available == null) available = 0
                    if (e.excluded) {
                        available -= e.cents
                        excluded += e.cents
                    }
                    balance = (balance ?: 0) - e.cents
                }
                EntryType.INCOME -> {
                    available = (available ?: 0) + e.cents
                    balance = (balance ?: 0) + e.cents
                }
            }
        }
        val current = balance ?: return null
        val start = available ?: current

        val days = daysLeft(today, settings.payday)
        val daily = (start - settings.reserveCents).coerceAtLeast(0) / days
        val spent = start - current
        val tomorrow = if (days > 1) (current - settings.reserveCents).coerceAtLeast(0) / (days - 1) else null

        return Summary(
            balanceCents = current,
            nextPayday = nextPayday(today, settings.payday),
            daysLeft = days,
            dailyCents = daily,
            spentTodayCents = spent,
            excludedTodayCents = excluded,
            remainingTodayCents = daily - spent,
            tomorrowDailyCents = tomorrow,
        )
    }

    private fun apply(balance: Long?, e: Entry): Long = when (e.type) {
        EntryType.BALANCE -> e.cents
        EntryType.EXPENSE -> (balance ?: 0) - e.cents
        EntryType.INCOME -> (balance ?: 0) + e.cents
    }

    /**
     * Accepte "12", "12,5", "12.50", "1 234,56", "1,234.50" (et "-50" si [allowNegative]) ; null si invalide.
     * Espaces et symboles monétaires sont ignorés ; si "," et "." sont présents, le dernier est décimal.
     */
    fun parseCents(input: String, allowNegative: Boolean = false): Long? {
        val stripped = input.trim().replace(Regex("^[A-Za-z]{3}|[A-Za-z]{3}$|[€$£¥]"), "")
        if (stripped.any { it.isLetter() }) return null
        var cleaned = stripped.filter { it.isDigit() || it in ",.-−" }.replace('−', '-')
        if (cleaned.contains(',') && cleaned.contains('.')) {
            val last = maxOf(cleaned.lastIndexOf(','), cleaned.lastIndexOf('.'))
            cleaned = cleaned.substring(0, last).filter { it != ',' && it != '.' } + "." + cleaned.substring(last + 1)
        }
        cleaned = cleaned.replace(',', '.')
        if (cleaned.isEmpty() || cleaned.count { it == '.' } > 1) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if ((value.signum() < 0 && !allowNegative) || value.scale() > 2) return null
        return value.movePointRight(2).toLong()
    }
}
