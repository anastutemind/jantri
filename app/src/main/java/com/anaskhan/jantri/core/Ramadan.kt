package com.anaskhan.jantri.core

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * Ramadan day numbering.
 *
 * The Islamic month starts with the local sighting of the crescent, not with an
 * arithmetic calendar, so a city can begin one or two days before or after what any
 * calculated Hijri date says. Printed jantris are set weeks in advance and cannot
 * follow the sighting; this app can. Day 1 is therefore an explicit date the user
 * controls, defaulted from the Umm al-Qura Hijri calendar.
 */
object Ramadan {

    /** Hijri date as a LocalDate, using the Umm al-Qura calendar that ships with java.time. */
    fun hijriOf(date: LocalDate): HijrahDate = HijrahDate.from(date)

    fun hijriYearOf(date: LocalDate): Int = hijriOf(date).get(ChronoField.YEAR)

    /** Gregorian date of 1 Ramadan for a given Hijri year (calculated, not sighted). */
    fun calculatedStart(hijriYear: Int): LocalDate = LocalDate.from(HijrahDate.of(hijriYear, 9, 1))

    /**
     * The Ramadan day 1 that applies on [today]: the start of the Ramadan window that
     * contains today, otherwise the next one coming up.
     */
    fun startFor(today: LocalDate): LocalDate {
        val y = hijriYearOf(today)
        val candidates = listOf(y - 1, y, y + 1).map { calculatedStart(it) }
        candidates.firstOrNull { start ->
            !today.isBefore(start) && today.isBefore(start.plusDays(30))
        }?.let { return it }
        return candidates.first { it.isAfter(today) }
    }

    /** Day of Ramadan (1..30), or null when [date] is outside the month. */
    fun dayOf(date: LocalDate, start: LocalDate): Int? {
        val idx = java.time.temporal.ChronoUnit.DAYS.between(start, date).toInt()
        return if (idx in 0..29) idx + 1 else null
    }

    data class Month(val start: LocalDate, val days: Int, val year: Int)

    fun monthOf(today: LocalDate, settings: Settings): Month {
        val override = settings.ramadanStartOverride?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val start = override ?: startFor(today)
        // A calculated month is 30 days; once the user fixes a real start date we keep 30
        // as the ceiling and let Eid end it from the sighting screen.
        return Month(start, 30, hijriYearOf(start))
    }

    fun hijriLabel(date: LocalDate, offsetDays: Int = 0): String {
        val d = hijriOf(date.plusDays(offsetDays.toLong()))
        val months = listOf(
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal",
            "Jumada al-Thani", "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhul Qadah", "Dhul Hijjah",
        )
        val day = d.get(ChronoField.DAY_OF_MONTH)
        val month = months[d.get(ChronoField.MONTH_OF_YEAR) - 1]
        val year = d.get(ChronoField.YEAR)
        return "$day $month $year AH"
    }
}
