package com.anaskhan.jantri

import com.anaskhan.jantri.core.Ramadan
import com.anaskhan.jantri.core.Settings
import com.anaskhan.jantri.notify.Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RamadanAndScheduleTest {

    @Test
    fun calculatedRamadanStartMatchesUmmAlQura() {
        // Umm al-Qura puts 1 Ramadan 1447 on 18 February 2026.
        assertEquals(LocalDate.of(2026, 2, 18), Ramadan.calculatedStart(1447))
        assertEquals(LocalDate.of(2027, 2, 8), Ramadan.calculatedStart(1448))
    }

    @Test
    fun sightingShiftOfOneDayIsHonoured() {
        val settings = Settings()
        val today = LocalDate.of(2026, 2, 19)
        // Calculated month starts a day before India actually began.
        val calculated = Ramadan.monthOf(today, settings)
        assertEquals(LocalDate.of(2026, 2, 18), calculated.start)
        assertEquals(2, Ramadan.dayOf(today, calculated.start))

        val sighted = settings.copy(ramadanStartOverride = "2026-02-19")
        val month = Ramadan.monthOf(today, sighted)
        assertEquals(1, Ramadan.dayOf(today, month.start))
        assertEquals(14, Ramadan.dayOf(LocalDate.of(2026, 3, 4), month.start))
    }

    @Test
    fun dateOutsideRamadanHasNoDay() {
        val start = LocalDate.of(2026, 2, 19)
        assertEquals(null, Ramadan.dayOf(LocalDate.of(2026, 1, 10), start))
        assertEquals(null, Ramadan.dayOf(LocalDate.of(2026, 3, 25), start))
        assertEquals(30, Ramadan.dayOf(LocalDate.of(2026, 3, 20), start))
    }

    @Test
    fun planArmsFourAlertsADayInOrder() {
        val settings = Settings(ramadanStartOverride = "2026-02-19")
        val today = LocalDate.of(2026, 3, 1)
        val now = LocalDateTime.of(2026, 3, 1, 0, 1)
        val planned = Scheduler.plan(settings, today, now)

        assertTrue("45 days of alerts expected", planned.size >= 45 * 4 - 2)
        val firstDay = planned.filter { it.at.toLocalDate() == today }
        assertEquals(4, firstDay.size)
        assertEquals(Scheduler.KIND_SEHRI_LEAD, firstDay[0].kind)
        assertEquals(Scheduler.KIND_SEHRI_END, firstDay[1].kind)
        assertEquals(Scheduler.KIND_IFTAR_LEAD, firstDay[2].kind)
        assertEquals(Scheduler.KIND_IFTAR, firstDay[3].kind)
        for (i in 1 until firstDay.size) {
            assertTrue(firstDay[i].at.isAfter(firstDay[i - 1].at))
        }
        // Distinct request codes so alarms never overwrite each other.
        assertEquals(planned.size, planned.map { it.requestCode }.distinct().size)
        // Nothing in the past.
        assertTrue(planned.all { it.at.isAfter(now) })
    }

    @Test
    fun alertsAreSkippedWhenTurnedOff() {
        val settings = Settings(
            notifySehriLeadEnabled = false,
            notifyIftarLeadEnabled = false,
            notifySehriEndEnabled = true,
            notifyIftarEnabled = true,
        )
        val planned = Scheduler.plan(settings, LocalDate.of(2026, 3, 1), LocalDateTime.of(2026, 3, 1, 0, 1))
        val firstDay = planned.filter { it.at.toLocalDate() == LocalDate.of(2026, 3, 1) }
        assertEquals(2, firstDay.size)
        assertEquals(
            listOf(Scheduler.KIND_SEHRI_END, Scheduler.KIND_IFTAR),
            firstDay.map { it.kind },
        )
        assertNotNull(planned)
    }
}
