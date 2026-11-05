package com.anaskhan.jantri

import com.anaskhan.jantri.core.AsrJuristic
import com.anaskhan.jantri.core.PrayerComputer
import com.anaskhan.jantri.core.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The engine is validated against a real printed jantri: the Bhopal sheet for Ramadan
 * 1447 (19 Feb to 20 Mar 2026), transcribed row for row.
 *
 * The sheet adds the precaution margins Indian masjids print: sehri ends two minutes
 * before Fajr and iftar comes one minute after sunset. With those two numbers set, the
 * calculated timetable has to agree with every printed row to within a minute. If this
 * test ever fails, the astronomy broke.
 */
class JantriFixtureTest {

    private val bhopal = Settings(
        placeName = "Bhopal",
        lat = 23.2547,
        lng = 77.4029,
        elevationMetres = 0,
        zoneId = "Asia/Kolkata",
        methodId = "karachi",
        asr = AsrJuristic.HANAFI,
        sehriSafetyMin = 2,
        iftarSafetyMin = 1,
    )

    // Ramadan day, Gregorian date 2026, printed sehri, printed iftar (minutes after midnight)
    private val printed: List<Triple<LocalDate, Int, Int>> = listOf(
        Triple(LocalDate.of(2026, 2, 19), 5 * 60 + 34, 18 * 60 + 19),
        Triple(LocalDate.of(2026, 2, 20), 5 * 60 + 33, 18 * 60 + 20),
        Triple(LocalDate.of(2026, 2, 21), 5 * 60 + 32, 18 * 60 + 20),
        Triple(LocalDate.of(2026, 2, 22), 5 * 60 + 30, 18 * 60 + 21),
        Triple(LocalDate.of(2026, 2, 23), 5 * 60 + 31, 18 * 60 + 22),
        Triple(LocalDate.of(2026, 2, 24), 5 * 60 + 30, 18 * 60 + 22),
        Triple(LocalDate.of(2026, 2, 25), 5 * 60 + 30, 18 * 60 + 23),
        Triple(LocalDate.of(2026, 2, 26), 5 * 60 + 29, 18 * 60 + 23),
        Triple(LocalDate.of(2026, 2, 27), 5 * 60 + 28, 18 * 60 + 24),
        Triple(LocalDate.of(2026, 2, 28), 5 * 60 + 27, 18 * 60 + 24),
        Triple(LocalDate.of(2026, 3, 1), 5 * 60 + 26, 18 * 60 + 24),
        Triple(LocalDate.of(2026, 3, 2), 5 * 60 + 26, 18 * 60 + 25),
        Triple(LocalDate.of(2026, 3, 3), 5 * 60 + 25, 18 * 60 + 25),
        Triple(LocalDate.of(2026, 3, 4), 5 * 60 + 24, 18 * 60 + 26),
        Triple(LocalDate.of(2026, 3, 5), 5 * 60 + 23, 18 * 60 + 26),
        Triple(LocalDate.of(2026, 3, 6), 5 * 60 + 22, 18 * 60 + 27),
        Triple(LocalDate.of(2026, 3, 7), 5 * 60 + 21, 18 * 60 + 27),
        Triple(LocalDate.of(2026, 3, 8), 5 * 60 + 20, 18 * 60 + 28),
        Triple(LocalDate.of(2026, 3, 9), 5 * 60 + 19, 18 * 60 + 28),
        Triple(LocalDate.of(2026, 3, 10), 5 * 60 + 19, 18 * 60 + 28),
        Triple(LocalDate.of(2026, 3, 11), 5 * 60 + 18, 18 * 60 + 29),
        Triple(LocalDate.of(2026, 3, 12), 5 * 60 + 17, 18 * 60 + 29),
        Triple(LocalDate.of(2026, 3, 13), 5 * 60 + 16, 18 * 60 + 30),
        Triple(LocalDate.of(2026, 3, 14), 5 * 60 + 15, 18 * 60 + 30),
        Triple(LocalDate.of(2026, 3, 15), 5 * 60 + 14, 18 * 60 + 30),
        Triple(LocalDate.of(2026, 3, 16), 5 * 60 + 13, 18 * 60 + 31),
        Triple(LocalDate.of(2026, 3, 17), 5 * 60 + 12, 18 * 60 + 31),
        Triple(LocalDate.of(2026, 3, 18), 5 * 60 + 11, 18 * 60 + 32),
        Triple(LocalDate.of(2026, 3, 19), 5 * 60 + 10, 18 * 60 + 32),
        Triple(LocalDate.of(2026, 3, 20), 5 * 60 + 9, 18 * 60 + 32),
    )

    @Test
    fun reproducesEveryPrintedRow() {
        var checked = 0
        for ((date, sehri, iftar) in printed) {
            val t = PrayerComputer.compute(date, bhopal)
            val calcSehri = PrayerComputer.sehriEnd(t, bhopal)
            val calcIftar = PrayerComputer.iftar(t, bhopal)
            assertEquals("sehri on $date", sehri.toDouble(), calcSehri.toDouble(), 1.0)
            assertEquals("iftar on $date", iftar.toDouble(), calcIftar.toDouble(), 1.0)
            checked += 2
        }
        assertEquals(60, checked)
    }

    @Test
    fun sehriFallsBeforeSunriseAndIftarAfterIt() {
        val t = PrayerComputer.compute(LocalDate.of(2026, 3, 1), bhopal)
        assertTrue(t.fajr < t.sunrise)
        assertTrue(t.sunrise < t.dhuhr)
        assertTrue(t.dhuhr < t.asr)
        assertTrue(t.asr < t.maghrib)
        assertTrue(t.maghrib < t.isha)
    }

    @Test
    fun hanafiAsrComesLaterThanShafii() {
        val shafi = PrayerComputer.compute(LocalDate.of(2026, 3, 1), bhopal.copy(asr = AsrJuristic.SHAFI))
        val hanafi = PrayerComputer.compute(LocalDate.of(2026, 3, 1), bhopal.copy(asr = AsrJuristic.HANAFI))
        assertTrue("Hanafi Asr should be later", hanafi.asr > shafi.asr)
        assertTrue("difference should be tens of minutes", hanafi.asr - shafi.asr in 30..120)
    }

    @Test
    fun widerTwilightAngleGivesEarlierSehri() {
        val eighteen = PrayerComputer.compute(LocalDate.of(2026, 3, 1), bhopal)
        val fifteen = PrayerComputer.compute(LocalDate.of(2026, 3, 1), bhopal.copy(methodId = "isna"))
        assertTrue("15 degrees must give a later sehri than 18 degrees", fifteen.fajr > eighteen.fajr)
        assertTrue("difference should be minutes, not seconds", fifteen.fajr - eighteen.fajr >= 8)
    }

    @Test
    fun offsetsMoveTheClockExactly() {
        val base = PrayerComputer.compute(LocalDate.of(2026, 3, 1), bhopal)
        val shiftedSettings = bhopal.copy(sehriSafetyMin = 5, iftarSafetyMin = 4)
        val shifted = PrayerComputer.compute(LocalDate.of(2026, 3, 1), shiftedSettings)
        assertEquals(PrayerComputer.sehriEnd(base, bhopal) - 3, PrayerComputer.sehriEnd(shifted, shiftedSettings))
        assertEquals(PrayerComputer.iftar(base, bhopal) + 3, PrayerComputer.iftar(shifted, shiftedSettings))
    }
}
